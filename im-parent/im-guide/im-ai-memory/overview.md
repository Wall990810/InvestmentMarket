# 模块概述

← [返回索引](../README.md)

## 模块概述

`im-ai-memory` 是 InvestmentMarket AI 体系中的**可插拔记忆存储后端模块**，为 Agent 提供短期记忆（short-term memory）与长期记忆（long-term memory）的统一存储能力。

本模块本身不定义记忆抽象，而是对 [im-ai-core](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core) 中的 `MemoryStore` SPI 提供三种开箱即用的实现：

- `InMemoryStore` —— 基于 JVM 内存，适用于开发测试与短期记忆；
- `JdbcMemoryStore` —— 基于关系型数据库，适用于需要持久化的长期记忆；
- `RedisMemoryStore` —— 基于 Redis List，适用于分布式共享与带 TTL 过期的记忆场景。

并通过 `DefaultMemoryStoreFactory` 按 `storeType` 字符串动态选择实现，便于在运行时切换后端。

模块源码位于：
[im-ai-memory/src/main/java/org/wall/im/ai/memory](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-memory/src/main/java/org/wall/im/ai/memory)

---

## Maven 坐标与依赖

模块坐标（继承自 `im-ai` 父模块，版本通过 `${revision}` 管理）：

```xml
<dependency>
    <groupId>org.wall.im</groupId>
    <artifactId>im-ai-memory</artifactId>
</dependency>
```

依据 [pom.xml](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-memory/pom.xml)，本模块的依赖关系如下：

| 依赖 | 说明 | 是否可选 |
| --- | --- | --- |
| `im-ai-core` | 提供 `MemoryStore` / `MemoryEntry` / `MemoryConfig` 等核心抽象 | 必选 |
| `im-ai-agent` | 提供 `MemoryStoreFactory` 工厂 SPI | 必选 |
| `spring-boot-starter-data-redis` | Redis 实现所需，对应 `RedisMemoryStore` | **optional** |
| `spring-boot-starter-jdbc` | JDBC 实现所需，对应 `JdbcMemoryStore`，需配合 `JdbcTemplate` | **optional** |
| `junit-jupiter` | 单元测试 | test |

> 由于 Redis 与 JDBC 依赖均标记为 `optional=true`，引入本模块后**不会**自动传递这两个 starter。仅当实际使用对应实现时，才需在应用层显式引入相应的 starter。