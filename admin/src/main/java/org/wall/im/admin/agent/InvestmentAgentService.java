package org.wall.im.admin.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.wall.im.ai.agent.registry.AgentRegistry;
import org.wall.im.ai.core.agent.Agent;
import org.wall.im.ai.core.model.AgentResult;
import org.wall.im.ai.core.model.Message;

import java.util.List;

/**
 * 投资建议Agent服务
 * <p>演示如何通过AgentRegistry获取已注册的Agent并调用其能力</p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 1. 对话式咨询：向Agent提问投资建议
 * AgentResult result = investmentAgentService.consult("我有一笔10万元资金，风险偏好稳健，请给出投资建议");
 *
 * // 2. 获取特定标的分析报告
 * AgentResult report = investmentAgentService.analyze("600519.SH 贵州茅台");
 *
 * // 3. 生成投资组合方案
 * AgentResult portfolio = investmentAgentService.recommendPortfolio("稳健型，投资期限1年，资金50万");
 * }</pre>
 */
@Service
public class InvestmentAgentService {

    private static final Logger log = LoggerFactory.getLogger(InvestmentAgentService.class);

    private static final String AGENT_NAME = "investment-advisor";

    private final AgentRegistry agentRegistry;

    public InvestmentAgentService(AgentRegistry agentRegistry) {
        this.agentRegistry = agentRegistry;
    }

    /**
     * 投资咨询：向Agent发送自然语言问题，获取投资建议回复
     *
     * @param question 用户投资咨询问题
     * @return Agent回复内容
     */
    public String consult(String question) {
        Agent agent = getAgent();
        log.info("投资咨询请求: {}", question);
        String reply = agent.chat(question);
        log.info("投资咨询响应完成");
        return reply;
    }

    /**
     * 标的分析：对指定标的进行深度分析
     *
     * @param symbolDescription 标的描述，如 "600519.SH 贵州茅台"
     * @return 分析报告内容
     */
    public String analyze(String symbolDescription) {
        Agent agent = getAgent();
        log.info("标的分析请求: {}", symbolDescription);
        return agent.chat("请对以下标的进行深度分析并给出投资建议：" + symbolDescription);
    }

    /**
     * 投资组合推荐：根据用户偏好生成资产配置方案（使用多轮消息）
     *
     * @param requirement 投资需求描述，如 "稳健型，投资期限1年，资金50万"
     * @return 投资组合方案结果
     */
    public AgentResult recommendPortfolio(String requirement) {
        Agent agent = getAgent();
        log.info("投资组合推荐请求: {}", requirement);
        List<Message> messages = List.of(
                Message.system("你是一个专业的投资顾问，请根据用户需求生成详细的资产配置方案。"),
                Message.user("请根据以下需求生成投资组合配置方案：" + requirement)
        );
        return agent.execute(messages);
    }

    /**
     * 重置Agent对话上下文（清除短期记忆）
     */
    public void resetSession() {
        Agent agent = getAgent();
        agent.reset();
        log.info("投资建议Agent会话已重置");
    }

    private Agent getAgent() {
        return agentRegistry.getRequired(AGENT_NAME);
    }
}
