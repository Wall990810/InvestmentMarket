package org.wall.im.ai.agent.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.ToolCallback;
import org.wall.im.ai.core.tool.Tool;

import java.util.Map;

/**
 * Spring AI工具适配器
 * <p>
 * 将自定义 {@link Tool} 接口桥接为Spring AI的 {@link ToolCallback}， 使得自定义工具可以被Spring AI
 * Alibaba的ReactAgent调用。
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

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	/**
	 * 将自定义Tool转换为Spring AI的ToolCallback
	 * <p>
	 * 适配过程：
	 * <ol>
	 * <li>将Tool的parameterSchema（JSON Schema）传递给FunctionToolCallback</li>
	 * <li>将Tool的execute方法包装为Function回调</li>
	 * <li>自动处理JSON字符串与Map之间的转换</li>
	 * </ol>
	 * </p>
	 * @param tool 自定义工具实例
	 * @return Spring AI ToolCallback实例
	 */
	public static ToolCallback toToolCallback(Tool tool) {
		return FunctionToolCallback.builder(tool.getName(), (String jsonInput) -> {
			try {
				Map<String, Object> parameters = OBJECT_MAPPER.readValue(jsonInput,
						new TypeReference<Map<String, Object>>() {
						});
				return tool.execute(parameters);
			}
			catch (JsonProcessingException e) {
				return "Error: Failed to parse parameters for tool '" + tool.getName() + "': " + e.getMessage();
			}
			catch (Exception e) {
				return "Error: Tool '" + tool.getName() + "' execution failed: " + e.getMessage();
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
