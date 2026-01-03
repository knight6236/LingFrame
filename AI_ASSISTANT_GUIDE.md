# AI Assistant Guide

本文件为 AI 助手在 LingFrame 仓库中工作的指导说明。

## 项目概览

**LingFrame（灵珑）** 是面向长期运行系统的 JVM 运行时治理框架。

**核心理念**：让已经服役多年的单体应用，在不重写系统、不强行拆分微服务的前提下，继续稳定、可控、可演进地运行下去。

**当前版本**：v0.1.0-preview

**已实现功能**：
- ✅ 插件生命周期管理（安装、卸载、热重载）
- ✅ 蓝绿部署 + 引用计数
- ✅ Child-First 类加载隔离
- ✅ Spring 父子上下文隔离
- ✅ 权限治理（@RequiresPermission + 智能推导）
- ✅ 服务扩展（@LingService + FQSID 路由）
- ✅ 服务注入（@LingReference - 推荐方式）
- ✅ 审计日志（@Auditable + 异步记录）
- ✅ 开发模式热重载
- ✅ Spring Boot 3 Starter

## 技术栈

- **Java 21** (兼容 JDK 8)
- **Maven 3.8+**
- **Spring Boot 3.5.6**
- **Lombok 1.18.30**
- **ASM 9.6** (字节码分析)
- **Guava 33.4.8**
- **SnakeYAML 2.2**

## 常用命令

```bash
# 完整构建
mvn clean install

# 跳过测试的快速构建
mvn clean install -DskipTests

# 构建特定模块
mvn clean install -pl lingframe-core -am

# 运行测试
mvn test

# 运行特定模块的测试
mvn test -pl lingframe-core

# 运行示例
cd lingframe-examples/lingframe-example-host-app
mvn spring-boot:run
```

## 核心架构

### 三层架构

```
Core 层（仲裁核心）
  ├─ PluginManager - 插件管理
  ├─ PluginRuntime - 插件运行时
  ├─ InstancePool - 实例池（蓝绿部署）
  ├─ ServiceRegistry - 服务注册表
  ├─ GovernanceKernel - 治理内核
  └─ EventBus - 事件总线
         ▼
Infrastructure 层（基础设施）
  ├─ lingframe-plugin-storage - 数据库访问
  └─ lingframe-plugin-cache - 缓存访问（待实现）
         ▼
Business 层（业务插件）
  ├─ user-plugin - 用户管理
  └─ order-plugin - 订单处理
```

### 关键类

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

## 核心机制

### 1. 服务暴露（@LingService）

```java
@Component
public class UserServiceImpl implements UserService {
    
    @LingService(id = "query_user", desc = "根据ID查询用户")
    @Override
    public Optional<User> queryUser(String userId) {
        return userRepository.findById(userId);
    }
    
    @LingService(id = "create_user", desc = "创建新用户", timeout = 5000)
    @RequiresPermission("user:write")
    @Auditable(action = "CREATE_USER", resource = "user")
    @Override
    public User createUser(User user) {
        return userRepository.save(user);
    }
}
```

### 2. 服务调用（三种方式）

#### 方式一：@LingReference 注入（强烈推荐）

```java
@Component
public class OrderService {
    
    @LingReference
    private UserService userService;  // 自动注入
    
    @LingReference(pluginId = "user-plugin", timeout = 5000)
    private UserService userServiceV2;  // 指定插件和超时
    
    public Order createOrder(String userId) {
        User user = userService.queryUser(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return new Order(user);
    }
}
```

#### 方式二：PluginContext.getService()

```java
@Component
public class OrderService {
    
    @Autowired
    private PluginContext context;
    
    public Order createOrder(String userId) {
        Optional<UserService> userService = context.getService(UserService.class);
        User user = userService.get().queryUser(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return new Order(user);
    }
}
```

#### 方式三：PluginContext.invoke() FQSID 调用

```java
@Component
public class OrderService {
    
    @Autowired
    private PluginContext context;
    
    public Order createOrder(String userId) {
        Optional<User> user = context.invoke("user-plugin:query_user", userId);
        return new Order(user.get());
    }
}
```

### 3. 权限治理

**显式声明**：
```java
@RequiresPermission("user:export")
public void exportUsers() { ... }
```

**智能推导**：
- `get`, `find`, `query`, `list`, `select` → READ
- `create`, `save`, `insert`, `update`, `delete` → WRITE
- 其他 → EXECUTE

**开发模式**：
```yaml
lingframe:
  dev-mode: true  # 权限不足时仅警告
```

### 4. 插件生命周期

**安装插件**：
```java
// 生产模式：加载 JAR
pluginManager.install("my-plugin", "1.0.0", new File("plugins/my-plugin.jar"));

// 开发模式：加载编译目录
pluginManager.installDev("my-plugin", "dev", new File("target/classes"));
```

**蓝绿部署流程**：
1. 新版本启动（创建 PluginInstance）
2. 添加到 InstancePool
3. 原子切换默认实例
4. 旧版本进入死亡队列
5. 引用计数归零后销毁

