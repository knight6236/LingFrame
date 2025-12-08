package com.lingframe.starter.adapter;

import com.lingframe.api.context.PluginContext;
import com.lingframe.api.plugin.LingPlugin;
import com.lingframe.core.spi.PluginContainer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

@Slf4j
public class SpringPluginContainer implements PluginContainer {

    private final SpringApplicationBuilder builder;
    private ConfigurableApplicationContext context;
    private final ClassLoader classLoader;
    // 保存 Context 以便 stop 时使用
    private PluginContext pluginContext;

    public SpringPluginContainer(SpringApplicationBuilder builder, ClassLoader classLoader) {
        this.builder = builder;
        this.classLoader = classLoader;
    }

    @Override
    public void start(PluginContext pluginContext) {
        this.pluginContext = pluginContext;

        // 1. TCCL 劫持
        Thread t = Thread.currentThread();
        ClassLoader old = t.getContextClassLoader();
        t.setContextClassLoader(classLoader);
        try {
            // 2. 启动 Spring
            this.context = builder.run();

            // 3. 【关键】寻找并触发 LingPlugin 生命周期
            // 尝试从 Spring 容器中获取实现了 LingPlugin 接口的 Bean
            try {
                LingPlugin plugin = this.context.getBean(LingPlugin.class);
                log.info("Triggering onStart for plugin: {}", pluginContext.getPluginId());
                plugin.onStart(pluginContext);
            } catch (Exception e) {
                log.warn("No LingPlugin entry point found in plugin: {}", pluginContext.getPluginId());
            }

        } finally {
            t.setContextClassLoader(old);
        }
    }

    @Override
    public void stop() {
        if (context != null && context.isActive()) {
            // 1. 【关键】触发 onStop
            try {
                LingPlugin plugin = this.context.getBean(LingPlugin.class);
                log.info("Triggering onStop for plugin: {}", pluginContext.getPluginId());
                plugin.onStop(pluginContext);
            } catch (Exception e) {
                // 忽略，可能没有入口类
            }

            context.close();
        }
        this.context = null;
    }

    @Override
    public boolean isActive() {
        return context != null && context.isActive();
    }

    @Override
    public <T> T getBean(Class<T> type) {
        if (!isActive()) return null;
        try {
            return context.getBean(type);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public ClassLoader getClassLoader() {
        return this.classLoader;
    }
}