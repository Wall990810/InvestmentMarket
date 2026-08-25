package org.wall.im.ai.core.sandbox;

/**
 * 沙盒类型枚举
 * <p>
 * 标识沙盒的实现后端，用于 {@link SandboxFactory} 路由和 {@code SandboxRegistry} 注册查找。
 * </p>
 */
public enum SandboxType {

	/** 本地进程级沙盒（基于 ProcessBuilder + bash，弱隔离） */
	LOCAL_PROCESS,

	/** 本地 Docker 容器沙盒（基于 docker CLI，强隔离） */
	LOCAL_DOCKER,

	/** 远端 HTTP 沙盒（基于 REST 调用远端执行服务） */
	REMOTE_HTTP,

	/** 自定义沙盒（第三方扩展用，避免枚举膨胀） */
	CUSTOM,

}
