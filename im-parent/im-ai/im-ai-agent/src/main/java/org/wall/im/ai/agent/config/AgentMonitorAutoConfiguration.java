package org.wall.im.ai.agent.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.wall.im.ai.core.model.MonitorConfig;
import org.wall.im.ai.core.monitor.AgentMonitor;
import org.wall.im.ai.monitor.langfuse.LangfuseMonitor;
import org.wall.im.ai.monitor.langfuse.LangfuseMonitorFactory;
import org.wall.im.ai.monitor.micrometer.MicrometerAgentMonitor;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Agent监控自动配置
 * <p>
 * 自动装配AgentMonitor到Spring容器中，支持：
 * <ul>
 * <li>Micrometer: 内置监控指标，对接Prometheus/Zipkin</li>
 * <li>Langfuse: LLM可观测性平台，记录每次Agent调用和Tool调用的完整trace</li>
 * </ul>
 *
 * <p>
 * 配置示例 (application.yml):
 * </p>
 * <pre>{@code
 * im.ai.monitor:
 *   enabled: true
 *   langfuse:
 *     enabled: true
 *     host: https://cloud.langfuse.com
 *     public-key: pk-lf-xxx
 *     secret-key: sk-lf-xxx
 *     debug: false
 * }</pre>
 */
@AutoConfiguration
@ConfigurationPropertiesScan(basePackageClasses = AgentMonitorProperties.class)
public class AgentMonitorAutoConfiguration {

	private static final Logger log = LoggerFactory.getLogger(AgentMonitorAutoConfiguration.class);

	private final AgentMonitorProperties properties;

	public AgentMonitorAutoConfiguration(AgentMonitorProperties properties) {
		this.properties = properties;
	}

	/**
	 * 创建MicrometerAgentMonitor作为基础监控，如果配置了Langfuse则进行包装
	 */
	@Bean
	@ConditionalOnMissingBean(AgentMonitor.class)
	@ConditionalOnProperty(prefix = "im.ai.monitor", name = "enabled", havingValue = "true", matchIfMissing = true)
	public AgentMonitor agentMonitor(MeterRegistry meterRegistry) {
		log.info("Creating MicrometerAgentMonitor as base AgentMonitor");

		MicrometerAgentMonitor micrometerMonitor = new MicrometerAgentMonitor(
				meterRegistry != null ? meterRegistry : new SimpleMeterRegistry());

		// 检查是否需要包装Langfuse
		AgentMonitorProperties.LangfuseProperties langfuseProps = properties.getLangfuse();
		if (langfuseProps != null && isLangfuseConfigured(langfuseProps)) {
			log.info("Langfuse monitoring enabled, wrapping MicrometerAgentMonitor with LangfuseMonitor");
			return createLangfuseMonitor(micrometerMonitor, langfuseProps);
		}
		else {
			log.info("Langfuse not configured, using MicrometerAgentMonitor only");
			return micrometerMonitor;
		}
	}

	/**
	 * 创建LangfuseMonitor包装MicrometerAgentMonitor
	 */
	private AgentMonitor createLangfuseMonitor(MicrometerAgentMonitor micrometerMonitor,
			AgentMonitorProperties.LangfuseProperties langfuseProps) {
		MonitorConfig.LangfuseConfig config = new MonitorConfig.LangfuseConfig();
		config.setEnabled(true);
		config.setHost(langfuseProps.getHost() != null ? langfuseProps.getHost() : "http://localhost:3000");
		config.setPublicKey(langfuseProps.getPublicKey());
		config.setSecretKey(langfuseProps.getSecretKey());
		config.setDebug(langfuseProps.isDebug());
		config.setFlushIntervalMs(langfuseProps.getFlushIntervalMs());
		config.setMaxBatchSize(langfuseProps.getMaxBatchSize());

		LangfuseMonitor langfuseMonitor = LangfuseMonitorFactory.create(config, micrometerMonitor);
		log.info("LangfuseMonitor created successfully: host={}, debug={}", config.getHost(), config.isDebug());
		return langfuseMonitor;
	}

	/**
	 * 判断Langfuse是否已配置
	 */
	private boolean isLangfuseConfigured(AgentMonitorProperties.LangfuseProperties props) {
		return props.getPublicKey() != null && !props.getPublicKey().isEmpty() && props.getSecretKey() != null
				&& !props.getSecretKey().isEmpty();
	}

}
