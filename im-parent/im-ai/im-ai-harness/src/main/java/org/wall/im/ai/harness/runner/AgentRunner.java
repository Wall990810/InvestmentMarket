package org.wall.im.ai.harness.runner;

import org.wall.im.ai.core.agent.Agent;
import org.wall.im.ai.core.model.AgentResult;
import org.wall.im.ai.core.model.Message;

import java.util.List;

/**
 * Agent运行器
 * <p>负责Agent的实际执行调度，支持串行、并行、条件等运行模式</p>
 */
public interface AgentRunner {

    /**
     * 运行Agent
     *
     * @param agent    目标Agent
     * @param messages 消息列表
     * @return 执行结果
     */
    AgentResult run(Agent agent, List<Message> messages);

    /**
     * 获取运行器类型
     */
    String getType();
}
