package com.lingframe.api.config;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 治理策略子节点
 */
@Getter
@Setter
public class GovernancePolicy implements Serializable {
    private List<PermissionRule> permissions = new ArrayList<>();
    private List<AuditRule> audits = new ArrayList<>();

    @Getter
    @Setter
    public static class PermissionRule implements Serializable {
        private String methodPattern;
        private String permissionId;
    }

    @Getter
    @Setter
    public static class AuditRule implements Serializable {
        private String methodPattern;
        private String action;
        private boolean enabled = true;
    }

}
