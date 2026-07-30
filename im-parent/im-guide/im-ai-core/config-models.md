# ← 返回索引

# 配置模型详解

配置模型位于 `org.wall.im.ai.core.model` 包下，整体以 [AgentConfig](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/model/AgentConfig.java) 为根，可由 YAML 直接反序列化。

## AgentConfig

源码：[AgentConfig.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/model/AgentConfig.java)

| 字段 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `name` | `String` | - | 智能体唯一标识。 |
| `description` | `String` | - | 智能体描述。 |
| `type` | `String` | `"chat"` | 智能体类型，如 `chat` / `task` / `workflow`。 |
| `model` | `ModelConfig` | - | 使用的模型配置。 |
| `skills` | `List<String>` | `[]` | 技能名称列表（引用已注册技能）。 |
| `tools` | `List<String>` | `[]` | 工具名称列表（引用已注册工具）。 |
| `memory` | `MemoryConfig` | - | 记忆配置。 |
| `sandbox` | `SandboxConfig` | - | 沙盒配置。 |
| `monitor` | `MonitorConfig` | - | 监控配置。 |
| `execution` | `ExecutionConfig` | - | 执行环境配置。 |
| `properties` | `Map<String, Object>` | `{}` | 扩展属性，供下游实现自定义使用。 |

> `skills` 与 `tools` 字段存储的是名称字符串，而非对象引用——框架在运行时根据名称到 `AgentContext` 中解析为具体的 `Skill` / `Tool` 实例。

## ModelConfig

源码：[ModelConfig.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/model/ModelConfig.java)

| 字段 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `provider` | `String` | - | 模型提供者，如 `openai` / `azure` / `local`。 |
| `name` | `String` | - | 模型名称，如 `gpt-4` / `qwen-72b`。 |
| `apiKey` | `String` | - | API 密钥。 |
| `endpoint` | `String` | - | API 端点。 |
| `temperature` | `Double` | `0.7` | 温度参数。 |
| `maxTokens` | `Integer` | `4096` | 最大 Token 数。 |

## ExecutionConfig

源码：[ExecutionConfig.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/model/ExecutionConfig.java)

| 字段 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `maxConcurrency` | `int` | `10` | 最大并发数。 |
| `timeoutSeconds` | `int` | `60` | 超时时间（秒）。 |
| `retryCount` | `int` | `3` | 重试次数。 |
| `envVars` | `Map<String, String>` | `{}` | 环境变量。 |

## MemoryConfig

源码：[MemoryConfig.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/model/MemoryConfig.java)

| 字段 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `shortTermStore` | `String` | `"memory"` | 短期记忆存储类型：`memory` / `redis` / `db`。 |
| `longTermStore` | `String` | `"memory"` | 长期记忆存储类型：`memory` / `redis` / `db`。 |
| `shortTermMaxEntries` | `int` | `100` | 短期记忆最大条目数。 |
| `longTermMaxEntries` | `int` | `10000` | 长期记忆最大条目数。 |
| `ttlSeconds` | `long` | `0` | 记忆过期时间（秒），`0` 表示不过期。 |

> `shortTermStore` / `longTermStore` 的取值与 `MemoryStore.getStoreType()` 返回值对应，框架据此选择具体的 `MemoryStore` 实现。

## SandboxConfig

源码：[SandboxConfig.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/model/SandboxConfig.java)

| 字段 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `enabled` | `boolean` | `true` | 是否启用沙盒。 |
| `workDir` | `String` | - | 允许的工作目录。 |
| `allowedPaths` | `List<String>` | - | 允许访问的路径白名单。 |
| `networkAccess` | `boolean` | `false` | 是否允许网络访问。 |
| `maxExecutionTime` | `int` | `300` | 最大执行时间（秒）。 |
| `maxMemoryMb` | `long` | `512` | 最大内存限制（MB）。 |

## MonitorConfig

源码：[MonitorConfig.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/model/MonitorConfig.java)

| 字段 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `enabled` | `boolean` | `true` | 是否启用监控。 |
| `zipkinEndpoint` | `String` | `http://localhost:9411/api/v2/spans` | Zipkin 上报端点。 |
| `langfuse` | `LangfuseConfig` | - | Langfuse 配置。 |
| `customMetrics` | `Map<String, String>` | `{}` | 自定义指标配置。 |

嵌套静态类 `LangfuseConfig`：

| 字段 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `enabled` | `boolean` | `false` | 是否启用 Langfuse。 |
| `host` | `String` | `http://localhost:3000` | Langfuse 服务地址。 |
| `publicKey` | `String` | - | 公钥。 |
| `secretKey` | `String` | - | 私钥。 |
| `debug` | `boolean` | `false` | 是否启用调试日志。 |
| `flushIntervalMs` | `long` | `5000` | 刷新间隔（毫秒），`0` 表示不自动刷新。 |
| `maxBatchSize` | `int` | `50` | 批量上报最大条数。 |

`LangfuseConfig` 还提供便捷方法：

```java
public boolean isConfigured()   // publicKey 与 secretKey 均非空时返回 true
```