# 注册中心详解

← 返回 [索引](../README.md)

## 4. 注册中心

本模块提供四个注册中心/工厂，均使用 `ConcurrentHashMap` 保证线程安全（`AgentRegistry` 的查询返回不可变视图或 `Optional`）。

### 4.1 AgentRegistry

[AgentRegistry.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/java/org/wall/im/ai/agent/registry/AgentRegistry.java)

| 方法 | 说明 |
| --- | --- |
| `register(Agent)` | 注册 Agent；若同名已存在，先 `destroy()` 旧实例再替换 |
| `get(String name)` | 返回 `Optional<Agent>` |
| `getRequired(String name)` | 不存在则抛 `IllegalArgumentException` |
| `getAll()` | 返回不可变 `Collection<Agent>` |
| `contains(String name)` | 是否已注册 |
| `unregister(String name)` | 移除并 `destroy()` 对应实例 |
| `destroyAll()` | 销毁全部并清空 |
| `size()` | 已注册数量 |

### 4.2 SkillRegistry

[SkillRegistry.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/java/org/wall/im/ai/agent/lifecycle/SkillRegistry.java)

```java
skillRegistry.register(skill);     // 按 skill.getName() 注册
Skill s = skillRegistry.get(name); // 查找
skillRegistry.getAll();            // Collection<Skill>
skillRegistry.unregister(name);    // 移除
```

### 4.3 ToolRegistry

[ToolRegistry.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/java/org/wall/im/ai/agent/lifecycle/ToolRegistry.java) 的 API 与 `SkillRegistry` 完全对称，只是元素类型为 `Tool`。

### 4.4 MemoryStoreFactory

[MemoryStoreFactory.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/java/org/wall/im/ai/agent/lifecycle/MemoryStoreFactory.java) 是一个接口：

```java
public interface MemoryStoreFactory {
    /**
     * @param storeType 存储类型: memory, redis, db
     */
    MemoryStore create(String storeType);
}
```

`AgentFactory` 在装配时会调用 `create(config.getMemory().getShortTermStore())` 与 `create(config.getMemory().getLongTermStore())`。具体实现由使用方提供（本模块未提供默认实现类）。