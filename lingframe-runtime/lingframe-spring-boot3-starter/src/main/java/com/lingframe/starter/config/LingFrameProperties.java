package com.lingframe.starter.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Setter
@Getter
@ConfigurationProperties(prefix = "lingframe.governance")
public class LingFrameProperties {
    // 格式: <pluginId.methodName, permissionId>
    private Map<String, String> forcePermissions = new HashMap<>();

}