package org.wall.im.ai.sandbox.config;

import org.wall.im.ai.core.sandbox.SandboxType;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 沙盒配置属性
 * <p>
 * 绑定 {@code im.ai.sandbox} 前缀的配置，便于 Spring Boot 项目通过 application.yml 调整默认沙盒类型、Docker 镜像、
 * 远端 endpoint 等。非 Spring 环境可忽略本类，直接编程式构造 {@link org.wall.im.ai.core.model.SandboxConfig}。
 * </p>
 */
@ConfigurationProperties(prefix = "im.ai.sandbox")
public class SandboxProperties {

	/** 是否启用沙盒自动装配 */
	private boolean enabled = true;

	/** 默认沙盒类型 */
	private SandboxType defaultType = SandboxType.LOCAL_PROCESS;

	/** Docker 沙盒配置 */
	private DockerProperties docker = new DockerProperties();

	/** 远端沙盒配置 */
	private RemoteProperties remote = new RemoteProperties();

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public SandboxType getDefaultType() {
		return defaultType;
	}

	public void setDefaultType(SandboxType defaultType) {
		this.defaultType = defaultType;
	}

	public DockerProperties getDocker() {
		return docker;
	}

	public void setDocker(DockerProperties docker) {
		this.docker = docker;
	}

	public RemoteProperties getRemote() {
		return remote;
	}

	public void setRemote(RemoteProperties remote) {
		this.remote = remote;
	}

	public static class DockerProperties {

		/** 默认镜像 */
		private String image = "openjdk:26-slim";

		/** 镜像不存在时是否自动拉取 */
		private boolean autoPull = false;

		public String getImage() {
			return image;
		}

		public void setImage(String image) {
			this.image = image;
		}

		public boolean isAutoPull() {
			return autoPull;
		}

		public void setAutoPull(boolean autoPull) {
			this.autoPull = autoPull;
		}

	}

	public static class RemoteProperties {

		/** 远端执行服务基地址 */
		private String endpoint;

		/** 连接超时（秒） */
		private int connectTimeoutSec = 10;

		public String getEndpoint() {
			return endpoint;
		}

		public void setEndpoint(String endpoint) {
			this.endpoint = endpoint;
		}

		public int getConnectTimeoutSec() {
			return connectTimeoutSec;
		}

		public void setConnectTimeoutSec(int connectTimeoutSec) {
			this.connectTimeoutSec = connectTimeoutSec;
		}

	}

}
