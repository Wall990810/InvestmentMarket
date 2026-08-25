package org.wall.im.ai.sandbox.local;

import java.util.List;

/**
 * Docker 命令执行器接口
 * <p>
 * 抽象 docker CLI 调用便于单元测试 mock，也方便替换为 Podman 或远端 docker host。 命令参数按 {@code List<String>}
 * 逐元素传递，避免 shell 注入。
 * </p>
 */
public interface DockerCommandExecutor {

	/**
	 * 执行 docker 命令
	 * @param command 命令参数列表（如 {@code ["docker", "run", "-d", ...]}）
	 * @param stdin 标准输入内容，{@code null} 表示无输入
	 * @param timeoutSec 超时秒数
	 * @return 执行结果
	 */
	DockerExecResult run(List<String> command, String stdin, int timeoutSec);

	/**
	 * 便捷重载，无 stdin
	 */
	default DockerExecResult run(List<String> command, int timeoutSec) {
		return run(command, null, timeoutSec);
	}

	/**
	 * Docker 命令执行结果
	 */
	record DockerExecResult(int exitCode, String stdout, String stderr) {

		public boolean isSuccess() {
			return exitCode == 0;
		}

		public static DockerExecResult success(String stdout) {
			return new DockerExecResult(0, stdout, "");
		}

		public static DockerExecResult failure(int exitCode, String stderr) {
			return new DockerExecResult(exitCode, "", stderr);
		}

	}

}
