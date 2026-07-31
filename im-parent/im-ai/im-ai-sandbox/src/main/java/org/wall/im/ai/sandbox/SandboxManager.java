package org.wall.im.ai.sandbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wall.im.ai.core.model.SandboxConfig;
import org.wall.im.ai.core.sandbox.Sandbox;
import org.wall.im.ai.core.sandbox.SandboxResult;

import java.util.List;

/**
 * 安全沙盒管理器
 * <p>
 * 封装沙盒的安全检查逻辑，提供统一的沙盒访问入口
 * </p>
 */
public class SandboxManager {

	private static final Logger log = LoggerFactory.getLogger(SandboxManager.class);

	private final Sandbox sandbox;

	private final SandboxConfig config;

	public SandboxManager(Sandbox sandbox, SandboxConfig config) {
		this.sandbox = sandbox;
		this.config = config;
	}

	/**
	 * 在沙盒中安全执行代码
	 * @param code 要执行的代码
	 * @return 执行结果
	 */
	public SandboxResult safeExecute(String code) {
		if (!config.isEnabled()) {
			log.warn("Sandbox is disabled, executing without restrictions");
			return sandbox.execute(code, null);
		}

		// 预检查：代码中是否包含危险操作
		if (containsDangerousOperations(code)) {
			log.warn("Dangerous operations detected in code, blocking execution");
			return SandboxResult.failure("Dangerous operations detected and blocked", -1, 0);
		}

		return sandbox.execute(code, config.getWorkDir());
	}

	/**
	 * 在沙盒中安全执行命令
	 * @param command 命令
	 * @return 执行结果
	 */
	public SandboxResult safeExecuteCommand(String command) {
		if (!config.isEnabled()) {
			return sandbox.executeCommand(command);
		}

		if (containsDangerousOperations(command)) {
			log.warn("Dangerous command detected, blocking execution");
			return SandboxResult.failure("Dangerous command blocked", -1, 0);
		}

		return sandbox.executeCommand(command);
	}

	/**
	 * 检查路径是否可访问
	 */
	public boolean canAccess(String path) {
		return sandbox.isPathAllowed(path);
	}

	/**
	 * 检测危险操作
	 */
	private boolean containsDangerousOperations(String code) {
		if (code == null)
			return false;

		List<String> dangerousPatterns = List.of("rm -rf /", "rm -rf ~", "mkfs", "dd if=", ":(){:|:&};:",
				"chmod -R 777 /", "wget", "curl -o", "nc -l", "> /dev/sda", "format c:");

		String lowerCode = code.toLowerCase();
		return dangerousPatterns.stream().anyMatch(lowerCode::contains);
	}

}
