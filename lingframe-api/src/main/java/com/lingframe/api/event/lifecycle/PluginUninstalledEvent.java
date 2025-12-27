package com.lingframe.api.event.lifecycle;

/**
 * 卸载完成事件
 * 场景：清理临时文件
 */
public class PluginUninstalledEvent extends PluginLifecycleEvent {
    public PluginUninstalledEvent(String pluginId) {
        super(pluginId, null);
    }
}
