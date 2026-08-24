package org.wall.im.ai.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Agent Monitor配置属性
 * <p>
 * 对应 application.yml 中的 im.ai.monitor 配置节点。
 * </p>
 *
 * <h3>配置示例</h3> <pre>
 * im:
 *   ai:
 *     monitor:
 *       enabled: true
 *       langfuse:
 *         enabled: true
 *         host: https://cloud.langfuse.com
 *         public-key: pk-lf-xxxxxxxxxxxx
 *         secret-key: sk-lf-xxxxxxxxxxxx
 *         debug: false
 *         flush-interval-ms: 5000
 *         max-batch-size: 50
 * </pre>
 */
@ConfigurationProperties(prefix = "im.ai.monitor")
public class AgentMonitorProperties {

	/**
	 * 是否启用Agent Monitor
	 */
	private boolean enabled = true;

	/**
	 * Langfuse配置
	 */
	private LangfuseProperties langfuse = new LangfuseProperties();

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public LangfuseProperties getLangfuse() {
		return langfuse;
	}

	public void setLangfuse(LangfuseProperties langfuse) {
		this.langfuse = langfuse;
	}

	public static class LangfuseProperties {

		/**
		 * 是否启用Langfuse上报
		 */
		private boolean enabled = false;

		/**
		 * Langfuse服务地址
		 */
		private String host = "http://localhost:3000";

		/**
		 * Langfuse公钥
		 */
		private String publicKey;

		/**
		 * Langfuse密钥
		 */
		private String secretKey;

		/**
		 * 是否开启调试模式
		 */
		private boolean debug = false;

		/**
		 * 批量刷新间隔（毫秒）
		 */
		private long flushIntervalMs = 5000;

		/**
		 * 最大批量大小
		 */
		private int maxBatchSize = 50;

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public String getHost() {
			return host;
		}

		public void setHost(String host) {
			this.host = host;
		}

		public String getPublicKey() {
			return publicKey;
		}

		public void setPublicKey(String publicKey) {
			this.publicKey = publicKey;
		}

		public String getSecretKey() {
			return secretKey;
		}

		public void setSecretKey(String secretKey) {
			this.secretKey = secretKey;
		}

		public boolean isDebug() {
			return debug;
		}

		public void setDebug(boolean debug) {
			this.debug = debug;
		}

		public long getFlushIntervalMs() {
			return flushIntervalMs;
		}

		public void setFlushIntervalMs(long flushIntervalMs) {
			this.flushIntervalMs = flushIntervalMs;
		}

		public int getMaxBatchSize() {
			return maxBatchSize;
		}

		public void setMaxBatchSize(int maxBatchSize) {
			this.maxBatchSize = maxBatchSize;
		}

	}

}
