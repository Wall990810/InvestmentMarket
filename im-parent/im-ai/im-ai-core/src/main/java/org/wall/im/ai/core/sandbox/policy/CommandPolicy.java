package org.wall.im.ai.core.sandbox.policy;

/**
 * 命令策略接口
 * <p>
 * 决定一条命令/代码是否允许在沙盒中执行。返回 {@code true} 表示放行， {@code false} 表示拦截。 第三方可实现该接口接入 AST
 * 级解析、频率限制、自定义审计等。
 * </p>
 */
public interface CommandPolicy {

	/**
	 * 判断命令是否允许执行
	 * @param command 命令或代码片段
	 * @return true 允许 / false 拒绝
	 */
	boolean isAllowed(String command);

	/**
	 * 策略名称，用于日志与可观测性
	 */
	String getPolicyName();

}
