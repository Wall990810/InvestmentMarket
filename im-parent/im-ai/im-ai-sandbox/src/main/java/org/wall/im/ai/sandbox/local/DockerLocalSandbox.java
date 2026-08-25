package org.wall.im.ai.sandbox.local;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wall.im.ai.core.model.SandboxConfig;
import org.wall.im.ai.core.sandbox.ResourceLimits;
import org.wall.im.ai.core.sandbox.Sandbox;
import org.wall.im.ai.core.sandbox.SandboxContext;
import org.wall.im.ai.core.sandbox.SandboxResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 本地 Docker 容器沙盒
 * <p>
 * 通过 docker CLI 创建容器，提供强隔离：CPU/内存/进程数限制、网络隔离、tmpfs 临时目录、 工作目录只读挂载（可选）。 所有 bash 执行在 Linux
 * 容器内，与宿主有无 bash 无关， 跨平台支持（Windows + Docker Desktop 可用）。
 * </p>
 *
 * <h3>命令模板</h3>
 * <ul>
 * <li>初始化：{@code docker run -d --name ... --cpus --memory --pids-limit --network --tmpfs
 * -v -w {image} sleep infinity}</li>
 * <li>执行命令：{@code docker exec -w {workDir} {name} bash -c {command}}</li>
 * <li>执行代码：{@code docker exec -i -w {workDir} {name} bash -s}（stdin 写 code）</li>
 * <li>销毁：{@code docker rm -f {name}}</li>
 * </ul>
 */
public class DockerLocalSandbox implements Sandbox {

	private static final Logger log = LoggerFactory.getLogger(DockerLocalSandbox.class);

	/** 容器内固定工作目录 */
	public static final String CONTAINER_WORK_DIR = "/workspace";

	/** 容器内 tmpfs 临时目录 */
	public static final String CONTAINER_TMP_DIR = "/tmp";

	/** 默认镜像（当 config.image 为空时使用） */
	public static final String DEFAULT_IMAGE = "openjdk:26-slim";

	private final SandboxConfig config;

	private final DockerCommandExecutor executor;

	private final ResourceLimits limits;

	private String containerName;

	private volatile boolean initialized = false;

	public DockerLocalSandbox(SandboxConfig config) {
		this(config, new ProcessDockerCommandExecutor());
	}

	public DockerLocalSandbox(SandboxConfig config, DockerCommandExecutor executor) {
		this.config = config;
		this.executor = executor;
		this.limits = config.getResourceLimits() != null ? config.getResourceLimits() : ResourceLimits.from(config);
	}

	@Override
	public void initialize() {
		if (initialized) {
			return;
		}
		String image = resolveImage();
		containerName = generateContainerName();

		// 1. 镜像探检：image inspect 失败说明镜像不存在或 docker 不可用
		DockerCommandExecutor.DockerExecResult imageCheck = executor.run(List.of("docker", "image", "inspect", image),
				null, limits.getMaxExecutionTimeSec());
		if (!imageCheck.isSuccess()) {
			throw new RuntimeException("Docker image '" + image + "' not available: " + imageCheck.stderr().trim());
		}

		// 2. 准备宿主工作目录（必须存在才能挂载）
		String hostWorkDir = resolveHostWorkDir();

		// 3. docker run -d ...
		List<String> runCmd = buildRunCommand(image, hostWorkDir);
		DockerCommandExecutor.DockerExecResult runResult = executor.run(runCmd, null, limits.getMaxExecutionTimeSec());
		if (!runResult.isSuccess()) {
			throw new RuntimeException("docker run failed: " + runResult.stderr().trim() + " (cmd: " + runCmd + ")");
		}

		// 4. 就绪轮询
		if (!waitForRunning()) {
			// 容器未运行，读日志清理
			DockerCommandExecutor.DockerExecResult logs = executor.run(List.of("docker", "logs", containerName), null,
					limits.getMaxExecutionTimeSec());
			executor.run(List.of("docker", "rm", "-f", containerName), null, 10);
			throw new RuntimeException("Container did not reach running state. Logs: " + logs.stdout().trim());
		}

		initialized = true;
		log.info("Docker sandbox initialized: name={}, image={}, workDir={}", containerName, image, hostWorkDir);
	}

	@Override
	public SandboxResult execute(String code, String workDir) {
		ensureInitialized();
		long start = System.currentTimeMillis();
		String targetDir = workDir != null ? workDir : CONTAINER_WORK_DIR;
		if (!isPathAllowed(targetDir)) {
			return SandboxResult.failure("Path not allowed in sandbox: " + targetDir, -1, 0);
		}
		try {
			List<String> cmd = new ArrayList<>(
					List.of("docker", "exec", "-i", "-w", targetDir, containerName, "bash", "-s"));
			applyEnvVars(cmd);
			DockerCommandExecutor.DockerExecResult r = executor.run(cmd, code, limits.getMaxExecutionTimeSec());
			return toSandboxResult(r, start);
		}
		catch (Exception e) {
			return SandboxResult.failure(e.getMessage(), -1, System.currentTimeMillis() - start);
		}
	}

