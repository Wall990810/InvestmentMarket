package org.wall.im.ai.monitor.langfuse;

import com.langfuse.client.LangfuseClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wall.im.ai.core.model.MonitorConfig;
import org.wall.im.ai.core.monitor.AgentMonitor;

/**
 * LangfuseMonitor工厂类
 * <p>
 * 提供LangfuseMonitor的默认配置入口，简化初始化和配置流程
 * </p>
 *
 * <h3>使用示例</h3> <pre>{@code
 * // 方式1: 使用环境变量（推荐）
 * // 设置环境变量: LANGFUSE_PUBLIC_KEY, LANGFUSE_SECRET_KEY, LANGFUSE_HOST(可选)
 * LangfuseMonitor monitor = LangfuseMonitorFactory.fromEnvironment(delegate);
 *
 * // 方式2: 使用默认本地配置
 * LangfuseMonitor monitor = LangfuseMonitorFactory.createDefault(delegate);
 *
 * // 方式3: 自定义配置
 * MonitorConfig.LangfuseConfig config = new MonitorConfig.LangfuseConfig();
 * config.setEnabled(true);
 * config.setHost("https://cloud.langfuse.com");
 * config.setPublicKey("pk-lf-xxx");
 * config.setSecretKey("sk-lf-xxx");
 * LangfuseMonitor monitor = LangfuseMonitorFactory.create(config, delegate);
 * }</pre>
 */
public final class LangfuseMonitorFactory {

	private static final Logger log = LoggerFactory.getLogger(LangfuseMonitorFactory.class);

	/** 默认Langfuse本地部署地址 */
	public static final String DEFAULT_HOST = "http://localhost:3000";

	/** 环境变量名 */
	public static final String ENV_PUBLIC_KEY = "LANGFUSE_PUBLIC_KEY";

	public static final String ENV_SECRET_KEY = "LANGFUSE_SECRET_KEY";

	public static final String ENV_HOST = "LANGFUSE_HOST";

	private LangfuseMonitorFactory() {
		// 工具类禁止实例化
	}

	/**
	 * 使用默认配置创建LangfuseMonitor
	 * <p>
	 * 默认连接到本地Langfuse实例 (http://localhost:3000)
	 * </p>
	 * @param delegate 委托监控器
	 * @return LangfuseMonitor实例，若配置不完整则返回未启用的包装
	 */
	public static LangfuseMonitor createDefault(AgentMonitor delegate) {
		MonitorConfig.LangfuseConfig config = new MonitorConfig.LangfuseConfig();
		config.setEnabled(false);
		config.setHost(DEFAULT_HOST);
		return new LangfuseMonitor(config, buildClient(config), delegate);
	}

	/**
	 * 根据指定配置创建LangfuseMonitor
	 * @param config Langfuse配置
	 * @param delegate 委托监控器
	 * @return LangfuseMonitor实例
	 */
	public static LangfuseMonitor create(MonitorConfig.LangfuseConfig config, AgentMonitor delegate) {
		if (config == null) {
			throw new IllegalArgumentException("LangfuseConfig must not be null");
		}
		return new LangfuseMonitor(config, buildClient(config), delegate);
	}

	/**
	 * 从环境变量创建LangfuseMonitor
	 * <p>
	 * 读取以下环境变量:
	 * </p>
	 * <ul>
	 * <li>LANGFUSE_PUBLIC_KEY - 公开API密钥 (必需)</li>
	 * <li>LANGFUSE_SECRET_KEY - 私有API密钥 (必需)</li>
	 * <li>LANGFUSE_HOST - Langfuse服务地址 (可选，默认http://localhost:3000)</li>
	 * </ul>
	 * @param delegate 委托监控器
	 * @return LangfuseMonitor实例
	 */
	public static LangfuseMonitor fromEnvironment(AgentMonitor delegate) {
		MonitorConfig.LangfuseConfig config = new MonitorConfig.LangfuseConfig();

		String publicKey = System.getenv(ENV_PUBLIC_KEY);
		String secretKey = System.getenv(ENV_SECRET_KEY);
		String host = System.getenv(ENV_HOST);

		config.setPublicKey(publicKey);
		config.setSecretKey(secretKey);
		config.setHost(host != null && !host.isEmpty() ? host : DEFAULT_HOST);
		config.setEnabled(publicKey != null && !publicKey.isEmpty() && secretKey != null && !secretKey.isEmpty());

		if (!config.isEnabled()) {
			log.warn("Langfuse environment variables not set or incomplete. "
					+ "Monitoring will be disabled. Required: {}, {}", ENV_PUBLIC_KEY, ENV_SECRET_KEY);
		}

		return new LangfuseMonitor(config, buildClient(config), delegate);
	}

	/**
	 * 从已有的LangfuseClient创建LangfuseMonitor
	 * <p>
	 * 适用于需要自定义LangfuseClient配置（如自定义HTTP客户端、连接池等）的场景
	 * </p>
	 * @param client 已构建的LangfuseClient实例
	 * @param config Langfuse配置
	 * @param delegate 委托监控器
	 * @return LangfuseMonitor实例
	 */
	public static LangfuseMonitor fromClient(LangfuseClient client, MonitorConfig.LangfuseConfig config,
			AgentMonitor delegate) {
		return new LangfuseMonitor(config, client, delegate);
	}

	/**
	 * 创建默认的LangfuseConfig
	 * @return 默认配置实例
	 */
	public static MonitorConfig.LangfuseConfig defaultConfig() {
		MonitorConfig.LangfuseConfig config = new MonitorConfig.LangfuseConfig();
		config.setEnabled(false);
		config.setHost(DEFAULT_HOST);
		return config;
	}

	/**
	 * 构建LangfuseClient
	 */
	private static LangfuseClient buildClient(MonitorConfig.LangfuseConfig config) {
		var builder = LangfuseClient.builder().url(config.getHost() != null ? config.getHost() : DEFAULT_HOST);

		if (config.isConfigured()) {
			builder.credentials(config.getPublicKey(), config.getSecretKey());
		}

		return builder.build();
	}

}
