package org.wall.im.ai.sandbox.remote;

import java.util.Map;

/**
 * 远端执行请求 DTO
 * <p>
 * 用于 {@code POST /sandboxes/{id}/execute} 与 {@code POST /sandboxes/{id}/command}。 无注解
 * POJO，由 {@link com.fasterxml.jackson.databind.ObjectMapper} 用字段名（camelCase）序列化。
 * </p>
 */
public class RemoteExecuteRequest {

	private String code;

	private String command;

	private String workDir;

	private Map<String, String> envVars;

	private Integer timeoutSec;

	public RemoteExecuteRequest() {
	}

	public static RemoteExecuteRequest forCode(String code, String workDir, Integer timeoutSec) {
		RemoteExecuteRequest r = new RemoteExecuteRequest();
		r.code = code;
		r.workDir = workDir;
		r.timeoutSec = timeoutSec;
		return r;
	}

	public static RemoteExecuteRequest forCommand(String command, Integer timeoutSec) {
		RemoteExecuteRequest r = new RemoteExecuteRequest();
		r.command = command;
		r.timeoutSec = timeoutSec;
		return r;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public String getWorkDir() {
		return workDir;
	}

	public void setWorkDir(String workDir) {
		this.workDir = workDir;
	}

	public Map<String, String> getEnvVars() {
		return envVars;
	}

	public void setEnvVars(Map<String, String> envVars) {
		this.envVars = envVars;
	}

	public Integer getTimeoutSec() {
		return timeoutSec;
	}

	public void setTimeoutSec(Integer timeoutSec) {
		this.timeoutSec = timeoutSec;
	}

}
