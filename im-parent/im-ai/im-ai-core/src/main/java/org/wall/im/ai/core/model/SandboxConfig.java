package org.wall.im.ai.core.model;

import org.wall.im.ai.core.sandbox.ResourceLimits;
import org.wall.im.ai.core.sandbox.SandboxContext;
import org.wall.im.ai.core.sandbox.SandboxType;
import org.wall.im.ai.core.sandbox.policy.CommandPolicy;

import java.util.List;
import java.util.Map;

/**
 * 沙盒配置
 * <p>
 * 旧字段（enabled/workDir/allowedPaths/networkAccess/maxExecutionTime/maxMemoryMb）与无参构造器全部保留，
 * 保证向后兼容。新增字段为可选，默认值保证未设置时退回本地进程级沙盒旧行为。
 * </p>
 */
public class SandboxConfig {

	/** 是否启用沙盒 */
	private boolean enabled = true;

	/** 允许的工作目录 */
	private String workDir;

	/** 允许访问的路径白名单 */
	private List<String> allowedPaths;

	/** 是否允许网络访问 */
	private boolean networkAccess = false;

	/** 最大执行时间(秒) */
	private int maxExecutionTime = 300;

	/** 最大内存限制(MB) */
	private long maxMemoryMb = 512;

	// --- 新增字段（向后兼容，默认值保证旧行为） ---

	/** 沙盒类型，默认 LOCAL_PROCESS（与旧 ProcessSandbox 行为一致） */
	private SandboxType type = SandboxType.LOCAL_PROCESS;

	/** 资源限制；null 时 SandboxManager 回退用 maxExecutionTime/maxMemoryMb */
	private ResourceLimits resourceLimits;

	/** 命令策略；null 时 SandboxManager 用 DefaultCommandPolicy */
	private CommandPolicy commandPolicy;

	/** 容器/远端镜像，如 "openjdk:26-slim" */
	private String image;

	/** 远端沙盒基地址，如 "https://sb.example.com" */
	private String remoteEndpoint;

	/** 额外环境变量（注入到本地进程/容器/远端执行上下文） */
	private Map<String, String> envVars;

	/** 执行上下文（agentId/sessionId/metadata，用于远端租户隔离与审计） */
	private SandboxContext context;

	// --- Getters and Setters ---

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getWorkDir() {
		return workDir;
	}

	public void setWorkDir(String workDir) {
		this.workDir = workDir;
	}

	public List<String> getAllowedPaths() {
		return allowedPaths;
	}

	public void setAllowedPaths(List<String> allowedPaths) {
		this.allowedPaths = allowedPaths;
	}

	public boolean isNetworkAccess() {
		return networkAccess;
	}

	public void setNetworkAccess(boolean networkAccess) {
		this.networkAccess = networkAccess;
	}

	public int getMaxExecutionTime() {
		return maxExecutionTime;
	}

	public void setMaxExecutionTime(int maxExecutionTime) {
		this.maxExecutionTime = maxExecutionTime;
	}

	public long getMaxMemoryMb() {
		return maxMemoryMb;
	}

	public void setMaxMemoryMb(long maxMemoryMb) {
		this.maxMemoryMb = maxMemoryMb;
	}

	public SandboxType getType() {
		return type;
	}

	public void setType(SandboxType type) {
		this.type = type;
	}

	public ResourceLimits getResourceLimits() {
		return resourceLimits;
	}

	public void setResourceLimits(ResourceLimits resourceLimits) {
		this.resourceLimits = resourceLimits;
	}

	public CommandPolicy getCommandPolicy() {
		return commandPolicy;
	}

	public void setCommandPolicy(CommandPolicy commandPolicy) {
		this.commandPolicy = commandPolicy;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public String getRemoteEndpoint() {
		return remoteEndpoint;
	}

	public void setRemoteEndpoint(String remoteEndpoint) {
		this.remoteEndpoint = remoteEndpoint;
	}

	public Map<String, String> getEnvVars() {
		return envVars;
	}

	public void setEnvVars(Map<String, String> envVars) {
		this.envVars = envVars;
	}

	public SandboxContext getContext() {
		return context;
	}

	public void setContext(SandboxContext context) {
		this.context = context;
	}

}
