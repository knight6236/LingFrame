package com.lingframe.core.security;

import com.lingframe.api.security.AccessType;
import com.lingframe.core.config.LingFrameConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultPermissionService 单元测试")
class DefaultPermissionServiceTest {

    private DefaultPermissionService permissionService;

    @BeforeEach
    void setUp() {
        permissionService = new DefaultPermissionService(null);
    }

    @AfterEach
    void tearDown() {
        // 重置开发模式设置
        LingFrameConfig.current().setDevMode(false);
    }

    @Nested
    @DisplayName("全局白名单")
    class GlobalWhitelistTests {
        @Test
        @DisplayName("应允许访问全局白名单 API")
        void shouldAllowGlobalWhitelist() {
            assertTrue(permissionService.isAllowed("any-plugin", "com.lingframe.api.SomeApi", AccessType.READ));
        }
    }

    @Nested
    @DisplayName("权限授予与检查")
    class GrantCheckTests {
        @Test
        @DisplayName("授予和检查权限应正常工作")
        void shouldGrantAndCheckPermission() {
            String pluginId = "test-plugin";
            String capability = "test-capability";
            AccessType accessType = AccessType.WRITE;

            // 初始时应该没有权限
            assertFalse(permissionService.isAllowed(pluginId, capability, accessType));

            // 授予权限
            permissionService.grant(pluginId, capability, accessType);

            // 现在应该有权限了
            assertTrue(permissionService.isAllowed(pluginId, capability, accessType));

            // READ 权限应该被 WRITE 权限覆盖 (假设层级关系或者具体实现允许)
            assertTrue(permissionService.isAllowed(pluginId, capability, AccessType.READ));
        }

        @Test
        @DisplayName("获取权限应返回已授予的权限")
        void shouldGetPermission() {
            String pluginId = "test-plugin";
            String capability = "test-capability";
            AccessType accessType = AccessType.WRITE;

            // 初始时应该返回 null
            assertNull(permissionService.getPermission(pluginId, capability));

            // 授予权限
            permissionService.grant(pluginId, capability, accessType);

            // 现在应该能获取到权限
            assertNotNull(permissionService.getPermission(pluginId, capability));
        }
    }

    @Nested
    @DisplayName("开发模式")
    class DevModeTests {
        @Test
        @DisplayName("开发模式下应默认允许所有访问")
        void shouldAllowAllInDevMode() {
            String pluginId = "test-plugin";
            String capability = "test-capability";
            AccessType accessType = AccessType.WRITE;

            // 开发模式下，即使没有权限也应该返回 true
            LingFrameConfig.current().setDevMode(true);
            assertTrue(permissionService.isAllowed(pluginId, capability, accessType));
        }
    }
}