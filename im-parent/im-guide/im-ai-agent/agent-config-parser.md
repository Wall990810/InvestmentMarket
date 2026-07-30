# AgentConfigParser 详解

← 返回 [索引](../README.md)

## 5. 配置解析

[AgentConfigParser.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/java/org/wall/im/ai/agent/config/AgentConfigParser.java) 负责把 YAML 形式的 Agent 配置反序列化为 `AgentsDefinition`。

### 5.1 输入与产出

- **输入**：YAML 文件 / classpath 资源 / YAML 字符串。
- **输出**：`org.wall.im.ai.core.config.AgentsDefinition`，包含 `defaults`（公共默认配置）与 `agents`（`List<AgentConfig>`）。
- **底层**：Jackson `ObjectMapper(new YAMLFactory())`，已关闭 `FAIL_ON_UNKNOWN_PROPERTIES`，可向前兼容未识别字段。

### 5.2 解析方法

| 方法 | 入参 | 说明 |
| --- | --- | --- |
| `parseFromClasspath(String resourcePath)` | classpath 路径 | 通过当前 `ClassLoader` 读取资源；找不到抛 `IllegalArgumentException`，解析失败抛 `RuntimeException` |
| `parseFromFile(String filePath)` | 文件系统路径 | 文件不存在抛 `IllegalArgumentException` |
| `parseFromString(String yamlContent)` | YAML 字符串 | 直接解析 |
| `static applyDefaults(AgentsDefinition)` | 解析后的定义 | 将 `defaults` 中的 `model`/`memory`/`sandbox`/`monitor`/`execution` 合并到那些**对应字段为 null** 的 Agent；Agent 自身已设置的字段不会被覆盖；`defaults` 为 null 时直接返回 |

### 5.3 AgentConfig 主要字段

依据 [agents.yml](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/resources/agents.yml) 与单元测试，`AgentConfig` 包含以下字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `name` | String | Agent 名称（必填，作为注册 key） |
| `description` | String | Agent 描述，被 `DefaultAgent` 用作系统提示词 |
| `type` | String | Agent 类型，如 `chat` / `task` / `workflow` |
| `skills` | `List<String>` | 引用的 Skill 名称列表 |
| `tools` | `List<String>` | 引用的 Tool 名称列表 |
| `model` | `ModelConfig` | provider、name、temperature、maxTokens |
| `memory` | `MemoryConfig` | shortTermStore、longTermStore、shortTermMaxEntries、longTermMaxEntries、ttlSeconds |
| `sandbox` | `SandboxConfig` | enabled、networkAccess、maxExecutionTime、maxMemoryMb、workDir |
| `monitor` | `MonitorConfig` | enabled、zipkinEndpoint、langfuse.{enabled,host,publicKey,secretKey} |
| `execution` | `ExecutionConfig` | maxConcurrency、timeoutSeconds、retryCount |

> 注意：`DefaultAgent` 当前实际消费的执行参数是 `execution.maxConcurrency`（用于推导 `recursionLimit`），其他配置项由使用方按需消费。