package org.wall.im.ai.core.sandbox;

/**
 * 沙盒执行结果
 */
public class SandboxResult {

	private boolean success;

	private String output;

	private String errorOutput;

	private int exitCode;

	private long executionTimeMs;

	public static SandboxResult success(String output, long executionTimeMs) {
		SandboxResult result = new SandboxResult();
		result.success = true;
		result.output = output;
		result.exitCode = 0;
		result.executionTimeMs = executionTimeMs;
		return result;
	}

	public static SandboxResult failure(String errorOutput, int exitCode, long executionTimeMs) {
		SandboxResult result = new SandboxResult();
		result.success = false;
		result.errorOutput = errorOutput;
		result.exitCode = exitCode;
		result.executionTimeMs = executionTimeMs;
		return result;
	}

	// --- Getters and Setters ---

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

}
