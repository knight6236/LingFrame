# 快速入门

本文档帮助你快速了解和使用 LingFrame。

## 环境准备

- JDK 21+
- Maven 3.8+

## 5 分钟 Hello World

### 1. 构建项目

```bash
git clone https://github.com/lingframe/lingframe.git
cd lingframe
mvn clean install -DskipTests
```

### 2. 启动示例应用

```bash
cd lingframe-examples/lingframe-example-host-app
mvn spring-boot:run
```

### 3. 测试插件服务

```bash
# 查询用户列表（user-plugin 提供的服务）
curl http://localhost:8888/user/listUsers

# 查询单个用户
curl "http://localhost:8888/user/queryUser?userId=1"
```

恭喜！你已经成功运行了第一个 LingFrame 应用！

---

## 核心概念

### 三层架构

```
┌─────────────────────────────────────────┐
│           Core（治理内核）               │
│   权限校验 · 审计记录 · 上下文隔离        │
└─────────────────┬───────────────────────┘
                  ▼
┌─────────────────────────────────────────┐
│      Infrastructure（基础设施层）        │
│          存储 · 缓存 · 消息              │
└─────────────────┬───────────────────────┘
                  ▼
┌─────────────────────────────────────────┐
│         Business（业务模块层）           │
│      用户中心 · 订单服务 · 支付模块       │
└─────────────────────────────────────────┘
```

### 关键原则

1. **零信任**：业务模块只能通过 Core 访问基础设施
2. **上下文隔离**：每个模块独立的 Spring 子上下文
3. **FQSID 路由**：服务通过 `pluginId:serviceId` 全局唯一标识

---

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

参考示例应用 `application.yaml`：

```yaml
server:
  port: 8888

lingframe:
  enabled: true
  dev-mode: true                    # 开发模式，权限不足时仅警告
  
  # 共享 API 预加载（支持目录/JAR/通配符）
  preload-api-jars:
    - lingframe-examples/lingframe-example-order-api
  
  # 插件目录
  plugin-home: plugins              # 生产模式：JAR 包目录
  plugin-roots:                     # 开发模式：源码目录
    - lingframe-examples/lingframe-example-plugin-order
    - lingframe-examples/lingframe-example-plugin-user
  
  # 宿主治理（可选）
  host-governance:
    enabled: false

# 高阶功能：可视化仪表盘（可选，默认关闭）
# dashboard:
#   enabled: true
```

### 4. 在宿主中调用插件服务

使用 `@LingReference` 自动注入插件服务。LingFrame 采用**消费者驱动契约**：

```java
// 宿主应用（消费者）定义它需要的接口
// 位置：host-api/.../OrderQueryService.java
public interface OrderQueryService {
    List<OrderDTO> findByUserId(Long userId);
}

// Order 插件（生产者）实现宿主定义的接口
// 位置：order-plugin/.../OrderQueryServiceImpl.java
@Component
public class OrderQueryServiceImpl implements OrderQueryService {
    @LingService(id = "find_orders_by_user", desc = "查询用户订单")
    @Override
    public List<OrderDTO> findByUserId(Long userId) {
        return orderRepository.findByUserId(userId);
    }
}

// 宿主应用使用自己定义的接口
@RestController
@RequiredArgsConstructor
public class OrderController {

    @LingReference
    private OrderQueryService orderQueryService;  // 框架自动路由到 Order 插件

    @GetMapping("/orders/user/{userId}")
    public List<OrderDTO> getUserOrders(@PathVariable Long userId) {
        return orderQueryService.findByUserId(userId);
    }
}
```

---

## 示例项目结构

```
lingframe-examples/
├── lingframe-example-host-app        # 宿主应用
├── lingframe-example-order-api       # 共享 API（消费者定义的接口）
├── lingframe-example-plugin-order    # 订单模块
└── lingframe-example-plugin-user     # 用户模块（提供 /user/* 接口）
```

## 创建业务模块

详见 [模块开发指南](plugin-development.md)

## 下一步

- [模块开发指南](plugin-development.md) - 学习如何开发业务模块
- [共享 API 设计规范](shared-api-guidelines.md) - API 设计最佳实践
- [Dashboard 可视化治理](dashboard.md) - 高阶可选功能
- [架构设计](architecture.md) - 深入了解治理原理
