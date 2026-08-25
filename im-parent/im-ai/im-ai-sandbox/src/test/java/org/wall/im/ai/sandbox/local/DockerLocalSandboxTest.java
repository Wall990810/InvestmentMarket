package org.wall.im.ai.sandbox.local;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.wall.im.ai.core.model.SandboxConfig;
import org.wall.im.ai.core.sandbox.ResourceLimits;
import org.wall.im.ai.core.sandbox.SandboxResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DockerLocalSandbox 单元测试
 * <p>
 * mock {@link DockerCommandExecutor} 验证 docker 命令编排与资源限制参数， 不依赖真实 docker daemon。
 * </p>
 */
@DisplayName("DockerLocalSandbox测试")
class DockerLocalSandboxTest {

	private SandboxConfig createConfig() {
		SandboxConfig c = new SandboxConfig();
		c.setEnabled(true);
		c.setWorkDir("/tmp/sandbox-test");
		c.setImage("test:latest");
		c.setResourceLimits(new ResourceLimits(2, 1024, 60, 2048, 128));
		return c;
	}

	private void stubDefaultDockerCommands(DockerCommandExecutor executor) {
		when(executor.run(any(), any(), anyInt())).thenAnswer(invocation -> {
			@SuppressWarnings("unchecked")
			List<String> cmd = invocation.getArgument(0);
			String cmdStr = String.valueOf(cmd);
			if (cmdStr.contains("{{.State.Running}}")) {
				return DockerCommandExecutor.DockerExecResult.success("true");
			}
			return DockerCommandExecutor.DockerExecResult.success("{}");
		});
	}

	@Test
	@DisplayName("initialize 应调用 docker image inspect + docker run -d 含资源限制参数")
	void initialize_shouldComposeRunCommand() {
		SandboxConfig config = createConfig();
		DockerCommandExecutor executor = mock(DockerCommandExecutor.class);
		stubDefaultDockerCommands(executor);

		new DockerLocalSandbox(config, executor).initialize();

		ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
		verify(executor, atLeastOnce()).run(captor.capture(), any(), anyInt());
		@SuppressWarnings("unchecked")
		List<String> runCmd = captor.getAllValues()
			.stream()
			.filter(c -> c.toString().contains("run") && c.toString().contains("-d") && c.toString().contains("--name"))
			.findFirst()
			.orElseThrow(() -> new AssertionError("docker run -d not called"));
		assertTrue(runCmd.contains("--cpus=2"));
		assertTrue(runCmd.contains("--memory=1024m"));
		assertTrue(runCmd.contains("--pids-limit=128"));
		assertTrue(runCmd.contains("--network=none"));
		assertTrue(runCmd.contains("--tmpfs"));
		assertTrue(runCmd.contains("test:latest"));
		assertTrue(runCmd.contains("sleep"));
		assertTrue(runCmd.contains("infinity"));
	}

	@Test
	@DisplayName("networkAccess=true 时 --network=bridge")
	void initialize_networkBridge() {
		SandboxConfig config = createConfig();
		config.setNetworkAccess(true);
		DockerCommandExecutor executor = mock(DockerCommandExecutor.class);
		stubDefaultDockerCommands(executor);

		new DockerLocalSandbox(config, executor).initialize();

		ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
		verify(executor, atLeastOnce()).run(captor.capture(), any(), anyInt());
		@SuppressWarnings("unchecked")
		List<String> runCmd = captor.getAllValues()
			.stream()
			.filter(c -> c.toString().contains("run") && c.toString().contains("-d"))
			.findFirst()
			.orElseThrow(() -> new AssertionError("docker run not called"));
		assertTrue(runCmd.contains("--network=bridge"));
	}

	@Test
	@DisplayName("镜像不存在应抛 RuntimeException")
	void initialize_imageNotFound_throws() {
		SandboxConfig config = createConfig();
		DockerCommandExecutor executor = mock(DockerCommandExecutor.class);
		when(executor.run(any(), any(), anyInt()))
			.thenReturn(DockerCommandExecutor.DockerExecResult.failure(1, "No such image"));

		assertThrows(RuntimeException.class, () -> new DockerLocalSandbox(config, executor).initialize());
	}

	@Test
	@DisplayName("executeCommand 应调用 docker exec bash -c 并返回结果")
	void executeCommand_shouldCallDockerExec() {
		SandboxConfig config = createConfig();
		DockerCommandExecutor executor = mock(DockerCommandExecutor.class);
		stubDefaultDockerCommands(executor);
		when(executor.run(
				org.mockito.ArgumentMatchers
					.argThat((List<String> c) -> c.toString().contains("exec") && c.toString().contains("-c")),
				any(), anyInt()))
			.thenReturn(DockerCommandExecutor.DockerExecResult.success("hello"));

		DockerLocalSandbox sandbox = new DockerLocalSandbox(config, executor);
		sandbox.initialize();
		SandboxResult result = sandbox.executeCommand("echo hello");

		assertTrue(result.isSuccess());
		assertEquals("hello", result.getOutput());
	}

	@Test
	@DisplayName("execute 应通过 stdin 传 code 调用 docker exec -i bash -s")
	void execute_shouldPassCodeViaStdin() {
		SandboxConfig config = createConfig();
		DockerCommandExecutor executor = mock(DockerCommandExecutor.class);
		stubDefaultDockerCommands(executor);
		when(executor.run(
				org.mockito.ArgumentMatchers
					.argThat((List<String> c) -> c.toString().contains("exec") && c.toString().contains("-i")),
				any(), anyInt()))
			.thenReturn(DockerCommandExecutor.DockerExecResult.success("output"));

		DockerLocalSandbox sandbox = new DockerLocalSandbox(config, executor);
		sandbox.initialize();
		SandboxResult result = sandbox.execute("echo hello", null);

		assertTrue(result.isSuccess());
		assertEquals("output", result.getOutput());
		ArgumentCaptor<String> stdinCaptor = ArgumentCaptor.forClass(String.class);
		verify(executor, atLeastOnce()).run(
				org.mockito.ArgumentMatchers
					.argThat((List<String> c) -> c.toString().contains("exec") && c.toString().contains("-i")),
				stdinCaptor.capture(), anyInt());
		assertTrue(stdinCaptor.getAllValues().contains("echo hello"));
	}

	@Test
	@DisplayName("destroy 应调用 docker rm -f")
	void destroy_shouldCallDockerRm() {
		SandboxConfig config = createConfig();
		DockerCommandExecutor executor = mock(DockerCommandExecutor.class);
		stubDefaultDockerCommands(executor);

		DockerLocalSandbox sandbox = new DockerLocalSandbox(config, executor);
		sandbox.initialize();
		sandbox.destroy();

		verify(executor, atLeastOnce()).run(
				org.mockito.ArgumentMatchers
					.argThat((List<String> c) -> c.toString().contains("rm") && c.toString().contains("-f")),
				any(), anyInt());
	}

	@Test
	@DisplayName("isPathAllowed 容器内 /workspace 与 /tmp 前缀允许，其他拒绝")
	void isPathAllowed_workspaceAndTmp() {
		SandboxConfig config = createConfig();
		DockerLocalSandbox sandbox = new DockerLocalSandbox(config, mock(DockerCommandExecutor.class));
		assertTrue(sandbox.isPathAllowed("/workspace/script.sh"));
		assertTrue(sandbox.isPathAllowed("/tmp/tmp1234"));
		assertFalse(sandbox.isPathAllowed("/etc/passwd"));
		assertFalse(sandbox.isPathAllowed(null));
	}

}
