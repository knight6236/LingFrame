# 贡献指南

感谢你对 LingFrame 的关注！我们欢迎任何形式的贡献。

## 开发环境

### 环境要求

- JDK 21+
- Maven 3.8+
- IDE 推荐：IntelliJ IDEA

### 本地构建

```bash
# 克隆仓库
git clone https://github.com/lingframe/lingframe.git
cd lingframe

# 编译安装
mvn clean install

# 跳过测试
mvn clean install -DskipTests

# 构建特定模块
mvn clean install -pl lingframe-core -am

# 运行测试
mvn test

# 运行特定模块的测试
mvn test -pl lingframe-core
```

### 运行示例

```bash
cd lingframe-examples/lingframe-example-host-app
mvn spring-boot:run
```

## 贡献流程

### 1. 认领任务

- 查看 [Issues](../../issues) 中的待办任务
- 在 Issue 下留言表示你想认领
- 等待维护者分配

### 2. 开发

```bash
# Fork 仓库后克隆
git clone https://github.com/YOUR_USERNAME/lingframe.git

# 创建特性分支
git checkout -b feature/your-feature

# 开发并提交
git add .
git commit -m "feat: add your feature"

# 推送
git push origin feature/your-feature
```

### 3. 提交 PR

- 确保代码通过编译：`mvn clean compile`
- 确保测试通过：`mvn test`
- 提交 Pull Request，描述清楚改动内容

## 代码规范

### 命名约定

| 类型       | 约定                     | 示例                              |
| ---------- | ------------------------ | --------------------------------- |
| 接口       | 描述性名称               | `PluginContext`, `LingPlugin`     |
| 实现类     | `Default` 或 `Core` 前缀 | `DefaultPermissionService`        |
| 异常       | `Exception` 后缀         | `LingException`                   |
| 注解       | 描述性名称               | `@LingService`, `@LingReference`  |
| 代理类     | `Proxy` 后缀             | `SmartServiceProxy`, `GlobalServiceRoutingProxy` |
| 事件类     | `Event` 后缀             | `PluginStartedEvent`              |
| 配置类     | `Config` 或 `Properties` 后缀 | `LingFrameProperties`        |

### 核心类说明

| 类                          | 职责                   |
| --------------------------- | ---------------------- |
| `PluginManager`             | 插件安装/卸载/服务路由 |
| `PluginRuntime`             | 插件运行时环境         |
| `InstancePool`              | 蓝绿部署和版本管理     |
| `ServiceRegistry`           | 服务注册表             |
| `InvocationExecutor`        | 调用执行器             |
| `PluginLifecycleManager`    | 生命周期管理           |
| `GovernanceKernel`          | 治理内核               |
| `SmartServiceProxy`         | 智能服务代理           |
| `GlobalServiceRoutingProxy` | 全局服务路由代理       |
| `PluginClassLoader`         | 插件类加载器           |
| `GovernanceStrategy`        | 权限/审计智能推导      |

### 模块依赖

- 新增依赖版本在 `lingframe-dependencies/pom.xml` 中管理
- 各模块通过 BOM 引用版本，不要硬编码版本号
- 插件只依赖 `lingframe-api`，不要依赖 `lingframe-core`

### 代码风格

- 使用 4 空格缩进
- 类和方法添加 Javadoc 注释
- 使用 Lombok 减少样板代码
- 使用 SLF4J 进行日志记录

## 目录结构

```
lingframe/
├── lingframe-api/              # 契约层（接口、注解、异常）
├── lingframe-core/             # 仲裁内核（插件管理、治理、安全）
├── lingframe-runtime/          # 运行时集成
│   └── lingframe-spring-boot3-starter/  # Spring Boot 3.x 集成
├── lingframe-plugins-infra/    # 基础设施插件
│   ├── lingframe-plugin-storage/  # 数据库访问
│   └── lingframe-plugin-cache/    # 缓存访问
├── lingframe-examples/         # 示例
│   ├── lingframe-example-host-app/     # 宿主应用
│   ├── lingframe-example-plugin-user/  # 用户插件
│   └── lingframe-example-plugin-order/ # 订单插件
├── lingframe-dependencies/     # 依赖版本管理
└── lingframe-bom/              # 对外提供的 BOM
```

### 包命名约定

