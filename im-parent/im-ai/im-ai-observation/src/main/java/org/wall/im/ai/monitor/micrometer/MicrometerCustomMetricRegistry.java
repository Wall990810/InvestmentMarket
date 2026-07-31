package org.wall.im.ai.monitor.micrometer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.wall.im.ai.core.monitor.CustomMetricRegistry;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于Micrometer的自定义指标注册器实现
 */
public class MicrometerCustomMetricRegistry implements CustomMetricRegistry {

	private final MeterRegistry meterRegistry;

	private final Map<String, AtomicLong> gaugeValues = new ConcurrentHashMap<>();

	public MicrometerCustomMetricRegistry(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	@Override
	public void registerCounter(String name, String description) {
		Counter.builder(name).description(description).register(meterRegistry);
	}

	@Override
	public void incrementCounter(String name) {
		Counter.builder(name).register(meterRegistry).increment();
	}

	@Override
	public void registerGauge(String name, String description) {
		AtomicLong value = new AtomicLong(0);
		gaugeValues.put(name, value);
		meterRegistry.gauge(name, value);
	}

	@Override
	public void setGaugeValue(String name, double value) {
		AtomicLong gauge = gaugeValues.get(name);
		if (gauge != null) {
			gauge.set((long) value);
		}
	}

	@Override
	public void registerTimer(String name, String description) {
		Timer.builder(name).description(description).register(meterRegistry);
	}

	@Override
	public void recordTimer(String name, long durationMs) {
		Timer.builder(name).register(meterRegistry).record(Duration.ofMillis(durationMs));
	}

}
