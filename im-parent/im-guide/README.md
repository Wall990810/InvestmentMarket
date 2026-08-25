# InvestmentMarket 使用指南

本模块（`im-guide`）集中存放 InvestmentMarket 各功能模块的使用指引。每个子目录对应一个功能模块，目录下按功能块细粒度拆分为独立 Markdown 文档，便于按需查阅。

---

## 指南索引

### AI 智能体框架（im-ai 子模块）

#### im-ai-core · 核心契约层

> 模块总览：[im-ai-core/overview.md](im-ai-core/overview.md)

| 功能块 | 说明 | 文档 |
| --- | --- | --- |
| Agent 契约 | Agent / AgentContext 接口定义 | [agent-contract.md](im-ai-core/agent-contract.md) |
| Skill & Tool | 技能与工具接口定义 | [skill-and-tool.md](im-ai-core/skill-and-tool.md) |
| Memory | MemoryStore / MemoryEntry 记忆模型 | [memory-contract.md](im-ai-core/memory-contract.md) |
| Monitor | AgentMonitor / CustomMetricRegistry 监控契约 | [monitor-contract.md](im-ai-core/monitor-contract.md) |
| Sandbox | Sandbox / SandboxResult 沙盒契约 | [sandbox-contract.md](im-ai-core/sandbox-contract.md) |
| 配置模型 | AgentConfig 及 ModelConfig/ExecutionConfig/MemoryConfig 等嵌套配置 | [config-models.md](im-ai-core/config-models.md) |
| Message & Result | Message / AgentResult / AgentsDefinition 模型 | [messages-and-results.md](im-ai-core/messages-and-results.md) |
| 使用示例 | 各接口的实现与扩展示例 | [integration-examples.md](im-ai-core/integration-examples.md) |

#### im-ai-agent · 默认 Agent 运行时

> 模块总览：[im-ai-agent/overview.md](im-ai-agent/overview.md)

| 功能块 | 说明 | 文档 |
| --- | --- | --- |
| Agent 生命周期 | DefaultAgent ReAct 推理流程、initialize/chat/execute/reset/destroy、AgentFactory、AgentLifecycleManager | [default-agent-lifecycle.md](im-ai-agent/default-agent-lifecycle.md) |
| 注册中心 | AgentRegistry / SkillRegistry / ToolRegistry / MemoryStoreFactory | [registries.md](im-ai-agent/registries.md) |
| 配置解析 | AgentConfigParser 解析方法与 AgentConfig 字段 | [agent-config-parser.md](im-ai-agent/agent-config-parser.md) |
| Spring AI 适配 | SpringAiToolAdapter 桥接 Tool 到 Spring AI | [spring-ai-tool-adapter.md](im-ai-agent/spring-ai-tool-adapter.md) |
| Markdown 技能加载 | MarkdownSkill / Loader / AutoConfiguration / Properties | [markdown-skill-loader.md](im-ai-agent/markdown-skill-loader.md) |
| agents.yml 参考 | agents.yml 完整配置示例与 defaults 继承 | [agents-yml-reference.md](im-ai-agent/agents-yml-reference.md) |
| Spring Boot 集成 | 自动装配、启用条件、配置项 | [spring-boot-integration.md](im-ai-agent/spring-boot-integration.md) |
| 端到端示例 | 从启动到调用的完整代码示例 | [integration-examples.md](im-ai-agent/integration-examples.md) |

#### im-ai-memory · 记忆存储

> 模块总览：[im-ai-memory/overview.md](im-ai-memory/overview.md)

| 功能块 | 说明 | 文档 |
| --- | --- | --- |
| MemoryStore SPI | MemoryStore 接口回顾 | [memory-store-spi.md](im-ai-memory/memory-store-spi.md) |
| InMemoryStore | 内存存储实现（ConcurrentHashMap，默认 1000 条上限） | [in-memory-store.md](im-ai-memory/in-memory-store.md) |
| JdbcMemoryStore | JDBC 存储实现（自动建表） | [jdbc-memory-store.md](im-ai-memory/jdbc-memory-store.md) |
| RedisMemoryStore | Redis 存储实现 + RedisOperationsAdapter | [redis-memory-store.md](im-ai-memory/redis-memory-store.md) |
| 存储工厂 | DefaultMemoryStoreFactory 按配置切换后端 | [memory-store-factory.md](im-ai-memory/memory-store-factory.md) |
| 配置指南 | YAML/Java 配置与后端切换 | [configuration-guide.md](im-ai-memory/configuration-guide.md) |
| 使用与扩展 | 典型使用示例与自定义存储扩展 | [integration-examples.md](im-ai-memory/integration-examples.md) |

