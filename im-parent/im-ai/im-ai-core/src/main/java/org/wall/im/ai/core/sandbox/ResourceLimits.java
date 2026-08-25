package org.wall.im.ai.core.sandbox;

import org.wall.im.ai.core.model.SandboxConfig;

/**
 * 沙盒资源限制值对象
 * <p>
 * 描述沙盒执行环境对 CPU、内存、磁盘、进程数、执行时间等的硬性约束。 适用于本地 Docker 容器与远端执行服务。
 * </p>
 */
public class ResourceLimits {

	/** CPU 核数（Docker --cpus） */
	private int cpuCores = 1;

	/** 内存上限（MB，Docker --memory） */
	private long memoryMb = 512;

	/** 单次执行最大耗时（秒） */
	private int maxExecutionTimeSec = 300;

	/** 磁盘上限（MB，Docker --tmpfs size 或远端配额） */
	private long diskMb = 1024;

	/** 最大进程数（Docker --pids-limit） */
	private int maxProcesses = 64;

	public ResourceLimits() {
	}

	public ResourceLimits(int cpuCores, long memoryMb, int maxExecutionTimeSec, long diskMb, int maxProcesses) {
		this.cpuCores = cpuCores;
		this.memoryMb = memoryMb;
		this.maxExecutionTimeSec = maxExecutionTimeSec;
		this.diskMb = diskMb;
		this.maxProcesses = maxProcesses;
	}

	/**
	 * 从旧版 {@link SandboxConfig} 字段（maxExecutionTime/maxMemoryMb）合并构造， 保证旧配置无资源限制字段时的兼容回退。
	 */
	public static ResourceLimits from(SandboxConfig config) {
		if (config == null) {
			return new ResourceLimits();
		}
		ResourceLimits limits = new ResourceLimits();
		// 旧 SandboxConfig 用秒与 MB，直接复用
		limits.maxExecutionTimeSec = config.getMaxExecutionTime();
		limits.memoryMb = config.getMaxMemoryMb();
		return limits;
	}

	// --- Getters and Setters ---

	public int getCpuCores() {
		return cpuCores;
	}

	public void setCpuCores(int cpuCores) {
		this.cpuCores = cpuCores;
	}

	public long getMemoryMb() {
		return memoryMb;
	}

	public void setMemoryMb(long memoryMb) {
		this.memoryMb = memoryMb;
	}

	public int getMaxExecutionTimeSec() {
		return maxExecutionTimeSec;
	}

	public void setMaxExecutionTimeSec(int maxExecutionTimeSec) {
		this.maxExecutionTimeSec = maxExecutionTimeSec;
	}

	public long getDiskMb() {
		return diskMb;
	}

	public void setDiskMb(long diskMb) {
		this.diskMb = diskMb;
	}

	public int getMaxProcesses() {
		return maxProcesses;
	}

	public void setMaxProcesses(int maxProcesses) {
		this.maxProcesses = maxProcesses;
	}

}
