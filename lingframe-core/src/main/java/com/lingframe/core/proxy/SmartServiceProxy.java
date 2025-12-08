package com.lingframe.core.proxy;

import com.lingframe.api.exception.PermissionDeniedException;
import com.lingframe.api.security.AccessType;
import com.lingframe.api.security.PermissionService;
import com.lingframe.core.plugin.PluginInstance;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 智能动态代理：动态路由 + TCCL 劫持
 * 负责在运行时将流量路由到最新的 PluginInstance
 */
public class SmartServiceProxy implements InvocationHandler {

    private final String callerPluginId; // 谁在调用？
    private final AtomicReference<PluginInstance> activeInstanceRef;
    private final Class<?> serviceInterface;
    private final PermissionService permissionService; // 鉴权服务

    public SmartServiceProxy(String callerPluginId,
                             AtomicReference<PluginInstance> activeInstanceRef,
                             Class<?> serviceInterface,
                             PermissionService permissionService) {
        this.callerPluginId = callerPluginId;
        this.activeInstanceRef = activeInstanceRef;
        this.serviceInterface = serviceInterface;
        this.permissionService = permissionService;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 1. 【新增】权限检查 (Zero Trust)
        // Capability = 接口全限定名
        String capability = serviceInterface.getName();
        if (!permissionService.isAllowed(callerPluginId, capability, AccessType.EXECUTE)) {
            permissionService.audit(callerPluginId, capability, method.getName(), false);
            throw new PermissionDeniedException("Access Denied: Plugin [" + callerPluginId + "] cannot access [" + capability + "]");
        }

        // 2. 获取活跃实例
        PluginInstance instance = activeInstanceRef.get();
        if (instance == null || !instance.getContainer().isActive()) {
            throw new IllegalStateException("Service unavailable: " + capability);
        }

        instance.enter();

        // 3. TCCL 劫持
        Thread currentThread = Thread.currentThread();
        ClassLoader originalClassLoader = currentThread.getContextClassLoader();
        currentThread.setContextClassLoader(instance.getContainer().getClassLoader());

        try {
            Object realBean = instance.getContainer().getBean(serviceInterface);
            if (realBean == null) throw new IllegalStateException("Implementation not found");

            // 4. 审计成功调用 (可选，生产环境需采样)
            // permissionService.audit(callerPluginId, capability, method.getName(), true);

            return method.invoke(realBean, args);
        } finally {
            currentThread.setContextClassLoader(originalClassLoader);
            instance.exit();
        }
    }
}