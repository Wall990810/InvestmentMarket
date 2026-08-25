package org.wall.im.ai.sandbox.local;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wall.im.ai.core.model.SandboxConfig;
import org.wall.im.ai.core.sandbox.Sandbox;
import org.wall.im.ai.core.sandbox.SandboxResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 进程隔离沙盒实现
 * <p>
 * 通过进程级别隔离限制Agent运行时的文件访问和执行环境。 基于宿主 {@code bash} 与 {@link ProcessBuilder}，弱隔离， 适合低风险场景或无
 * Docker 环境。强隔离场景请使用 {@link DockerLocalSandbox}。
 * </p>
 */
public class ProcessSandbox implements Sandbox {

	private static final Logger log = LoggerFactory.getLogger(ProcessSandbox.class);

	private final SandboxConfig config;

	private Path sandboxWorkDir;

	private volatile boolean initialized = false;

	public ProcessSandbox(SandboxConfig config) {
		this.config = config;
	}

	@Override
	public void initialize() {
		if (initialized) {
			return;
		}
		try {
			// 创建沙盒工作目录
			if (config.getWorkDir() != null) {
				sandboxWorkDir = Paths.get(config.getWorkDir()).toAbsolutePath().normalize();
			}
			else {
				sandboxWorkDir = Files.createTempDirectory("ai-sandbox-").toAbsolutePath();
			}
			Files.createDirectories(sandboxWorkDir);
			initialized = true;
			log.info("Sandbox initialized with work dir: {}", sandboxWorkDir);
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to initialize sandbox", e);
		}
	}

	@Override
	public SandboxResult execute(String code, String workDir) {
		ensureInitialized();

		String targetDir = workDir != null ? workDir : sandboxWorkDir.toString();
		if (!isPathAllowed(targetDir)) {
			return SandboxResult.failure("Path not allowed in sandbox: " + targetDir, -1, 0);
		}

		long startTime = System.currentTimeMillis();
		try {
			// 将代码写入临时文件并执行
			Path scriptFile = sandboxWorkDir.resolve("script_" + System.currentTimeMillis() + ".sh");
			Files.writeString(scriptFile, code);

			ProcessBuilder pb = new ProcessBuilder("bash", scriptFile.toString()).directory(new File(targetDir))
				.redirectErrorStream(false);

			// 设置环境变量限制
			pb.environment().clear();
			pb.environment().put("HOME", sandboxWorkDir.toString());
			pb.environment().put("TMPDIR", sandboxWorkDir.toString());

			Process process = pb.start();

			// 超时控制
			boolean finished = process.waitFor(config.getMaxExecutionTime(), TimeUnit.SECONDS);
			if (!finished) {
				process.destroyForcibly();
				return SandboxResult.failure("Execution timed out", -1, System.currentTimeMillis() - startTime);
			}

			String stdout = new String(process.getInputStream().readAllBytes());
			String stderr = new String(process.getErrorStream().readAllBytes());
			int exitCode = process.exitValue();

			// 清理临时文件
			Files.deleteIfExists(scriptFile);

			if (exitCode == 0) {
				return SandboxResult.success(stdout, System.currentTimeMillis() - startTime);
			}
			else {
				return SandboxResult.failure(stderr, exitCode, System.currentTimeMillis() - startTime);
			}
		}
		catch (Exception e) {
			return SandboxResult.failure(e.getMessage(), -1, System.currentTimeMillis() - startTime);
		}
	}

	@Override
	public SandboxResult executeCommand(String command) {
		ensureInitialized();

		long startTime = System.currentTimeMillis();
		try {
			ProcessBuilder pb = new ProcessBuilder("bash", "-c", command).directory(sandboxWorkDir.toFile())
				.redirectErrorStream(false);

			// 限制环境变量
			pb.environment().clear();
			pb.environment().put("HOME", sandboxWorkDir.toString());
			pb.environment().put("TMPDIR", sandboxWorkDir.toString());

			Process process = pb.start();

			boolean finished = process.waitFor(config.getMaxExecutionTime(), TimeUnit.SECONDS);
			if (!finished) {
				process.destroyForcibly();
				return SandboxResult.failure("Command execution timed out", -1, System.currentTimeMillis() - startTime);
			}

			String stdout = new String(process.getInputStream().readAllBytes());
			String stderr = new String(process.getErrorStream().readAllBytes());
			int exitCode = process.exitValue();

			if (exitCode == 0) {
				return SandboxResult.success(stdout, System.currentTimeMillis() - startTime);
			}
			else {
				return SandboxResult.failure(stderr, exitCode, System.currentTimeMillis() - startTime);
			}
		}
		catch (Exception e) {
			return SandboxResult.failure(e.getMessage(), -1, System.currentTimeMillis() - startTime);
		}
	}

	@Override
	public boolean isPathAllowed(String path) {
		if (!config.isEnabled()) {
			return true;
		}

		try {
			Path normalizedPath = Paths.get(path).toAbsolutePath().normalize();

			// 检查工作目录
			if (normalizedPath.startsWith(sandboxWorkDir)) {
				return true;
			}

			// 检查白名单
			List<String> allowedPaths = config.getAllowedPaths();
			if (allowedPaths != null) {
				for (String allowed : allowedPaths) {
					Path allowedPath = Paths.get(allowed).toAbsolutePath().normalize();
					if (normalizedPath.startsWith(allowedPath)) {
						return true;
					}
				}
			}

			return false;
		}
		catch (Exception e) {
			log.warn("Error checking path: {}", path, e);
			return false;
		}
	}

	@Override
	public void destroy() {
		if (sandboxWorkDir != null) {
			try {
				// 清理沙盒目录
				if (Files.exists(sandboxWorkDir)) {
					try (var walk = Files.walk(sandboxWorkDir)) {
						walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
							try {
								Files.deleteIfExists(p);
							}
							catch (IOException ignored) {
							}
						});
					}
				}
				log.info("Sandbox destroyed: {}", sandboxWorkDir);
			}
			catch (IOException e) {
				log.warn("Error cleaning sandbox directory", e);
			}
		}
		initialized = false;
	}

	private void ensureInitialized() {
		if (!initialized) {
			throw new IllegalStateException("Sandbox not initialized");
		}
	}

}
