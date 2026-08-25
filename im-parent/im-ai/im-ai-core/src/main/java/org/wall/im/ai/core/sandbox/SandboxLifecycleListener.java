package org.wall.im.ai.core.sandbox;

/**
 * 沙盒生命周期监听器
 * <p>
 * 所有方法均为 default 实现，第三方按需覆盖。SandboxManager 在关键节点回调， 用于审计日志、指标采集、资源预热、trace 上报等扩展。
 * </p>
 */
public interface SandboxLifecycleListener {

	/** 沙盒初始化完成后回调 */
	default void onInitialize(Sandbox sandbox, SandboxContext ctx) {
	}

	/** 执行代码/命令前回调（策略拒绝前调用，便于审计原始请求） */
	default void onPreExecute(Sandbox sandbox, String code, SandboxContext ctx) {
	}

	/** 执行完成后回调（含成功与失败） */
	default void onPostExecute(Sandbox sandbox, SandboxResult result, SandboxContext ctx) {
	}

	/** 沙盒销毁后回调 */
	default void onDestroy(Sandbox sandbox, SandboxContext ctx) {
	}

}
