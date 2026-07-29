package org.wall.im.ai.harness.component;

import org.wall.im.ai.core.agent.Agent;
import org.wall.im.ai.core.model.Message;

import java.util.List;

/**
 * Harness组件接口
 * <p>参考AgentScope-harness中的组件设计，定义可复用的Agent协作组件</p>
 */
public interface HarnessComponent {

    /**
     * 获取组件名称
     */
    String getName();

    /**
     * 获取组件描述
     */
    String getDescription();

    /**
     * 初始化组件
     */
    void initialize();

    /**
     * 执行组件逻辑
     *
     * @param agent    关联的Agent
     * @param input    输入消息
     * @return 输出消息
     */
    List<Message> execute(Agent agent, List<Message> input);

    /**
     * 销毁组件
     */
    void destroy();
}
