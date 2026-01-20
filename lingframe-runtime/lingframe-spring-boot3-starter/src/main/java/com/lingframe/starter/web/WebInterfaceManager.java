package com.lingframe.starter.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Web 接口动态管理器（原生注册版）
 * 职责：
 * 1. 将插件 Controller 方法直接注册到宿主 Spring MVC
 * 2. 维护 HandlerMethod -> Metadata 映射，供 Interceptor 查询
 * 3. 插件卸载时彻底清理路由，防止内存泄漏
 */
@Slf4j
public class WebInterfaceManager {

    // HandlerMethod 标识 -> 元数据映射
    private final Map<String, WebInterfaceMetadata> metadataMap = new ConcurrentHashMap<>();

    // 路由键 -> RequestMappingInfo 映射（用于卸载）
    private final Map<String, RequestMappingInfo> mappingInfoMap = new ConcurrentHashMap<>();

    private RequestMappingHandlerMapping hostMapping;

    /**
     * 初始化方法，由 AutoConfiguration 调用
     */
    public void init(RequestMappingHandlerMapping mapping) {
        this.hostMapping = mapping;
        log.info("🌍 [LingFrame Web] WebInterfaceManager initialized with native registration");
    }

    /**
     * 注册插件 Controller 方法到 Spring MVC
     */
    public void register(WebInterfaceMetadata metadata) {
        if (hostMapping == null) {
            log.warn("WebInterfaceManager not initialized, skipping registration: {}", metadata.getUrlPattern());
            return;
        }

        String routeKey = buildRouteKey(metadata);

        // 检查路由冲突
        if (metadataMap.containsKey(routeKey)) {
            log.warn("⚠️ [LingFrame Web] Route conflict detected, overwriting: {} [{}]",
                    metadata.getHttpMethod(), metadata.getUrlPattern());
        }

        try {
            // 构建 RequestMappingInfo
            RequestMappingInfo info = RequestMappingInfo
                    .paths(metadata.getUrlPattern())
                    .methods(RequestMethod.valueOf(metadata.getHttpMethod()))
                    .build();

            // 直接注册插件 Controller Bean 和 Method 到 Spring MVC
            hostMapping.registerMapping(info, metadata.getTargetBean(), metadata.getTargetMethod());

            // 存储映射关系
            metadataMap.put(routeKey, metadata);
            mappingInfoMap.put(routeKey, info);

            log.info("🌍 [LingFrame Web] Registered: {} {} -> {}.{}",
                    metadata.getHttpMethod(), metadata.getUrlPattern(),
                    metadata.getPluginId(), metadata.getTargetMethod().getName());
        } catch (Exception e) {
            log.error("Failed to register web mapping: {} {}", metadata.getHttpMethod(), metadata.getUrlPattern(), e);
        }
    }

    /**
     * 注销插件的所有接口
     */
    public void unregister(String pluginId) {
        if (hostMapping == null)
            return;

        log.info("♻️ [LingFrame Web] Unregistering interfaces for plugin: {}", pluginId);

        List<String> keysToRemove = new ArrayList<>();

        metadataMap.forEach((key, meta) -> {
            if (meta.getPluginId().equals(pluginId)) {
                keysToRemove.add(key);

                // 从 Spring MVC 注销
                RequestMappingInfo info = mappingInfoMap.get(key);
                if (info != null) {
                    try {
                        hostMapping.unregisterMapping(info);
                        log.debug("Unregistered mapping: {}", key);
                    } catch (Exception e) {
                        log.warn("Failed to unregister mapping: {}", key, e);
                    }
                }
            }
        });

        // 清理本地缓存
        for (String key : keysToRemove) {
            metadataMap.remove(key);
            mappingInfoMap.remove(key);
        }

        log.info("♻️ [LingFrame Web] Unregistered {} interfaces for plugin: {}", keysToRemove.size(), pluginId);
    }

    /**
     * 根据 HandlerMethod 获取元数据
     * 供 LingWebGovernanceInterceptor 调用
     */
    public WebInterfaceMetadata getMetadata(HandlerMethod handlerMethod) {
        // 通过 Bean 和 Method 构建查找键
        Object bean = handlerMethod.getBean();
        Method method = handlerMethod.getMethod();

        // 遍历查找匹配的元数据
        for (WebInterfaceMetadata meta : metadataMap.values()) {
            if (isSameHandler(meta, bean, method)) {
                return meta;
            }
        }
        return null;
    }

    /**
     * 判断是否是同一个处理器
     */
    private boolean isSameHandler(WebInterfaceMetadata meta, Object bean, Method method) {
        // 比较 Bean 实例和方法签名
        if (meta.getTargetBean() == bean) {
            return meta.getTargetMethod().equals(method);
        }
        // 处理代理情况：比较方法名和参数类型
        if (meta.getTargetMethod().getName().equals(method.getName())) {
            Class<?>[] metaParams = meta.getTargetMethod().getParameterTypes();
            Class<?>[] methodParams = method.getParameterTypes();
            if (metaParams.length == methodParams.length) {
                for (int i = 0; i < metaParams.length; i++) {
                    if (!metaParams[i].equals(methodParams[i])) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * 构建路由键：httpMethod#urlPattern
     */
    private String buildRouteKey(WebInterfaceMetadata metadata) {
        return metadata.getHttpMethod() + "#" + metadata.getUrlPattern();
    }
}