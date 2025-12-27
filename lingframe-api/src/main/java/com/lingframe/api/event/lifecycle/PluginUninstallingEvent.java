package com.lingframe.api.event.lifecycle;

/**
 * 卸载前置事件 (可拦截)
 * 场景：防止误删核心插件
 */
public class PluginUninstallingEvent extends PluginLifecycleEvent {
    public PluginUninstallingEvent(String pluginId) {
        super(pluginId, null); // 卸载时可能只知道 ID，Version 视上下文而定
    }
}
