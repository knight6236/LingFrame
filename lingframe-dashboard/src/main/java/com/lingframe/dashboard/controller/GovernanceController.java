package com.lingframe.dashboard.controller;

import com.lingframe.api.config.GovernancePolicy;
import com.lingframe.dashboard.dto.ApiResponse;
import com.lingframe.dashboard.dto.ResourcePermissionDTO;
import com.lingframe.dashboard.service.DashboardService;
import com.lingframe.core.governance.LocalGovernanceRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/lingframe/dashboard/governance")
@CrossOrigin(origins = "*") // 开发阶段允许跨域
@ConditionalOnProperty(prefix = "lingframe.dashboard", name = "enabled", havingValue = "true", matchIfMissing = false)
public class GovernanceController {

    private final LocalGovernanceRegistry registry;
    private final DashboardService dashboardService;

    /**
     * 获取所有治理规则
     */
    @GetMapping("/rules")
    public ApiResponse<Map<String, GovernancePolicy>> getRules() {
        try {
            return ApiResponse.ok(registry.getAllPatches());
        } catch (Exception e) {
            log.error("Failed to get rules", e);
            return ApiResponse.error("获取规则失败: " + e.getMessage());
        }
    }

    /**
     * 获取指定插件的治理策略
     */
    @GetMapping("/{pluginId}")
    public ApiResponse<GovernancePolicy> getPatch(@PathVariable String pluginId) {
        try {
            GovernancePolicy policy = registry.getPatch(pluginId);
            return ApiResponse.ok(policy);
        } catch (Exception e) {
            log.error("Failed to get patch for: {}", pluginId, e);
            return ApiResponse.error("获取策略失败: " + e.getMessage());
        }
    }

    /**
     * 更新治理策略 (完整策略对象)
     */
    @PostMapping("/patch/{pluginId}")
    public ApiResponse<GovernancePolicy> updatePatch(
            @PathVariable String pluginId,
            @RequestBody GovernancePolicy policy) {
        try {
            registry.updatePatch(pluginId, policy);
            return ApiResponse.ok("策略已更新", policy);
        } catch (Exception e) {
            log.error("Failed to update patch for: {}", pluginId, e);
            return ApiResponse.error("策略更新失败: " + e.getMessage());
        }
    }

    /**
     * 更新资源权限 (简化接口，供 Dashboard 使用)
     * 调用 DashboardService 以使用 PermissionService.grant/revoke
     */
    @PostMapping("/{pluginId}/permissions")
    public ApiResponse<ResourcePermissionDTO> updatePermissions(
            @PathVariable String pluginId,
            @RequestBody ResourcePermissionDTO dto) {
        try {
            dashboardService.updatePermissions(pluginId, dto);
            return ApiResponse.ok("权限已更新", dto);
        } catch (Exception e) {
            log.error("Failed to update permissions for: {}", pluginId, e);
            return ApiResponse.error("权限更新失败: " + e.getMessage());
        }
    }
}
