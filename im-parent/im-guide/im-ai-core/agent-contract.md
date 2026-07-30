# ← 返回索引

# Agent / AgentContext 接口详解

## Agent —— 智能体核心接口

源码：[Agent.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/agent/Agent.java)

`Agent` 是整个框架的顶层契约，所有智能体实现必须遵循此接口。它定义了智能体的生命周期与两种交互方式（单轮对话 / 多轮任务执行）。

| 方法签名 | 说明 |
| --- | --- |
| `String getName()` | 获取 Agent 名称（唯一标识）。 |
| `AgentConfig getConfig()` | 获取 Agent 对应的配置对象。 |
| `void initialize()` | 初始化 Agent（如加载模型、建立连接等）。 |
| `String chat(String input)` | 单轮对话：接收用户输入，返回 Agent 回复字符串。 |
| `AgentResult execute(List<Message> messages)` | 多轮任务执行：接收消息列表，返回结构化执行结果 `AgentResult`。 |
| `void reset()` | 重置 Agent 状态（清空上下文等）。 |
| `void destroy()` | 销毁 Agent，释放资源。 |

典型生命周期为：`initialize()` → 多次 `chat()` / `execute()` → `reset()`（可选）→ `destroy()`。

## AgentContext —— 执行上下文

源码：[AgentContext.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/agent/AgentContext.java)

`AgentContext` 封装 Agent 运行时所需的全部依赖与状态，包括关联的 Agent、已注册的技能 / 工具、短期 / 长期记忆以及上下文变量。

构造方法：

```java
public AgentContext(Agent agent)
```

核心方法：

| 方法签名 | 说明 |
| --- | --- |
| `void registerSkill(Skill skill)` | 按 `skill.getName()` 注册技能。 |
| `void registerTool(Tool tool)` | 按 `tool.getName()` 注册工具。 |
| `Skill getSkill(String name)` | 按名称获取技能。 |
| `Tool getTool(String name)` | 按名称获取工具。 |
| `void setVariable(String key, Object value)` | 设置上下文变量。 |
| `<T> T getVariable(String key)` | 获取上下文变量（泛型强转，调用方需自行保证类型安全）。 |

属性访问器：`getAgent()`、`getSkills()`、`getTools()`、`getShortTermMemory()` / `setShortTermMemory(MemoryStore)`、`getLongTermMemory()` / `setLongTermMemory(MemoryStore)`、`getVariables()`。

> 技能与工具均以 `Map<String, ?>` 形式按名称索引；短期记忆（`shortTermMemory`）与长期记忆（`longTermMemory`）均为 `MemoryStore` 类型，初始为 `null`，需由外部注入。