package com.lingframe.starter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.ResponseBody;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class LingWebProxyController {

    private final WebInterfaceManager webInterfaceManager;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // 默认的 ObjectMapper，用于兜底（比如插件没配 Jackson）
    private final ObjectMapper fallbackMapper = new ObjectMapper();

    @ResponseBody
    public void dispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String uri = request.getRequestURI();

        // 1. 路由匹配
        WebInterfaceMetadata meta = webInterfaceManager.match(uri);
        if (meta == null) {
            response.sendError(404);
            return;
        }

        // 2. 切换 TCCL (进入插件世界)
        ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(meta.getClassLoader());

        try {
            // 3. 【核心黑魔法】获取插件的 ObjectMapper
            // 因为已经在 TCCL 下，且 Bean 也是插件加载的，所以这个 Mapper 能读懂插件的 DTO
            ObjectMapper pluginMapper = getPluginObjectMapper(meta);

            // 4. 准备参数
            Object[] args = new Object[meta.getParameters().size()];

            for (int i = 0; i < meta.getParameters().size(); i++) {
                WebInterfaceMetadata.ParamDef def = meta.getParameters().get(i);

                if (def.getSourceType() == WebInterfaceMetadata.ParamType.REQUEST_BODY) {
                    // 🔥 自动反序列化
                    // 读取 Host 的流 -> 用 Plugin 的 Mapper -> 转成 Plugin 的对象
                    String json = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
                    args[i] = pluginMapper.readValue(json, def.getType());

                } else if (def.getSourceType() == WebInterfaceMetadata.ParamType.PATH_VARIABLE) {
                    // 基础类型转换 (String -> Long/Int)
                    Map<String, String> vars = pathMatcher.extractUriTemplateVariables(meta.getUrlPattern(), uri);
                    args[i] = convert(vars.get(def.getName()), def.getType(), pluginMapper);

                } else if (def.getSourceType() == WebInterfaceMetadata.ParamType.REQUEST_PARAM) {
                    String val = request.getParameter(def.getName());
                    args[i] = convert(val, def.getType(), pluginMapper);
                }
            }

            // 5. 反射调用
            Object result = meta.getTargetMethod().invoke(meta.getTargetBean(), args);

            // 6. 【核心黑魔法】处理返回值
            if (result != null) {
                response.setContentType("application/json;charset=UTF-8");
                // 🔥 自动序列化
                // 对象 -> 用 Plugin 的 Mapper -> 转成 JSON String -> 写入 Host Response
                // 这样 Host 不需要认识这个对象，只需要传输它的 JSON 形式
                String jsonResult = pluginMapper.writeValueAsString(result);
                response.getWriter().write(jsonResult);
            }

        } catch (Exception e) {
            log.error("Plugin dispatch failed", e);
            // 这里可以做一个全局异常处理，把异常转成 JSON 返回
            response.sendError(500, "Plugin Error: " + e.getMessage());
        } finally {
            // 7. 还原现场
            Thread.currentThread().setContextClassLoader(originalCL);
        }
    }

    /**
     * 从插件容器中捞取 ObjectMapper
     */
    private ObjectMapper getPluginObjectMapper(WebInterfaceMetadata meta) {
        try {
            // targetBean 是插件里的对象，通过它可以拿到插件的 Class，进而操作插件的 Context
            // 这里假设我们能访问到插件的 ApplicationContext。
            // 实际上 WebInterfaceMetadata 里最好直接存一个 PluginContext 引用
            // 暂时用反射或者新实例兜底：

            // 最佳实践：meta 里应该持有一个 PluginContext 引用
            // 这里演示用 new，实际生产中应该从 meta.getPluginContext().getBean(ObjectMapper.class) 获取
            return new ObjectMapper();
        } catch (Exception e) {
            return fallbackMapper;
        }
    }

    // 简单的类型转换器
    private Object convert(String val, Class<?> type, ObjectMapper mapper) {
        if (val == null) return null;
        if (type == String.class) return val;
        // 借用 Jackson 做基础类型转换，它很擅长这个
        try {
            return mapper.convertValue(val, type);
        } catch (Exception e) {
            // 降级处理
            if (type == Integer.class || type == int.class) return Integer.valueOf(val);
            if (type == Long.class || type == long.class) return Long.valueOf(val);
            return val;
        }
    }
}