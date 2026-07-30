[← 返回索引](../README.md)

> 本文档为 [`usage-guide.md`](./usage-guide.md) 的拆分章节之一。

# AgentMonitor SPI 回顾

本模块所有实现都基于 `im-ai-core` 的两个 SPI，定义见：

- [AgentMonitor.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/monitor/AgentMonitor.java)
- [CustomMetricRegistry.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/monitor/CustomMetricRegistry.java)

## 3.1 AgentMonitor

```java
public interface AgentMonitor {
    String traceStart(String agentName, String input);
    void traceEnd(String traceId, String agentName, String output, long costTimeMs, int tokenUsage);
    void traceError(String traceId, String agentName, String error);
    void traceToolCall(String traceId, String toolName, Map<String, Object> parameters,
                       String result, long costTimeMs);
    void recordMetric(String metricName, double value, Map<String, String> tags);
    CustomMetricRegistry getCustomMetricRegistry();
}
```

- `traceStart` 返回 `traceId`，后续 `traceEnd` / `traceError` / `traceToolCall` 均以该 `traceId` 关联。
- `traceToolCall` 用于在某个 Agent trace 下记录工具调用子项。
- `recordMetric` 记录一次性自定义指标，`getCustomMetricRegistry` 返回可复用的注册器。

## 3.2 CustomMetricRegistry

```java
public interface CustomMetricRegistry {
    void registerCounter(String name, String description);
    void incrementCounter(String name);
    void registerGauge(String name, String description);
    void setGaugeValue(String name, double value);
    void registerTimer(String name, String description);
    void recordTimer(String name, long durationMs);
}
```

提供 Counter / Gauge / Timer 三类指标的注册与更新能力。