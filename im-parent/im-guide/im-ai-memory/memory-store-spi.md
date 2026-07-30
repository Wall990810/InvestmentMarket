# MemoryStore SPI 回顾

← [返回索引](../README.md)

本模块所有实现均实现自 [im-ai-core 的 MemoryStore 接口](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/memory/MemoryStore.java)。其方法契约如下：

```java
public interface MemoryStore {
    void store(String key, MemoryEntry entry);
    void storeAll(String key, List<MemoryEntry> entries);
    List<MemoryEntry> retrieve(String key);
    List<MemoryEntry> retrieveRecent(String key, int count);
    List<MemoryEntry> search(String key, String query);
    void clear(String key);
    String getStoreType();
}
```

- `key` 通常表示一个会话或 Agent 的记忆命名空间（例如 `session-123`）；
- `MemoryEntry`（见 [MemoryEntry.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/memory/MemoryEntry.java)）承载单条记忆，字段包括：`id`、`content`、`role`、`createdAt`（`Instant`）、`importance`（0.0~1.0）、`metadata`（`Map`）；
- `getStoreType()` 返回实现标识，三种实现分别返回 `"memory"` / `"db"` / `"redis"`。

后端选择由 [MemoryConfig](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/model/MemoryConfig.java) 中的 `shortTermStore` / `longTermStore` 字段决定（取值为 `"memory"` / `"redis"` / `"db"`），并配合 `shortTermMaxEntries`、`longTermMaxEntries`、`ttlSeconds` 控制容量与过期策略。