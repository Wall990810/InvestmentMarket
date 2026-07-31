package org.wall.im.ai.harness.pipeline;

import org.wall.im.ai.core.model.Message;

import java.util.List;
import java.util.function.Function;

/**
 * 函数式管道阶段
 * <p>
 * 允许通过Lambda表达式快速定义处理阶段
 * </p>
 */
public class FunctionalStage implements PipelineStage {

	private final String name;

	private final Function<List<Message>, List<Message>> processor;

	public FunctionalStage(String name, Function<List<Message>, List<Message>> processor) {
		this.name = name;
		this.processor = processor;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public List<Message> execute(List<Message> messages) {
		return processor.apply(messages);
	}

}
