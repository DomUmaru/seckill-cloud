# Seckill Cloud - 高并发微服务秒杀系统

![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023.0.0-blue) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.1-green) ![RocketMQ](https://img.shields.io/badge/RocketMQ-2.3.0-orange) ![Nacos](https://img.shields.io/badge/Nacos-Discovery-blue)

本项目是一个基于 **Spring Cloud Alibaba** 微服务架构的高并发秒杀系统，涵盖了**高并发**、**高可用**、**分布式事务**等核心场景。通过 **RocketMQ 削峰填谷**、**Redis 缓存预热**、**Redisson 分布式锁**、**Sentinel 限流熔断** 等技术手段，实现了能够应对瞬时高流量的健壮系统。

---

## ?? 架构设计

### 技术栈

| 类别 | 技术组件 | 说明 |
| :--- | :--- | :--- |
| **核心框架** | Spring Boot 3.2.1 | 基础应用框架 |
| **微服务治理** | Spring Cloud Alibaba 2023.0.1 | 微服务全家桶 |
| **注册/配置中心** | Nacos | 服务注册与发现 |
| **流量控制** | Sentinel | 熔断降级、热点参数限流 |
| **网关** | Spring Cloud Gateway | 统一入口、鉴权过滤器 |
| **消息队列** | RocketMQ | 异步解耦、流量削峰、分布式事务 |
| **缓存/锁** | Redis + Redisson | 库存预热、分布式锁、防重 |
| **数据库** | MySQL 8.0 + MyBatis | 持久化存储 |
| **搜索引擎** | ElasticSearch (Optional) | 商品搜索服务 |
| **认证授权** | JWT (JSON Web Token) | 无状态身份认证 |

### 模块划分

```text
seckill-cloud
├── seckill-gateway    # [网关服务] 统一入口、全局鉴权 (AuthGlobalFilter)
├── seckill-user       # [用户服务] 用户注册、登录、JWT 签发
├── seckill-service    # [秒杀核心服务] 秒杀接口、库存扣减、MQ 消息生产与消费
├── seckill-search     # [搜索服务] 商品搜索 (ES 集成)
└── seckill-common     # [公共模块] 全局异常、统一结果、工具类 (MD5, JWT)
```

---

## ? 核心业务流程

### 1. 秒杀全流程 (Seckill Flow)

系统采用了 **异步下单** 的设计模式，将秒杀请求的“处理”与“落库”分离，最大程度提高吞吐量。

1.  **秒杀地址获取 (接口隐藏)**
    *   为了防止脚本刷单，用户在秒杀开始前需请求 `/seckill/path` 接口。
    *   后端校验用户登录状态，结合 `userId`、`goodsId` 和 `timestamp` 生成 **MD5 签名** 返回给前端。
    *   前端后续请求必须携带此签名。

2.  **秒杀请求处理 (SeckillController)**
    *   **签名校验**：验证 MD5 签名是否合法，防止链接被篡改。
    *   **时效性校验**：校验请求时间戳是否在允许的误差范围内（如 60s）。
    *   **分布式锁 (Redisson)**：基于 `userId` 加锁，确保 **一人一单**。
    *   **库存预检 (Redis)**：直接在 Redis 中扣减库存（原子操作），如果库存不足直接返回，**阻挡绝大部分流量打到数据库**。
    *   **防重判断 (Redis)**：检查 Redis 中是否已有该用户的购买记录。

3.  **异步下单 (RocketMQ)**
    *   如果 Redis 预扣成功，发送 **RocketMQ 事务消息**。
    *   **本地事务 (SeckillTransactionListener)**：在本地事务中再次确认 Redis 库存和购买记录，确保消息发送与本地执行的原子性。
    *   **削峰填谷**：MQ 堆积海量请求，消费者端慢慢消费，保护数据库不被打挂。

4.  **订单落库 (SeckillConsumer)**
    *   监听 `seckill-topic`。
    *   **扣减数据库库存**：执行 `UPDATE seckill_goods SET stock_count = stock_count - 1 ...`。
    *   **创建订单**：插入订单记录。
    *   **幂等性保障**：通过数据库唯一索引捕获 `DuplicateKeyException`，防止消息重复消费导致的重复下单。

---

## ?? 关键代码解析

### 1. RocketMQ 事务消息保证最终一致性
使用 `sendMessageInTransaction` 发送半消息，确保本地 Redis 操作成功后才投递消息给消费者。

```java
// SeckillController.java
rocketMQTemplate.sendMessageInTransaction("seckill-topic", message, new Object[]{userId, goodsId, stockKey, boughtKey});
```

### 2. Redisson 分布式锁
解决集群环境下的“一人一单”并发安全问题。

```java
// SeckillController.java
RLock lock = redissonClient.getLock("lock:user:" + userId);
if (lock.tryLock(0, 10, TimeUnit.SECONDS)) {
    try {
        // 执行业务...
    } finally {
        lock.unlock();
    }
}
```

### 3. 库存预热 (StockWarmup)
项目启动时实现 `CommandLineRunner` 接口，自动将数据库中的秒杀商品库存加载到 Redis。

```java
// StockWarmup.java
redisTemplate.opsForValue().set("seckill:stock:" + goods.getId(), goods.getStockCount());
```

---

## ? 快速开始

### 前置要求
*   JDK 17+
*   Maven 3.8+
*   MySQL 8.0
*   Redis 6.0+
*   RocketMQ 4.9+ / 5.0
*   Nacos 2.x

### 启动步骤

1.  **启动基础设施**
    *   启动 Nacos Server。
    *   启动 RocketMQ NameServer 和 Broker。
    *   启动 Redis。

2.  **配置数据库**
    *   创建数据库 `seckill`。
    *   导入 SQL 脚本（如果有）。
    *   修改各模块 `application.yml` 中的数据库连接信息。

3.  **启动微服务**
    *   启动 `SeckillUserApplication`。
    *   启动 `SeckillServiceApplication`。
    *   启动 `GatewayApplication`。

4.  **测试接口**
    *   登录获取 Token。
    *   调用 `/seckill/path` 获取签名。
    *   调用 `/seckill/{sign}/seckill` 进行秒杀。

---

## ? 开发计划 & 待优化项

*   [x] 基础秒杀流程
*   [x] 集成 RocketMQ 削峰
*   [x] 集成 Sentinel 限流
*   [ ] 引入 ElasticSearch 实现商品搜索
*   [ ] 增加验证码机制进一步防刷
*   [ ] 增加多级缓存 (Caffeine + Redis)
