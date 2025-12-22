package com.lingframe.starter.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class WebInterfaceManager {

    private static WebInterfaceManager INSTANCE;

    private final Map<String, WebInterfaceMetadata> routeMap = new ConcurrentHashMap<>();
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private RequestMappingHandlerMapping hostMapping;
    private Object proxyController;
    private Method proxyMethod;

    public WebInterfaceManager() {
        INSTANCE = this;
    }

    public static WebInterfaceManager getInstance() {
        return INSTANCE;
    }

    // 初始化方法，由 AutoConfiguration 调用
    public void init(RequestMappingHandlerMapping mapping, Object controller, Method method) {
        this.hostMapping = mapping;
        this.proxyController = controller;
        this.proxyMethod = method;
    }

    public void register(WebInterfaceMetadata metadata) {
        if (hostMapping == null) {
            log.warn("WebInterfaceManager not initialized, skipping registration: {}", metadata.getUrlPattern());
            return;
        }

        String url = metadata.getUrlPattern();
        routeMap.put(url, metadata);

        try {
            // 动态注册到宿主 Spring MVC
            RequestMappingInfo info = RequestMappingInfo
                    .paths(url)
                    .methods(RequestMethod.valueOf(metadata.getHttpMethod()))
                    .build();

            // 核心魔法：将所有插件 URL 映射到同一个 Proxy 方法上
            hostMapping.registerMapping(info, proxyController, proxyMethod);

            log.info("🌍 [LingFrame Web] Mapped: {} -> {}.{}", url, metadata.getPluginId(), metadata.getTargetMethod().getName());
        } catch (Exception e) {
            log.error("Failed to register web mapping: " + url, e);
        }
    }

    public WebInterfaceMetadata match(String path) {
        if (routeMap.containsKey(path)) return routeMap.get(path);

        for (String pattern : routeMap.keySet()) {
            if (pathMatcher.match(pattern, path)) {
                return routeMap.get(pattern);
            }
        }
        return null;
    }
}