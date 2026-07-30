[← 返回索引](../README.md)

> 本文档为 [`usage-guide.md`](./usage-guide.md) 的拆分章节之一。

# 模块概述

## 一、模块概述

`im-ai-observation` 是 InvestmentMarket AI 体系的"监控/追踪层"。它实现了 `im-ai-core` 中的 `AgentMonitor` SPI，将 Agent 执行过程中的 trace、tool 调用、自定义指标分发到一种或多种后端：

- **Micrometer**：通过 `MicrometerAgentMonitor` 把 Agent 调用计数、耗时、token 用量等记录到任意 `MeterRegistry`（Prometheus、Datadog 等），并由 `MicrometerCustomMetricRegistry` 暴露自定义指标注册能力。
- **Zipkin Brave**：通过 `ZipkinAgentTracer` 基于 Brave `Tracing` 创建分布式 Span，刻画 Agent 与 Tool 调用链路。
- **Langfuse**：通过 `LangfuseMonitor`（配合 `LangfuseMonitorFactory`）使用 langfuse-java SDK 把 trace、generation、span、error 等事件批量上报到 Langfuse 平台，用于 LLM 专项可观测性分析。
- **组合**：通过 `CompositeAgentMonitor` 把多个监控器组合，主监控器负责返回 `traceId` 与 `CustomMetricRegistry`，副监控器尽力执行、异常静默，不影响主流程。

源码目录：[im-ai-observation/src/main/java/org/wall/im/ai/monitor](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-observation/src/main/java/org/wall/im/ai/monitor)

---

## 二、Maven 坐标与依赖

### 2.1 坐标

```xml
<dependency>
    <groupId>org.wall.im</groupId>
    <artifactId>im-ai-observation</artifactId>
    <version>${revision}</version>
</dependency>
```

`${revision}` 由父 POM `im-ai` 统一管理，当前值为 `1.0.0-Beta0-SNAPSHOT`。

### 2.2 模块自身的依赖（来自 [pom.xml](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-observation/pom.xml)）

| 依赖 | scope | 说明 |
| --- | --- | --- |
| `org.wall.im:im-ai-core` | compile | 提供 `AgentMonitor` / `CustomMetricRegistry` SPI 及 `MonitorConfig` |
| `io.micrometer:micrometer-core` | compile | `MeterRegistry`、`Counter`、`Timer` 基础设施 |
| `io.micrometer:micrometer-tracing` | compile | Micrometer 追踪抽象 |
| `io.micrometer:micrometer-tracing-bridge-brave` | compile | 与 Brave 桥接 |
| `io.micrometer:micrometer-registry-prometheus` | optional | Prometheus 注册器，按需引入 |
| `io.zipkin.brave:brave` | compile | Brave `Tracing` / `Tracer` / `Span` |
| `com.langfuse:langfuse-java` | compile | Langfuse Java SDK（`LangfuseClient`、Ingestion API） |
| `org.junit.jupiter:junit-jupiter` | test | 单元测试 |
| `org.mockito:mockito-core` | test | 单元测试 Mock |
| `io.micrometer:micrometer-test` | test | Micrometer 测试工具 |

> 说明：`micrometer-registry-prometheus` 标记为 `optional`，需要暴露 Prometheus 端点时由使用方显式引入。Brave 与 Langfuse 为强依赖，但运行时是否真正上报由各自的配置开关（如 `MonitorConfig.LangfuseConfig.isEnabled()`）控制。

---

## 源码索引

| 文件 | 链接 |
| --- | --- |
| CompositeAgentMonitor.java | [CompositeAgentMonitor.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-observation/src/main/java/org/wall/im/ai/monitor/composite/CompositeAgentMonitor.java) |
| LangfuseMonitor.java | [LangfuseMonitor.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-observation/src/main/java/org/wall/im/ai/monitor/langfuse/LangfuseMonitor.java) |
| LangfuseMonitorFactory.java | [LangfuseMonitorFactory.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-observation/src/main/java/org/wall/im/ai/monitor/langfuse/LangfuseMonitorFactory.java) |
| MicrometerAgentMonitor.java | [MicrometerAgentMonitor.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-observation/src/main/java/org/wall/im/ai/monitor/micrometer/MicrometerAgentMonitor.java) |
| MicrometerCustomMetricRegistry.java | [MicrometerCustomMetricRegistry.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-observation/src/main/java/org/wall/im/ai/monitor/micrometer/MicrometerCustomMetricRegistry.java) |
| ZipkinAgentTracer.java | [ZipkinAgentTracer.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-observation/src/main/java/org/wall/im/ai/monitor/zipkin/ZipkinAgentTracer.java) |
| AgentMonitor.java (SPI) | [AgentMonitor.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/monitor/AgentMonitor.java) |
| CustomMetricRegistry.java (SPI) | [CustomMetricRegistry.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/monitor/CustomMetricRegistry.java) |
| MonitorConfig.java | [MonitorConfig.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/model/MonitorConfig.java) |
| pom.xml | [pom.xml](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-observation/pom.xml) |