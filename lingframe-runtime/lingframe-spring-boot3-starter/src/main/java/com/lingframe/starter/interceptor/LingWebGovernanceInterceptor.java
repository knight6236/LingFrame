package com.lingframe.starter.interceptor;

import com.lingframe.api.annotation.Auditable;
import com.lingframe.api.annotation.RequiresPermission;
import com.lingframe.api.context.PluginContextHolder;
import com.lingframe.api.exception.PermissionDeniedException;
import com.lingframe.api.security.AccessType;
import com.lingframe.api.security.PermissionService;
import com.lingframe.core.audit.AuditManager;
import com.lingframe.core.event.EventBus;
import com.lingframe.core.event.monitor.MonitoringEvents;
import com.lingframe.core.kernel.InvocationContext;
import com.lingframe.core.monitor.TraceContext;
import com.lingframe.core.strategy.GovernanceStrategy;
import com.lingframe.starter.config.LingFrameProperties;
import com.lingframe.starter.web.WebInterfaceManager;
import com.lingframe.starter.web.WebInterfaceMetadata;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * 统一 Web 层治理拦截器
 * 处理宿主和插件 Controller 的 HTTP 请求，实现：
 * 1. 链路追踪（TraceContext）
 * 2. 插件上下文设置（PluginContextHolder）
 * 3. ClassLoader 切换（插件请求）
 * 4. 权限检查（通过 PermissionService）
 * 5. 异步审计（通过 AuditManager）
 */
@Slf4j
@RequiredArgsConstructor
public class LingWebGovernanceInterceptor implements HandlerInterceptor {

    private final PermissionService permissionService;
    private final WebInterfaceManager webInterfaceManager;
    private final LingFrameProperties properties;
    private final EventBus eventBus;

