# ← 返回索引

# MemoryStore / MemoryEntry 详解

## MemoryStore —— 记忆存储抽象

源码：[MemoryStore.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/memory/MemoryStore.java)

记忆存储统一抽象，同时支撑短期记忆与长期记忆。所有方法均以 `key`（记忆键，通常为会话 ID 或 Agent 实例 ID）作为隔离维度。

| 方法签名 | 说明 |
| --- | --- |
| `void store(String key, MemoryEntry entry)` | 存储单条记忆。 |
| `void storeAll(String key, List<MemoryEntry> entries)` | 批量存储记忆。 |
| `List<MemoryEntry> retrieve(String key)` | 取回该 key 下全部记忆。 |
| `List<MemoryEntry> retrieveRecent(String key, int count)` | 取回最近 N 条记忆。 |
| `List<MemoryEntry> search(String key, String query)` | 按关键词搜索记忆。 |
| `void clear(String key)` | 清空该 key 下所有记忆。 |
| `String getStoreType()` | 返回存储类型标识（如 `memory` / `redis` / `db`，与 `MemoryConfig` 中的 store 类型对应）。 |

## MemoryEntry —— 记忆条目

源码：[MemoryEntry.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/memory/MemoryEntry.java)

`MemoryStore` 中存储的基本单元。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `String` | 唯一 ID。 |
| `content` | `String` | 记忆内容。 |
| `role` | `String` | 角色（与 `Message.role` 语义一致）。 |
| `createdAt` | `Instant` | 创建时间，构造时默认 `Instant.now()`。 |
| `importance` | `double` | 重要性评分，范围 `0.0 ~ 1.0`。 |
| `metadata` | `Map<String, Object>` | 扩展元数据。 |

构造方法：

```java
public MemoryEntry()                                   // createdAt = now
public MemoryEntry(String id, String content, String role)  // createdAt = now, importance = 0.5
```