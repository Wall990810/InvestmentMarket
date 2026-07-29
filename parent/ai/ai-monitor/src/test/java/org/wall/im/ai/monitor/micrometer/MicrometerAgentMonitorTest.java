package org.wall.im.ai.monitor.micrometer;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.wall.im.ai.core.monitor.CustomMetricRegistry;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MicrometerAgentMonitor单元测试
 */
@DisplayName("MicrometerAgentMonitor测试")
class MicrometerAgentMonitorTest {

    private MeterRegistry meterRegistry;
    private MicrometerAgentMonitor monitor;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        monitor = new MicrometerAgentMonitor(meterRegistry);
    }

    @Nested
    @DisplayName("Trace生命周期测试")
    class TraceLifecycleTest {

        @Test
        @DisplayName("traceStart应返回非空traceId")
        void traceStart_shouldReturnTraceId() {
            String traceId = monitor.traceStart("test-agent", "hello");
            assertNotNull(traceId);
            assertFalse(traceId.isEmpty());
        }

        @Test
        @DisplayName("traceStart应记录started计数器")
        void traceStart_shouldIncrementStartedCounter() {
            monitor.traceStart("my-agent", "input");

            var counter = meterRegistry.find("ai.agent.calls")
                    .tag("agent", "my-agent")
                    .tag("status", "started")
                    .counter();
            assertNotNull(counter);
            assertEquals(1.0, counter.count());
        }

        @Test
        @DisplayName("traceEnd应记录success计数器和耗时")
        void traceEnd_shouldRecordSuccessAndDuration() {
            String traceId = monitor.traceStart("agent", "input");
            monitor.traceEnd(traceId, "agent", "output", 500, 100);

            var successCounter = meterRegistry.find("ai.agent.calls")
                    .tag("agent", "agent")
                    .tag("status", "success")
                    .counter();
            assertNotNull(successCounter);
            assertEquals(1.0, successCounter.count());

            var timer = meterRegistry.find("ai.agent.duration")
                    .tag("agent", "agent")
                    .timer();
            assertNotNull(timer);
            assertEquals(1, timer.count());
        }

        @Test
        @DisplayName("traceError应记录error计数器")
        void traceError_shouldIncrementErrorCounter() {
            String traceId = monitor.traceStart("agent", "input");
            monitor.traceError(traceId, "agent", "NullPointerException");

            var errorCounter = meterRegistry.find("ai.agent.calls")
                    .tag("agent", "agent")
                    .tag("status", "error")
                    .counter();
            assertNotNull(errorCounter);
            assertEquals(1.0, errorCounter.count());
        }
    }

    @Nested
    @DisplayName("Tool调用追踪测试")
    class ToolCallTraceTest {

        @Test
        @DisplayName("traceToolCall应记录tool调用计数和耗时")
        void traceToolCall_shouldRecordToolMetrics() {
            String traceId = monitor.traceStart("agent", "input");
            monitor.traceToolCall(traceId, "calculator", Map.of("a", 1), "42", 50);

            var toolCounter = meterRegistry.find("ai.tool.calls")
                    .tag("tool", "calculator")
                    .counter();
            assertNotNull(toolCounter);
            assertEquals(1.0, toolCounter.count());

            var toolTimer = meterRegistry.find("ai.tool.duration")
                    .tag("tool", "calculator")
                    .timer();
            assertNotNull(toolTimer);
        }
    }

    @Nested
    @DisplayName("自定义指标测试")
    class CustomMetricTest {

        @Test
        @DisplayName("recordMetric应注册gauge指标")
        void recordMetric_shouldRegisterGauge() {
            monitor.recordMetric("custom.metric", 42.0, Map.of("env", "test"));

            var gauge = meterRegistry.find("custom.metric")
                    .tag("env", "test")
                    .gauge();
            assertNotNull(gauge);
            assertEquals(42.0, gauge.value());
        }

        @Test
        @DisplayName("getCustomMetricRegistry应返回非null")
        void getCustomMetricRegistry_shouldNotBeNull() {
            CustomMetricRegistry registry = monitor.getCustomMetricRegistry();
            assertNotNull(registry);
        }
    }

    @Nested
    @DisplayName("自定义指标注册器测试")
    class CustomMetricRegistryTest {

        @Test
        @DisplayName("Counter注册和递增")
        void counterRegisterAndIncrement() {
            CustomMetricRegistry registry = monitor.getCustomMetricRegistry();
            registry.registerCounter("test.counter", "A test counter");
            registry.incrementCounter("test.counter");
            registry.incrementCounter("test.counter");

            var counter = meterRegistry.find("test.counter").counter();
            assertNotNull(counter);
            assertEquals(2.0, counter.count());
        }

        @Test
        @DisplayName("Timer注册和记录")
        void timerRegisterAndRecord() {
            CustomMetricRegistry registry = monitor.getCustomMetricRegistry();
            registry.registerTimer("test.timer", "A test timer");
            registry.recordTimer("test.timer", 100);

            var timer = meterRegistry.find("test.timer").timer();
            assertNotNull(timer);
            assertEquals(1, timer.count());
        }
    }
}
