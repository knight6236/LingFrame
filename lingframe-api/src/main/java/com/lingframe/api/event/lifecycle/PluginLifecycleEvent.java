package com.lingframe.api.event.lifecycle;

import com.lingframe.api.event.AbstractLingEvent;
import lombok.Getter;

/**
 * 插件生命周期事件基类
 */
@Getter
public abstract class PluginLifecycleEvent extends AbstractLingEvent {
    private final String pluginId;
    private final String version;

    public PluginLifecycleEvent(String pluginId, String version) {
        super();
        this.pluginId = pluginId;
        this.version = version;
    }

    @Override
    public String toString() {
        return super.toString() + " source=" + pluginId + ":" + version;
    }
}
