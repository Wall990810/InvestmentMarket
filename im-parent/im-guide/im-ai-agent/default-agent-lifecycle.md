# DefaultAgent 运行时与生命周期

← 返回 [索引](../README.md)

## 3. Agent 运行时与生命周期

### 3.1 DefaultAgent —— ReAct 推理流程

[DefaultAgent.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/java/org/wall/im/ai/agent/lifecycle/DefaultAgent.java) 是 `Agent` 接口的默认实现，内部委托给 Spring AI Alibaba 的 `com.alibaba.cloud.ai.graph.agent.ReactAgent`。

**构造方法：**

```java
public DefaultAgent(AgentConfig config, ChatModel chatModel, List<Tool> tools)
```

- `config`：Agent 配置（名称、描述、执行参数等）。
- `chatModel`：Spring AI `ChatModel` 实例（例如 `DashScopeChatModel`）。
- `tools`：该 Agent 可使用的自定义工具列表（允许为 `null`）。

**`initialize()` 流程：**

1. 幂等保护：已初始化则仅打印 warn 日志并返回。
2. 构建系统提示词：优先取 `config.getDescription()`，否则回退为 `"You are a helpful AI assistant named {name}."`。
3. 使用 `ReactAgent.builder()` 装配 `.name(...)`、`.model(chatModel)`、`.instruction(systemPrompt)`。
4. 若 `agentTools` 非空，通过 `SpringAiToolAdapter.toToolCallbacks(...)` 转换后调用 `builder.tools(toolCallbacks)`。
5. 计算最大迭代次数 `maxIterations`：
   - 默认 `10`；
   - 当 `config.getExecution().getMaxConcurrency() > 0` 时，取 `min(maxConcurrency, 20)`；
   - 通过 `CompileConfig.builder().recursionLimit(maxIterations)` 设置到 `ReactAgent`。
6. `build()` 生成 `ReactAgent` 实例并标记 `initialized = true`。

**`chat(String input)` 流程：**

1. 若未初始化，抛出 `IllegalStateException`。
2. 若存在短期记忆，将用户输入包装为 `MemoryEntry`（role=`user`）存入 key `{agentName}:conversation`。
3. 调用 `reactAgent.call(input)`，取响应文本 `response.getText()`；异常被捕获并返回 `"Agent执行异常: " + e.getMessage()`。
4. 若存在长期记忆，将 `Q: {input} A: {result}` 作为 `MemoryEntry`（role=`assistant`，importance=`0.5`）存入 key `{agentName}:history`。

**`execute(List<Message> messages)` 流程：**

遍历消息链，对每条 `role == "user"` 的消息调用 `chat(content)`，拼接输出；最终封装为 `AgentResult`（含 `success`、`output`、`errorMessage`、`costTimeMs`、`messageChain`）。

**其余方法：**

- `getName()` / `getConfig()`：返回配置信息。
- `reset()`：清空短期记忆 key `{agentName}:conversation`。
- `destroy()`：先 `reset()`，再清空长期记忆 key `{agentName}:history`，置空 `reactAgent`，`initialized = false`。
- `getContext()`：返回内部 `AgentContext`（含 skills、short/long term memory）。
- `getReactAgent()`：暴露底层 `ReactAgent`，用于高级场景直接操作。

> ReAct 范式说明：`ReactAgent` 会自动进行 **推理 (Reason) → 调用工具 (Act) → 观察结果 (Observe) → 循环**，直到得出最终答案或触达 `recursionLimit`。

### 3.2 AgentFactory —— 装配工厂

[AgentFactory.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/java/org/wall/im/ai/agent/lifecycle/AgentFactory.java) 负责按 `AgentConfig` 把各组件组装成一个可运行的 `Agent`。

**构造方法：**

```java
public AgentFactory(SkillRegistry skillRegistry,
                    ToolRegistry toolRegistry,
                    MemoryStoreFactory memoryStoreFactory,
                    ChatModel chatModel)
```

**`create(AgentConfig config)` 流程：**

1. 遍历 `config.getTools()`，从 `ToolRegistry` 按 name 查找；找不到仅打印 warn 日志，跳过。
2. 以 `(config, chatModel, tools)` 创建 `DefaultAgent`。
3. 遍历 `config.getSkills()`，从 `SkillRegistry` 按 name 查找，通过 `agent.getContext().registerSkill(skill)` 装配到上下文。
4. 若 `config.getMemory()` 非空，分别用 `memoryStoreFactory.create(shortTermStore)` / `create(longTermStore)` 创建短期与长期记忆存储，注入到 `AgentContext`。

### 3.3 AgentLifecycleManager —— 生命周期管理

[AgentLifecycleManager.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/java/org/wall/im/ai/agent/lifecycle/AgentLifecycleManager.java) 负责批量创建、启停、销毁 Agent，并向监听器广播事件。

**构造方法：**

```java
public AgentLifecycleManager(AgentRegistry registry, AgentFactory factory)
```

**核心方法：**

| 方法 | 行为 |
| --- | --- |
| `addListener(AgentLifecycleListener)` | 注册生命周期监听器 |
| `createAgents(AgentsDefinition)` | 遍历配置，对每个 Agent 执行 `factory.create` → `agent.initialize()` → `registry.register()`，并 `notifyCreated`；失败时 `notifyError`，不影响后续 Agent |
| `startAll()` | 对所有已注册 Agent 触发 `notifyStarted` |
| `stopAll()` | 对所有已注册 Agent 调用 `agent.reset()` 并 `notifyStopped` |
| `destroyAll()` | 对所有 Agent 调用 `agent.destroy()` 并 `notifyDestroyed`，最后 `registry.destroyAll()` 清空注册表 |

### 3.4 AgentLifecycleListener —— 生命周期监听

[AgentLifecycleListener.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/java/org/wall/im/ai/agent/lifecycle/AgentLifecycleListener.java) 是一个接口，所有方法均有默认空实现，可按需覆写：

```java
public interface AgentLifecycleListener {
    default void onCreated(Agent agent) {}
    default void onStarted(Agent agent) {}
    default void onStopped(Agent agent) {}
    default void onDestroyed(Agent agent) {}
    default void onError(String agentName, Exception e) {}
}
```

典型用途：监控指标上报、日志审计、创建失败告警等。