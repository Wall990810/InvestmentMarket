package org.wall.im.ai.core.tool;

import java.util.Map;

/**
 * 工具接口
 * <p>
 * 工具是Agent可调用的外部能力，如API调用、文件操作等
 * </p>
 */
public interface Tool {

	/**
	 * 获取工具名称
	 */
	String getName();

	/**
	 * 获取工具描述
	 */
	String getDescription();

	/**
	 * 获取参数定义(JSON Schema格式)
	 */
	Map<String, Object> getParameterSchema();

	/**
	 * 执行工具
	 * @param parameters 参数Map
	 * @return 执行结果
	 */
	String execute(Map<String, Object> parameters);

}
