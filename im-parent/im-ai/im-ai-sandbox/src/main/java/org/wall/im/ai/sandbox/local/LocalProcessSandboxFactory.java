package org.wall.im.ai.sandbox.local;

import org.wall.im.ai.core.model.SandboxConfig;
import org.wall.im.ai.core.sandbox.Sandbox;
import org.wall.im.ai.core.sandbox.SandboxFactory;
import org.wall.im.ai.core.sandbox.SandboxType;

/**
 * 本地进程级沙盒工厂
 * <p>
 * 路由 {@link SandboxType#LOCAL_PROCESS} 类型，创建 {@link ProcessSandbox} 实例。
 * </p>
 */
public class LocalProcessSandboxFactory implements SandboxFactory {

	@Override
	public Sandbox create(SandboxConfig config) {
		return new ProcessSandbox(config);
	}

	@Override
	public SandboxType supportedType() {
		return SandboxType.LOCAL_PROCESS;
	}

}
