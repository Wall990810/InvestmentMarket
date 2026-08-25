# InvestmentMarket Code Wiki

> 开源金融数据分析平台 · AI 智能体配置接入框架
> 版本：1.0.0-Beta0-SNAPSHOT · 许可证：Apache 2.0
> 仓库：https://github.com/Wall990810/InvestmentMarket

---

## 目录

- [1. 项目概览](#1-项目概览)
- [2. 项目整体架构](#2-项目整体架构)
- [3. 模块划分与职责](#3-模块划分与职责)
- [4. 关键类与函数说明](#4-关键类与函数说明)
- [5. 模块依赖关系](#5-模块依赖关系)
- [6. 核心设计模式与运行流程](#6-核心设计模式与运行流程)
- [7. 配置体系](#7-配置体系)
- [8. 项目运行方式](#8-项目运行方式)
- [9. 工程化与质量保障](#9-工程化与质量保障)

---

## 1. 项目概览

InvestmentMarket 是一个基于 Java 26 + Spring Boot 4.1 + Spring AI Alibaba 构建的多模块 Maven 项目，定位为 **AI 智能体配置接入框架**，同时附带一个金融投资建议示例应用（`im-admin`）。

项目核心目标（见 [Design.md](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/Design.md)）：

1. 通过 YAML 配置文件在项目启动时生成业务智能体，预置 Skills、Tool、执行环境；
2. 提供多样化记忆存储方式（短期 / 长期记忆，内存 / Redis / 数据库可选）；
3. 接入 Micrometer 监控体系，链路追踪使用 Zipkin Brave，并将 trace 上送 Langfuse，提供自定义指标开发接口；
4. 提供智能体运行沙盒，限制运行时工作路径；
5. 提供 Harness 组件（参考 AgentScope-harness 设计）。

模块设计遵循 **高内聚、低耦合** 原则。

### 技术栈

| 类别 | 选型 |
|------|------|
| 语言 / JDK | Java 26（同时支持 Kotlin 2.1.10 编译） |
| 构建 | Maven + flatten-maven-plugin（`${revision}` 多模块版本管理） |
| 应用框架 | Spring Boot 4.1.0、Spring Cloud 2025.1.2 |
| AI 框架 | Spring AI Alibaba 1.1.2.3（DashScope / 通义千问、ReactAgent） |
| 监控 | Micrometer 1.13、Zipkin Brave 6.0.3、Langfuse Java 0.2.0 |
| 持久化 | Spring Data Redis、Spring JDBC（MySQL） |
| 序列化 | Jackson 2.17.1（含 YAML 支持） |
| 测试 | JUnit 5.10、Mockito 5.11 |
| 代码质量 | Spring Java Format、Checkstyle、Qodana（GitHub Actions） |

---

## 2. 项目整体架构

项目分为两大顶层部分：

```
InvestmentMarket/
├── im-admin/                 # 示例应用：基于框架的投资建议 Spring Boot 服务
├── im-parent/                # 框架 SDK 父 POM（发布到 Maven Central）
│   ├── im-ai/                # AI 智能体框架（核心）
│   │   ├── im-ai-core        # 核心抽象层（接口与模型）
│   │   ├── im-ai-agent       # Agent 生命周期与 Spring AI 适配
│   │   ├── im-ai-memory      # 记忆存储实现（内存/Redis/DB）
│   │   ├── im-ai-harness     # 消息管道与协作组件
│   │   ├── im-ai-observation # 可观测性（Micrometer/Langfuse/Zipkin）
│   │   └── im-ai-sandbox     # 进程隔离沙盒
│   ├── im-base / im-common / im-core / im-starter   # 预留扩展模块（当前为空）
│   ├── im-guide              # 文档模块（不产出 jar）
│   ├── im-quant / im-quant-factor  # 量化模块（预留）
│   └── im-observation        # 可观测性集成测试 Spring Boot 应用
├── .github/workflows/        # Qodana 代码质量 CI
└── README.md
```

### 分层架构

```
┌──────────────────────────────────────────────────────────┐
│  应用层 (im-admin)  InvestmentAgentService / Tools / Skills │
├──────────────────────────────────────────────────────────┤
│  框架适配层 (im-ai-agent)                                  │
│  AgentFactory · DefaultAgent(ReactAgent) · SpringAiToolAdapter │
│  AgentLifecycleManager · Registry · MarkdownSkillLoader  │
├───────────────┬──────────────┬──────────────┬────────────┤
│ im-ai-memory  │ im-ai-harness│ im-ai-sandbox│ im-ai-obs  │
│ 记忆存储实现   │ 消息管道组件 │ 进程沙盒     │ 可观测性   │
├───────────────┴──────────────┴──────────────┴────────────┤
│  核心抽象层 (im-ai-core)                                   │
│  Agent · AgentContext · MemoryStore · Skill · Tool         │
│  Sandbox · AgentMonitor · CustomMetricRegistry            │
│  AgentConfig / Message / AgentResult 等模型               │
└──────────────────────────────────────────────────────────┘
```

- **核心抽象层** 定义接口契约与纯数据模型，零 Spring 依赖（仅 Jackson + slf4j），保证 SDK 可被任意 Java 项目复用；
- **适配层** 将抽象桥接到 Spring AI Alibaba 的 `ReactAgent`（ReAct 推理范式）；
- **实现层** 提供记忆、沙盒、Harness、可观测性等可选能力；
- **应用层** 通过 YAML 配置 + Spring Bean 装配即可生成业务 Agent。

---

## 3. 模块划分与职责

### 3.1 im-ai-core — 核心抽象层

> 最底层模块，仅依赖 Jackson + slf4j。定义整个框架的接口契约和数据模型。

| 包 | 职责 |
|----|------|
| `org.wall.im.ai.core.agent` | Agent 核心接口与执行上下文 |
| `org.wall.im.ai.core.model` | 配置与运行数据模型（POJO） |
| `org.wall.im.ai.core.memory` | 记忆存储抽象与条目模型 |
| `org.wall.im.ai.core.skill` | 技能（Skill）抽象 |
| `org.wall.im.ai.core.tool` | 工具（Tool）抽象 |
| `org.wall.im.ai.core.sandbox` | 沙盒接口与结果模型 |
| `org.wall.im.ai.core.monitor` | 监控与自定义指标抽象 |
| `org.wall.im.ai.core.config` | 全局 Agent 配置集合 |

### 3.2 im-ai-agent — Agent 生命周期与适配层

> 依赖 `im-ai-core` + `im-ai-observation` + Spring AI Alibaba。负责 Agent 的创建、初始化、注册、销毁全生命周期，并将自定义 `Tool` 适配为 Spring AI `ToolCallback`。

| 包 | 职责 |
|----|------|
| `org.wall.im.ai.agent.lifecycle` | 工厂、注册表、生命周期管理、默认实现 |
| `org.wall.im.ai.agent.registry` | Agent 注册表（ConcurrentHashMap） |
| `org.wall.im.ai.agent.config` | YAML 配置解析、Spring Boot 自动装配 |
| `org.wall.im.ai.agent.skill` | Markdown 技能加载器与自动配置 |
| `org.wall.im.ai.agent.adapter` | Spring AI 工具适配器 |
| `org.wall.im.ai.agent.trace` | 基于 ThreadLocal 的调用链上下文 |

### 3.3 im-ai-memory — 记忆存储实现

> 依赖 `im-ai-core` + `im-ai-agent`。提供三种 `MemoryStore` 实现：内存、Redis、JDBC。

| 类 | 说明 |
|----|------|
| `InMemoryStore` | 基于 `ConcurrentHashMap` 的内存实现，适合短期记忆与测试 |
| `RedisMemoryStore` | 基于 Redis List 的实现，支持 TTL、自动裁剪 |
| `JdbcMemoryStore` | 基于 `JdbcTemplate` 的关系型存储，自动建表（`ai_memory`） |
| `DefaultMemoryStoreFactory` | 根据类型字符串创建对应实现 |
| `RedisOperationsAdapter` | Redis 操作适配接口，隔离 Spring Data Redis 强依赖 |

### 3.4 im-ai-harness — 消息管道与协作组件

> 依赖 `im-ai-core` + `im-ai-agent`。参考 AgentScope-harness 设计，提供可组合的消息处理流水线。

| 包 | 关键组件 |
|----|----------|
| `harness.pipeline` | `MessagePipeline`、`PipelineStage`、`FunctionalStage` |
| `harness.runner` | `AgentRunner`、`SequentialRunner`、`ParallelRunner` |
| `harness.component` | `HarnessComponent`、`MessageFilterComponent`、`MessageRouterComponent`、`MemoryAugmentComponent` |

### 3.5 im-ai-sandbox — 进程隔离沙盒

> 依赖 `im-ai-core`。提供进程级隔离的代码 / 命令执行环境。

| 类 | 说明 |
|----|------|
| `ProcessSandbox` | 通过 `ProcessBuilder` 启动 bash 子进程，清理环境变量、限制工作目录、超时控制 |
| `SandboxManager` | 封装安全检查（危险操作黑名单）与统一访问入口 |

### 3.6 im-ai-observation — 可观测性

> 依赖 `im-ai-core` + Micrometer + Brave + Langfuse SDK。提供监控实现与组合能力。

| 类 | 说明 |
|----|------|
| `MicrometerAgentMonitor` | 基础监控，记录调用计数、耗时、token 用量、工具调用 |
| `MicrometerCustomMetricRegistry` | 自定义 Counter / Gauge / Timer 注册 |
| `LangfuseMonitor` | 包装委托监控器，将 trace 上送 Langfuse（Trace/Span/Generation 事件） |
| `LangfuseMonitorFactory` | LangfuseMonitor 工厂，支持环境变量、自定义配置 |
| `ZipkinAgentTracer` | 基于 Brave 的分布式追踪，父子 Span 记录工具调用 |
| `CompositeAgentMonitor` | 组合多个 AgentMonitor，主监控器 + 附属监控器模式 |

### 3.7 im-admin — 示例应用

> 独立的 Spring Boot 4.1 应用，演示如何使用框架构建投资建议服务。

| 类 | 说明 |
|----|------|
| `AdminApplication` | Spring Boot 启动入口 |
| `config.AiAgentConfig` | 装配 DashScope ChatModel、AgentFactory、Registry、Skill/Tool 等 Bean |
| `agent.InvestmentAgentService` | 业务服务，封装咨询、分析、组合推荐 API |
| `agent.skill.InvestmentAnalysisSkill` | 投资分析技能（Java 实现） |
| `agent.skill.PortfolioRecommendSkill` | 投资组合推荐技能（Java 实现） |
| `agent.tool.MarketDataTool` | 行情数据查询工具（含 JSON Schema 参数定义） |
| `agent.tool.RiskAssessmentTool` | 风险评估工具（VaR / 夏普比率等） |

### 3.8 其它模块

| 模块 | 状态 | 说明 |
|------|------|------|
| `im-base` / `im-common` / `im-core` | 预留 | 仅含 POM，无源码，规划为基础/通用能力层 |
| `im-starter` | 预留 | 聚合 Starter（packaging=pom，无子模块） |
| `im-guide` | 文档 | 仅发布文档，跳过 jar/source/javadoc 生成 |
| `im-quant` (`im-quant-factor`) | 预留 | 量化因子模块（规划中） |
| `im-observation` | Spring Boot | 可观测性集成测试应用（含 Langfuse trace 测试） |

---

## 4. 关键类与函数说明

### 4.1 核心抽象（im-ai-core）

#### Agent 接口 — [Agent.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/agent/Agent.java)

所有 Agent 实现的顶层契约：

```java
public interface Agent {
    String getName();              // Agent 唯一标识
    AgentConfig getConfig();       // 获取配置
    void initialize();             // 初始化（构建 ReactAgent）
    String chat(String input);     // 单轮对话
    AgentResult execute(List<Message> messages); // 多轮任务执行
    void reset();                  // 重置状态（清短期记忆）
    void destroy();                // 销毁（清记忆 + 释放 ReactAgent）
}
```

#### AgentContext — [AgentContext.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/agent/AgentContext.java)

封装 Agent 运行时依赖：`skills` / `tools` / `shortTermMemory` / `longTermMemory` / `variables`。通过 `registerSkill` / `registerTool` / `setVariable` 装配组件。

#### MemoryStore 接口 — [MemoryStore.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/memory/MemoryStore.java)

记忆存储统一抽象：`store` / `storeAll` / `retrieve` / `retrieveRecent` / `search` / `clear` / `getStoreType`。

#### Skill 接口 — [Skill.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/skill/Skill.java)

技能抽象：`getName` / `getDescription` / `execute(String input)` / `canExecute`（默认 true）。

#### Tool 接口 — [Tool.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/tool/Tool.java)

工具抽象：`getName` / `getDescription` / `getParameterSchema`（JSON Schema） / `execute(Map<String, Object> parameters)`。

#### Sandbox 接口 — [Sandbox.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/sandbox/Sandbox.java)

沙盒抽象：`initialize` / `execute(code, workDir)` / `executeCommand` / `isPathAllowed` / `destroy`。

#### AgentMonitor 接口 — [AgentMonitor.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/monitor/AgentMonitor.java)

监控抽象，统一对接 Micrometer / Zipkin / Langfuse：

```java
String traceStart(String agentName, String input);                    // 返回 traceId
void traceEnd(String traceId, String agentName, String output, long costTimeMs, int tokenUsage);
void traceError(String traceId, String agentName, String error);
void traceToolCall(String traceId, String toolName, Map<String, Object> parameters, String result, long costTimeMs);
void recordMetric(String metricName, double value, Map<String, String> tags);
CustomMetricRegistry getCustomMetricRegistry();
```

#### CustomMetricRegistry 接口 — [CustomMetricRegistry.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/monitor/CustomMetricRegistry.java)

自定义指标注册：Counter / Gauge / Timer 的注册与更新。

#### 配置模型

| 类 | 说明 |
|----|------|
| [AgentConfig](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/model/AgentConfig.java) | 对应 YAML 中单个 Agent：name/description/type/model/skills/tools/memory/sandbox/monitor/execution/properties |
| [AgentsDefinition](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/config/AgentsDefinition.java) | 对应 YAML 根节点：`defaults`（全局默认）+ `agents` 列表 |
| [ModelConfig](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/model/ModelConfig.java) | provider/name/apiKey/endpoint/temperature/maxTokens |
| [MemoryConfig](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/model/MemoryConfig.java) | shortTermStore/longTermStore（memory/redis/db）+ 容量与 TTL |
| [SandboxConfig](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/model/SandboxConfig.java) | enabled/workDir/allowedPaths/networkAccess/maxExecutionTime/maxMemoryMb |
| [MonitorConfig](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/model/MonitorConfig.java) | enabled/zipkinEndpoint/langfuse（内嵌 `LangfuseConfig`）/customMetrics |
| [ExecutionConfig](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/model/ExecutionConfig.java) | maxConcurrency/timeoutSeconds/retryCount/envVars |
| [Message](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/model/Message.java) | role/content/name/timestamp/metadata/traceId，含 `system/user/assistant/tool` 工厂方法 |
| [AgentResult](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/model/AgentResult.java) | success/output/costTimeMs/tokenUsage/messageChain/errorMessage/traceId |
| [MemoryEntry](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/memory/MemoryEntry.java) | id/content/role/createdAt/importance(0~1)/metadata |

### 4.2 Agent 生命周期与适配（im-ai-agent）

#### AgentFactory — [AgentFactory.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/java/org/wall/im/ai/agent/lifecycle/AgentFactory.java)

根据 `AgentConfig` 创建 `DefaultAgent` 实例。创建流程：
1. 从 `ToolRegistry` 解析配置指定的工具；
2. 构造 `DefaultAgent`（传入 `ChatModel`、工具列表、`AgentMonitor`）；
3. 将 `SkillRegistry` 中的技能注册到 `AgentContext`；
4. 通过 `MemoryStoreFactory` 创建短期 / 长期记忆并注入上下文。

#### DefaultAgent — [DefaultAgent.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/java/org/wall/im/ai/agent/lifecycle/DefaultAgent.java)

框架默认 `Agent` 实现，内部委托 Spring AI Alibaba 的 `ReactAgent` 执行 **ReAct（Reasoning + Acting）** 推理循环。

- `initialize()`：构造 `ReactAgent.builder()`，设置系统提示词（优先 `description`），通过 `SpringAiToolAdapter` 注册工具，配置 `CompileConfig.recursionLimit`（取自 `execution.maxConcurrency`，上限 20）。
- `chat(input)`：
  1. 通过 `AgentMonitor.traceStart` 生成 traceId，`AgentTraceContext.setup` 注入 ThreadLocal；
  2. 将输入存入短期记忆；
  3. 调用 `reactAgent.call(input)` 执行 ReAct 循环（推理 → 调用工具 → 观察结果 → 循环）；
  4. 成功：`traceEnd` 记录；失败：`traceError` 记录；最终 `AgentTraceContext.clear()`；
  5. 将 Q&A 存入长期记忆。
- `execute(messages)`：遍历 `user` 消息逐条调用 `chat`，汇总输出。
- `reset()`：清除短期记忆；`destroy()`：清除全部记忆并释放 ReactAgent。

#### AgentLifecycleManager — [AgentLifecycleManager.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/java/org/wall/im/ai/agent/lifecycle/AgentLifecycleManager.java)

全生命周期管理：`createAgents`（创建+初始化+注册+通知）、`startAll`、`stopAll`、`destroyAll`。通过 `AgentLifecycleListener`（default 方法接口）发布创建/启动/停止/销毁/错误事件。

#### 注册表三件套

- [AgentRegistry](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/java/org/wall/im/ai/agent/registry/AgentRegistry.java)：基于 `ConcurrentHashMap`，提供 `register/get/getRequired/getAll/contains/unregister/destroyAll`。同名注册会先销毁旧实例。
- [SkillRegistry](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/java/org/wall/im/ai/agent/lifecycle/SkillRegistry.java) / [ToolRegistry](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/java/org/wall/im/ai/agent/lifecycle/ToolRegistry.java)：技能与工具注册表，结构类似。

#### AgentConfigParser — [AgentConfigParser.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/java/org/wall/im/ai/agent/config/AgentConfigParser.java)

YAML 配置解析器，使用 `ObjectMapper(new YAMLFactory())`，支持 `parseFromClasspath` / `parseFromFile` / `parseFromString`。静态方法 `applyDefaults` 将全局 `defaults` 合并到每个 Agent（model/memory/sandbox/monitor/execution 缺失时回填）。

#### SpringAiToolAdapter — [SpringAiToolAdapter.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/java/org/wall/im/ai/agent/adapter/SpringAiToolAdapter.java)

将自定义 `Tool` 桥接到 Spring AI 的 `FunctionToolCallback`：
- 将 `tool.getParameterSchema()` 作为输入类型描述；
- 把 `tool.execute(Map)` 包装为 `Function<String,String>` 回调；
- **自动记录工具调用 trace**：通过 `AgentTraceContext` 获取 traceId 与 monitor，调用 `monitor.traceToolCall`；
- 自动处理 JSON 字符串与 Map 之间转换、异常包装。

#### AgentTraceContext — [AgentTraceContext.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/java/org/wall/im/ai/agent/trace/AgentTraceContext.java)

基于 `InheritableThreadLocal` 的链路上下文，存储 `traceId` / `AgentMonitor` / `agentName`。使得 `SpringAiToolAdapter` 在工具回调中能自动获取 trace 信息并上报，无需显式传参。

#### Markdown 技能体系

- [MarkdownSkill](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/java/org/wall/im/ai/agent/skill/MarkdownSkill.java)：基于 MD 文件的 `Skill` 实现，支持 `{{input}}` / `{{skillName}}` / `{{description}}` / 自定义元数据变量占位符（正则 `\{\{(\w+)\}\}` 渲染）。
- [MarkdownSkillLoader](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/java/org/wall/im/ai/agent/skill/MarkdownSkillLoader.java)：扫描 classpath（含 JAR 内资源）/ 文件系统的 `.md` 文件，解析 YAML frontmatter（`---` 包裹的头部）+ 正文模板，生成 `MarkdownSkill` 注册到 `SkillRegistry`。
- [MarkdownSkillAutoConfiguration](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/java/org/wall/im/ai/agent/skill/MarkdownSkillAutoConfiguration.java)：Spring Boot 自动装配，条件：存在 `SkillRegistry` Bean + `im.ai.markdown-skills.enabled=true`（默认开启）。
- [MarkdownSkillLoaderInitializer](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/java/org/wall/im/ai/agent/skill/MarkdownSkillLoaderInitializer.java)：监听 `ApplicationReadyEvent`，应用就绪后加载配置目录中的技能。
- [MarkdownSkillProperties](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/java/org/wall/im/ai/agent/skill/MarkdownSkillProperties.java)：`im.ai.markdown-skills.classpath-dirs` / `file-system-dirs`。

#### 监控自动装配

- [AgentMonitorAutoConfiguration](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/java/org/wall/im/ai/agent/config/AgentMonitorAutoConfiguration.java)：`@AutoConfiguration`，创建 `MicrometerAgentMonitor` 作为基础；若 Langfuse 公钥/密钥配置完整，则用 `LangfuseMonitorFactory.create` 包装。
- [AgentMonitorProperties](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/java/org/wall/im/ai/agent/config/AgentMonitorProperties.java)：`im.ai.monitor.*` 配置绑定。

自动装配注册文件 [AutoConfiguration.imports](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports)：
```
org.wall.im.ai.agent.skill.MarkdownSkillAutoConfiguration
org.wall.im.ai.agent.config.AgentMonitorAutoConfiguration
```

### 4.3 记忆存储实现（im-ai-memory）

| 类 | 关键实现 |
|----|----------|
| [InMemoryStore](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-memory/src/main/java/org/wall/im/ai/memory/store/InMemoryStore.java) | `ConcurrentHashMap<String, synchronizedList>`，超 `maxEntries` 移除最旧；`search` 用 `contains` 过滤 |
| [RedisMemoryStore](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-memory/src/main/java/org/wall/im/ai/memory/store/RedisMemoryStore.java) | Key 前缀 `ai:memory:`；`RPUSH` + `LTRIM` 裁剪 + `EXPIRE` TTL；序列化格式 `id\|role\|importance\|content` |
| [JdbcMemoryStore](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-memory/src/main/java/org/wall/im/ai/memory/store/JdbcMemoryStore.java) | 表 `ai_memory`（构造时 `CREATE TABLE IF NOT EXISTS`）；支持 `LIKE` 搜索；超量删除最旧 |
| [DefaultMemoryStoreFactory](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-memory/src/main/java/org/wall/im/ai/memory/factory/DefaultMemoryStoreFactory.java) | `switch(storeType)`：`memory` / `redis`（需 adapter）/ `db`（需 JdbcTemplate） |

### 4.4 Harness（im-ai-harness）

- [MessagePipeline](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-harness/src/main/java/org/wall/im/ai/harness/pipeline/MessagePipeline.java)：链式 `addStage`，`process` 依次执行各 stage。
- [PipelineStage](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-harness/src/main/java/org/wall/im/ai/harness/pipeline/PipelineStage.java) / [FunctionalStage](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-harness/src/main/java/org/wall/im/ai/harness/pipeline/FunctionalStage.java)：阶段接口与 Lambda 友好的函数式实现。
- [AgentRunner](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-harness/src/main/java/org/wall/im/ai/harness/runner/AgentRunner.java) / [SequentialRunner](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-harness/src/main/java/org/wall/im/ai/harness/runner/SequentialRunner.java) / [ParallelRunner](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-harness/src/main/java/org/wall/im/ai/harness/runner/ParallelRunner.java)：运行策略，`ParallelRunner` 用 `CachedThreadPool`，60s 超时，汇总输出与 token。
- [HarnessComponent](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-harness/src/main/java/org/wall/im/ai/harness/component/HarnessComponent.java) 接口及实现：`MessageFilterComponent`（去空/截断）、`MessageRouterComponent`（规则路由，内嵌 `RouteRule`）、`MemoryAugmentComponent`（注入历史记忆）。

### 4.5 沙盒（im-ai-sandbox）

- [ProcessSandbox](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox/ProcessSandbox.java)：`ProcessBuilder("bash", ...)`，清空环境变量仅保留 `HOME`/`TMPDIR`，超时 `destroyForcibly`，路径校验（工作目录前缀 + 白名单），`destroy` 递归清理临时目录。
- [SandboxManager](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox/SandboxManager.java)：封装 `safeExecute` / `safeExecuteCommand`，含危险操作黑名单（`rm -rf /`、`mkfs`、`dd if=`、fork 炸弹等）。

### 4.6 可观测性（im-ai-observation）

- [MicrometerAgentMonitor](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-observation/src/main/java/org/wall/im/ai/monitor/micrometer/MicrometerAgentMonitor.java)：记录 `ai.agent.calls`（Counter，tag=agent/status）、`ai.agent.duration`（Timer）、`ai.agent.tokens`（Gauge）、`ai.tool.calls` / `ai.tool.duration`。
- [MicrometerCustomMetricRegistry](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-observation/src/main/java/org/wall/im/ai/monitor/micrometer/MicrometerCustomMetricRegistry.java)：Counter/Gauge(AtomicLong)/Timer 的注册与更新。
- [LangfuseMonitor](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-observation/src/main/java/org/wall/im/ai/monitor/langfuse/LangfuseMonitor.java)：**装饰器模式**，包装 `delegate`，在每次 trace 方法中先委托再上送 Langfuse（`TraceEvent` + `CreateObservationEvent` 的 `GENERATION` / `SPAN` 事件，含 `Usage` 与时间窗）。维护 `traceId → langfuseTraceId` 映射。
- [LangfuseMonitorFactory](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-observation/src/main/java/org/wall/im/ai/monitor/langfuse/LangfuseMonitorFactory.java)：工厂，支持 `createDefault` / `create(config, delegate)` / `fromEnvironment`（读 `LANGFUSE_PUBLIC_KEY` / `LANGFUSE_SECRET_KEY` / `LANGFUSE_HOST`）/ `fromClient`。
- [ZipkinAgentTracer](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-observation/src/main/java/org/wall/im/ai/monitor/zipkin/ZipkinAgentTracer.java)：基于 Brave `Tracer`，`traceStart` 开 Span、`traceEnd` finish、`traceToolCall` 创建子 Span；输入/输出截断到 500 字符。
- [CompositeAgentMonitor](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-observation/src/main/java/org/wall/im/ai/monitor/composite/CompositeAgentMonitor.java)：主监控器 + 附属监控器列表，附属失败不影响主流程（异常被吞）。

### 4.7 应用层（im-admin）

- [AiAgentConfig](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-admin/src/main/java/org/wall/im/admin/config/AiAgentConfig.java)：`@Configuration`，装配全部 Bean：`AgentConfigParser`、`AgentRegistry`、`SkillRegistry`（注册 Java 技能 + 加载 `skills/` 下的 MD 技能）、`MarkdownSkillLoader`、`ToolRegistry`（注册 `MarketDataTool` / `RiskAssessmentTool`）、`MemoryStoreFactory`（默认内存实现）、`DashScopeApi` + `DashScopeChatModel`（从 `spring.ai.dashscope.*` 读配置）、`AgentFactory`、`AgentLifecycleManager`、`AgentsDefinition`（启动时解析 `agents/investment-advisor.yml` 并 `createAgents`）。
- [InvestmentAgentService](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-admin/src/main/java/org/wall/im/admin/agent/InvestmentAgentService.java)：`@Service`，通过 `AgentRegistry.getRequired("investment-advisor")` 获取 Agent，提供 `consult` / `analyze` / `recommendPortfolio` / `resetSession` 业务方法。
- [MarketDataTool](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-admin/src/main/java/org/wall/im/admin/agent/tool/MarketDataTool.java) / [RiskAssessmentTool](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-admin/src/main/java/org/wall/im/admin/agent/tool/RiskAssessmentTool.java)：示例工具，返回模拟 JSON 行情/风险数据，实际可对接 Tushare/Wind。
- [InvestmentAnalysisSkill](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-admin/src/main/java/org/wall/im/admin/agent/skill/InvestmentAnalysisSkill.java) / [PortfolioRecommendSkill](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-admin/src/main/java/org/wall/im/admin/agent/skill/PortfolioRecommendSkill.java)：Java 技能实现示例。

---

## 5. 模块依赖关系

### 5.1 Maven 模块树

```
im-parent (pom)
├── im-base          (预留)
├── im-common        (预留)
├── im-core          (预留)
├── im-starter (pom) (预留)
├── im-guide (pom)   (文档)
├── im-quant (pom)
│   └── im-quant-factor (预留)
├── im-observation   (Spring Boot 应用, 独立 parent=spring-boot-starter-parent)
└── im-ai (pom)
    ├── im-ai-core        [Jackson, slf4j]
    ├── im-ai-observation [im-ai-core, Micrometer, Brave, Langfuse]
    ├── im-ai-agent       [im-ai-core, im-ai-observation, spring-ai-alibaba-agent-framework, dashscope, spring-boot-autoconfigure]
    ├── im-ai-memory       [im-ai-core, im-ai-agent, spring-data-redis(opt), spring-jdbc(opt)]
    ├── im-ai-harness     [im-ai-core, im-ai-agent]
    └── im-ai-sandbox     [im-ai-core, slf4j]
```

### 5.2 内部模块依赖矩阵

| 模块 | 依赖（compile） |
|------|-----------------|
| im-ai-core | —（仅第三方） |
| im-ai-observation | im-ai-core |
| im-ai-agent | im-ai-core, im-ai-observation |
| im-ai-memory | im-ai-core, im-ai-agent |
| im-ai-harness | im-ai-core, im-ai-agent |
| im-ai-sandbox | im-ai-core |
| im-admin | im-ai-core, im-ai-agent, spring-ai-alibaba（独立 parent） |
| im-observation | im-ai-core, im-ai-agent, im-ai-observation, dashscope（独立 parent） |

### 5.3 关键外部依赖

- **Spring AI Alibaba**：`spring-ai-alibaba-agent-framework`（提供 `ReactAgent`）、`spring-ai-alibaba-starter-dashscope`（通义千问 `ChatModel`）。排除 `mcp-core` 与 `spring-ai-vector-store` 以规避 CVE，并用安全版本（`mcp`/`mcp-core` 1.1.3、`spring-ai-vector-store` 1.1.8）显式补回。
- **Micrometer**：`micrometer-core`、`micrometer-tracing`、`micrometer-tracing-bridge-brave`、`micrometer-registry-prometheus`。
- **Zipkin Brave**：`brave`、`brave-instrumentation-okhttp3`。
- **Langfuse**：`com.langfuse:langfuse-java`。
- **Jackson**：`jackson-databind`、`jackson-dataformat-yaml`（配置与技能解析）。

### 5.4 依赖关系图

```
                    ┌──────────────┐
                    │  im-ai-core  │ ← 抽象层
                    └──────┬───────┘
          ┌────────────────┼─────────────────────────┐
          │                │                          │
   ┌──────▼──────┐  ┌──────▼────────┐         ┌──────▼──────┐
   │ im-ai-obs   │  │ im-ai-agent   │         │ im-ai-sandbox│
   └──────┬──────┘  └───────┬───────┘         └─────────────┘
          │                 │
          │        ┌────────┼────────┐
          │        │        │        │
          │ ┌──────▼──────┐ │ ┌──────▼──────┐
          │ │im-ai-memory │ │ │im-ai-harness│
          │ └─────────────┘ │ └─────────────┘
          │                 │
          └───── 被下列使用 ─┘
                    │
        ┌───────────┴───────────┐
        │                       │
   ┌────▼─────┐           ┌──────▼──────┐
   │ im-admin │           │im-observation│
   │ (示例应用)│           │ (测试应用)   │
   └──────────┘           └─────────────┘
```

---

## 6. 核心设计模式与运行流程

### 6.1 设计模式一览

| 模式 | 应用位置 |
|------|----------|
| 工厂方法 | `AgentFactory`、`MemoryStoreFactory`、`LangfuseMonitorFactory` |
| 注册表 | `AgentRegistry`、`SkillRegistry`、`ToolRegistry` |
| 适配器 | `SpringAiToolAdapter`（Tool → ToolCallback）、`RedisOperationsAdapter` |
| 装饰器 / 包装 | `LangfuseMonitor`（包装 delegate）、`ZipkinAgentTracer`（包装 delegate） |
| 组合 | `CompositeAgentMonitor`（主 + 附属列表） |
| 管道 | `MessagePipeline` + `PipelineStage` |
| 策略 | `AgentRunner`（`SequentialRunner` / `ParallelRunner`） |
| 观察者 / 监听器 | `AgentLifecycleListener`（default 方法接口） |
| 模板方法 | `DefaultAgent.chat` 固定 trace + 记忆 + 推理骨架 |
| ThreadLocal 上下文 | `AgentTraceContext`（跨工具回调传递 trace） |
| 自动装配 | `@AutoConfiguration` + `AutoConfiguration.imports` |

### 6.2 Agent 启动与调用流程

```
Spring Boot 启动
   │
   ▼
AgentMonitorAutoConfiguration 装配 AgentMonitor
   │（Micrometer 基础 + 可选 Langfuse 包装）
   ▼
MarkdownSkillAutoConfiguration 装配 MarkdownSkillLoader
   │
   ▼
ApplicationReadyEvent
   │ → MarkdownSkillLoaderInitializer.loadSkills()
   │     扫描 classpath(skills/) + 文件系统目录
   │     解析 frontmatter → MarkdownSkill → SkillRegistry
   ▼
应用 @Configuration（如 im-admin.AiAgentConfig）
   │ 注册 Java Skill/Tool 到对应 Registry
   │ 装配 DashScopeApi + ChatModel
   │ 装配 AgentFactory（注入 Registries/ChatModel/Monitor）
   │ 装配 AgentLifecycleManager
   │
   ▼
investmentAgentsDefinition Bean 初始化
   │ → AgentConfigParser.parseFromClasspath("agents/investment-advisor.yml")
   │ → AgentLifecycleManager.createAgents(definition)
   │     for each AgentConfig:
   │       AgentFactory.create()
   │         - 从 ToolRegistry 解析工具
   │         - new DefaultAgent(config, chatModel, tools, monitor)
   │         - 从 SkillRegistry 解析技能 → AgentContext.registerSkill
   │         - MemoryStoreFactory 创建短期/长期记忆 → AgentContext
   │       agent.initialize()
   │         - 构建 ReactAgent（系统提示词 + ToolCallback + recursionLimit）
   │       AgentRegistry.register(agent)
   │       通知 AgentLifecycleListener.onCreated
   ▼
运行时调用（InvestmentAgentService.consult）
   │
   ▼
agent.chat(input)
   │ 1. monitor.traceStart → traceId；AgentTraceContext.setup
   │ 2. 短期记忆 store(input, role=user)
   │ 3. reactAgent.call(input)  ← ReAct 循环
   │      推理 → 调用 ToolCallback → SpringAiToolAdapter
   │        → tool.execute(params)
   │        → AgentTraceContext 自动获取 traceId
   │        → monitor.traceToolCall（Langfuse 上送 Span 事件）
   │      观察结果 → 继续推理 / 返回最终答案
   │ 4. monitor.traceEnd（耗时、token、输出）
   │ 5. 长期记忆 store("Q:.. A:..")
   │ 6. AgentTraceContext.clear()
   ▼
返回响应
```

---

## 7. 配置体系

### 7.1 Agent 配置（agents.yml）

文件示例见 [investment-advisor.yml](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-admin/src/main/resources/agents/investment-advisor.yml) 与 [agents.yml](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/resources/agents.yml)。

```yaml
defaults:           # 全局默认（AgentConfig 未配置时回填）
  model: { provider, name, temperature, maxTokens }
  memory: { shortTermStore, longTermStore, shortTermMaxEntries, longTermMaxEntries, ttlSeconds }
  sandbox: { enabled, networkAccess, maxExecutionTime, maxMemoryMb }
  monitor: { enabled, zipkinEndpoint, langfuse: {...} }
  execution: { maxConcurrency, timeoutSeconds, retryCount }

agents:              # Agent 列表
  - name: investment-advisor
    description: "..."   # 同时作为 ReactAgent 系统提示词
    type: task            # chat / task / workflow
    model: {...}
    skills: [investment-analysis-skill, portfolio-recommend-skill]
    tools: [market-data-tool, risk-assessment-tool]
    memory: {...}
    sandbox: { enabled, workDir, networkAccess, ... }
    monitor: {...}
    execution: {...}
    properties: {...}     # 业务扩展属性
```

### 7.2 Markdown 技能文件

文件示例见 [investment-analysis.md](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-admin/src/main/resources/skills/investment-analysis.md)：

```markdown
---
name: investment-analysis-skill
description: 投资分析技能...
tools: [market-data-tool, risk-assessment-tool]
---

# 技能指令模板
支持 {{input}}、{{skillName}}、{{description}} 变量占位符
```

### 7.3 Spring Boot 配置（application.yml）

见 [application.yml](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-admin/src/main/resources/application.yml)：

```yaml
server:
  port: 8080
spring:
  ai:
    dashscope:
      api-key: ${AI_DASHSCOPE_API_KEY:}
      chat: { model: qwen-plus, temperature: 0.7, max-tokens: 4096 }

# 框架自动装配属性
im:
  ai:
    monitor:
      enabled: true
      langfuse:
        enabled: true
        host: https://cloud.langfuse.com
        public-key: pk-lf-xxx
        secret-key: sk-lf-xxx
        debug: false
        flush-interval-ms: 5000
        max-batch-size: 50
    markdown-skills:
      enabled: true
      classpath-dirs: [skills]
      file-system-dirs: []
```

### 7.4 关键环境变量

| 变量 | 用途 |
|------|------|
| `AI_DASHSCOPE_API_KEY` | 阿里云 DashScope（通义千问）API Key（必需） |
| `LANGFUSE_PUBLIC_KEY` | Langfuse 公钥 |
| `LANGFUSE_SECRET_KEY` | Langfuse 私钥 |
| `LANGFUSE_HOST` | Langfuse 服务地址（默认 http://localhost:3000） |
| `MAVEN_GPG_PASSPHRASE` | `sonatype` profile 发布签名 |

---

## 8. 项目运行方式

### 8.1 环境准备

- JDK 26（OpenJDK 25 亦可，项目 target=26）
- Maven 3.9+
- （可选）MySQL、Redis（使用 redis/db 记忆时）
- （可选）Langfuse 实例（trace 上送时）
- 阿里云 DashScope API Key（使用通义千问模型时）

### 8.2 构建整个框架

```bash
# 在仓库根目录
mvn clean install -DskipTests
```

`im-parent` 使用 `flatten-maven-plugin` + `${revision}=1.0.0-Beta0-SNAPSHOT` 管理多模块版本，构建后会生成各模块 jar、sources jar、javadoc jar。

### 8.3 运行 im-admin 示例应用

```bash
# 1. 设置 DashScope API Key
set AI_DASHSCOPE_API_KEY=your-api-key        # Windows
export AI_DASHSCOPE_API_KEY=your-api-key     # Linux/Mac

# （可选）开启 Langfuse
set LANGFUSE_PUBLIC_KEY=pk-lf-xxx
set LANGFUSE_SECRET_KEY=sk-lf-xxx

# 2. 进入 im-admin 目录构建
cd im-admin
mvn clean package -DskipTests

# 3. 运行
java -jar target/im-admin-0.0.1-SNAPSHOT.jar
# 或开发模式
mvn spring-boot:run
```

应用启动后：
- 解析 `agents/investment-advisor.yml`，创建并注册 `investment-advisor` Agent；
- 加载 `skills/` 目录下的 Markdown 技能；
- 注册 Java 技能（`InvestmentAnalysisSkill`、`PortfolioRecommendSkill`）与工具（`MarketDataTool`、`RiskAssessmentTool`）；
- 服务端口 `8080`，可通过 `InvestmentAgentService` 调用 `consult` / `analyze` / `recommendPortfolio`。

### 8.4 运行 im-observation 测试应用

```bash
cd im-parent/im-observation

export AI_DASHSCOPE_API_KEY=your-dashscope-api-key
export LANGFUSE_PUBLIC_KEY=pk-lf-xxx
export LANGFUSE_SECRET_KEY=sk-lf-xxx
export LANGFUSE_HOST=http://localhost:3000

mvn test -Dtest=ReActAgentLangfuseTraceTest
```

测试场景见 [ReActAgentLangfuseTraceTest.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-observation/src/test/java/org/wall/im/imobservation/ReActAgentLangfuseTraceTest.java)：
- 基础 Trace 记录（start → end）
- Tool 调用 Trace 记录
- 多次调用 Trace 记录

### 8.5 在自有 Spring Boot 项目中集成框架

`im-ai-agent` 与 `im-ai-core` 已发布为 `org.wall.im:im-ai-*:1.0.0-Beta0-SNAPSHOT`，依赖即可获得自动装配：

```xml
<dependency>
    <groupId>org.wall.im</groupId>
    <artifactId>im-ai-core</artifactId>
    <version>1.0.0-Beta0-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>org.wall.im</groupId>
    <artifactId>im-ai-agent</artifactId>
    <version>1.0.0-Beta0-SNAPSHOT</version>
</dependency>
```

最小集成步骤：
1. 提供 `agents/*.yml` 配置与 `skills/*.md` 技能；
2. 装配 `ChatModel`（如 `DashScopeChatModel`）、`SkillRegistry`、`ToolRegistry`、`MemoryStoreFactory`、`AgentFactory`、`AgentLifecycleManager`、`AgentRegistry`（参考 `im-admin` 的 `AiAgentConfig`）；
3. 启动时调用 `AgentLifecycleManager.createAgents(definition)`；
4. 通过 `AgentRegistry.getRequired(name).chat(input)` 调用。

### 8.6 发布到 Sonatype（中央仓库）

```bash
mvn -P sonatype deploy
```

`sonatype` profile 启用 GPG 签名（`maven-gpg-plugin`）与 `central-publishing-maven-plugin`，跳过测试。

---

## 9. 工程化与质量保障

### 9.1 代码格式与风格

- **Spring Java Format**：`spring-javaformat-maven-plugin`，CI 环境（`env.CI=true`）执行 `validate` 校验，本地执行 `apply` 自动格式化。
- **Checkstyle**：`maven-checkstyle-plugin` + `puppycrawl-tools-checkstyle 9.3` + `spring-javaformat-checkstyle 0.0.47`，`process-sources` 阶段校验，配置位于 `src/checkstyle/checkstyle.xml`。
- **License**：`com.mycila:license-maven-plugin`（`license` profile），Apache 2.0 头校验。

### 9.2 测试

- 单元测试：`maven-surefire-plugin`，`useFile=false`、`reportFormat=plain`；
- 集成测试：`maven-failsafe-plugin`（`integration-tests` profile）；
- 覆盖率：`jacoco-maven-plugin`（`test-coverage` profile）；
- 测试依赖：JUnit 5.10、Mockito 5.11、`micrometer-test`。

### 9.3 CI/CD

[Qodana 代码质量](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/.github/workflows/qodana_code_quality.yml)：在 `pull_request` 与 `push`（main / releases/*）时触发 `JetBrains/qodana-action@v2026.1`，使用 `QODANA_TOKEN` 进行静态分析。

### 9.4 文档与产物

- `maven-javadoc-plugin`：各模块产出 `*-javadoc.jar`（`failOnError=false`、`doclint=none`）；
- `maven-source-plugin`：产出 `*-sources.jar`；
- `im-guide` 模块：仅文档，禁用 jar/source/javadoc 生成；
- 各 `im-ai-*` 模块 `target/apidocs` 已含 Javadoc HTML 站点。

### 9.5 安全

显式修复传递依赖中的 CVE：
- `io.modelcontextprotocol.sdk:mcp` / `mcp-core` → 1.1.3（修复 CVE-2026-35568 DNS Rebinding、CVE-2026-34237 Wildcard CORS）；
- `org.springframework.ai:spring-ai-vector-store` → 1.1.8（修复 CVE-2026-22738）；
- `commons-lang3` → 3.18.0（修复 Uncontrolled Recursion）。

---

> 本 Wiki 基于代码库当前状态生成。如需了解具体实现细节，可点击文中类名链接跳转至源码；如需扩展，请参考 [Design.md](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/Design.md) 的设计目标。
