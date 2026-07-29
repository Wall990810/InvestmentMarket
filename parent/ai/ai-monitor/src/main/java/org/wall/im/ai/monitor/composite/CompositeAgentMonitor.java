package org.wall.im.ai.monitor.composite;

import org.wall.im.ai.core.monitor.AgentMonitor;
import org.wall.im.ai.core.monitor.CustomMetricRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 组合监控器
 * <p>将多个AgentMonitor组合在一起，实现Micrometer + Zipkin + Langfuse的联合监控</p>
 */
public class CompositeAgentMonitor implements AgentMonitor {

    private final List<AgentMonitor> monitors = new ArrayList<>();
    private final AgentMonitor primary;

    public CompositeAgentMonitor(AgentMonitor primary) {
        this.primary = primary;
    }

    public void addMonitor(AgentMonitor monitor) {
        monitors.add(monitor);
    }

    @Override
    public String traceStart(String agentName, String input) {
        String traceId = primary.traceStart(agentName, input);
        for (AgentMonitor monitor : monitors) {
            try {
                monitor.traceStart(agentName, input);
            } catch (Exception ignored) {
                // 非主要监控器失败不影响主流程
            }
        }
        return traceId;
    }

    @Override
    public void traceEnd(String traceId, String agentName, String output, long costTimeMs, int tokenUsage) {
        primary.traceEnd(traceId, agentName, output, costTimeMs, tokenUsage);
        for (AgentMonitor monitor : monitors) {
            try {
                monitor.traceEnd(traceId, agentName, output, costTimeMs, tokenUsage);
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void traceError(String traceId, String agentName, String error) {
        primary.traceError(traceId, agentName, error);
        for (AgentMonitor monitor : monitors) {
            try {
                monitor.traceError(traceId, agentName, error);
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void traceToolCall(String traceId, String toolName, Map<String, Object> parameters,
                               String result, long costTimeMs) {
        primary.traceToolCall(traceId, toolName, parameters, result, costTimeMs);
        for (AgentMonitor monitor : monitors) {
            try {
                monitor.traceToolCall(traceId, toolName, parameters, result, costTimeMs);
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void recordMetric(String metricName, double value, Map<String, String> tags) {
        primary.recordMetric(metricName, value, tags);
        for (AgentMonitor monitor : monitors) {
            try {
                monitor.recordMetric(metricName, value, tags);
            } catch (Exception ignored) {}
        }
    }

    @Override
    public CustomMetricRegistry getCustomMetricRegistry() {
        return primary.getCustomMetricRegistry();
    }
}