```
com.lingframe.api/          # 契约层
├── annotation/             # 框架注解 (@LingService, @LingReference, @RequiresPermission)
├── config/                 # 配置类 (GovernancePolicy, PluginDefinition)
├── context/                # 插件上下文接口 (PluginContext)
├── event/                  # 事件系统 (LingEvent, LingEventListener)
├── exception/              # 框架异常 (LingException, PermissionDeniedException)
├── plugin/                 # 插件生命周期接口 (LingPlugin)
└── security/               # 安全契约 (PermissionService, AccessType)

com.lingframe.core/         # 核心实现
├── audit/                  # 审计管理 (AuditManager)
├── classloader/            # 插件类加载隔离 (PluginClassLoader)
├── context/                # 插件上下文实现 (CorePluginContext)
├── dev/                    # 开发工具 (HotSwapWatcher)
├── event/                  # 事件总线实现 (EventBus)
├── governance/             # 治理仲裁 (GovernanceArbitrator)
├── kernel/                 # 核心内核 (GovernanceKernel)
├── plugin/                 # 插件管理 (PluginManager, PluginRuntime, InstancePool)
├── proxy/                  # 服务代理 (SmartServiceProxy, GlobalServiceRoutingProxy)
├── security/               # 安全实现 (DefaultPermissionService)
└── strategy/               # 治理策略 (GovernanceStrategy)
```

## 配置格式注意事项

### 宿主应用配置 (application.yaml)

配置使用 **kebab-case** 命名（短横线分隔）：

```yaml
lingframe:
  enabled: true
  dev-mode: true              # 不是 devMode
  plugin-home: "plugins"      # 不是 pluginHome
  plugin-roots:
    - "../my-plugin/target/classes"
  auto-scan: true
  
  audit:
    enabled: true
    log-console: true         # 不是 logConsole
    queue-size: 1000          # 不是 queueSize
  
  runtime:
    max-history-snapshots: 5
    default-timeout: 3s
    bulkhead-max-concurrent: 10
    force-cleanup-delay: 30s
    dying-check-interval: 5s
  
  rules:
    - pattern: "com.example.*Service#delete*"
      permission: "order:delete"
      access: WRITE
      audit: true
      audit-action: "DANGEROUS_DELETE"
      timeout: 5s
```

### 插件元数据 (plugin.yml)

属性直接在根节点定义，**没有** `plugin:` 根节点：

```yaml
# 正确格式
id: my-plugin
version: 1.0.0
provider: "My Company"
description: "我的插件"
mainClass: "com.example.MyPlugin"

governance:
  permissions:
    - methodPattern: "storage:sql"    # 不是 capability
      permissionId: "READ"            # 不是 access
  audits:
    - methodPattern: "com.example.*Service#delete*"
      action: "DELETE_OPERATION"
      enabled: true

properties:
  custom-config: "value"
```

## 提交信息规范

使用 [Conventional Commits](https://www.conventionalcommits.org/) 格式：

```
<type>: <description>

[optional body]
```

类型：

- `feat`: 新功能
- `fix`: 修复 Bug
- `docs`: 文档更新
- `refactor`: 重构
- `test`: 测试相关
- `chore`: 构建/工具相关

示例：

```
feat: add permission check for SQL execution
fix: fix classloader memory leak on plugin unload
docs: update quick start guide
refactor: extract InstancePool from PluginRuntime
```

## 服务调用方式

开发时请注意三种服务调用方式的优先级：

1. **@LingReference 注入**（强烈推荐）：最接近 Spring 原生体验
2. **PluginContext.getService()**：适合需要显式错误处理的场景
3. **PluginContext.invoke()**：适合松耦合场景，不需要接口依赖

```java
// 推荐方式
@LingReference
private UserService userService;

// 显式获取
Optional<UserService> service = context.getService(UserService.class);

// FQSID 调用
Optional<User> user = context.invoke("user-plugin:query_user", userId);
```

## 问题反馈

- **Bug 报告**：请在 Issues 中使用 Bug 模板
- **功能建议**：请在 Discussions 中讨论
- **安全问题**：请私信维护者，不要公开

## 行为准则

- 尊重每一位贡献者
- 保持友善和专业的交流
- 接受建设性的批评

感谢你的贡献！🎉
