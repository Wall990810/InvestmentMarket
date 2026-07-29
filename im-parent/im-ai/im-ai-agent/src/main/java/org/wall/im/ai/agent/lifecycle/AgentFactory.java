package org.wall.im.ai.agent.lifecycle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.wall.im.ai.core.agent.Agent;
import org.wall.im.ai.core.agent.AgentContext;
import org.wall.im.ai.core.memory.MemoryStore;
import org.wall.im.ai.core.model.AgentConfig;
import org.wall.im.ai.core.skill.Skill;
import org.wall.im.ai.core.tool.Tool;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent工厂
 * <p>根据AgentConfig创建Agent实例，组装Skill、Tool、Memory等组件。</p>
 * <p>底层使用Spring AI Alibaba的ReactAgent实现ReAct推理范式。</p>
 */
public class AgentFactory {

    private static final Logger log = LoggerFactory.getLogger(AgentFactory.class);

    private final SkillRegistry skillRegistry;
    private final ToolRegistry toolRegistry;
    private final MemoryStoreFactory memoryStoreFactory;
    private final ChatModel chatModel;

    /**
     * 创建AgentFactory
     *
     * @param skillRegistry     技能注册表
     * @param toolRegistry      工具注册表
     * @param memoryStoreFactory 记忆存储工厂
     * @param chatModel         Spring AI ChatModel（如DashScopeChatModel）
     */
    public AgentFactory(SkillRegistry skillRegistry, ToolRegistry toolRegistry,
                        MemoryStoreFactory memoryStoreFactory, ChatModel chatModel) {
        this.skillRegistry = skillRegistry;
        this.toolRegistry = toolRegistry;
        this.memoryStoreFactory = memoryStoreFactory;
        this.chatModel = chatModel;
    }

    /**
     * 根据配置创建Agent
     * <p>流程：
     * <ol>
     *     <li>从ToolRegistry中查找配置指定的工具</li>
     *     <li>创建DefaultAgent（内部封装ReactAgent）</li>
     *     <li>组装Skills到AgentContext</li>
     *     <li>组装Memory到AgentContext</li>
     * </ol>
     * </p>
     */
    public Agent create(AgentConfig config) {
        // 收集配置中指定的工具实例
        List<Tool> tools = new ArrayList<>();
        for (String toolName : config.getTools()) {
            Tool tool = toolRegistry.get(toolName);
            if (tool != null) {
                tools.add(tool);
                log.debug("Resolved tool '{}' for agent '{}'", toolName, config.getName());
            } else {
                log.warn("Tool '{}' not found in registry for agent '{}'", toolName, config.getName());
            }
        }

        // 创建DefaultAgent（传入ChatModel和工具列表）
        DefaultAgent agent = new DefaultAgent(config, chatModel, tools);
        AgentContext context = agent.getContext();

        // 组装Skills到上下文
        for (String skillName : config.getSkills()) {
            Skill skill = skillRegistry.get(skillName);
            if (skill != null) {
                context.registerSkill(skill);
                log.debug("Resolved skill '{}' for agent '{}'", skillName, config.getName());
            } else {
                log.warn("Skill '{}' not found in registry for agent '{}'", skillName, config.getName());
            }
        }

        // 组装Memory
        if (config.getMemory() != null) {
            MemoryStore shortTerm = memoryStoreFactory.create(config.getMemory().getShortTermStore());
            MemoryStore longTerm = memoryStoreFactory.create(config.getMemory().getLongTermStore());
            context.setShortTermMemory(shortTerm);
            context.setLongTermMemory(longTerm);
        }

        log.info("Created agent '{}' with {} tools and {} skills",
                config.getName(), tools.size(), context.getSkills().size());
        return agent;
    }
}
