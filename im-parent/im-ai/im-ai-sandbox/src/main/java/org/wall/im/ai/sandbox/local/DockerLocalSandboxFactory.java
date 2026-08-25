package org.wall.im.ai.sandbox.local;

import org.wall.im.ai.core.model.SandboxConfig;
import org.wall.im.ai.core.sandbox.Sandbox;
import org.wall.im.ai.core.sandbox.SandboxFactory;
import org.wall.im.ai.core.sandbox.SandboxType;

/**
 * 本地 Docker 容器沙盒工厂
 * <p>
 * 路由 {@link SandboxType#LOCAL_DOCKER} 类型，创建 {@link DockerLocalSandbox} 实例。 需要宿主 PATH 中存在
 * {@code docker} CLI。
 * </p>
 */
public class DockerLocalSandboxFactory implements SandboxFactory {

	@Override
	public Sandbox create(SandboxConfig config) {
		return new DockerLocalSandbox(config);
	}

	@Override
	public SandboxType supportedType() {
		return SandboxType.LOCAL_DOCKER;
	}

}
