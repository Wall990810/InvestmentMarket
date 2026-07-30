[← 返回索引](../README.md)

> 本文档为 [`usage-guide.md`](./usage-guide.md) 的拆分章节之一。

# MicrometerAgentMonitor

源码：[MicrometerAgentMonitor.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-observation/src/main/java/org/wall/im/ai/monitor/micrometer/MicrometerAgentMonitor.java)

## 4.1 构造

```java
public MicrometerAgentMonitor(MeterRegistry meterRegistry);
```

构造时会把传入的 `MeterRegistry` 同时包装为一个 `MicrometerCustomMetricRegistry`（见第五节），通过 `getCustomMetricRegistry()` 返回。

## 4.2 记录的指标

| 触发方法 | 指标名 | 类型 | Tags | 说明 |
| --- | --- | --- | --- | --- |
| `traceStart` | `ai.agent.calls` | Counter | `agent`, `status=started` | 调用开始计数 |
| `traceEnd` | `ai.agent.calls` | Counter | `agent`, `status=success` | 成功计数 |
| `traceEnd` | `ai.agent.duration` | Timer | `agent` | 记录 `costTimeMs` |
| `traceEnd` | `ai.agent.tokens` | Gauge | `agent` | 直接以 `tokenUsage` 为值注册 gauge |
| `traceError` | `ai.agent.calls` | Counter | `agent`, `status=error` | 错误计数 |
| `traceToolCall` | `ai.tool.calls` | Counter | `tool`, `traceId` | 工具调用计数 |
| `traceToolCall` | `ai.tool.duration` | Timer | `tool` | 工具调用耗时 |
| `recordMetric` | 调用方传入的 `metricName` | Gauge | 调用方传入的 `tags` | 一次性 gauge |

实现要点：

- `traceStart` 生成 `UUID` 作为 `traceId`，并在 `activeTraces` 中记录起始时间戳（用于内部追踪，`traceEnd`/`traceError` 时移除）。
- `recordMetric` 会把 `Map<String,String> tags` 转换为 Micrometer `Tag` 列表，再以 `value` 为数值注册 gauge。
- `ai.agent.tokens` 与 `recordMetric` 的 gauge 使用 `meterRegistry.gauge(...)` 注册**当前值**，适合做瞬时观测（如最近一次 token 用量）。

## 4.3 接入 Micrometer 注册器

```java
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.wall.im.ai.monitor.micrometer.MicrometerAgentMonitor;

MeterRegistry registry = new SimpleMeterRegistry();   // 或 PrometheusMeterRegistry
MicrometerAgentMonitor monitor = new MicrometerAgentMonitor(registry);

// 在 Agent 执行前后调用
String traceId = monitor.traceStart("market-analyst", "今日A股走势");
try {
    // ... Agent 执行 ...
    monitor.traceEnd(traceId, "market-analyst", output, costMs, tokens);
} catch (Exception e) {
    monitor.traceError(traceId, "market-analyst", e.getMessage());
    throw e;
}
```

如需对接 Prometheus，只需在工程中引入 `micrometer-registry-prometheus` 并把 `PrometheusMeterRegistry` 传入构造器即可，本模块无需改动。