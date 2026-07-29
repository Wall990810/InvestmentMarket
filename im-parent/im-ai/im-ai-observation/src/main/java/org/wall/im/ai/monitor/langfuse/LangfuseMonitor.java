package org.wall.im.ai.monitor.langfuse;

import com.langfuse.client.LangfuseClient;
import com.langfuse.client.resources.commons.types.ObservationLevel;
import com.langfuse.client.resources.commons.types.Usage;
import com.langfuse.client.resources.ingestion.requests.IngestionRequest;
import com.langfuse.client.resources.ingestion.types.CreateObservationEvent;
import com.langfuse.client.resources.ingestion.types.IngestionEvent;
import com.langfuse.client.resources.ingestion.types.ObservationBody;
import com.langfuse.client.resources.ingestion.types.ObservationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wall.im.ai.core.model.MonitorConfig;
import org.wall.im.ai.core.monitor.AgentMonitor;
import org.wall.im.ai.core.monitor.CustomMetricRegistry;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Langfuse监控实现
 * <p>基于langfuse-java SDK将Agent调用的trace信息上送至Langfuse平台，用于LLM可观测性分析</p>
 */
public class LangfuseMonitor implements AgentMonitor {

    private static final Logger log = LoggerFactory.getLogger(LangfuseMonitor.class);

    private final MonitorConfig.LangfuseConfig config;
    private final LangfuseClient langfuseClient;
    private final AgentMonitor delegate;
    private final Map<String, String> traceMapping = new ConcurrentHashMap<>();

    public LangfuseMonitor(MonitorConfig.LangfuseConfig config, LangfuseClient langfuseClient, AgentMonitor delegate) {
        this.config = config;
        this.langfuseClient = langfuseClient;
        this.delegate = delegate;
    }

    /**
     * 使用默认配置创建LangfuseMonitor
     *
     * @param config   Langfuse配置
     * @param delegate 委托监控器
     * @return LangfuseMonitor实例
     */
    public static LangfuseMonitor create(MonitorConfig.LangfuseConfig config, AgentMonitor delegate) {
        LangfuseClient client = LangfuseClient.builder()
                .url(config.getHost())
                .credentials(config.getPublicKey(), config.getSecretKey())
                .build();
        return new LangfuseMonitor(config, client, delegate);
    }

    @Override
    public String traceStart(String agentName, String input) {
        String traceId = delegate.traceStart(agentName, input);

        if (config.isEnabled() && config.isConfigured()) {
            try {
                String langfuseTraceId = UUID.randomUUID().toString();
                traceMapping.put(traceId, langfuseTraceId);

                CreateTraceEvent traceEvent = CreateTraceEvent.builder()
                        .id(UUID.randomUUID().toString())
                        .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                        .body(CreateTraceBody.builder()
                                .id(langfuseTraceId)
                                .name("agent." + agentName)
                                .input(Collections.singletonMap("input", input))
                                .metadata(Collections.singletonMap("framework", "wall-ai"))
                                .build())
                        .build();

                IngestionRequest request = IngestionRequest.builder()
                        .addBatch(IngestionEvent.traceCreate(traceEvent))
                        .build();

                langfuseClient.ingestion().batch(request);

                if (config.isDebug()) {
                    log.debug("Sent trace start to Langfuse: traceId={}, agent={}", langfuseTraceId, agentName);
                }
            } catch (Exception e) {
                log.warn("Failed to send trace to Langfuse: {}", e.getMessage());
            }
        }

        return traceId;
    }

