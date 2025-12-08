package com.lingframe.starter.configuration;

import com.lingframe.core.plugin.PluginManager;
import com.lingframe.core.spi.ContainerFactory;
import com.lingframe.starter.adapter.SpringContainerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LingFrameAutoConfiguration {

    /**
     * 注册 SPI 工厂实现
     */
    @Bean
    public ContainerFactory containerFactory(ApplicationContext parentContext) {
        return new SpringContainerFactory(parentContext);
    }

    /**
     * 注册核心 PluginManager
     */
    @Bean
    public PluginManager pluginManager(ContainerFactory containerFactory) {
        return new PluginManager(containerFactory);
    }
}