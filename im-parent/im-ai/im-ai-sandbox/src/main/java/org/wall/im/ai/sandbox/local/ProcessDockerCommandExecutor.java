package org.wall.im.ai.sandbox.local;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 基于 {@link ProcessBuilder} 调用 docker CLI 的默认实现。
 * <p>
 * 需要宿主 PATH 中存在 {@code docker} 命令。Windows 下配合 Docker Desktop 可用， Linux/Mac 下需 docker
 * daemon 运行。
 * </p>
 */
public class ProcessDockerCommandExecutor implements DockerCommandExecutor {

	private static final Logger log = LoggerFactory.getLogger(ProcessDockerCommandExecutor.class);

	@Override
	public DockerExecResult run(List<String> command, String stdin, int timeoutSec) {
		if (command == null || command.isEmpty()) {
			return DockerExecResult.failure(-1, "empty command");
		}
		ProcessBuilder pb = new ProcessBuilder(command).redirectErrorStream(false);
		try {
			Process process = pb.start();
			if (stdin != null) {
				process.getOutputStream().write(stdin.getBytes(StandardCharsets.UTF_8));
				process.getOutputStream().flush();
				process.getOutputStream().close();
			}
			boolean finished = process.waitFor(timeoutSec, TimeUnit.SECONDS);
			if (!finished) {
				process.destroyForcibly();
				return DockerExecResult.failure(-1, "docker command timeout after " + timeoutSec + "s: " + command);
			}
			String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
			String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
			return new DockerExecResult(process.exitValue(), stdout, stderr);
		}
		catch (IOException | InterruptedException e) {
			log.warn("Docker command failed: {}", command, e);
			if (e instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			return DockerExecResult.failure(-1, e.getMessage());
		}
	}

}
