# 快速入门

本文档帮助你快速了解和使用 LingFrame。

## 环境准备

- JDK 21+（后续兼容 JDK 8）
- Maven 3.8+

## 构建框架

```bash
git clone https://github.com/lingframe/lingframe.git
cd lingframe
mvn clean install -DskipTests
```

## 运行示例

```bash
cd lingframe-examples/lingframe-example-host-app
mvn spring-boot:run
```

启动后，框架会自动扫描并加载配置的插件目录中的插件。

## 核心概念

### 三层架构

```
┌─────────────────────────────────────────┐
│           Core（仲裁核心）               │
│   生命周期管理 · 权限治理 · 上下文隔离    │
└─────────────────┬───────────────────────┘
                  ▼
┌─────────────────────────────────────────┐
│     Infrastructure Plugins（基础设施）   │
│          存储 · 缓存 · 消息              │
└─────────────────┬───────────────────────┘
                  ▼
┌─────────────────────────────────────────┐
│       Business Plugins（业务插件）       │
│      用户中心 · 订单服务 · 支付模块       │
└─────────────────────────────────────────┘
```

### 关键原则

1. **零信任**：业务插件只能通过 Core 访问基础设施
2. **上下文隔离**：每个插件独立的 Spring 子上下文
3. **FQSID 路由**：服务通过 `pluginId:serviceId` 全局唯一标识

## 创建宿主应用

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.lingframe</groupId>
    <artifactId>lingframe-spring-boot3-starter</artifactId>
    <version>${lingframe.version}</version>
</dependency>
```

### 2. 启动类

```java
@SpringBootApplication
public class HostApplication {
    public static void main(String[] args) {
        SpringApplication.run(HostApplication.class, args);
    }
}
```

### 3. 配置文件

在 `application.yaml` 中配置 LingFrame：

```yaml
lingframe:
  enabled: true               # 是否启用框架（默认 true）
  dev-mode: true              # 开发模式，权限不足时仅警告
  plugin-home: "plugins"      # 插件 JAR 包目录
  plugin-roots:               # 插件源码目录（开发模式）
    - "../my-plugin/target/classes"
  auto-scan: true             # 启动时自动扫描插件（默认 true）
  
  # 审计配置
  audit:
    enabled: true             # 开启审计（默认 true）
    log-console: true         # 控制台输出审计日志（默认 true）
    queue-size: 1000          # 异步队列大小（默认 1000）
  
  # 运行时配置
  runtime:
    max-history-snapshots: 5          # 最大历史快照数（默认 5）
    default-timeout: 3s               # 默认超时时间（默认 3s）
    bulkhead-max-concurrent: 10       # 最大并发数（默认 10）
    bulkhead-acquire-timeout: 3s      # 获取许可超时（默认 3s）
    force-cleanup-delay: 30s          # 强制清理延迟（默认 30s）
    dying-check-interval: 5s          # 死亡检查间隔（默认 5s）
  
  # 治理规则（可选）
  rules:
    - pattern: "com.example.*Service#delete*"
      permission: "order:delete"
      access: WRITE
      audit: true
      audit-action: "DANGEROUS_DELETE"
      timeout: 5s
```

### 4. 使用插件服务（推荐方式）

使用 `@LingReference` 注解自动注入插件服务：

```java
@RestController
@RequiredArgsConstructor
public class UserController {

    @LingReference
    private UserService userService;  // 自动注入用户服务

    @GetMapping("/users/{id}")
    public User getUser(@PathVariable String id) {
        return userService.queryUser(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }
}
```

`@LingReference` 支持以下配置：

```java
@LingReference(
    pluginId = "user-plugin",  // 可选：指定插件ID
    timeout = 5000             // 可选：超时时间（毫秒）
)
private UserService userService;
```

## 创建插件

详见 [插件开发指南](plugin-development.md)

## 下一步

- [插件开发指南](plugin-development.md) - 学习如何开发插件
- [架构设计](architecture.md) - 深入了解框架架构
- [API 参考](api-reference.md) - 查看完整 API 文档
