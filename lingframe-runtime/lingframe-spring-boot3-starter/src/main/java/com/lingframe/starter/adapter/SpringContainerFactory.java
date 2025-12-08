package com.lingframe.starter.adapter;

import com.lingframe.core.spi.ContainerFactory;
import com.lingframe.core.spi.PluginContainer;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;

import java.io.File;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class SpringContainerFactory implements ContainerFactory {

    private final ApplicationContext parentContext;

    public SpringContainerFactory(ApplicationContext parentContext) {
        this.parentContext = parentContext;
    }

    @Override
    public PluginContainer create(String pluginId, File jarFile, ClassLoader classLoader) {
        String mainClass = getMainClass(jarFile);
        try {
            Class<?> sourceClass = classLoader.loadClass(mainClass);

            SpringApplicationBuilder builder = new SpringApplicationBuilder()
                    .parent((ConfigurableApplicationContext) parentContext) // 父子上下文
                    .resourceLoader(new DefaultResourceLoader(classLoader)) // 使用隔离加载器
                    .sources(sourceClass)
                    .bannerMode(Banner.Mode.OFF)
                    .web(WebApplicationType.NONE) // 【生产级】禁止插件启动 Tomcat
                    .properties("spring.main.allow-bean-definition-overriding=true") // 【生产级】允许覆盖 Bean
                    .properties("spring.application.name=plugin-" + pluginId); // 【生产级】独立应用名

            return new SpringPluginContainer(builder, classLoader);
        } catch (Exception e) {
            throw new RuntimeException("Create container failed", e);
        }
    }

    private String getMainClass(File jarFile) {
        try (JarFile jar = new JarFile(jarFile)) {
            Manifest manifest = jar.getManifest();
            Attributes attrs = manifest.getMainAttributes();
            // 兼容 Spring Boot 打包插件
            String cls = attrs.getValue("Start-Class");
            if (cls == null) cls = attrs.getValue("Main-Class");
            if (cls == null) throw new IllegalArgumentException("No Main-Class found");
            return cls;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}