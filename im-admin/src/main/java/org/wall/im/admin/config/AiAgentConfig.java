package org.wall.im.admin.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.wall.im.ai.agent.config.AgentConfigParser;
import org.wall.im.ai.agent.lifecycle.AgentFactory;
import org.wall.im.ai.agent.lifecycle.AgentLifecycleManager;
import org.wall.im.ai.agent.lifecycle.MemoryStoreFactory;
import org.wall.im.ai.agent.lifecycle.SkillRegistry;
import org.wall.im.ai.agent.lifecycle.ToolRegistry;
import org.wall.im.ai.agent.registry.AgentRegistry;
import org.wall.im.ai.core.config.AgentsDefinition;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;

/**
 * AI智能体配置类
 * 负责加载YAML配置、注册Skill/Tool、创建并管理Agent实例
 */
@Configuration
public class AiAgentConfig {

    private static final Logger log = LoggerFactory.getLogger(AiAgentConfig.class);

    /**
     * 配置解析器：解析 investment-advisor.yml
     */
    @Bean
    public AgentConfigParser agentConfigParser() {
        return new AgentConfigParser();
    }

    /**
     * Agent注册表
     */
    @Bean
    public AgentRegistry agentRegistry() {
        return new AgentRegistry();
    }

    /**
     * 技能注册表：注册投资建议相关技能
     */
    @Bean
    public SkillRegistry skillRegistry() {
        SkillRegistry registry = new SkillRegistry();
        registry.register(new org.wall.im.admin.agent.skill.InvestmentAnalysisSkill());
        registry.register(new org.wall.im.admin.agent.skill.PortfolioRecommendSkill());
        log.info("已注册投资建议技能: investment-analysis-skill, portfolio-recommend-skill");
        return registry;
    }

    /**
     * 工具注册表：注册行情查询和风险评估工具
     */
    @Bean
    public ToolRegistry toolRegistry() {
        ToolRegistry registry = new ToolRegistry();
        registry.register(new org.wall.im.admin.agent.tool.MarketDataTool());
        registry.register(new org.wall.im.admin.agent.tool.RiskAssessmentTool());
        log.info("已注册投资分析工具: market-data-tool, risk-assessment-tool");
        return registry;
    }

    /**
     * MemoryStore工厂（默认使用内存存储）
     */
    @Bean
    public MemoryStoreFactory memoryStoreFactory() {
        return storeType -> {
            // 默认返回内存存储实现，实际可替换为Redis/DB
            return new org.wall.im.ai.core.memory.MemoryStore() {
                private final java.util.Map<String, java.util.List<org.wall.im.ai.core.memory.MemoryEntry>> data
                        = new java.util.concurrent.ConcurrentHashMap<>();

                @Override public void store(String key, org.wall.im.ai.core.memory.MemoryEntry entry) {
                    data.computeIfAbsent(key, k -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(entry);
                }
                @Override public void storeAll(String key, java.util.List<org.wall.im.ai.core.memory.MemoryEntry> entries) {
                    entries.forEach(e -> store(key, e));
                }
                @Override public java.util.List<org.wall.im.ai.core.memory.MemoryEntry> retrieve(String key) {
                    return data.getOrDefault(key, java.util.List.of());
                }
                @Override public java.util.List<org.wall.im.ai.core.memory.MemoryEntry> retrieveRecent(String key, int limit) {
                    var list = retrieve(key);
                    return list.subList(Math.max(0, list.size() - limit), list.size());
                }
                @Override public java.util.List<org.wall.im.ai.core.memory.MemoryEntry> search(String key, String keyword) {
                    return retrieve(key);
                }
                @Override public void clear(String key) {
                    data.remove(key);
                }
                @Override public String getStoreType() { return "memory"; }
            };
        };
    }

    /**
     * DashScope API客户端
     * <p>API Key通过环境变量 AI_DASHSCOPE_API_KEY 或配置文件 spring.ai.dashscope.api-key 提供</p>
     */
    @Bean
    public DashScopeApi dashScopeApi(
            @Value("${spring.ai.dashscope.api-key:${AI_DASHSCOPE_API_KEY:}}") String apiKey) {
        return DashScopeApi.builder()
                .apiKey(apiKey)
                .build();
    }

    /**
     * DashScope ChatModel（通义千问大模型）
     * <p>作为ReactAgent的“大脑”，提供推理能力</p>
     */
    @Bean
    public ChatModel chatModel(DashScopeApi dashScopeApi,
                               @Value("${spring.ai.dashscope.chat.model:qwen-plus}") String modelName,
                               @Value("${spring.ai.dashscope.chat.temperature:0.7}") double temperature,
                               @Value("${spring.ai.dashscope.chat.max-tokens:4096}") int maxTokens) {
        return DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(DashScopeChatOptions.builder()
                        .model(modelName)
                        .temperature(temperature)
                        .maxToken(maxTokens)
                        .build())
                .build();
    }

    /**
     * Agent工厂
     * <p>注入ChatModel，用于创建基于ReactAgent的智能体实例</p>
     */
    @Bean
    public AgentFactory agentFactory(SkillRegistry skillRegistry, ToolRegistry toolRegistry,
                                     MemoryStoreFactory memoryStoreFactory, ChatModel chatModel) {
        return new AgentFactory(skillRegistry, toolRegistry, memoryStoreFactory, chatModel);
    }

    /**
     * 生命周期管理器
     */
    @Bean
    public AgentLifecycleManager agentLifecycleManager(AgentRegistry agentRegistry, AgentFactory agentFactory) {
        return new AgentLifecycleManager(agentRegistry, agentFactory);
    }

    /**
     * 加载投资建议Agent配置并创建Agent实例
     * 在Spring容器启动时自动执行
     */
    @Bean
    public AgentsDefinition investmentAgentsDefinition(
            AgentConfigParser parser,
            AgentLifecycleManager lifecycleManager) {
        // 从 classpath 加载配置文件
        AgentsDefinition definition = parser.parseFromClasspath("agents/investment-advisor.yml");
        log.info("成功加载投资建议Agent配置，共 {} 个Agent", definition.getAgents().size());

        // 创建并注册所有Agent（createAgents内部完成注册）
        lifecycleManager.createAgents(definition);
        log.info("成功创建并注册投资建议Agent实例");

        return definition;
    }
}