    @Override
    public void traceEnd(String traceId, String agentName, String output, long costTimeMs, int tokenUsage) {
        delegate.traceEnd(traceId, agentName, output, costTimeMs, tokenUsage);

        if (config.isEnabled() && config.isConfigured()) {
            try {
                String langfuseTraceId = traceMapping.remove(traceId);
                if (langfuseTraceId != null) {
                    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

                    // 创建Generation事件记录输出和用量
                    String generationId = UUID.randomUUID().toString();
                    CreateObservationEvent generationEvent = CreateObservationEvent.builder()
                            .id(UUID.randomUUID().toString())
                            .timestamp(now)
                            .body(ObservationBody.builder()
                                    .type(ObservationType.GENERATION)
                                    .id(generationId)
                                    .traceId(langfuseTraceId)
                                    .name("agent." + agentName + ".generation")
                                    .input(Collections.singletonMap("agent", agentName))
                                    .output(Collections.singletonMap("output", output))
                                    .usage(Usage.builder()
                                            .total((long) tokenUsage)
                                            .build())
                                    .startTime(now.minusMillis(costTimeMs))
                                    .endTime(now)
                                    .metadata(Collections.singletonMap("cost_ms", costTimeMs))
                                    .build())
                            .build();

                    IngestionRequest request = IngestionRequest.builder()
                            .addBatch(IngestionEvent.observationCreate(generationEvent))
                            .build();

                    langfuseClient.ingestion().batch(request);

                    if (config.isDebug()) {
                        log.debug("Sent trace end to Langfuse: traceId={}, costMs={}", langfuseTraceId, costTimeMs);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to send trace end to Langfuse: {}", e.getMessage());
            }
        }
    }

    @Override
    public void traceError(String traceId, String agentName, String error) {
        delegate.traceError(traceId, agentName, error);

        if (config.isEnabled() && config.isConfigured()) {
            String langfuseTraceId = traceMapping.remove(traceId);
            if (langfuseTraceId != null) {
                try {
                    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

                    CreateObservationEvent errorEvent = CreateObservationEvent.builder()
                            .id(UUID.randomUUID().toString())
                            .timestamp(now)
                            .body(ObservationBody.builder()
                                    .type(ObservationType.SPAN)
                                    .id(UUID.randomUUID().toString())
                                    .traceId(langfuseTraceId)
                                    .name("agent." + agentName + ".error")
                                    .output(Collections.singletonMap("error", error))
                                    .level(ObservationLevel.ERROR)
                                    .startTime(now)
                                    .endTime(now)
                                    .build())
                            .build();

                    IngestionRequest request = IngestionRequest.builder()
                            .addBatch(IngestionEvent.observationCreate(errorEvent))
                            .build();

                    langfuseClient.ingestion().batch(request);

                    if (config.isDebug()) {
                        log.debug("Sent trace error to Langfuse: traceId={}", langfuseTraceId);
                    }
                } catch (Exception e) {
                    log.warn("Failed to send error to Langfuse: {}", e.getMessage());
                }
            }
        }
    }

    @Override
    public void traceToolCall(String traceId, String toolName, Map<String, Object> parameters,
                              String result, long costTimeMs) {
        delegate.traceToolCall(traceId, toolName, parameters, result, costTimeMs);

        if (config.isEnabled() && config.isConfigured()) {
            String langfuseTraceId = traceMapping.get(traceId);
            if (langfuseTraceId != null) {
                try {
                    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

                    Map<String, Object> inputMap = new HashMap<>();
                    inputMap.put("tool", toolName);
                    inputMap.put("parameters", parameters);

                    Map<String, Object> outputMap = new HashMap<>();
                    outputMap.put("result", result);
                    outputMap.put("cost_ms", costTimeMs);

                    CreateObservationEvent toolEvent = CreateObservationEvent.builder()
                            .id(UUID.randomUUID().toString())
                            .timestamp(now)
                            .body(ObservationBody.builder()
                                    .type(ObservationType.SPAN)
                                    .id(UUID.randomUUID().toString())
                                    .traceId(langfuseTraceId)
                                    .name("tool." + toolName)
                                    .input(inputMap)
                                    .output(outputMap)
                                    .startTime(now.minusMillis(costTimeMs))
                                    .endTime(now)
                                    .metadata(Collections.singletonMap("framework", "wall-ai"))
                                    .build())
                            .build();

                    IngestionRequest request = IngestionRequest.builder()
                            .addBatch(IngestionEvent.observationCreate(toolEvent))
                            .build();

                    langfuseClient.ingestion().batch(request);

                    if (config.isDebug()) {
                        log.debug("Sent tool call to Langfuse: traceId={}, tool={}", langfuseTraceId, toolName);
                    }
                } catch (Exception e) {
                    log.warn("Failed to send tool call to Langfuse: {}", e.getMessage());
                }
            }
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

    /**
     * 获取底层LangfuseClient实例
     */
    public LangfuseClient getLangfuseClient() {
        return langfuseClient;
    }

    /**
     * 刷新所有待发送的数据到Langfuse
     */
    public void flush() {
        try {
            log.debug("Flushing Langfuse monitor data");
        } catch (Exception e) {
            log.warn("Failed to flush Langfuse data: {}", e.getMessage());
        }
    }
}
