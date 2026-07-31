package org.wall.im.ai.harness.runner;

import org.wall.im.ai.core.agent.Agent;
import org.wall.im.ai.core.model.AgentResult;
import org.wall.im.ai.core.model.Message;

import java.util.List;

/**
 * 串行Agent运行器
 * <p>
 * 按顺序依次执行Agent
 * </p>
 */
public class SequentialRunner implements AgentRunner {

	@Override
	public AgentResult run(Agent agent, List<Message> messages) {
		return agent.execute(messages);
	}

	@Override
	public String getType() {
		return "sequential";
	}

}
