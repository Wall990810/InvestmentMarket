package org.wall.im.ai.sandbox.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.wall.im.ai.core.sandbox.SandboxFactory;
import org.wall.im.ai.core.sandbox.policy.CommandPolicy;
import org.wall.im.ai.sandbox.SandboxRegistry;
import org.wall.im.ai.sandbox.policy.DefaultCommandPolicy;

import java.util.List;

/**
 * 沙盒 Spring Boot 自动装配
 * <p>
 * 扫描所有 {@link SandboxFactory} Bean（Spring 注入优先），用 {@link SandboxRegistry} 收集并按
 * {@link org.wall.im.ai.core.sandbox.SandboxType} 路由。非 Spring 环境下 {@link SandboxRegistry}
 * 兜底用 {@link java.util.ServiceLoader} 加载内置 factory。
 * </p>
 *
 * <p>
 * 注意：不创建 {@code SandboxManager} Bean，因为每个 Agent 任务通常需要独立的 sandbox 实例（per-task
 * config）。调用方应按需 {@code new SandboxManager(config, registry, policy)}。
 * </p>
 */
@AutoConfiguration
@EnableConfigurationProperties(SandboxProperties.class)
@ConditionalOnProperty(prefix = "im.ai.sandbox", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SandboxAutoConfiguration {

	/**
	 * 沙盒工厂注册表：收集所有 SandboxFactory Bean + ServiceLoader 兜底
	 */
	@Bean
	@ConditionalOnMissingBean
	public SandboxRegistry sandboxRegistry(List<SandboxFactory> factories) {
		return new SandboxRegistry(factories);
	}

	/**
	 * 默认命令策略：迁移旧硬编码黑名单
	 */
	@Bean
	@ConditionalOnMissingBean
	public CommandPolicy defaultCommandPolicy() {
		return new DefaultCommandPolicy();
	}

}
