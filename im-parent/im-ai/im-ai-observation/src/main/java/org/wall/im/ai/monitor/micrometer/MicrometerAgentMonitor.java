package org.wall.im.ai.monitor.micrometer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.wall.im.ai.core.monitor.AgentMonitor;
import org.wall.im.ai.core.monitor.CustomMetricRegistry;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于Micrometer的Agent监控实现
 * <p>集成Micrometer指标体系，支持自定义指标注册</p>
 */
public class MicrometerAgentMonitor implements AgentMonitor {

    private final MeterRegistry meterRegistry;
    private final MicrometerCustomMetricRegistry customMetricRegistry;
    private final Map<String, Long> activeTraces = new ConcurrentHashMap<>();

    public MicrometerAgentMonitor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.customMetricRegistry = new MicrometerCustomMetricRegistry(meterRegistry);
    }

    @Override
    public String traceStart(String agentName, String input) {
        String traceId = UUID.randomUUID().toString();
        activeTraces.put(traceId, System.currentTimeMillis());

        // 记录调用开始计数
        Counter.builder("ai.agent.calls")
                .tag("agent", agentName)
                .tag("status", "started")
                .register(meterRegistry)
                .increment();

        return traceId;
    }

    @Override
    public void traceEnd(String traceId, String agentName, String output, long costTimeMs, int tokenUsage) {
        activeTraces.remove(traceId);

        // 记录成功计数
        Counter.builder("ai.agent.calls")
                .tag("agent", agentName)
                .tag("status", "success")
                .register(meterRegistry)
                .increment();

        // 记录耗时
        Timer.builder("ai.agent.duration")
                .tag("agent", agentName)
                .register(meterRegistry)
                .record(java.time.Duration.ofMillis(costTimeMs));

        // 记录token使用量
        meterRegistry.gauge("ai.agent.tokens",
                io.micrometer.core.instrument.Tags.of("agent", agentName),
                tokenUsage);
    }

    @Override
    public void traceError(String traceId, String agentName, String error) {
        activeTraces.remove(traceId);

        Counter.builder("ai.agent.calls")
                .tag("agent", agentName)
                .tag("status", "error")
                .register(meterRegistry)
                .increment();
    }

    @Override
    public void traceToolCall(String traceId, String toolName, Map<String, Object> parameters,
                               String result, long costTimeMs) {
        Counter.builder("ai.tool.calls")
                .tag("tool", toolName)
                .tag("traceId", traceId)
                .register(meterRegistry)
                .increment();

        Timer.builder("ai.tool.duration")
                .tag("tool", toolName)
                .register(meterRegistry)
                .record(java.time.Duration.ofMillis(costTimeMs));
    }

    @Override
    public void recordMetric(String metricName, double value, Map<String, String> tags) {
        var tagList = tags.entrySet().stream()
                .map(e -> io.micrometer.core.instrument.Tag.of(e.getKey(), e.getValue()))
                .toList();
        meterRegistry.gauge(metricName, io.micrometer.core.instrument.Tags.of(tagList), value);
    }

    @Override
    public CustomMetricRegistry getCustomMetricRegistry() {
        return customMetricRegistry;
    }
}
