package com.lingframe.api.config;

import lombok.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 治理策略子节点
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GovernancePolicy implements Serializable {

    // 给字段赋默认值，防止 Builder 覆盖导致为 null (需要 @Builder.Default)
    @Builder.Default
    private List<PermissionRule> permissions = new ArrayList<>();

    @Builder.Default
    private List<CapabilityRule> capabilities = new ArrayList<>();

    @Builder.Default
    private List<AuditRule> audits = new ArrayList<>();

    public GovernancePolicy copy() {
        GovernancePolicy copy = new GovernancePolicy();

        if (this.permissions != null) {
            copy.permissions = this.permissions.stream()
                    .map(PermissionRule::copy)
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        if (this.capabilities != null) {
            copy.capabilities = this.capabilities.stream()
                    .map(CapabilityRule::copy)
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        if (this.audits != null) {
            copy.audits = this.audits.stream()
                    .map(AuditRule::copy)
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        return copy;
    }

    /**
     * 资源能力申请规则
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CapabilityRule implements Serializable {
        private String capability;
        private String accessType;

        public CapabilityRule copy() {
            CapabilityRule copy = new CapabilityRule();
            copy.capability = this.capability;
            copy.accessType = this.accessType;
            return copy;
        }
    }

    /**
     * ACL 权限控制规则
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PermissionRule implements Serializable {
        private String methodPattern;
        private String permissionId;

        public PermissionRule copy() {
            PermissionRule copy = new PermissionRule();
            copy.methodPattern = this.methodPattern;
            copy.permissionId = this.permissionId;
            return copy;
        }
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuditRule implements Serializable {
        private String methodPattern;
        private String action;
        private boolean enabled = true;

        public AuditRule copy() {
            AuditRule copy = new AuditRule();
            copy.methodPattern = this.methodPattern;
            copy.action = this.action;
            copy.enabled = this.enabled;
            return copy;
        }
    }

}
