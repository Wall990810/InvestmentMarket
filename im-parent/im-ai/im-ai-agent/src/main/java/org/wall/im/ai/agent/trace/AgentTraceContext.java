package org.wall.im.ai.agent.trace;

import org.wall.im.ai.core.monitor.AgentMonitor;

/**
 * Agent调用链路追踪上下文
 * <p>
 * 基于ThreadLocal传递当前Agent调用的traceId和AgentMonitor， 使得工具回调（ToolCallback）可以自动记录工具调用trace信息。
 * </p>
 *
 * <h3>使用方式</h3> <pre>{@code
 * // Agent调用入口设置上下文
 * AgentTraceContext.setup(traceId, monitor, agentName);
 * try {
 *     agent.call(input);
 * } finally {
 *     AgentTraceContext.clear();
 * }
 *
 * // 工具回调中自动记录
 * if (AgentTraceContext.isAvailable()) {
 *     AgentTraceContext.getMonitor().traceToolCall(
 *         AgentTraceContext.getTraceId(), toolName, params, result, costMs);
 * }
 * }</pre>
 */
public final class AgentTraceContext {

	private static final ThreadLocal<String> TRACE_ID = new InheritableThreadLocal<>();

	private static final ThreadLocal<AgentMonitor> MONITOR = new InheritableThreadLocal<>();

	private static final ThreadLocal<String> AGENT_NAME = new InheritableThreadLocal<>();

	private AgentTraceContext() {
		// 工具类，禁止实例化
	}

	/**
	 * 设置当前Agent调用的追踪上下文
	 * @param traceId 当前调用traceId
	 * @param monitor AgentMonitor实例
	 * @param agentName Agent名称
	 */
	public static void setup(String traceId, AgentMonitor monitor, String agentName) {
		TRACE_ID.set(traceId);
		MONITOR.set(monitor);
		AGENT_NAME.set(agentName);
	}

	/**
	 * 清理当前线程的追踪上下文
	 */
	public static void clear() {
		TRACE_ID.remove();
		MONITOR.remove();
		AGENT_NAME.remove();
	}

	/**
	 * 获取当前traceId
	 */
	public static String getTraceId() {
		return TRACE_ID.get();
	}

	/**
	 * 获取当前AgentMonitor
	 */
	public static AgentMonitor getMonitor() {
		return MONITOR.get();
	}

	/**
	 * 获取当前Agent名称
	 */
	public static String getAgentName() {
		return AGENT_NAME.get();
	}

	/**
	 * 检查当前线程是否有可用的追踪上下文
	 * @return true如果traceId和monitor都已设置
	 */
	public static boolean isAvailable() {
		return TRACE_ID.get() != null && MONITOR.get() != null;
	}

}
