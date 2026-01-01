package com.lingframe.core.spi;

import java.io.File;

/**
 * 插件类加载器工厂 SPI
 */
public interface PluginLoaderFactory {
    ClassLoader create(String pluginId, File sourceFile, ClassLoader parent);
}