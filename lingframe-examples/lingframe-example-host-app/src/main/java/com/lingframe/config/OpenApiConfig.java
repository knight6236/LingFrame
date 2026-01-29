package com.lingframe.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springdoc.core.models.GroupedOpenApi;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI lingFrameOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("LingFrame Example API")
                        .description("LingFrame 示例应用 API 文档")
                        .version("1.0"));
    }

    @Bean
    public GroupedOpenApi coreApi() {
        return GroupedOpenApi.builder()
                .group("01. Core (Dashboard)")
                .pathsToMatch("/**/dashboard/**")
                .build();
    }

    @Bean
    public GroupedOpenApi hostApi() {
        return GroupedOpenApi.builder()
                .group("02. Host Application")
                .packagesToScan("com.lingframe")
                // 排除 Core 和 Plugin 的路径
                .pathsToExclude("/**/dashboard/**", "/*-plugin/**")
                .build();
    }

    @Bean
    public GroupedOpenApi pluginApi() {
        return GroupedOpenApi.builder()
                .group("03. Plugins")
                // 匹配所有以 -plugin 结尾的第一级路径
                .pathsToMatch("/*-plugin/**")
                .build();
    }

}
