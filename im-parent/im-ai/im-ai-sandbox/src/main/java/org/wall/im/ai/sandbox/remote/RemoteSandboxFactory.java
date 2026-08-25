package org.wall.im.ai.sandbox.remote;

import org.wall.im.ai.core.model.SandboxConfig;
import org.wall.im.ai.core.sandbox.Sandbox;
import org.wall.im.ai.core.sandbox.SandboxFactory;
import org.wall.im.ai.core.sandbox.SandboxType;

/**
 * 远端 HTTP 沙盒工厂
 * <p>
 * 路由 {@link SandboxType#REMOTE_HTTP} 类型，校验 {@link SandboxConfig#getRemoteEndpoint()} 非空后
 * 创建 {@link RemoteSandbox}（基于 {@link HttpRemoteSandboxClient}）。
 * </p>
 */
public class RemoteSandboxFactory implements SandboxFactory {

	@Override
	public Sandbox create(SandboxConfig config) {
		if (config == null) {
			throw new IllegalArgumentException("SandboxConfig is required for REMOTE_HTTP sandbox");
		}
		String endpoint = config.getRemoteEndpoint();
		if (endpoint == null || endpoint.isBlank()) {
			throw new IllegalArgumentException("remoteEndpoint is required for REMOTE_HTTP sandbox");
		}
		HttpRemoteSandboxClient client = new HttpRemoteSandboxClient(endpoint);
		return new RemoteSandbox(config, client);
	}

	@Override
	public SandboxType supportedType() {
		return SandboxType.REMOTE_HTTP;
	}

}
