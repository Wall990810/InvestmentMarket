package org.wall.im.ai.core.model;

import java.util.List;

/**
 * 沙盒配置
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

}
