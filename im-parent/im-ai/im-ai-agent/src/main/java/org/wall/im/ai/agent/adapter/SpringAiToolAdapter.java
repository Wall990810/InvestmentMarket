package org.wall.im.ai.agent.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.ToolCallback;
import org.wall.im.ai.agent.trace.AgentTraceContext;
import org.wall.im.ai.core.monitor.AgentMonitor;
import org.wall.im.ai.core.tool.Tool;

import java.util.Map;

/**
 * Spring AI工具适配器
 * <p>
 * 将自定义 {@link Tool} 接口桥接到Spring AI的 {@link ToolCallback}， 使得自定义工具可以被Spring AI
 * Alibaba的ReactAgent调用。
 * </p>
 *
 * <p>
 * 同时在工具调用过程中，自动通过 {@link AgentTraceContext} 记录trace信息到Langfuse等监控平台。
 * </p>
 *
 * <p>
 * 使用方式： <pre>{@code
 * Tool myTool = new MyCustomTool();
 * ToolCallback callback = SpringAiToolAdapter.toToolCallback(myTool);
 * ReactAgent agent = ReactAgent.builder()
 *     .tools(callback)
 *     .build();
 * }</pre>
 * </p>
 */
public class SpringAiToolAdapter {

	private static final Logger log = LoggerFactory.getLogger(SpringAiToolAdapter.class);

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	/**
	 * 将自定义Tool转换为Spring AI的ToolCallback
	 * <p>
	 * 适配过程：
	 * <ol>
	 * <li>将Tool的parameterSchema（JSON Schema）传递给FunctionToolCallback</li>
	 * <li>将Tool的execute方法包装为Function回调</li>
	 * <li>自动记录tool调用trace信息（如果AgentTraceContext可用）</li>
	 * <li>自动处理JSON字符串与Map之间的转换</li>
	 * </ol>
	 * </p>
	 * @param tool 自定义工具实例
	 * @return Spring AI ToolCallback实例
	 */
	public static ToolCallback toToolCallback(Tool tool) {
		return FunctionToolCallback.builder(tool.getName(), (String jsonInput) -> {
			Map<String, Object> parameters = null;
			long startTime = System.currentTimeMillis();
			String result = null;
			boolean hasError = false;
			String output = null;
			try {
				parameters = OBJECT_MAPPER.readValue(jsonInput, new TypeReference<Map<String, Object>>() {
				});
				result = tool.execute(parameters);
				output = result;
				return result;
			}
			catch (JsonProcessingException e) {
				hasError = true;
				result = "Error: Failed to parse parameters for tool '" + tool.getName() + "': " + e.getMessage();
				output = result;
				return result;
			}
			catch (Exception e) {
				hasError = true;
				result = "Error: Tool '" + tool.getName() + "' execution failed: " + e.getMessage();
				output = result;
				return result;
			}
			finally {
				// 自动记录工具调用trace信息
				if (AgentTraceContext.isAvailable()) {
					try {
						String traceId = AgentTraceContext.getTraceId();
						AgentMonitor monitor = AgentTraceContext.getMonitor();
						String toolName = tool.getName();
						long costTimeMs = System.currentTimeMillis() - startTime;
						monitor.traceToolCall(traceId, toolName, parameters, hasError ? ("[ERROR] " + output) : output,
								costTimeMs);
					}
					catch (Exception e) {
						log.warn("Failed to record tool trace for '{}': {}", tool.getName(), e.getMessage());
					}
				}
			}
		}).description(tool.getDescription()).inputType(String.class).build();
	}

	/**
	 * 批量转换工具数组为ToolCallback数组
	 * @param tools 自定义工具数组
	 * @return ToolCallback数组
	 */
	public static ToolCallback[] toToolCallbacks(Tool... tools) {
		ToolCallback[] callbacks = new ToolCallback[tools.length];
		for (int i = 0; i < tools.length; i++) {
			callbacks[i] = toToolCallback(tools[i]);
		}
		return callbacks;
	}

}
