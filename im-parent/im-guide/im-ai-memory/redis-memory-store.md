# RedisMemoryStore 详解

← [返回索引](../README.md)

源码：[RedisMemoryStore.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-memory/src/main/java/org/wall/im/ai/memory/store/RedisMemoryStore.java)、[RedisOperationsAdapter.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-memory/src/main/java/org/wall/im/ai/memory/store/RedisOperationsAdapter.java)

**用途**：基于 Redis List 结构存储记忆条目，天然支持多实例共享、持久化与 TTL 过期，适合生产环境下的短期/会话级记忆。

**Key 设计**：

- 统一前缀 `ai:memory:`，最终 Redis Key 为 `ai:memory:{key}`（`buildKey` 方法拼接）。

**TTL 与容量控制**：

- 构造参数 `ttlSeconds`：大于 0 时，每次写入后对 Key 调用 `expire` 设置过期；为 0 表示不过期。
- `maxEntries`：每次写入后调用 `listTrim(key, -maxEntries, -1)`，仅保留最近 `maxEntries` 条（List 尾部为最新）。

**序列化协议**：`serialize` 将一条 `MemoryEntry` 拼接为字符串 `id|role|importance|content`；`deserialize` 使用 `split("\\|", 4)` 限定最多切 4 段，从而允许 `content` 自身包含 `|`。若拆分不足 4 段，则将整串作为 `content` 兜底。

> 注意：该序列化是简易文本拼接，**不会**保存 `createdAt` 与 `metadata`。若需要保留这些字段，请替换序列化逻辑（例如改用 JSON）。

**RedisOperationsAdapter 适配器**：

`RedisMemoryStore` 不直接依赖 `StringRedisTemplate`，而是通过 [RedisOperationsAdapter](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-memory/src/main/java/org/wall/im/ai/memory/store/RedisOperationsAdapter.java) 接口隔离，便于测试与替换底层客户端。接口方法如下：

```java
public interface RedisOperationsAdapter {
    void listRightPush(String key, String value);
    void listRightPushAll(String key, List<String> values);
    List<String> listRange(String key, long start, long end);
    void listTrim(String key, long start, long end);
    void expire(String key, long seconds);
    void delete(String key);
}
```

应用层只需实现该适配器（例如用 `StringRedisTemplate` 的 `opsForList()` 转发），即可接入 `RedisMemoryStore`。`getStoreType()` 返回 `"redis"`。