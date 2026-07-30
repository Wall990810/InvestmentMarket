[← 返回索引](../README.md)

> 本文档为 [`usage-guide.md`](./usage-guide.md) 的拆分章节之一。

# 典型使用示例

## 10.1 组合 Micrometer + Langfuse，挂载到 Agent 调用

```java
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.wall.im.ai.core.model.MonitorConfig;
import org.wall.im.ai.core.monitor.AgentMonitor;
import org.wall.im.ai.monitor.composite.CompositeAgentMonitor;
import org.wall.im.ai.monitor.langfuse.LangfuseMonitor;
import org.wall.im.ai.monitor.langfuse.LangfuseMonitorFactory;
import org.wall.im.ai.monitor.micrometer.MicrometerAgentMonitor;

// 1) 主监控器：Micrometer（提供 traceId 与 CustomMetricRegistry）
MeterRegistry meterRegistry = new SimpleMeterRegistry();
MicrometerAgentMonitor primary = new MicrometerAgentMonitor(meterRegistry);

// 2) Langfuse 配置（可从 YAML 绑定得到）
MonitorConfig.LangfuseConfig langfuseConfig = new MonitorConfig.LangfuseConfig();
langfuseConfig.setEnabled(true);
langfuseConfig.setHost("https://cloud.langfuse.com");
langfuseConfig.setPublicKey("pk-lf-xxx");
langfuseConfig.setSecretKey("sk-lf-xxx");
langfuseConfig.setDebug(false);

// 3) 用 Langfuse 装饰 primary，使 Langfuse 上报与 Micrometer 指标同时生效
LangfuseMonitor langfuse = LangfuseMonitorFactory.create(langfuseConfig, primary);

// 4) 用 Composite 把 Langfuse 链作为 primary，再追加裸 Micrometer（如需独立计数）
//    这里演示最常见用法：直接把装饰后的 Langfuse 作为最终监控器使用
AgentMonitor monitor = langfuse;

// 5) 挂载到 Agent 执行流程
String traceId = monitor.traceStart("market-analyst", "今日A股走势");
try {
    // ... Agent 执行，得到 output / costMs / tokens ...
    monitor.traceToolCall(traceId, "stock-query",
            Map.of("symbol", "600519"), "{price: 1680}", 45);
    monitor.traceEnd(traceId, "market-analyst", output, costMs, tokens);
} catch (Exception e) {
    monitor.traceError(traceId, "market-analyst", e.getMessage());
    throw e;
}

// 6) 通过 CustomMetricRegistry 记录自定义指标（来自 primary）
monitor.getCustomMetricRegistry().incrementCounter("ai.rag.hit");
```

## 10.2 三后端联合（Composite + 装饰器链）

```java
// primary: Micrometer（指标 + traceId）
MicrometerAgentMonitor micrometer = new MicrometerAgentMonitor(meterRegistry);

// 用 Zipkin 包装 Micrometer：叠加链路，traceId 仍来自 Micrometer
ZipkinAgentTracer zipkin = new ZipkinAgentTracer(tracing, micrometer);

// 再用 Langfuse 包装：叠加 LLM 平台上报，traceId 仍来自最内层 Micrometer
LangfuseMonitor langfuse = LangfuseMonitorFactory.create(langfuseConfig, zipkin);

// 最终 monitor 即装饰器链顶端，一次调用同时触发 Langfuse → Zipkin → Micrometer
AgentMonitor monitor = langfuse;
```

说明：装饰器链天然保证调用顺序"由外向内"先执行外层逻辑再委托 delegate，因此上例中 `traceStart` 会先发 Langfuse 事件、再开 Zipkin Span、最后记 Micrometer 计数；`traceEnd` 同理。`traceId` 始终来自链最内层的 `MicrometerAgentMonitor`。

## 10.3 用 Composite 聚合互不包装的监控器

```java
CompositeAgentMonitor composite = new CompositeAgentMonitor(micrometer); // primary
composite.addMonitor(anotherMonitor);   // 副监控器，异常静默
// composite.getCustomMetricRegistry() 返回 micrometer 的注册器
```