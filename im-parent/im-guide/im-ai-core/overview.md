# ← 返回索引

# im-ai-core 使用指南

> InvestmentMarket 项目 AI 智能体框架核心模块使用文档

## 模块概述

`im-ai-core` 是 InvestmentMarket 项目 AI 智能体框架的**核心抽象层**。它本身不提供任何具体实现，而是为整个 AI Agent 框架定义统一的接口契约、配置模型与数据模型，相当于框架的"契约层 / SPI 层"。

该模块的职责包括：

- **定义智能体核心接口**：[Agent](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/agent/Agent.java) 作为所有智能体实现必须遵循的契约。
- **定义运行时上下文**：[AgentContext](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/agent/AgentContext.java) 封装 Agent 执行时所需的技能、工具、记忆与上下文变量。
- **定义可扩展能力单元**：[Skill](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/skill/Skill.java)（技能）与 [Tool](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/tool/Tool.java)（工具）两大能力抽象。
- **定义存储与监控抽象**：[MemoryStore](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/memory/MemoryStore.java)（记忆存储）、[AgentMonitor](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/monitor/AgentMonitor.java)（监控）与 [Sandbox](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/sandbox/Sandbox.java)（沙盒）。
- **定义配置模型**：以 [AgentConfig](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/model/AgentConfig.java) 为根的配置类树，可由 YAML 文件直接反序列化加载（依赖 Jackson）。

凭借本模块定义的统一契约，上层模块（如 `im-ai-*` 的实现模块）可以聚焦于具体实现，而无需重复定义数据结构与接口；第三方也可通过实现这些接口接入框架。

## Maven 坐标与依赖

### 坐标信息

`im-ai-core` 的 Maven 坐标定义于 [pom.xml](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/pom.xml)：

- **groupId**：`org.wall.im`（继承自父模块 `im-ai`）
- **artifactId**：`im-ai-core`
- **version**：`${revision}`（由顶层 `im-parent` 统一通过 `revision` 属性管理）

### 引入方式

在子模块的 `pom.xml` 中添加依赖：

```xml
<dependency>
    <groupId>org.wall.im</groupId>
    <artifactId>im-ai-core</artifactId>
    <version>${revision}</version>
</dependency>
```

### 自身依赖

本模块自身仅依赖如下库，保持轻量、无侵入：

| 依赖 | 用途 |
| --- | --- |
| `com.fasterxml.jackson.core:jackson-databind` | JSON / 对象序列化与反序列化 |
| `com.fasterxml.jackson.dataformat:jackson-dataformat-yaml` | 解析 `agents.yml` 等 YAML 配置 |
| `org.slf4j:slf4j-api` | 日志门面 |
| `org.junit.jupiter:junit-jupiter`（test） | 单元测试 |
| `org.mockito:mockito-core`（test） | 单元测试 Mock |

> 注意：本模块不绑定任何具体 LLM SDK、数据库驱动或 Web 框架，所有具体实现均由下游模块提供。

## 源码索引

### 核心接口

| 类 | 源码 |
| --- | --- |
| Agent | [Agent.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/agent/Agent.java) |
| AgentContext | [AgentContext.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/agent/AgentContext.java) |
| Skill | [Skill.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/skill/Skill.java) |
| Tool | [Tool.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/tool/Tool.java) |
| MemoryStore | [MemoryStore.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/memory/MemoryStore.java) |
| MemoryEntry | [MemoryEntry.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/memory/MemoryEntry.java) |
| AgentMonitor | [AgentMonitor.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/monitor/AgentMonitor.java) |
| CustomMetricRegistry | [CustomMetricRegistry.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/monitor/CustomMetricRegistry.java) |
| Sandbox | [Sandbox.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/sandbox/Sandbox.java) |
| SandboxResult | [SandboxResult.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/sandbox/SandboxResult.java) |

### 配置模型

| 类 | 源码 |
| --- | --- |
| AgentConfig | [AgentConfig.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/model/AgentConfig.java) |
| ModelConfig | [ModelConfig.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/model/ModelConfig.java) |
| ExecutionConfig | [ExecutionConfig.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/model/ExecutionConfig.java) |
| MemoryConfig | [MemoryConfig.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/model/MemoryConfig.java) |
| SandboxConfig | [SandboxConfig.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/model/SandboxConfig.java) |
| MonitorConfig | [MonitorConfig.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/model/MonitorConfig.java) |

### 数据模型

| 类 | 源码 |
| --- | --- |
| Message | [Message.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/model/Message.java) |
| AgentResult | [AgentResult.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/model/AgentResult.java) |
| AgentsDefinition | [AgentsDefinition.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/config/AgentsDefinition.java) |