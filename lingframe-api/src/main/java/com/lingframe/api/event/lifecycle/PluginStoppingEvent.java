package com.lingframe.api.event.lifecycle;

/**
 * 停止前置事件
 * 场景：流量摘除、拒绝新请求
 */
public class PluginStoppingEvent extends PluginLifecycleEvent {
    public PluginStoppingEvent(String pluginId, String version) {
        super(pluginId, version);
    }
}
