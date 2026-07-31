package org.wall.im.ai.harness.component;

import org.wall.im.ai.core.agent.Agent;
import org.wall.im.ai.core.model.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * 消息过滤组件
 * <p>
 * 对输入消息进行预处理和过滤，如去除空消息、截断过长内容等
 * </p>
 */
public class MessageFilterComponent implements HarnessComponent {

	private final int maxMessageLength;

	private final boolean removeEmpty;

	public MessageFilterComponent() {
		this(10000, true);
	}

	public MessageFilterComponent(int maxMessageLength, boolean removeEmpty) {
		this.maxMessageLength = maxMessageLength;
		this.removeEmpty = removeEmpty;
	}

	@Override
	public String getName() {
		return "message-filter";
	}

	@Override
	public String getDescription() {
		return "消息预处理和过滤";
	}

	@Override
	public void initialize() {
	}

	@Override
	public List<Message> execute(Agent agent, List<Message> input) {
		List<Message> filtered = new ArrayList<>();
		for (Message msg : input) {
			if (removeEmpty && (msg.getContent() == null || msg.getContent().isBlank())) {
				continue;
			}
			if (msg.getContent() != null && msg.getContent().length() > maxMessageLength) {
				msg.setContent(msg.getContent().substring(0, maxMessageLength) + "...[truncated]");
			}
			filtered.add(msg);
		}
		return filtered;
	}

	@Override
	public void destroy() {
	}

}
