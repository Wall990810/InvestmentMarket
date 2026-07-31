package org.wall.im.ai.core.agent;

import org.wall.im.ai.core.model.AgentConfig;
import org.wall.im.ai.core.model.AgentResult;
import org.wall.im.ai.core.model.Message;

import java.util.List;

/**
 * 智能体核心接口
 * <p>
 * 所有Agent实现必须遵循此接口契约
 * </p>
 */
public interface Agent {

	/**
	 * 获取Agent名称
	 */
	String getName();

	/**
	 * 获取Agent配置
	 */
	AgentConfig getConfig();

	/**
	 * 初始化Agent
	 */
	void initialize();

	/**
	 * 执行对话
	 * @param input 用户输入
	 * @return Agent回复内容
	 */
	String chat(String input);

	/**
	 * 执行任务（多轮消息）
	 * @param messages 消息列表
	 * @return 执行结果
	 */
	AgentResult execute(List<Message> messages);

	/**
	 * 重置Agent状态
	 */
	void reset();

	/**
	 * 销毁Agent
	 */
	void destroy();

}
