package com.lingframe.api.event.lifecycle;

import java.io.File;

/**
 * 安装前置事件 (可拦截)
 * 场景：签名校验、依赖检查
 */
public class PluginInstallingEvent extends PluginLifecycleEvent {
    private final File sourceFile;

    public PluginInstallingEvent(String pluginId, String version, File sourceFile) {
        super(pluginId, version);
        this.sourceFile = sourceFile;
    }

    public File getSourceFile() { return sourceFile; }
}