	@Override
	public SandboxResult executeCommand(String command) {
		ensureInitialized();
		long start = System.currentTimeMillis();
		try {
			List<String> cmd = new ArrayList<>(
					List.of("docker", "exec", "-w", CONTAINER_WORK_DIR, containerName, "bash", "-c", command));
			applyEnvVars(cmd);
			DockerCommandExecutor.DockerExecResult r = executor.run(cmd, null, limits.getMaxExecutionTimeSec());
			return toSandboxResult(r, start);
		}
		catch (Exception e) {
			return SandboxResult.failure(e.getMessage(), -1, System.currentTimeMillis() - start);
		}
	}

	@Override
	public boolean isPathAllowed(String path) {
		if (!config.isEnabled()) {
			return true;
		}
		if (path == null) {
			return false;
		}
		// 容器内允许：工作目录 + tmpfs
		if (path.startsWith(CONTAINER_WORK_DIR) || path.startsWith(CONTAINER_TMP_DIR)) {
			return true;
		}
		// 额外白名单（容器内只读挂载路径）
		List<String> allowed = config.getAllowedPaths();
		if (allowed != null) {
			for (String p : allowed) {
				if (path.startsWith(p)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void destroy() {
		if (containerName != null) {
			try {
				executor.run(List.of("docker", "rm", "-f", containerName), null, 10);
				log.info("Docker sandbox destroyed: {}", containerName);
			}
			catch (Exception e) {
				log.warn("Error destroying docker container {}", containerName, e);
			}
		}
		initialized = false;
	}

	// --- 内部辅助 ---

	private String resolveImage() {
		return config.getImage() != null && !config.getImage().isBlank() ? config.getImage() : DEFAULT_IMAGE;
	}

	private String resolveHostWorkDir() {
		String workDir = config.getWorkDir();
		try {
			if (workDir == null || workDir.isBlank()) {
				Path tmp = Files.createTempDirectory("ai-docker-sandbox-").toAbsolutePath();
				return tmp.toString();
			}
			Path p = Paths.get(workDir).toAbsolutePath().normalize();
			Files.createDirectories(p);
			return p.toString();
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to resolve host work dir: " + workDir, e);
		}
	}

	private List<String> buildRunCommand(String image, String hostWorkDir) {
		List<String> cmd = new ArrayList<>();
		cmd.add("docker");
		cmd.add("run");
		cmd.add("-d");
		cmd.add("--name");
		cmd.add(containerName);
		cmd.add("--cpus=" + limits.getCpuCores());
		cmd.add("--memory=" + limits.getMemoryMb() + "m");
		cmd.add("--pids-limit=" + limits.getMaxProcesses());
		cmd.add("--network=" + (config.isNetworkAccess() ? "bridge" : "none"));
		cmd.add("--tmpfs");
		cmd.add(CONTAINER_TMP_DIR + ":size=" + limits.getDiskMb() + "m");
		cmd.add("-v");
		cmd.add(hostWorkDir + ":" + CONTAINER_WORK_DIR);
		cmd.add("-w");
		cmd.add(CONTAINER_WORK_DIR);
		cmd.add(image);
		cmd.add("sleep");
		cmd.add("infinity");
		return cmd;
	}

	private void applyEnvVars(List<String> cmd) {
		Map<String, String> env = config.getEnvVars();
		if (env == null || env.isEmpty()) {
			return;
		}
		// 在 "exec" 之后、容器名之前插入 -e KEY=VAL
		int idx = cmd.indexOf("exec");
		if (idx < 0) {
			return;
		}
		List<String> envArgs = new ArrayList<>();
		env.forEach((k, v) -> {
			envArgs.add("-e");
			envArgs.add(k + "=" + (v == null ? "" : v));
		});
		// 插入到 idx+1 之后
		cmd.addAll(idx + 1, envArgs);
	}

	private boolean waitForRunning() {
		for (int i = 0; i < 3; i++) {
			DockerCommandExecutor.DockerExecResult r = executor
				.run(List.of("docker", "inspect", "-f", "{{.State.Running}}", containerName), null, 5);
			if (r.isSuccess() && r.stdout().trim().equalsIgnoreCase("true")) {
				return true;
			}
			try {
				Thread.sleep(200);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return false;
			}
		}
		return false;
	}

	private SandboxResult toSandboxResult(DockerCommandExecutor.DockerExecResult r, long start) {
		long cost = System.currentTimeMillis() - start;
		if (r.isSuccess()) {
			return SandboxResult.success(r.stdout(), cost);
		}
		return SandboxResult.failure(r.stderr().isBlank() ? r.stdout() : r.stderr(), r.exitCode(), cost);
	}

	private String generateContainerName() {
		SandboxContext ctx = config.getContext();
		String agentId = (ctx != null && ctx.getAgentId() != null) ? ctx.getAgentId() : "default";
		String sessionId = (ctx != null && ctx.getSessionId() != null) ? ctx.getSessionId() : "session";
		String raw = "sandbox-" + agentId + "-" + sessionId + "-" + System.currentTimeMillis();
		return raw.toLowerCase().replaceAll("[^a-z0-9-]", "-");
	}

	private void ensureInitialized() {
		if (!initialized) {
			throw new IllegalStateException("Docker sandbox not initialized");
		}
	}

}
