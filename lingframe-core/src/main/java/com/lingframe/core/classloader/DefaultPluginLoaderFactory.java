package com.lingframe.core.classloader;

import com.lingframe.core.spi.PluginLoaderFactory;

import java.io.File;
import java.net.URL;

public class DefaultPluginLoaderFactory implements PluginLoaderFactory {
    @Override
    public ClassLoader create(File sourceFile, ClassLoader parent) {
        try {
            // 默认实现：标准的 URLClassLoader 加载
            return new PluginClassLoader(new URL[]{sourceFile.toURI().toURL()}, parent);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create classloader", e);
        }
    }
}