#### im-ai-harness · 多智能体编排

> 模块总览：[im-ai-harness/overview.md](im-ai-harness/overview.md)

| 功能块 | 说明 | 文档 |
| --- | --- | --- |
| AgentRunner | Runner SPI + SequentialRunner / ParallelRunner | [agent-runner.md](im-ai-harness/agent-runner.md) |
| MessagePipeline | PipelineStage / FunctionalStage / MessagePipeline | [message-pipeline.md](im-ai-harness/message-pipeline.md) |
| Harness 组件 | HarnessComponent SPI + MessageRouter/MessageFilter/MemoryAugment | [harness-components.md](im-ai-harness/harness-components.md) |
| 使用示例 | 顺序/并行管道、路由过滤、记忆增强 | [integration-examples.md](im-ai-harness/integration-examples.md) |
| 扩展 | 自定义组件、Runner、PipelineStage | [extending-the-harness.md](im-ai-harness/extending-the-harness.md) |

#### im-ai-observation · 监控集成

> 模块总览：[im-ai-observation/overview.md](im-ai-observation/overview.md)

| 功能块 | 说明 | 文档 |
| --- | --- | --- |
| AgentMonitor SPI | AgentMonitor / CustomMetricRegistry 接口回顾 | [agent-monitor-spi.md](im-ai-observation/agent-monitor-spi.md) |
| MicrometerAgentMonitor | 基于 Micrometer 的指标监控实现 | [micrometer-agent-monitor.md](im-ai-observation/micrometer-agent-monitor.md) |
| Micrometer 自定义指标 | MicrometerCustomMetricRegistry 详解 | [micrometer-custom-metric-registry.md](im-ai-observation/micrometer-custom-metric-registry.md) |
| ZipkinAgentTracer | 基于 Brave 的链路追踪装饰器 | [zipkin-agent-tracer.md](im-ai-observation/zipkin-agent-tracer.md) |
| LangfuseMonitor | Langfuse 装饰器 + LangfuseMonitorFactory | [langfuse-monitor.md](im-ai-observation/langfuse-monitor.md) |
| CompositeAgentMonitor | 多监控器组合、容错与顺序 | [composite-agent-monitor.md](im-ai-observation/composite-agent-monitor.md) |
| 配置指南 | YAML 配置、部分启用、环境变量 | [configuration-guide.md](im-ai-observation/configuration-guide.md) |
| 集成示例 | Micrometer+Langfuse、三后端联合、Composite 聚合 | [integration-examples.md](im-ai-observation/integration-examples.md) |
| 扩展开发 | 自定义 AgentMonitor 装饰器模板 | [extending-the-monitor.md](im-ai-observation/extending-the-monitor.md) |

#### im-ai-sandbox · 运行沙盒

> 模块总览：[im-ai-sandbox/overview.md](im-ai-sandbox/overview.md)

| 功能块 | 说明 | 文档 |
| --- | --- | --- |
| Sandbox SPI 全集 | Sandbox/Result/Config/Type/Limits/Context/Factory/Listener 与 CommandPolicy 策略体系 | [sandbox-spi.md](im-ai-sandbox/sandbox-spi.md) |
| ProcessSandbox | 本地进程级沙盒实现（ProcessBuilder + bash，弱隔离） | [process-sandbox.md](im-ai-sandbox/process-sandbox.md) |
| DockerLocalSandbox | 本地 Docker 容器沙盒实现（CPU/内存/进程/网络/tmpfs 强隔离） | [docker-sandbox.md](im-ai-sandbox/docker-sandbox.md) |
| RemoteSandbox | 远端 HTTP 沙盒客户端 + REST 协议约定 | [remote-sandbox.md](im-ai-sandbox/remote-sandbox.md) |
| SandboxManager | 统一入口（危险操作预检 + 策略注入 + 生命周期钩子 + Registry 路由） | [sandbox-manager.md](im-ai-sandbox/sandbox-manager.md) |
| 配置指南 | SandboxConfig、SandboxProperties YAML、Spring Boot 自动装配 | [configuration-guide.md](im-ai-sandbox/configuration-guide.md) |
| 使用示例 | 兼容场景 / Docker 强隔离 / 远端 HTTP / 策略链与监听器 | [integration-examples.md](im-ai-sandbox/integration-examples.md) |
| 扩展指南 | 自定义 SandboxFactory、CommandPolicy 链、DockerCommandExecutor | [extension-guide.md](im-ai-sandbox/extension-guide.md) |
| 安全注意事项 | 12 条安全要点与已知风险 | [security-notes.md](im-ai-sandbox/security-notes.md) |

---

### 应用与可观测性

#### im-admin · 投资助手示例应用
