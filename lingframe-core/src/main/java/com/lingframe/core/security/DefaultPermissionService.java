package com.lingframe.core.security;

import com.lingframe.api.security.AccessType;
import com.lingframe.api.security.PermissionService;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认权限服务实现
 * 职责：管理权限策略，提供鉴权查询，记录审计日志
 */
@Slf4j
public class DefaultPermissionService implements PermissionService {

    // 简单的权限表: Map<PluginId, Map<Capability, AccessType>>
    // 实际生产中应从数据库或配置文件加载
    private final Map<String, Map<String, AccessType>> permissions = new ConcurrentHashMap<>();

    // 全局白名单服务 (例如基础的日志服务)
    private static final String GLOBAL_WHITELIST_PREFIX = "com.lingframe.api.";

    public void grant(String pluginId, String capability, AccessType accessType) {
        permissions.computeIfAbsent(pluginId, k -> new ConcurrentHashMap<>())
                .put(capability, accessType);
    }

    @Override
    public boolean isAllowed(String pluginId, String capability, AccessType accessType) {
        // 1. 只有 Core 自身调用（pluginId=null）或白名单服务，默认放行
        if (pluginId == null || capability.startsWith(GLOBAL_WHITELIST_PREFIX)) {
            return true;
        }

        // 2. 查表鉴权
        Map<String, AccessType> pluginPerms = permissions.get(pluginId);
        if (pluginPerms == null) {
            log.warn("DENY: Plugin [{}] has no permissions configured.", pluginId);
            return false;
        }

        AccessType granted = pluginPerms.get(capability);
        if (granted == null) {
            log.warn("DENY: Plugin [{}] tried to access [{}] without permission.", pluginId, capability);
            return false;
        }

        // 简单级别判断: WRITE 包含 READ, EXECUTE 独立
        // 这里简化逻辑：必须完全匹配或者是 WRITE (假设 WRITE > READ)
        boolean allowed = granted == accessType || (granted == AccessType.WRITE && accessType == AccessType.READ);

        if (!allowed) {
            log.warn("DENY: Plugin [{}] capability [{}] requires [{}] but has [{}]",
                    pluginId, capability, accessType, granted);
        }
        return allowed;
    }

    @Override
    public Object getPermission(String pluginId, String capability) {
        return permissions.getOrDefault(pluginId, Map.of()).get(capability);
    }

    @Override
    public void audit(String pluginId, String capability, String operation, boolean allowed) {
        // 生产环境应写入审计日志文件或 DB
        if (!allowed) {
            log.warn("[AUDIT] Plugin: {}, Cap: {}, Op: {}, Allowed: {}", pluginId, capability, operation, allowed);
        } else {
            log.debug("[AUDIT] Plugin: {}, Cap: {}, Op: {}, Allowed: {}", pluginId, capability, operation, allowed);
        }
    }
}