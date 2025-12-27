package com.lingframe.api.event.lifecycle;

/**
 * 停止完成事件
 * 场景：资源释放通知
 */
public class PluginStoppedEvent extends PluginLifecycleEvent {
    public PluginStoppedEvent(String pluginId, String version) {
        super(pluginId, version);
    }
}