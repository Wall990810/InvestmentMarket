[← 返回索引](../README.md)

> 本文档为 [`usage-guide.md`](./usage-guide.md) 的拆分章节之一。

# MicrometerCustomMetricRegistry

源码：[MicrometerCustomMetricRegistry.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-observation/src/main/java/org/wall/im/ai/monitor/micrometer/MicrometerCustomMetricRegistry.java)

该类实现 `CustomMetricRegistry`，是 `MicrometerAgentMonitor.getCustomMetricRegistry()` 的返回对象。它把 SPI 的三类操作映射到 Micrometer：

```java
public MicrometerCustomMetricRegistry(MeterRegistry meterRegistry);
```

## SPI 方法到 Micrometer 的映射

| SPI 方法 | 实现行为 |
| --- | --- |
| `registerCounter(name, desc)` | `Counter.builder(name).description(desc).register(meterRegistry)` |
| `incrementCounter(name)` | `Counter.builder(name).register(meterRegistry).increment()` |
| `registerGauge(name, desc)` | 新建 `AtomicLong(0)` 存入内部 `gaugeValues` map，并 `meterRegistry.gauge(name, value)` 注册 |
| `setGaugeValue(name, value)` | 从 `gaugeValues` 取出对应 `AtomicLong`，调用 `set((long) value)` |
| `registerTimer(name, desc)` | `Timer.builder(name).description(desc).register(meterRegistry)` |
| `recordTimer(name, durationMs)` | `Timer.builder(name).register(meterRegistry).record(Duration.ofMillis(durationMs))` |

## 使用示例

```java
MicrometerAgentMonitor monitor = new MicrometerAgentMonitor(registry);
CustomMetricRegistry custom = monitor.getCustomMetricRegistry();

// 1) 注册并使用计数器
custom.registerCounter("ai.rag.hit", "RAG 命中次数");
custom.incrementCounter("ai.rag.hit");

// 2) 注册并更新 gauge
custom.registerGauge("ai.context.tokens", "当前上下文 token 数");
custom.setGaugeValue("ai.context.tokens", 3200);

// 3) 注册并记录耗时
custom.registerTimer("ai.retrieval.duration", "检索耗时");
custom.recordTimer("ai.retrieval.duration", 120);
```

> 注意：`registerGauge` 必须在 `setGaugeValue` 之前调用——内部 `gaugeValues` map 在注册时才创建条目，未注册的 name 调用 `setGaugeValue` 会被静默忽略（`gauge == null`）。