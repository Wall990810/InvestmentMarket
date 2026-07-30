[← 返回索引](../README.md)

> 本文档为 [`usage-guide.md`](./usage-guide.md) 的拆分章节之一。

# ZipkinAgentTracer

源码：[ZipkinAgentTracer.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-observation/src/main/java/org/wall/im/ai/monitor/zipkin/ZipkinAgentTracer.java)

`ZipkinAgentTracer` 是**装饰器**风格的实现：它持有一个 `delegate AgentMonitor`，所有调用都会**先委托给 delegate**，再叠加 Brave Span 逻辑。这样可以在不丢失既有监控（如 Micrometer）的前提下叠加链路追踪。

## 6.1 构造

```java
public ZipkinAgentTracer(Tracing tracing, AgentMonitor delegate);
```

- `tracing`：Brave `Tracing` 实例（由使用方构建，通常配合 `AsyncReporter` + `ZipkinSpanHandler` 上报到 Zipkin）。
- `delegate`：被包装的监控器，`traceStart` 返回的 `traceId` 即来自 delegate；`recordMetric` 与 `getCustomMetricRegistry` 完全透传给 delegate。

## 6.2 Span 创建与 trace 传播

| 方法 | Brave 行为 |
| --- | --- |
| `traceStart` | `tracer.nextSpan().name("agent." + agentName).start()`；打 tag `agent.name`、`agent.input`（超过 500 字符截断为 `前500 + "..."`）；以 delegate 的 `traceId` 为 key 存入 `activeSpans` |
| `traceEnd` | 取出 Span，打 tag `agent.output`（截断 500）、`agent.cost_ms`、`agent.token_usage`，调用 `span.finish()` |
| `traceError` | 取出 Span，打 tag `error`，`span.finish()` |
| `traceToolCall` | 以父 Span `context()` 调 `tracer.newChild(...)` 创建子 Span，命名 `tool.<toolName>`，打 tag `tool.name`、`tool.cost_ms`，`finish()` |

要点：

- **链路层级**：Agent 主 Span 为父，每个 `traceToolCall` 产生一个子 Span，形成 `agent → tool` 的父子关系，可在 Zipkin UI 中聚合查看。
- **上下文传播**：依赖 Brave 自身的 `TraceContext`，通过 `newChild(parentSpan.context())` 实现；当前实现不跨进程注入（如 HTTP header），适用于单进程内的 Agent/Tool 调用树。
- **输入截断**：`agent.input` 与 `agent.output` 都用私有方法 `truncate(str, 500)` 处理，避免超大文本污染 Span。

## 6.3 构建示例

```java
import brave.Tracing;
import brave.handler.SpanHandler;
import brave.propagation.B3Propagation;
import brave.sampler.Sampler;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.brave.ZipkinSpanHandler;
import zipkin2.reporter.urlconnection.URLConnectionSender;

AsyncReporter<zipkin2.Span> reporter = AsyncReporter.create(
        URLConnectionSender.create("http://localhost:9411/api/v2/spans"));

Tracing tracing = Tracing.newBuilder()
        .localServiceName("im-ai-agent")
        .addSpanHandler(ZipkinSpanHandler.create(reporter))
        .sampler(Sampler.ALWAYS_SAMPLE)
        .propagationFactory(B3Propagation.FACTORY)
        .build();

// 用 MicrometerAgentMonitor 作为 delegate，叠加 Zipkin 追踪
ZipkinAgentTracer tracer = new ZipkinAgentTracer(tracing, micrometerMonitor);
```

> Zipkin 上报依赖 `zipkin-reporter` 等工件，需由使用方自行引入；本模块只依赖 `brave` 核心。