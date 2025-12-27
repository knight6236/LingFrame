package com.lingframe.core.governance.provider;

import com.lingframe.api.annotation.Auditable;
import com.lingframe.api.annotation.RequiresPermission;
import com.lingframe.api.config.GovernancePolicy;
import com.lingframe.core.governance.LocalGovernanceRegistry;
import com.lingframe.core.kernel.InvocationContext;
import com.lingframe.core.plugin.PluginInstance;
import com.lingframe.core.plugin.PluginSlot;
import com.lingframe.core.spi.GovernancePolicyProvider;
import com.lingframe.core.strategy.GovernanceStrategy;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * 标准治理策略
 * 优先级：100
 */
public class StandardGovernancePolicyProvider implements GovernancePolicyProvider {

    private final LocalGovernanceRegistry localRegistry;
    private final Map<String, String> hostForceRules;

    public StandardGovernancePolicyProvider(LocalGovernanceRegistry localRegistry, Map<String, String> hostForceRules) {
        this.localRegistry = localRegistry;
        this.hostForceRules = hostForceRules;
    }

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    public String resolvePermission(PluginSlot slot, Method method, InvocationContext ctx) {
        String pid = slot.getPluginId();
        String mName = method.getName();

        // P0: 宿主 YML 强制
        String p0 = hostForceRules.get(pid + "." + mName);
        if (p0 != null) return p0;

        // P1: 动态补丁
        GovernancePolicy patch = localRegistry.getPatch(pid);
        String p1 = matchPermission(patch, mName);
        if (p1 != null) return p1;

        // P2: 插件定义
        PluginInstance instance = slot.selectInstance(ctx);
        if (instance != null && instance.getDefinition() != null) {
            String p2 = matchPermission(instance.getDefinition().getGovernance(), mName);
            if (p2 != null) return p2;
        }

        // P3: 注解
        RequiresPermission ann = method.getAnnotation(RequiresPermission.class);
        if (ann != null) return ann.value();

        // P4: 智能推导
        return GovernanceStrategy.inferPermission(method);
    }

    @Override
    public Boolean shouldAudit(PluginSlot slot, Method method, InvocationContext ctx) {
        String pid = slot.getPluginId();
        String mName = method.getName();

        // P1: 动态补丁
        Boolean a1 = matchAudit(localRegistry.getPatch(pid), mName);
        if (a1 != null) return a1;

        // P2: 插件定义
        PluginInstance instance = slot.selectInstance(ctx);
        if (instance != null && instance.getDefinition() != null) {
            Boolean a2 = matchAudit(instance.getDefinition().getGovernance(), mName);
            if (a2 != null) return a2;
        }

        // P3: 注解
        return method.isAnnotationPresent(Auditable.class);
    }

    // --- 辅助匹配逻辑 ---
    private String matchPermission(GovernancePolicy policy, String methodName) {
        if (policy == null || policy.getPermissions() == null) return null;
        for (GovernancePolicy.PermissionRule rule : policy.getPermissions()) {
            if (isMatch(rule.getMethodPattern(), methodName)) return rule.getPermissionId();
        }
        return null;
    }

    private Boolean matchAudit(GovernancePolicy policy, String methodName) {
        if (policy == null || policy.getAudits() == null) return null;
        for (GovernancePolicy.AuditRule rule : policy.getAudits()) {
            if (isMatch(rule.getMethodPattern(), methodName)) return rule.isEnabled();
        }
        return null;
    }

    private boolean isMatch(String pattern, String methodName) {
        if (pattern == null) return false;
        if (pattern.equals(methodName)) return true;
        return pattern.endsWith("*") && methodName.startsWith(pattern.substring(0, pattern.length() - 1));
    }
}