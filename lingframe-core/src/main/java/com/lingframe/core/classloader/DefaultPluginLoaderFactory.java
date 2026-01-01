package com.lingframe.core.classloader;

import com.lingframe.core.spi.PluginLoaderFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

// ✅ 默认插件加载器工厂
public class DefaultPluginLoaderFactory implements PluginLoaderFactory {

    @Override
    public ClassLoader create(String pluginId, File sourceFile, ClassLoader parent) {
        try {
            URL[] urls;

            if (sourceFile.isDirectory()) {
                // 开发模式：classes 目录
                urls = new URL[]{sourceFile.toURI().toURL()};
            } else if (sourceFile.getName().endsWith(".jar")) {
                // 生产模式：JAR 包
                urls = new URL[]{sourceFile.toURI().toURL()};
            } else {
                throw new IllegalArgumentException("Unsupported sourceFile: " + sourceFile);
            }

            // ✅ 传入 pluginId
            return new PluginClassLoader(pluginId, urls, parent);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error creating PluginClassLoader for pluginId: " + pluginId, e);
        }
    }
}