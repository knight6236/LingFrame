package com.lingframe.core.spi;

import java.io.File;

/**
 * 容器工厂 SPI
 */
public interface ContainerFactory {

    /**
     * 创建容器实例
     * @param pluginId 插件ID
     * @param jarFile 插件 Jar 文件
     * @param classLoader 插件专用的类加载器
     * @return 容器实例
     */
    PluginContainer create(String pluginId, File jarFile, ClassLoader classLoader);
}