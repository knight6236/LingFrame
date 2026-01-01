package com.lingframe.api.config;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 对应 plugin.yml 的根节点
 * 作为标准契约
 */
@Setter
@Getter
public class PluginDefinition implements Serializable {
    // === 基础元数据 ===
    private String id;
    private String version;
    private String provider;
    private String description;

    // === 运行时配置 ===
    private String mainClass; // 插件入口类全限定名

    // 依赖列表
    private List<PluginDependency> dependencies = new ArrayList<>();

    // === 治理配置 ===
    private GovernancePolicy governance = new GovernancePolicy();

    // === 扩展配置 (KV 键值对，用于业务参数) ===
    private Map<String, Object> properties = new HashMap<>();

    @Setter
    @Getter
    public static class PluginDependency implements Serializable {
        private String id;
        private String minVersion;
    }
}
