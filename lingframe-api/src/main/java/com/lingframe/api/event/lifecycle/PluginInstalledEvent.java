package com.lingframe.api.event.lifecycle;

/**
 * 安装完成事件
 * 场景：记录审计日志
 */
public class PluginInstalledEvent extends PluginLifecycleEvent {
    public PluginInstalledEvent(String pluginId, String version) {
        super(pluginId, version);
    }
}
