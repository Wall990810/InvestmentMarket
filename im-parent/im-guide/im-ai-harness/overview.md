# im-ai-harness 模块概述

← [返回索引](../README.md)

> 适用版本：`${revision}`（当前为 `1.0.0-Beta0-SNAPSHOT`）
> 模块定位：多 Agent 编排套件，提供 Runner（运行器）、Pipeline（消息管道）与可复用的 Harness 组件，灵感来自 AgentScope-harness 的设计思路。

---

## 一、模块概述

`im-ai-harness` 是 InvestmentMarket AI 体系中的"编排层"。它在 `im-ai-core` / `im-ai-agent` 提供的单体 Agent 能力之上，解决以下三类问题：

1. **运行调度（Runner）**：定义 `AgentRunner` SPI，并提供串行（`SequentialRunner`）与并行（`ParallelRunner`）两种实现，决定一个或多个 Agent 实际如何被执行。
2. **消息管道（Pipeline）**：参考 AgentScope 的 Pipeline 设计，将消息处理流程拆分为多个可组合的 `PipelineStage`，以责任链方式对 `List<Message>` 进行逐步加工。
3. **可复用组件（HarnessComponent）**：定义 `HarnessComponent` SPI，并内置消息路由、消息过滤、记忆增强三类通用组件，可在 Agent 调用前后插入横切逻辑。

三者可以独立使用，也可以组合：例如用 `MessagePipeline` 预处理消息，再交给 `ParallelRunner` 对多个 Agent 做扇出执行，最后由 `HarnessComponent` 注入记忆上下文。

源码目录：[im-ai-harness/src/main/java/org/wall/im/ai/harness](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-harness/src/main/java/org/wall/im/ai/harness)

---

## 二、Maven 坐标与依赖

### 2.1 坐标

```xml
<dependency>
    <groupId>org.wall.im</groupId>
    <artifactId>im-ai-harness</artifactId>
    <version>${revision}</version>
</dependency>
```

其中 `${revision}` 由父 POM `im-ai` 统一管理，当前值为 `1.0.0-Beta0-SNAPSHOT`。

### 2.2 模块自身的依赖（来自 [pom.xml](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-harness/pom.xml)）

| 依赖 | scope | 说明 |
| --- | --- | --- |
| `org.wall.im:im-ai-core` | compile | 提供 `Agent`、`Message`、`AgentResult`、`MemoryStore`、`MemoryEntry` 等基础模型 |
| `org.wall.im:im-ai-agent` | compile | 提供 Agent 实现，Runner 调用 `Agent.execute(...)` |
| `org.wall.im:im-ai-memory` | test | 仅测试用，提供 `MemoryStore` 实现用于 `MemoryAugmentComponent` 的单测 |
| `org.junit.jupiter:junit-jupiter` | test | 单元测试 |
| `org.mockito:mockito-core` / `mockito-junit-jupiter` | test | 单元测试 Mock |

> 注意：本模块不直接依赖任何三方运行时框架（如 Spring），可被任意 Java 应用引入。`MemoryAugmentComponent` 依赖的 `MemoryStore` / `MemoryEntry` 来自 `im-ai-core`，运行时需要由内存模块（如 `im-ai-memory`）提供具体实现。

---

## 八、源码索引

| 文件 | 链接 |
| --- | --- |
| AgentRunner.java | [AgentRunner.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-harness/src/main/java/org/wall/im/ai/harness/runner/AgentRunner.java) |
| SequentialRunner.java | [SequentialRunner.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-harness/src/main/java/org/wall/im/ai/harness/runner/SequentialRunner.java) |
| ParallelRunner.java | [ParallelRunner.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-harness/src/main/java/org/wall/im/ai/harness/runner/ParallelRunner.java) |
| MessagePipeline.java | [MessagePipeline.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-harness/src/main/java/org/wall/im/ai/harness/pipeline/MessagePipeline.java) |
| PipelineStage.java | [PipelineStage.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-harness/src/main/java/org/wall/im/ai/harness/pipeline/PipelineStage.java) |
| FunctionalStage.java | [FunctionalStage.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-harness/src/main/java/org/wall/im/ai/harness/pipeline/FunctionalStage.java) |
| HarnessComponent.java | [HarnessComponent.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-harness/src/main/java/org/wall/im/ai/harness/component/HarnessComponent.java) |
| MessageRouterComponent.java | [MessageRouterComponent.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-harness/src/main/java/org/wall/im/ai/harness/component/MessageRouterComponent.java) |
| MessageFilterComponent.java | [MessageFilterComponent.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-harness/src/main/java/org/wall/im/ai/harness/component/MessageFilterComponent.java) |
| MemoryAugmentComponent.java | [MemoryAugmentComponent.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-harness/src/main/java/org/wall/im/ai/harness/component/MemoryAugmentComponent.java) |
| pom.xml | [pom.xml](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-harness/pom.xml) |