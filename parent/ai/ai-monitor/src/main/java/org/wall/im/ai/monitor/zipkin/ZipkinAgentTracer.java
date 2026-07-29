package org.wall.im.ai.monitor.zipkin;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import brave.propagation.TraceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wall.im.ai.core.monitor.AgentMonitor;
import org.wall.im.ai.core.monitor.CustomMetricRegistry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于Zipkin Brave的链路追踪实现
 * <p>将Agent调用链路通过Zipkin Brave进行分布式追踪记录</p>
 */
public class ZipkinAgentTracer implements AgentMonitor {

    private static final Logger log = LoggerFactory.getLogger(ZipkinAgentTracer.class);

    private final Tracing tracing;
    private final Tracer tracer;
    private final Map<String, Span> activeSpans = new ConcurrentHashMap<>();
    private final AgentMonitor delegate;

    public ZipkinAgentTracer(Tracing tracing, AgentMonitor delegate) {
        this.tracing = tracing;
        this.tracer = tracing.tracer();
        this.delegate = delegate;
    }

    @Override
    public String traceStart(String agentName, String input) {
        String traceId = delegate.traceStart(agentName, input);

        Span span = tracer.nextSpan().name("agent." + agentName).start();
        span.tag("agent.name", agentName);
        span.tag("agent.input", truncate(input, 500));
        activeSpans.put(traceId, span);

        log.debug("Zipkin span started for agent: {}, traceId: {}", agentName, traceId);
        return traceId;
    }

    @Override
    public void traceEnd(String traceId, String agentName, String output, long costTimeMs, int tokenUsage) {
        delegate.traceEnd(traceId, agentName, output, costTimeMs, tokenUsage);

        Span span = activeSpans.remove(traceId);
        if (span != null) {
            span.tag("agent.output", truncate(output, 500));
            span.tag("agent.cost_ms", String.valueOf(costTimeMs));
            span.tag("agent.token_usage", String.valueOf(tokenUsage));
            span.finish();
            log.debug("Zipkin span finished for agent: {}, traceId: {}", agentName, traceId);
        }
    }

    @Override
    public void traceError(String traceId, String agentName, String error) {
        delegate.traceError(traceId, agentName, error);

        Span span = activeSpans.remove(traceId);
        if (span != null) {
            span.tag("error", error);
            span.finish();
        }
    }

    @Override
    public void traceToolCall(String traceId, String toolName, Map<String, Object> parameters,
                               String result, long costTimeMs) {
        delegate.traceToolCall(traceId, toolName, parameters, result, costTimeMs);

        Span parentSpan = activeSpans.get(traceId);
        if (parentSpan != null) {
            Span childSpan = tracer.newChild(parentSpan.context())
                    .name("tool." + toolName)
                    .start();
            childSpan.tag("tool.name", toolName);
            childSpan.tag("tool.cost_ms", String.valueOf(costTimeMs));
            childSpan.finish();
        }
    }

    @Override
    public void recordMetric(String metricName, double value, Map<String, String> tags) {
        delegate.recordMetric(metricName, value, tags);
    }

    @Override
    public CustomMetricRegistry getCustomMetricRegistry() {
        return delegate.getCustomMetricRegistry();
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        return str.length() > maxLen ? str.substring(0, maxLen) + "..." : str;
    }
}
