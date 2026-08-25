package org.wall.im.ai.sandbox.remote;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wall.im.ai.core.model.SandboxConfig;
import org.wall.im.ai.core.sandbox.Sandbox;
import org.wall.im.ai.core.sandbox.SandboxResult;

/**
 * 远端沙盒
 * <p>
 * 适配 {@link Sandbox} SPI，所有调用委托给 {@link RemoteSandboxClient}。 {@link #initialize()}
 * 后持有远端分配的 sandboxId，后续 execute/executeCommand/isPathAllowed 通过该 id 路由。
 * {@link #destroy()} 调用 client.destroy。
 * </p>
 */
public class RemoteSandbox implements Sandbox {

	private static final Logger log = LoggerFactory.getLogger(RemoteSandbox.class);

	private final SandboxConfig config;

	private final RemoteSandboxClient client;

	private String sandboxId;

	private volatile boolean initialized = false;

	public RemoteSandbox(SandboxConfig config, RemoteSandboxClient client) {
		this.config = config;
		this.client = client;
	}

	@Override
	public void initialize() {
		if (initialized) {
			return;
		}
		this.sandboxId = client.initialize(config);
		initialized = true;
		log.info("Remote sandbox initialized: sandboxId={}", sandboxId);
	}

	@Override
	public SandboxResult execute(String code, String workDir) {
		ensureInitialized();
		return client.execute(sandboxId, code, workDir);
	}

	@Override
	public SandboxResult executeCommand(String command) {
		ensureInitialized();
		return client.executeCommand(sandboxId, command);
	}

	@Override
	public boolean isPathAllowed(String path) {
		if (!initialized) {
			return false;
		}
		return client.isPathAllowed(sandboxId, path);
	}

	@Override
	public void destroy() {
		if (sandboxId != null) {
			try {
				client.destroy(sandboxId);
				log.info("Remote sandbox destroyed: sandboxId={}", sandboxId);
			}
			catch (Exception e) {
				log.warn("Error destroying remote sandbox {}", sandboxId, e);
			}
		}
		initialized = false;
	}

	/**
	 * 暴露 sandboxId 便于调试与测试
	 */
	public String getSandboxId() {
		return sandboxId;
	}

	private void ensureInitialized() {
		if (!initialized) {
			throw new IllegalStateException("Remote sandbox not initialized");
		}
	}

}
