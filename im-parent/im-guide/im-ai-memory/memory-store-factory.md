# DefaultMemoryStoreFactory 详解

← [返回索引](../README.md)

源码：[DefaultMemoryStoreFactory.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-memory/src/main/java/org/wall/im/ai/memory/factory/DefaultMemoryStoreFactory.java)

`DefaultMemoryStoreFactory` 实现 [im-ai-agent 的 MemoryStoreFactory](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/java/org/wall/im/ai/agent/lifecycle/MemoryStoreFactory.java) 接口，是模块对外暴露的统一创建入口。

**构造参数**：

```java
public DefaultMemoryStoreFactory(RedisOperationsAdapter redisAdapter,
                                 JdbcTemplate jdbcTemplate,
                                 int defaultMaxEntries,
                                 long defaultTtlSeconds)
```

- `redisAdapter`、`jdbcTemplate` 可为 `null`（当不使用对应后端时）；
- `defaultMaxEntries` 与 `defaultTtlSeconds` 作为创建实例时的默认容量与 TTL。

**选择逻辑**：`create(String storeType)` 对入参做 `toLowerCase()` 后使用 `switch` 分发：

| storeType | 产物 | 缺失依赖时的行为 |
| --- | --- | --- |
| `"memory"` | `new InMemoryStore(defaultMaxEntries)` | 无外部依赖，始终可用 |
| `"redis"` | `new RedisMemoryStore(redisAdapter, defaultMaxEntries, defaultTtlSeconds)` | `redisAdapter == null` 抛 `IllegalStateException("Redis adapter not configured")` |
| `"db"` | `new JdbcMemoryStore(jdbcTemplate, defaultMaxEntries)` | `jdbcTemplate == null` 抛 `IllegalStateException("JdbcTemplate not configured")` |
| 其它 | —— | 抛 `IllegalArgumentException("Unknown store type: " + storeType)` |

**扩展点**：

- 工厂方法只接收 `storeType` 字符串，调用方通常结合 [MemoryConfig](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/model/MemoryConfig.java) 的 `shortTermStore` / `longTermStore` 取值传入；
- 如需为短期/长期记忆设置不同容量，可在调用方层面分别构造两个工厂实例（传入不同 `defaultMaxEntries`），或自定义新的工厂实现。