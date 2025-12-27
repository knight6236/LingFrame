package com.lingframe.core.spi;

import com.lingframe.core.kernel.InvocationContext;
import com.lingframe.core.plugin.PluginSlot;

import java.lang.reflect.Method;

/**
 * 治理策略提供者 (责任链模式) SPI
 */
public interface GovernancePolicyProvider {

    /**
     * 判定权限
     *
     * @return 返回 权限ID (表示命中)，返回 null (表示我不处理，交给下一个)
     */
    String resolvePermission(PluginSlot slot, Method method, InvocationContext ctx);

    /**
     * 判定审计
     *
     * @return 返回 true/false (表示命中)，返回 null (表示不处理)
     */
    Boolean shouldAudit(PluginSlot slot, Method method, InvocationContext ctx);

    /**
     * 排序优先级 (越小越优先)
     */
    int getOrder();
}
