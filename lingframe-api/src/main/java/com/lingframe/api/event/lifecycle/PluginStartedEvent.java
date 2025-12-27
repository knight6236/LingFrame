package com.lingframe.api.event.lifecycle;

/**
 * 启动完成事件
 * 场景：服务注册、监控上报
 */
public class PluginStartedEvent extends PluginLifecycleEvent {
    public PluginStartedEvent(String pluginId, String version) {
        super(pluginId, version);
    }
}
