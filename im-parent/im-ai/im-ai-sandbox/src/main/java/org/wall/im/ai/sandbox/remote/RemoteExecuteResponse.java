package org.wall.im.ai.sandbox.remote;

import org.wall.im.ai.core.sandbox.SandboxResult;

/**
 * 远端执行响应 DTO
 * <p>
 * 同时承载 {@code POST /sandboxes}（含 sandboxId/status） 与 {@code POST
 * /sandboxes/{id}/execute|command}（含 success/output/errorOutput/exitCode/executionTimeMs）
 * 响应。无注解 POJO。
 * </p>
 */
public class RemoteExecuteResponse {

	private boolean success;

	private String output;

	private String errorOutput;

	private int exitCode;

	private long executionTimeMs;

	/** initialize 响应的沙盒 ID */
	private String sandboxId;

	/** initialize 响应的状态，如 READY */
	private String status;

	/** 错误码（非 2xx 响应体中的 code 字段） */
	private String code;

	/** 错误信息（非 2xx 响应体中的 error 字段） */
	private String error;

	public RemoteExecuteResponse() {
	}

	/**
	 * 转换为 {@link SandboxResult}（execute/command 路径）
	 */
	public SandboxResult toSandboxResult() {
		if (success) {
			return SandboxResult.success(output, executionTimeMs);
		}
		String errMsg = errorOutput != null && !errorOutput.isBlank() ? errorOutput
				: (error != null ? error : "Remote sandbox execution failed");
		return SandboxResult.failure(errMsg, exitCode, executionTimeMs);
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getOutput() {
		return output;
	}

	public void setOutput(String output) {
		this.output = output;
	}

	public String getErrorOutput() {
		return errorOutput;
	}

	public void setErrorOutput(String errorOutput) {
		this.errorOutput = errorOutput;
	}

	public int getExitCode() {
		return exitCode;
	}

	public void setExitCode(int exitCode) {
		this.exitCode = exitCode;
	}

	public long getExecutionTimeMs() {
		return executionTimeMs;
	}

	public void setExecutionTimeMs(long executionTimeMs) {
		this.executionTimeMs = executionTimeMs;
	}

	public String getSandboxId() {
		return sandboxId;
	}

	public void setSandboxId(String sandboxId) {
		this.sandboxId = sandboxId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

}