**热重载**：
- `HotSwapWatcher` 监听 target/classes 变化
- 500ms 防抖延迟
- 自动触发重新加载

### 5. 类加载隔离

**Child-First 策略**：
- 优先加载插件自己的类
- 避免版本冲突

**白名单委派**（强制走父加载器）：
- `java.*`, `javax.*`, `jdk.*`, `sun.*`
- `com.lingframe.api.*` (契约层必须共享)
- `org.slf4j.*` (日志门面共享)

### 6. Spring 上下文隔离

- 每个插件独立的 Spring 子上下文
- 通过 `SpringContainerFactory` 创建
- 父上下文提供公共 Bean（PluginManager, PermissionService 等）

## 配置格式

### 宿主应用配置 (application.yaml)

```yaml
lingframe:
  enabled: true
  dev-mode: true
  plugin-home: "plugins"
  plugin-roots:
    - "../my-plugin/target/classes"
  auto-scan: true
  
  audit:
    enabled: true
    log-console: true
    queue-size: 1000
  
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
      timeout: 5s
```

### 插件元数据 (plugin.yml)

```yaml
id: my-plugin
version: 1.0.0
provider: "My Company"
description: "我的插件"
mainClass: "com.example.MyPlugin"

dependencies:
  - id: "base-plugin"
    minVersion: "1.0.0"

governance:
  permissions:
    - methodPattern: "storage:sql"
      permissionId: "READ"
    - methodPattern: "cache:redis"
      permissionId: "WRITE"
  
  audits:
    - methodPattern: "com.example.*Service#delete*"
      action: "DELETE_OPERATION"
      enabled: true

properties:
  custom-config: "value"
```

## 模块结构

```
lingframe/
├── lingframe-api/              # 契约层（接口、注解、异常）
├── lingframe-core/             # 仲裁内核（插件管理、治理、安全）
├── lingframe-runtime/          # 运行时集成
│   └── lingframe-spring-boot3-starter/  # Spring Boot 3.x 集成
├── lingframe-plugins-infra/    # 基础设施插件
│   ├── lingframe-plugin-storage/  # 数据库访问
│   └── lingframe-plugin-cache/    # 缓存访问（待实现）
├── lingframe-examples/         # 示例
│   ├── lingframe-example-host-app/     # 宿主应用
│   ├── lingframe-example-plugin-user/  # 用户插件
│   └── lingframe-example-plugin-order/ # 订单插件
├── lingframe-dependencies/     # 依赖版本管理
└── lingframe-bom/             # 对外提供的 BOM
```

## 架构原则

1. **零信任架构**：业务插件不能直接访问基础设施，所有访问必须经过 Core 仲裁
2. **微内核设计**：Core 只负责调度和仲裁，不包含业务逻辑
3. **契约优先**：所有交互通过 `lingframe-api` 接口
4. **上下文隔离**：每个插件独立的 Spring 子上下文和类加载器
5. **FQSID 路由**：服务通过 `pluginId:serviceId` 全局唯一标识
6. **蓝绿部署**：支持插件热更新，通过引用计数实现无缝版本切换

## 开发指南

### 创建插件

1. 依赖 `lingframe-api`（不要依赖 `lingframe-core`）
2. 实现 `LingPlugin` 接口
3. 创建 `plugin.yml` 元数据
4. 使用 `@LingService` 暴露服务
5. 使用 `@LingReference` 调用其他服务

### 测试插件

1. 配置 `plugin-roots` 指向 target/classes
2. 开启 `dev-mode: true`
3. 修改代码后重新编译
4. HotSwapWatcher 自动热重载

### 常见问题

**权限被拒绝**：
- 检查 `plugin.yml` 中的权限声明
- 开启 `dev-mode: true`

**@LingReference 注入失败**：
- 确保目标插件已启动
- 检查接口类型是否匹配
- 如果指定了 pluginId，确保插件ID正确

**热重载不生效**：
- 确保配置了 `dev-mode: true`
- 确保在 `plugin-roots` 中配置了 target/classes 目录
- 重新编译后等待 500ms

## 文档

- [快速入门](docs/getting-started.md) - 5分钟上手
- [插件开发指南](docs/plugin-development.md) - 详细开发指南
- [架构设计](docs/architecture.md) - 深入了解架构
- [API 参考](docs/api-reference.md) - 完整 API 文档
- [基础设施插件开发](docs/infrastructure-plugins.md) - 开发基础设施插件
- [路线图](docs/roadmap.md) - 未来规划

## 注意事项

1. **配置格式**：使用短横线命名（`dev-mode`），不是驼峰命名（`devMode`）
2. **插件元数据**：`plugin.yml` 直接在根节点定义属性，没有 `plugin:` 根节点
3. **权限配置**：使用 `methodPattern` 和 `permissionId`，不是 `capability` 和 `access`
4. **服务调用**：优先使用 `@LingReference`，其次 `getService()`，最后 `invoke()`
5. **类名**：使用 `PluginRuntime` 和 `InstancePool`，没有 `PluginSlot` 类
