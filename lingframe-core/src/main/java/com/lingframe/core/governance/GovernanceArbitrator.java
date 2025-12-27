package com.lingframe.core.governance;

import com.lingframe.core.kernel.InvocationContext;
import com.lingframe.core.plugin.PluginSlot;
import com.lingframe.core.spi.GovernancePolicyProvider;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;

/**
 * 治理仲裁器 (责任链核心)
 * 职责：遍历所有 Provider，找到第一个处理结果
 */
@Slf4j
public class GovernanceArbitrator {

    private final List<GovernancePolicyProvider> providers;

    public GovernanceArbitrator(List<GovernancePolicyProvider> providers) {
        // 构造时进行排序：Order 小的在前
        this.providers = providers.stream()
                .sorted(Comparator.comparingInt(GovernancePolicyProvider::getOrder))
                .toList();
        log.info("GovernanceArbitrator initialized with {} providers", providers.size());
    }

    public String resolvePermission(PluginSlot slot, Method method, InvocationContext ctx) {
        for (GovernancePolicyProvider provider : providers) {
            String result = provider.resolvePermission(slot, method, ctx);
            if (result != null) return result;
        }
        return "default:execute"; // 绝对兜底
    }

    public boolean shouldAudit(PluginSlot slot, Method method, InvocationContext ctx) {
        for (GovernancePolicyProvider provider : providers) {
            Boolean result = provider.shouldAudit(slot, method, ctx);
            if (result != null) return result;
        }
        return false;
    }

}
