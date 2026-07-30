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
| Sandbox SPI | Sandbox / SandboxResult / SandboxConfig 接口回顾 | [sandbox-spi.md](im-ai-sandbox/sandbox-spi.md) |
| ProcessSandbox | 进程级沙盒实现（工作目录限制、危险操作拦截） | [process-sandbox.md](im-ai-sandbox/process-sandbox.md) |
| SandboxManager | 沙盒管理器入口与调用路由 | [sandbox-manager.md](im-ai-sandbox/sandbox-manager.md) |
| 配置指南 | YAML + Java Bean 配置示例 | [configuration-guide.md](im-ai-sandbox/configuration-guide.md) |
| 使用示例 | execute/canAccess/destroy 完整示例 | [integration-examples.md](im-ai-sandbox/integration-examples.md) |
| 安全注意事项 | 8 条安全要点与已知风险 | [security-notes.md](im-ai-sandbox/security-notes.md) |

---

### 应用与可观测性

#### im-admin · 投资助手示例应用

> 模块总览：[im-admin/overview.md](im-admin/overview.md)

| 功能块 | 说明 | 文档 |
| --- | --- | --- |
| 应用入口 | AdminApplication + AiAgentConfig 配置类 | [application-entry.md](im-admin/application-entry.md) |
| 业务服务 | InvestmentAgentService 公共 API 与使用示例 | [investment-agent-service.md](im-admin/investment-agent-service.md) |
| 业务技能 | InvestmentAnalysisSkill / PortfolioRecommendSkill | [business-skills.md](im-admin/business-skills.md) |
| 业务工具 | MarketDataTool / RiskAssessmentTool | [business-tools.md](im-admin/business-tools.md) |
| Agent 配置 | agents/investment-advisor.yml 详解 | [agent-config-reference.md](im-admin/agent-config-reference.md) |
| Markdown 技能 | skills/*.md 技能文件说明 | [skills-markdown-reference.md](im-admin/skills-markdown-reference.md) |
| 应用配置 | application.yml 关键配置 | [application-config.md](im-admin/application-config.md) |
| 启动与测试 | 构建、启动、示例请求 | [run-and-test.md](im-admin/run-and-test.md) |

#### im-observation · 可观测性聚合应用

> 模块总览：[im-observation/overview.md](im-observation/overview.md)

| 功能块 | 说明 | 文档 |
| --- | --- | --- |
| 应用入口 | ImObservationApplication 启动类 | [application-entry.md](im-observation/application-entry.md) |
| 配置指南 | application.properties 与启动方式 | [configuration-guide.md](im-observation/configuration-guide.md) |
| 与监控模块关系 | 与 im-ai-observation 的组件对应 | [relation-to-im-ai-observation.md](im-observation/relation-to-im-ai-observation.md) |
| 运行方式 | 开发调试 / 打包部署 / 测试验证 | [run-and-test.md](im-observation/run-and-test.md) |

---

### 预留模块（待实现）

下列模块目前仅包含 `pom.xml`，尚无源代码，已在指南中标注其预设角色与后续规划：

| 模块 | 预设角色 | 指南 |
| --- | --- | --- |
| `im-base` | 基础工具类与底层支撑 | [usage-guide.md](im-base/usage-guide.md) |
| `im-common` | 跨模块通用工具与常量 | [usage-guide.md](im-common/usage-guide.md) |
| `im-core` | 业务领域核心模型与领域服务 | [usage-guide.md](im-core/usage-guide.md) |
| `im-starter` | Spring Boot Starter 聚合模块 | [usage-guide.md](im-starter/usage-guide.md) |
| `im-quant-factor` | 量化因子计算 | [usage-guide.md](im-quant-factor/usage-guide.md) |

---

## 模块定位

`im-guide` 自身是一个 `packaging=pom` 的文档模块，不产出任何 Java 产物（已禁用 jar / source / javadoc 生成）。它被注册在 `im-parent` 的 `<modules>` 列表中，随主构建一同构建，便于在 IDE 与 Maven 站点中统一浏览。

## 阅读建议

1. **初次接入**：先阅读 [im-ai-core/overview.md](im-ai-core/overview.md)，按顺序了解 Agent → Skill/Tool → Memory → Monitor → Sandbox 各契约。
2. **运行时与配置**：阅读 [im-ai-agent/overview.md](im-ai-agent/overview.md)，重点关注 `agents-yml-reference.md` 与 `markdown-skill-loader.md`。
3. **端到端示例**：参考 [im-admin/overview.md](im-admin/overview.md) 中完整的投资助手示例，串联上述模块。
4. **进阶能力**：按需阅读 harness / observation / memory / sandbox 各子文档。
