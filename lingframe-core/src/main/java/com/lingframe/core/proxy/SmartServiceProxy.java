package com.lingframe.core.proxy;

import com.lingframe.api.context.PluginContextHolder;
import com.lingframe.api.security.AccessType;
import com.lingframe.core.governance.GovernanceArbitrator;
import com.lingframe.core.kernel.GovernanceKernel;
import com.lingframe.core.kernel.InvocationContext;
import com.lingframe.core.plugin.PluginInstance;
import com.lingframe.core.plugin.PluginSlot;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 智能动态代理
 * 特性：元数据缓存 + ThreadLocal 上下文复用 + 零GC开销（除第一次）
 */
@Slf4j
public class SmartServiceProxy implements InvocationHandler {

    private final String callerPluginId; // 谁在调用
    private final PluginSlot targetSlot; // 核心锚点
    private final Class<?> serviceInterface;
    private final GovernanceKernel governanceKernel;// 内核
    private final GovernanceArbitrator governanceArbitrator; // 治理仲裁器

    // ================= 性能优化：ThreadLocal 对象池 =================
    // 在同一线程内复用 InvocationContext，避免每次 new 造成的 GC 压力
    private static final ThreadLocal<InvocationContext> CTX_POOL = ThreadLocal.withInitial(() -> null);

    // 缓存静态元数据 (如 ResourceId)，不再缓存动态权限
    private static final ConcurrentHashMap<Method, String> RESOURCE_ID_CACHE = new ConcurrentHashMap<>();

    public SmartServiceProxy(String callerPluginId,
                             PluginSlot targetSlot, // 核心锚点,
                             Class<?> serviceInterface,
                             GovernanceKernel governanceKernel,
                             GovernanceArbitrator governanceArbitrator) {
        this.callerPluginId = callerPluginId;
        this.targetSlot = targetSlot;
        this.serviceInterface = serviceInterface;
        this.governanceKernel = governanceKernel;
        this.governanceArbitrator = governanceArbitrator;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class) return method.invoke(this, args);

        // 从 ThreadLocal 获取/复用 InvocationContext
        InvocationContext ctx = CTX_POOL.get();
        if (ctx == null) {
            // 第一次使用，创建新对象并存入 ThreadLocal
            ctx = InvocationContext.builder().build();
            CTX_POOL.set(ctx);
        }

        try {
            // 【关键】重置/填充上下文属性 (利用 @Data 生成的 setter)
            // Identity
            ctx.setTraceId(null); // 由 Kernel 处理
            ctx.setCallerPluginId(this.callerPluginId);
            ctx.setPluginId(targetSlot.getPluginId());
            ctx.setOperation(method.getName());
            // Runtime Data (每次请求必变)
            ctx.setArgs(args);
            // Resource
            ctx.setResourceType("RPC");
            // Labels
            Map<String, String> labels = PluginContextHolder.getLabels();
            ctx.setLabels(labels != null ? labels : Collections.emptyMap());

            String resourceId = RESOURCE_ID_CACHE.computeIfAbsent(method,
                    m -> serviceInterface.getName() + ":" + m.getName());
            ctx.setResourceId(resourceId);

            // 🔥【核心升级】动态治理仲裁
            // 必须在 Context 填充了 Labels 之后调用，以便 Arbitrator 选择正确的实例版本
            String permission = governanceArbitrator.resolvePermission(targetSlot, method, ctx);
            boolean audit = governanceArbitrator.shouldAudit(targetSlot, method, ctx);

            ctx.setRequiredPermission(permission);
            ctx.setShouldAudit(audit);
            ctx.setAccessType(AccessType.EXECUTE); // 简化处理
            ctx.setAuditAction(resourceId);

            // 清理上一次请求可能遗留的 metadata
            ctx.setMetadata(null);

            // 4. 委托内核执行
            InvocationContext finalCtx = ctx;
            return governanceKernel.invoke(ctx, () -> {
                PluginInstance instance = targetSlot.selectInstance(finalCtx);
                if (instance == null) throw new IllegalStateException("Service unavailable");

                instance.enter();
                PluginContextHolder.set(this.callerPluginId);
                Thread t = Thread.currentThread();
                ClassLoader oldCL = t.getContextClassLoader();
                t.setContextClassLoader(instance.getContainer().getClassLoader());
                try {
                    Object bean = instance.getContainer().getBean(serviceInterface);
                    try {
                        return method.invoke(bean, args);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                } finally {
                    t.setContextClassLoader(oldCL);
                    PluginContextHolder.clear();
                    instance.exit();
                }
            });
        } finally {
            // 5. 【核心】清理大对象引用，防止内存泄漏
            // args 可能很大（如上传文件），labels 可能有脏数据，必须清空
            // 注意：这里不要 remove()，目的是为了复用 ctx 对象本身
            ctx.setArgs(null);
            ctx.setLabels(null);
            ctx.setMetadata(null);
            // TraceId 不需要清空，会被下一次 setTraceId 覆盖
        }
    }

}