    private static final String HOST_PLUGIN_ID = "host-app";
    private static final String ATTR_ORIGINAL_CL = "LING_ORIGINAL_CL";
    private static final String ATTR_CONTEXT = "LING_INVOCATION_CTX";
    private static final String ATTR_START_TIME = "LING_START_TIME";
    private static final String ATTR_IS_PLUGIN = "LING_IS_PLUGIN";

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull Object handler)
            throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 判断是插件请求还是宿主请求
        WebInterfaceMetadata pluginMeta = webInterfaceManager.getMetadata(handlerMethod);
        boolean isPluginRequest = (pluginMeta != null);

        // 宿主请求：检查是否启用宿主治理
        if (!isPluginRequest && !properties.getHostGovernance().isEnabled()) {
            return true;
        }

        request.setAttribute(ATTR_IS_PLUGIN, isPluginRequest);
        request.setAttribute(ATTR_START_TIME, System.nanoTime());

        // 1. 链路追踪初始化
        String traceId = request.getHeader("X-Trace-Id");
        if (traceId != null && !traceId.isBlank()) {
            TraceContext.setTraceId(traceId);
        } else {
            TraceContext.start();
        }
        TraceContext.increaseDepth();

        // 2. ClassLoader 切换（仅插件请求）
        if (isPluginRequest) {
            ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
            request.setAttribute(ATTR_ORIGINAL_CL, originalCL);
            Thread.currentThread().setContextClassLoader(pluginMeta.getClassLoader());
        }

        // 3. 设置插件上下文
        String pluginId = isPluginRequest ? pluginMeta.getPluginId() : HOST_PLUGIN_ID;
        PluginContextHolder.set(pluginId);

        // 4. 构建治理上下文
        Method method = handlerMethod.getMethod();
        InvocationContext ctx = buildInvocationContext(request, method, pluginId, isPluginRequest ? pluginMeta : null);
        request.setAttribute(ATTR_CONTEXT, ctx);

        // 5. 权限检查（直接使用 PermissionService）
        log.debug("[LingWeb] preHandle: {} {} -> plugin={}", request.getMethod(), request.getRequestURI(), pluginId);

        // 发布入站追踪
        publishTrace(ctx.getTraceId(), pluginId,
                String.format("→ INGRESS: HTTP %s %s", request.getMethod(), request.getRequestURI()),
                "IN", TraceContext.getDepth());

        // 检查插件是否启用
        if (!permissionService.isAllowed(pluginId, "PLUGIN_ENABLE", AccessType.EXECUTE)) {
            throw new PermissionDeniedException(pluginId, "PLUGIN_ENABLE");
        }

        // 检查请求权限
        String permission = ctx.getRequiredPermission();
        if (permission != null && !permission.isBlank()) {
            AccessType accessType = ctx.getAccessType() != null ? ctx.getAccessType() : AccessType.EXECUTE;
            if (!permissionService.isAllowed(pluginId, permission, accessType)) {
                log.warn("⛔ Permission Denied: Plugin=[{}] needs=[{}] type=[{}]", pluginId, permission, accessType);
                throw new PermissionDeniedException(pluginId, permission, accessType);
            }
        }

        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull Object handler, Exception ex) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return;
        }

        Boolean isPluginRequest = (Boolean) request.getAttribute(ATTR_IS_PLUGIN);
        if (isPluginRequest == null) {
            return; // 未经过 preHandle 处理
        }

        try {
            // 异步审计记录
            InvocationContext ctx = (InvocationContext) request.getAttribute(ATTR_CONTEXT);
            Long startTime = (Long) request.getAttribute(ATTR_START_TIME);

            if (ctx != null && startTime != null) {
                long cost = System.nanoTime() - startTime;
                boolean success = (ex == null && response.getStatus() < 400);

                // 发布出站追踪
                publishTrace(ctx.getTraceId(), ctx.getPluginId(),
                        success ? "← RETURN: Success"
                                : "✖ ERROR: " + (ex != null ? ex.getMessage() : "HTTP " + response.getStatus()),
                        success ? "OUT" : "ERROR", TraceContext.getDepth());

                // 异步审计（仅对需要审计的请求）
                if (ctx.isShouldAudit()) {
                    AuditManager.asyncRecord(
                            ctx.getTraceId(),
                            ctx.getCallerPluginId() != null ? ctx.getCallerPluginId() : ctx.getPluginId(),
                            ctx.getAuditAction(),
                            ctx.getResourceId(),
                            null, // Web 请求不记录参数
                            success ? "HTTP " + response.getStatus() : ex,
                            cost);

                    // 发布实时审计事件
                    if (eventBus != null) {
                        eventBus.publish(new MonitoringEvents.AuditLogEvent(
                                ctx.getTraceId(),
                                ctx.getPluginId(),
                                ctx.getAuditAction(),
                                ctx.getResourceId(),
                                success,
                                cost));
                    }
                }
            }
        } finally {
            // 恢复 ClassLoader（仅插件请求）
            if (isPluginRequest) {
                ClassLoader originalCL = (ClassLoader) request.getAttribute(ATTR_ORIGINAL_CL);
                if (originalCL != null) {
                    Thread.currentThread().setContextClassLoader(originalCL);
                }
            }

            // 深度递减
            TraceContext.decreaseDepth();

            // 清理上下文
            PluginContextHolder.clear();

            // 链路追踪清理（仅在根节点清理）
            if (TraceContext.getDepth() == 0) {
                TraceContext.clear();
            }
        }
    }

    private void publishTrace(String traceId, String pluginId, String action, String type, int depth) {
        if (eventBus != null) {
            try {
                eventBus.publish(new MonitoringEvents.TraceLogEvent(traceId, pluginId, action, type, depth));
            } catch (Exception e) {
                log.warn("Failed to publish trace event", e);
            }
        }
    }

    /**
     * 构建治理上下文
     */
    private InvocationContext buildInvocationContext(HttpServletRequest request, Method method,
            String pluginId, WebInterfaceMetadata meta) {
        // 智能权限推导
        String permission;
        RequiresPermission permAnn = AnnotatedElementUtils.findMergedAnnotation(method, RequiresPermission.class);
        if (permAnn != null) {
            permission = permAnn.value();
        } else if (meta != null && meta.getRequiredPermission() != null) {
            permission = meta.getRequiredPermission();
        } else {
            permission = GovernanceStrategy.inferPermission(method);
        }

        // 智能审计推导
        boolean shouldAudit = false;
        String auditAction = request.getMethod() + " " + request.getRequestURI();
        Auditable auditAnn = AnnotatedElementUtils.findMergedAnnotation(method, Auditable.class);
        if (auditAnn != null) {
            shouldAudit = true;
            auditAction = auditAnn.action();
        } else if (meta != null) {
            shouldAudit = meta.isShouldAudit();
            if (meta.getAuditAction() != null) {
                auditAction = meta.getAuditAction();
            }
        } else if (!"GET".equals(request.getMethod())) {
            // 非 GET 请求默认审计
            shouldAudit = true;
        }

        // 推导访问类型
        AccessType accessType = switch (request.getMethod()) {
            case "GET", "HEAD", "OPTIONS" -> AccessType.READ;
            case "POST", "PUT", "PATCH", "DELETE" -> AccessType.WRITE;
            default -> AccessType.EXECUTE;
        };

        return InvocationContext.builder()
                .traceId(TraceContext.get())
                .pluginId(pluginId)
                .callerPluginId("http-gateway") // Web 请求来源标记
                .resourceType("HTTP")
                .resourceId(request.getMethod() + " " + request.getRequestURI())
                .operation(method.getName())
                .requiredPermission(permission)
                .accessType(accessType)
                .auditAction(auditAction)
                .shouldAudit(shouldAudit)
                .metadata(new HashMap<>())
                .labels(new HashMap<>())
                .build();
    }
}
