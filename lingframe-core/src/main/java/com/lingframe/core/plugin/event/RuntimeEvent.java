package com.lingframe.core.plugin.event;

import com.lingframe.core.plugin.PluginInstance;

/**
 * 插件运行时内部事件（组件间通信用）
 * 注意：这是内部事件，不暴露给外部
 */
public sealed interface RuntimeEvent {

    String pluginId();

    // ===== 生命周期事件 =====

    /**
     * 实例升级中（新版本即将启动）
     */
    record InstanceUpgrading(String pluginId, String newVersion) implements RuntimeEvent {
    }

    /**
     * 实例已就绪
     */
    record InstanceReady(String pluginId, String version, PluginInstance instance) implements RuntimeEvent {
    }

    /**
     * 实例进入死亡状态
     */
    record InstanceDying(String pluginId, String version, PluginInstance instance) implements RuntimeEvent {
    }

    /**
     * 实例已销毁
     */
    record InstanceDestroyed(String pluginId, String version) implements RuntimeEvent {
    }

    // ===== 运行时事件 =====

    /**
     * 运行时关闭中
     */
    record RuntimeShuttingDown(String pluginId) implements RuntimeEvent {
    }

    /**
     * 运行时已关闭
     */
    record RuntimeShutdown(String pluginId) implements RuntimeEvent {
    }

    // ===== 调用事件（用于指标/监控）=====

    /**
     * 调用开始
     */
    record InvocationStarted(String pluginId, String fqsid, String caller) implements RuntimeEvent {
    }

    /**
     * 调用完成
     */
    record InvocationCompleted(String pluginId, String fqsid, long durationMs,
                               boolean success) implements RuntimeEvent {
    }

    /**
     * 调用被拒绝（舱壁满）
     */
    record InvocationRejected(String pluginId, String fqsid, String reason) implements RuntimeEvent {
    }
}