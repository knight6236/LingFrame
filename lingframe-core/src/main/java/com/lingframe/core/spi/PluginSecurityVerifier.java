package com.lingframe.core.spi;

import java.io.File;

/**
 * 插件安全验证器 SPI
 * 专门用于在安装前执行签名校验、哈希比对等阻断性操作
 */
public interface PluginSecurityVerifier {

    /**
     * 校验插件包
     *
     * @throws SecurityException 如果校验失败，抛出异常阻止安装
     */
    void verify(String pluginId, File sourceFile) throws SecurityException;
}