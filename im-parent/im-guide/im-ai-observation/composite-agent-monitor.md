[← 返回索引](../README.md)

> 本文档为 [`usage-guide.md`](./usage-guide.md) 的拆分章节之一。

# CompositeAgentMonitor

源码：[CompositeAgentMonitor.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-observation/src/main/java/org/wall/im/ai/monitor/composite/CompositeAgentMonitor.java)

## 8.1 设计

```java
public CompositeAgentMonitor(AgentMonitor primary);
public void addMonitor(AgentMonitor monitor);
```

- 构造时传入一个 `primary`（主监控器）；通过 `addMonitor` 追加任意数量的副监控器。
- 所有 SPI 方法都**先调用 primary**，再遍历副监控器列表逐个调用。
- **`traceId` 来源**：`traceStart` 返回的是 primary 的 `traceId`，副监控器的返回值被丢弃。这一点对 `ZipkinAgentTracer` / `LangfuseMonitor` 等"装饰器"型监控尤其重要——它们内部会以 delegate 的 traceId 作为 key 管理 Span/映射，因此把它们作为副监控器直接加入 Composite 时，其 delegate 可能与 primary 不一致，建议把装饰器型的监控器**串成链**（A 包装 B 包装 C）再加入 Composite，或直接作为 primary 使用。

## 8.2 容错与顺序

- 副监控器的每次调用都包裹在 `try/catch (Exception ignored)` 中，**异常被静默吞掉**，不影响主流程与其他副监控器。
- `recordMetric` 与 `traceToolCall` 同样遵循"primary 先行 + 副监控器尽力"模式。
- `getCustomMetricRegistry()` 仅返回 primary 的注册器，副监控器的注册器不暴露。

## 8.3 何时使用

- 需要"指标 + 链路 + LLM 平台"同时上报时，把三者组合。
- 想要主备监控：primary 提供权威 traceId 与自定义指标，副监控器做旁路上报。
- 不适合需要严格事务一致性的场景：副监控器失败不会重试，也不会通知调用方。