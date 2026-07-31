package org.wall.im.ai.harness.pipeline;

import org.wall.im.ai.core.agent.Agent;
import org.wall.im.ai.core.model.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * 消息处理管道
 * <p>
 * 参考AgentScope的Pipeline设计，将消息处理流程拆分为多个可组合的处理阶段
 * </p>
 */
public class MessagePipeline {

	private final List<PipelineStage> stages = new ArrayList<>();

	private final String name;

	public MessagePipeline(String name) {
		this.name = name;
	}

	/**
	 * 添加处理阶段
	 */
	public MessagePipeline addStage(PipelineStage stage) {
		stages.add(stage);
		return this;
	}

	/**
	 * 执行管道
	 * @param messages 输入消息列表
	 * @return 处理后的消息列表
	 */
	public List<Message> process(List<Message> messages) {
		List<Message> current = new ArrayList<>(messages);
		for (PipelineStage stage : stages) {
			current = stage.execute(current);
		}
		return current;
	}

	/**
	 * 获取管道名称
	 */
	public String getName() {
		return name;
	}

	/**
	 * 获取所有阶段
	 */
	public List<PipelineStage> getStages() {
		return stages;
	}

}
