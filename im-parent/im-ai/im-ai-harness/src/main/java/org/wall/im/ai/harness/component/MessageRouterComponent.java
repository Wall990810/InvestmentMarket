package org.wall.im.ai.harness.component;

import org.wall.im.ai.core.agent.Agent;
import org.wall.im.ai.core.model.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * 消息路由组件
 * <p>
 * 根据消息内容将请求路由到不同的处理逻辑
 * </p>
 */
public class MessageRouterComponent implements HarnessComponent {

	private final List<RouteRule> rules = new ArrayList<>();

	@Override
	public String getName() {
		return "message-router";
	}

	@Override
	public String getDescription() {
		return "根据消息内容进行路由分发";
	}

	/**
	 * 添加路由规则
	 */
	public MessageRouterComponent addRule(RouteRule rule) {
		rules.add(rule);
		return this;
	}

	@Override
	public void initialize() {
	}

	@Override
	public List<Message> execute(Agent agent, List<Message> input) {
		// 按规则匹配，找到第一个匹配的规则进行处理
		for (RouteRule rule : rules) {
			for (Message msg : input) {
				if (rule.matches(msg)) {
					return rule.process(msg);
				}
			}
		}
		// 无匹配规则，原样返回
		return input;
	}

	@Override
	public void destroy() {
	}

	/**
	 * 路由规则接口
	 */
	public interface RouteRule {

		boolean matches(Message message);

		List<Message> process(Message message);

	}

}
