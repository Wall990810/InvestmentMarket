# 典型使用示例与扩展自定义存储

← [返回索引](../README.md)

## 扩展自定义存储

如需接入其它后端（例如向量库、MongoDB），扩展步骤如下：

1. 实现 [MemoryStore](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/memory/MemoryStore.java) 接口，并实现全部 7 个方法，`getStoreType()` 返回一个自定义标识（如 `"vector"`）；
2. 选择注册方式之一：
   - **工厂方式**：扩展或替换 `DefaultMemoryStoreFactory`，在 `switch` 中新增对应分支；也可直接实现 [MemoryStoreFactory](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/java/org/wall/im/ai/agent/lifecycle/MemoryStoreFactory.java)，在 `create` 中根据 `storeType` 返回自定义实现；
3. 在配置中将 `short-term-store` / `long-term-store` 指向自定义标识。

实现时建议遵循现有实现的约定：超容量时裁剪旧条目、`retrieve` 返回不可变副本、`getStoreType()` 返回值与配置字符串一致。

---

## 典型使用示例

以下示例演示一个 Agent 在处理一轮对话后，将用户消息与助手回复写入短期记忆，并检索最近若干条作为上下文：

```java
// 1. 通过工厂获取短期记忆存储
MemoryStore shortTerm = memoryStoreFactory.create(memoryConfig.getShortTermStore());

String sessionKey = "session-" + sessionId;

// 2. 写入用户输入
MemoryEntry userEntry = new MemoryEntry(UUID.randomUUID().toString(), userMessage, "user");
shortTerm.store(sessionKey, userEntry);

// 3. 写入助手回复
MemoryEntry assistantEntry = new MemoryEntry(UUID.randomUUID().toString(), assistantMessage, "assistant");
shortTerm.store(sessionKey, assistantEntry);

// 4. 取最近 10 条作为上下文
List<MemoryEntry> recent = shortTerm.retrieveRecent(sessionKey, 10);

// 5. 关键词搜索
List<MemoryEntry> hits = shortTerm.search(sessionKey, "止损");

// 6. 会话结束时清理
shortTerm.clear(sessionKey);
```

如需切换为持久化后端，只需把 `memoryConfig.getShortTermStore()` 改为 `"db"` 或 `"redis"`（并确保对应依赖与 Bean 已配置），业务代码无需改动。