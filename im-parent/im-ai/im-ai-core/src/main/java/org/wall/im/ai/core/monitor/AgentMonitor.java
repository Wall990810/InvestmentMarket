package org.wall.im.ai.core.monitor;

import java.util.Map;

/**
 * Agent监控器接口
 * <p>
 * 统一监控抽象，对接Micrometer/Zipkin/Langfuse
 * </p>
 */
public interface AgentMonitor {

	/**
	 * 记录Agent调用开始
	 * @param agentName Agent名称
	 * @param input 输入内容
	 * @return traceId
	 */
	String traceStart(String agentName, String input);

	/**
	 * 记录Agent调用结束
	 * @param traceId traceId
	 * @param agentName Agent名称
	 * @param output 输出内容
	 * @param costTimeMs 耗时(毫秒)
	 * @param tokenUsage token使用量
	 */
	void traceEnd(String traceId, String agentName, String output, long costTimeMs, int tokenUsage);

	/**
	 * 记录Agent调用异常
	 * @param traceId traceId
	 * @param agentName Agent名称
	 * @param error 异常信息
	 */
	void traceError(String traceId, String agentName, String error);

	/**
	 * 记录Tool调用
	 * @param traceId traceId
	 * @param toolName 工具名称
	 * @param parameters 参数
	 * @param result 结果
	 * @param costTimeMs 耗时
	 */
	void traceToolCall(String traceId, String toolName, Map<String, Object> parameters, String result, long costTimeMs);

	/**
	 * 记录自定义指标
	 * @param metricName 指标名称
	 * @param value 指标值
	 * @param tags 标签
	 */
	void recordMetric(String metricName, double value, Map<String, String> tags);

	/**
	 * 获取自定义指标注册器
	 */
	CustomMetricRegistry getCustomMetricRegistry();

}
