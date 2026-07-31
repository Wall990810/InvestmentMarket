package org.wall.im.ai.agent.lifecycle;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.wall.im.ai.core.agent.Agent;
import org.wall.im.ai.core.agent.AgentContext;
import org.wall.im.ai.core.memory.MemoryEntry;
import org.wall.im.ai.core.memory.MemoryStore;
import org.wall.im.ai.core.model.AgentConfig;
import org.wall.im.ai.core.model.AgentResult;
import org.wall.im.ai.core.model.Message;
import org.wall.im.ai.core.skill.Skill;
import org.wall.im.ai.core.tool.Tool;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 基于Spring AI Alibaba ReactAgent的默认Agent实现
 * <p>
 * 内部委托给 {@link ReactAgent} 执行ReAct（Reasoning + Acting）范式的智能体推理循环。
 * ReactAgent会自动进行：推理(Reason) → 调用工具(Act) → 观察结果(Observe) → 循环直到得出最终答案。
 * </p>
 */
public class DefaultAgent implements Agent {

	private static final Logger log = LoggerFactory.getLogger(DefaultAgent.class);

	private final AgentConfig config;

	private final AgentContext context;

	private final ChatModel chatModel;

	private final List<Tool> agentTools;

	private ReactAgent reactAgent;

	private volatile boolean initialized = false;

	/**
	 * 创建DefaultAgent
	 * @param config Agent配置
	 * @param chatModel Spring AI ChatModel实例（如DashScopeChatModel）
	 * @param tools 该Agent可用的工具列表
	 */
	public DefaultAgent(AgentConfig config, ChatModel chatModel, List<Tool> tools) {
		this.config = config;
		this.context = new AgentContext(this);
		this.chatModel = chatModel;
		this.agentTools = tools != null ? new ArrayList<>(tools) : new ArrayList<>();
	}

	@Override
	public String getName() {
		return config.getName();
	}

	@Override
	public AgentConfig getConfig() {
		return config;
	}

	@Override
	public void initialize() {
		if (initialized) {
			log.warn("Agent '{}' already initialized", getName());
			return;
		}
		log.info("Initializing ReactAgent: {}", getName());

		// 构建系统提示词：优先使用description，兜底使用name
		String systemPrompt = config.getDescription() != null ? config.getDescription()
				: "You are a helpful AI assistant named " + getName() + ".";

		// 构建ReactAgent
		var builder = ReactAgent.builder().name(getName()).model(chatModel).instruction(systemPrompt);

		// 注册工具：将自定义Tool适配为Spring AI的ToolCallback
		if (!agentTools.isEmpty()) {
			ToolCallback[] toolCallbacks = org.wall.im.ai.agent.adapter.SpringAiToolAdapter
				.toToolCallbacks(agentTools.toArray(new Tool[0]));
			builder.tools(toolCallbacks);
		}

		// 设置最大迭代次数（从execution配置中获取，默认10）
		int maxIterations = 10;
		if (config.getExecution() != null && config.getExecution().getMaxConcurrency() > 0) {
			maxIterations = Math.min(config.getExecution().getMaxConcurrency(), 20);
		}
		builder.compileConfig(CompileConfig.builder().recursionLimit(maxIterations).build());

		this.reactAgent = builder.build();
		this.initialized = true;
		log.info("ReactAgent '{}' initialized successfully with {} tools", getName(), agentTools.size());
	}

	@Override
	public String chat(String input) {
		if (!initialized) {
			throw new IllegalStateException("Agent not initialized: " + getName());
		}

		// 存储到短期记忆
		if (context.getShortTermMemory() != null) {
			context.getShortTermMemory()
				.store(getName() + ":conversation", new MemoryEntry(UUID.randomUUID().toString(), input, "user"));
		}

		// 委托给ReactAgent执行ReAct推理循环
		log.debug("ReactAgent '{}' processing input: {}", getName(), input);
		String result;
		try {
			var response = reactAgent.call(input);
			// AssistantMessage支持getText()和content()两种方式获取文本
			result = response != null ? response.getText() : "";
		}
		catch (Exception e) {
			log.error("ReactAgent '{}' execution failed: {}", getName(), e.getMessage(), e);
			result = "Agent执行异常: " + e.getMessage();
		}

		// 存储到长期记忆
		if (context.getLongTermMemory() != null) {
			MemoryEntry entry = new MemoryEntry(UUID.randomUUID().toString(), "Q: " + input + " A: " + result,
					"assistant");
			entry.setImportance(0.5);
			context.getLongTermMemory().store(getName() + ":history", entry);
		}

		return result;
	}

	@Override
	public AgentResult execute(List<Message> messages) {
		long startTime = System.currentTimeMillis();
		AgentResult result = new AgentResult();
		result.setMessageChain(new ArrayList<>(messages));

		try {
			StringBuilder outputBuilder = new StringBuilder();
			for (Message msg : messages) {
				if ("user".equals(msg.getRole())) {
					String response = chat(msg.getContent());
					outputBuilder.append(response).append("\n");
				}
			}
			result.setSuccess(true);
			result.setOutput(outputBuilder.toString().trim());
		}
		catch (Exception e) {
			result.setSuccess(false);
			result.setErrorMessage(e.getMessage());
		}

		result.setCostTimeMs(System.currentTimeMillis() - startTime);
		return result;
	}

	@Override
	public void reset() {
		if (context.getShortTermMemory() != null) {
			context.getShortTermMemory().clear(getName() + ":conversation");
		}
		log.info("Agent '{}' reset", getName());
	}

	@Override
	public void destroy() {
		reset();
		if (context.getLongTermMemory() != null) {
			context.getLongTermMemory().clear(getName() + ":history");
		}
		reactAgent = null;
		initialized = false;
		log.info("Agent '{}' destroyed", getName());
	}

	public AgentContext getContext() {
		return context;
	}

	/**
	 * 获取内部的ReactAgent实例（用于高级场景直接操作）
	 */
	public ReactAgent getReactAgent() {
		return reactAgent;
	}

}
