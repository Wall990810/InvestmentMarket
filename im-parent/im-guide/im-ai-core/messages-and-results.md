# ← 返回索引

# Message / AgentResult / AgentsDefinition 详解

## AgentsDefinition

源码：[AgentsDefinition.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/config/AgentsDefinition.java)

`AgentsDefinition` 对应 `agents.yml` 配置文件的**根节点**，承载全局默认配置与 Agent 列表。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `defaults` | `AgentConfig` | 全局默认配置，单个 Agent 配置缺省时继承。 |
| `agents` | `List<AgentConfig>` | Agent 配置列表，默认空列表。 |

配合 `jackson-dataformat-yaml`，可一行加载：

```java
ObjectMapper mapper = new ObjectMapper(new YAMLLoaderFactory().createParser());
// 或使用 YAMLFactory
AgentsDefinition def = mapper.readValue(yamlFile, AgentsDefinition.class);
```

对应的 YAML 结构示例：

```yaml
defaults:
  type: chat
  model:
    provider: openai
    temperature: 0.7
    maxTokens: 4096
agents:
  - name: market-analyst
    description: 投资市场分析助手
    type: chat
    skills:
      - stock-lookup
    tools:
      - market-data-api
```

## Message

源码：[Message.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/model/Message.java)

对话消息模型，是 `Agent.execute(List<Message>)` 的入参单元，也是 `AgentResult.messageChain` 的元素类型。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `role` | `String` | 消息角色：`system` / `user` / `assistant` / `tool`。 |
| `content` | `String` | 消息内容。 |
| `name` | `String` | 消息名称（用于 tool 调用）。 |
| `timestamp` | `Instant` | 时间戳，构造时默认 `Instant.now()`。 |
| `metadata` | `Map<String, Object>` | 扩展元数据。 |
| `traceId` | `String` | 关联的 traceId。 |

提供两个构造方法与四个工厂方法：

```java
public Message()
public Message(String role, String content)

public static Message system(String content)
public static Message user(String content)
public static Message assistant(String content)
public static Message tool(String name, String content)   // role 固定为 "tool"
```

## AgentResult

源码：[AgentResult.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/model/AgentResult.java)

`Agent.execute(...)` 的返回类型，承载执行结果与可观测数据。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `success` | `boolean` | 是否成功。 |
| `output` | `String` | 输出内容。 |
| `costTimeMs` | `long` | 执行耗时（毫秒）。 |
| `tokenUsage` | `int` | 使用的 token 数。 |
| `messageChain` | `List<Message>` | 消息链路。 |
| `errorMessage` | `String` | 错误信息。 |
| `traceId` | `String` | 关联 traceId。 |

提供两个静态工厂方法以便快速构造：

```java
public static AgentResult success(String output)
public static AgentResult failure(String errorMessage)
```