[← 返回索引](../README.md)

> 本文档为 [`usage-guide.md`](./usage-guide.md) 的拆分章节之一。

# 扩展自定义监控

实现 `AgentMonitor` 即可接入。可参考本模块的装饰器模式：包装一个已有监控器，仅覆写需要增强的方法，其余透传。

```java
public class LoggingAgentMonitor implements AgentMonitor {
    private final AgentMonitor delegate;
    public LoggingAgentMonitor(AgentMonitor delegate) { this.delegate = delegate; }

    @Override
    public String traceStart(String agentName, String input) {
        String traceId = delegate.traceStart(agentName, input);
        System.out.println("[trace-start] agent=" + agentName + " traceId=" + traceId);
        return traceId;   // 必须返回 delegate 的 traceId 以保持链路一致
    }

    @Override
    public void traceEnd(String traceId, String agentName, String output,
                         long costTimeMs, int tokenUsage) {
        delegate.traceEnd(traceId, agentName, output, costTimeMs, tokenUsage);
        System.out.println("[trace-end] agent=" + agentName
                + " costMs=" + costTimeMs + " tokens=" + tokenUsage);
    }

    @Override
    public void traceError(String traceId, String agentName, String error) {
        delegate.traceError(traceId, agentName, error);
        System.err.println("[trace-error] agent=" + agentName + " error=" + error);
    }

    @Override
    public void traceToolCall(String traceId, String toolName, Map<String, Object> parameters,
                              String result, long costTimeMs) {
        delegate.traceToolCall(traceId, toolName, parameters, result, costTimeMs);
    }

    @Override
    public void recordMetric(String metricName, double value, Map<String, String> tags) {
        delegate.recordMetric(metricName, value, tags);
    }

    @Override
    public CustomMetricRegistry getCustomMetricRegistry() {
        return delegate.getCustomMetricRegistry();
    }
}
```

## 扩展建议

- **保持 traceId 一致**：装饰器应返回 delegate 的 `traceId`，避免下游 `traceEnd` 找不到上下文。
- **先 delegate 后增强**：参考 `ZipkinAgentTracer` / `LangfuseMonitor`，先调用 delegate 再执行副作用，确保主流程数据不丢。
- **静默容错**：若自定义监控器副作用可能失败，请内部 try/catch，避免影响业务；加入 `CompositeAgentMonitor` 作为副监控器时框架已统一兜底，但作为 primary 时仍需自行保证。
- **自定义指标**：通过 `getCustomMetricRegistry()` 暴露 `CustomMetricRegistry`，或直接复用 `MicrometerCustomMetricRegistry` 包装自有 `MeterRegistry`。