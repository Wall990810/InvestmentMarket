package org.wall.im.ai.harness;

import org.junit.jupiter.api.*;
import org.wall.im.ai.agent.config.AgentConfigParser;
import org.wall.im.ai.agent.lifecycle.*;
import org.wall.im.ai.agent.registry.AgentRegistry;
import org.wall.im.ai.core.agent.Agent;
import org.wall.im.ai.core.config.AgentsDefinition;
import org.wall.im.ai.core.memory.MemoryEntry;
import org.wall.im.ai.core.memory.MemoryStore;
import org.wall.im.ai.core.model.AgentResult;
import org.wall.im.ai.core.model.Message;
import org.wall.im.ai.core.skill.Skill;
import org.wall.im.ai.core.tool.Tool;
import org.wall.im.ai.harness.component.MessageFilterComponent;
import org.springframework.ai.chat.model.ChatModel;
import org.wall.im.ai.harness.pipeline.FunctionalStage;
import org.wall.im.ai.harness.pipeline.MessagePipeline;
import org.wall.im.ai.harness.runner.SequentialRunner;
import org.wall.im.ai.memory.store.InMemoryStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * AI智能体框架整体集成测试
 * <p>
 * 验证从配置解析 → Agent创建 → 组件组装 → 消息处理 → 记忆存储的完整流程
 * </p>
 */
@DisplayName("AI智能体框架集成测试")
class AgentFrameworkIntegrationTest {

	private AgentRegistry registry;

	private SkillRegistry skillRegistry;

	private ToolRegistry toolRegistry;

	private InMemoryStore memoryStore;

	@BeforeEach
	void setUp() {
		registry = new AgentRegistry();
		skillRegistry = new SkillRegistry();
		toolRegistry = new ToolRegistry();
		memoryStore = new InMemoryStore(100);
	}

	@Nested
	@DisplayName("端到端流程测试")
	class EndToEndFlowTest {

		@Test
		@DisplayName("完整流程: YAML配置解析 → Agent创建 → 对话 → 记忆存储")
		void fullFlow_configToConversation() {
			// 1. 解析YAML配置
			String yaml = """
					agents:
					  - name: test-chat-agent
					    description: "Test chat agent for integration test"
					    type: chat
					    skills:
					      - echo-skill
					    tools:
					      - mock-tool
					""";

			AgentConfigParser parser = new AgentConfigParser();
			AgentsDefinition definition = parser.parseFromString(yaml);
			assertNotNull(definition);
			assertEquals(1, definition.getAgents().size());

			// 2. 注册Skill和Tool
			Skill echoSkill = new Skill() {
				@Override
				public String getName() {
					return "echo-skill";
				}

				@Override
				public String getDescription() {
					return "Echo skill";
				}

				@Override
				public String execute(String input) {
					return "Echo: " + input;
				}
			};
			skillRegistry.register(echoSkill);

			Tool mockTool = new Tool() {
				@Override
				public String getName() {
					return "mock-tool";
				}

				@Override
				public String getDescription() {
					return "Mock tool";
				}

				@Override
				public Map<String, Object> getParameterSchema() {
					return Map.of();
				}

				@Override
				public String execute(Map<String, Object> parameters) {
					return "tool-result";
				}
			};
			toolRegistry.register(mockTool);

			// 3. 创建Agent工厂并创建Agent
			MemoryStoreFactory memFactory = storeType -> memoryStore;
			AgentFactory factory = new AgentFactory(skillRegistry, toolRegistry, memFactory, mock(ChatModel.class),
					mock(org.wall.im.ai.core.monitor.AgentMonitor.class));

			Agent agent = factory.create(definition.getAgents().get(0));
			agent.initialize();
			registry.register(agent);

			// 4. 验证Agent已注册
			assertTrue(registry.contains("test-chat-agent"));
			assertEquals(1, registry.size());

			// 5. 执行对话
			String response = agent.chat("Hello World");
			assertNotNull(response);
			// 由于注册了echo-skill，应返回echo结果
			assertEquals("Echo: Hello World", response);

			// 6. 验证记忆已存储
			List<MemoryEntry> shortTerm = memoryStore.retrieve("test-chat-agent:conversation");
			assertFalse(shortTerm.isEmpty());
			assertEquals("Hello World", shortTerm.get(0).getContent());

			// 7. 清理
			agent.destroy();
			registry.destroyAll();
			assertEquals(0, registry.size());
		}

		@Test
		@DisplayName("多Agent协作流程: 配置解析 → 注册 → 串行执行")
		void multiAgentFlow_configToSequentialExecution() {
			String yaml = """
					agents:
					  - name: agent-alpha
					    description: "Alpha agent"
					    type: chat
					  - name: agent-beta
					    description: "Beta agent"
					    type: task
					""";

			AgentConfigParser parser = new AgentConfigParser();
			AgentsDefinition definition = parser.parseFromString(yaml);
			AgentConfigParser.applyDefaults(definition);

			MemoryStoreFactory memFactory = storeType -> new InMemoryStore();
			AgentFactory factory = new AgentFactory(skillRegistry, toolRegistry, memFactory, mock(ChatModel.class),
					mock(org.wall.im.ai.core.monitor.AgentMonitor.class));

			// 创建并注册所有Agent
			for (var config : definition.getAgents()) {
				Agent agent = factory.create(config);
				agent.initialize();
				registry.register(agent);
			}

			assertEquals(2, registry.size());

			// 使用SequentialRunner执行
			SequentialRunner runner = new SequentialRunner();
			Agent alpha = registry.getRequired("agent-alpha");
			Agent beta = registry.getRequired("agent-beta");

			AgentResult resultAlpha = runner.run(alpha, List.of(Message.user("test alpha")));
			AgentResult resultBeta = runner.run(beta, List.of(Message.user("test beta")));

			assertTrue(resultAlpha.isSuccess());
			assertTrue(resultBeta.isSuccess());
		}

	}

	@Nested
	@DisplayName("Pipeline + Component集成测试")
	class PipelineComponentIntegrationTest {

		@Test
		@DisplayName("Pipeline + MessageFilter + Agent对话集成")
		void pipelineWithFilterAndAgent() {
			// 创建Agent
			String yaml = """
					agents:
					  - name: pipeline-agent
					    description: "Pipeline integration agent"
					    type: chat
					""";

			AgentConfigParser parser = new AgentConfigParser();
			AgentsDefinition definition = parser.parseFromString(yaml);
			MemoryStoreFactory memFactory = storeType -> new InMemoryStore();
			AgentFactory factory = new AgentFactory(skillRegistry, toolRegistry, memFactory, mock(ChatModel.class),
					mock(org.wall.im.ai.core.monitor.AgentMonitor.class));

			Agent agent = factory.create(definition.getAgents().get(0));
			agent.initialize();

			// 构建Pipeline: 过滤 → 标记
			MessageFilterComponent filter = new MessageFilterComponent(50, true);
			MessagePipeline pipeline = new MessagePipeline("integration-pipeline")
				.addStage(new FunctionalStage("filter", msgs -> filter.execute(agent, msgs)))
				.addStage(new FunctionalStage("tag", msgs -> {
					List<Message> tagged = new ArrayList<>(msgs);
					tagged.add(0, Message.system("[TAGGED]"));
					return tagged;
				}));

			// 执行Pipeline
			List<Message> input = List.of(Message.user("Hello"), new Message("user", ""), // 应被过滤
					Message.user("World"));

			List<Message> processed = pipeline.process(input);

			// 验证: 空消息被过滤 + 系统标签被添加
			assertEquals(3, processed.size()); // [TAGGED] + Hello + World
			assertEquals("system", processed.get(0).getRole());
			assertEquals("[TAGGED]", processed.get(0).getContent());
		}

	}

	@Nested
	@DisplayName("记忆存储集成测试")
	class MemoryIntegrationTest {

		@Test
		@DisplayName("短期记忆和长期记忆应独立工作")
		void shortTermAndLongTermMemory_shouldWorkIndependently() {
			InMemoryStore shortTerm = new InMemoryStore(5);
			InMemoryStore longTerm = new InMemoryStore(1000);

			// 模拟对话存储
			shortTerm.store("agent:conv", new MemoryEntry("1", "Hi", "user"));
			shortTerm.store("agent:conv", new MemoryEntry("2", "Hello!", "assistant"));
			longTerm.store("agent:history", new MemoryEntry("h1", "Q: Hi A: Hello!", "assistant"));

			// 短期记忆验证
			List<MemoryEntry> recent = shortTerm.retrieveRecent("agent:conv", 1);
			assertEquals(1, recent.size());
			assertEquals("Hello!", recent.get(0).getContent());

			// 长期记忆验证
			List<MemoryEntry> history = longTerm.search("agent:history", "Hi");
			assertEquals(1, history.size());

			// 短期记忆限制测试
			for (int i = 0; i < 10; i++) {
				shortTerm.store("agent:conv", new MemoryEntry(String.valueOf(i), "msg" + i, "user"));
			}
			assertEquals(5, shortTerm.retrieve("agent:conv").size());
		}

	}

	@Nested
	@DisplayName("配置默认值合并集成测试")
	class ConfigDefaultsIntegrationTest {

		@Test
		@DisplayName("默认配置应正确合并到所有Agent")
		void defaultsShouldMergeCorrectly() {
			String yaml = """
					defaults:
					  model:
					    provider: openai
					    name: gpt-4
					    temperature: 0.7
					  memory:
					    shortTermStore: memory
					    longTermStore: memory
					agents:
					  - name: agent-default
					  - name: agent-custom
					    model:
					      provider: azure
					      name: gpt-4-azure
					      temperature: 0.3
					""";

			AgentConfigParser parser = new AgentConfigParser();
			AgentsDefinition def = parser.parseFromString(yaml);
			AgentConfigParser.applyDefaults(def);

			// agent-default应继承默认model
			var defaultAgent = def.getAgents().get(0);
			assertEquals("openai", defaultAgent.getModel().getProvider());
			assertEquals("gpt-4", defaultAgent.getModel().getName());
			assertEquals(0.7, defaultAgent.getModel().getTemperature());
			assertNotNull(defaultAgent.getMemory());
			assertEquals("memory", defaultAgent.getMemory().getShortTermStore());

			// agent-custom应保留自己的model
			var customAgent = def.getAgents().get(1);
			assertEquals("azure", customAgent.getModel().getProvider());
			assertEquals(0.3, customAgent.getModel().getTemperature());
		}

	}

	@Nested
	@DisplayName("生命周期管理集成测试")
	class LifecycleIntegrationTest {

		@Test
		@DisplayName("AgentLifecycleManager应正确管理Agent完整生命周期")
		void lifecycleManager_shouldManageFullLifecycle() {
			List<String> events = new ArrayList<>();

			String yaml = """
					agents:
					  - name: lifecycle-agent
					    description: "Lifecycle test agent"
					""";

			AgentConfigParser parser = new AgentConfigParser();
			AgentsDefinition def = parser.parseFromString(yaml);

			MemoryStoreFactory memFactory = storeType -> new InMemoryStore();
			AgentFactory factory = new AgentFactory(skillRegistry, toolRegistry, memFactory, mock(ChatModel.class),
					mock(org.wall.im.ai.core.monitor.AgentMonitor.class));
			AgentLifecycleManager lifecycleManager = new AgentLifecycleManager(registry, factory);

			// 添加监听器
			lifecycleManager.addListener(new AgentLifecycleListener() {
				@Override
				public void onCreated(Agent agent) {
					events.add("created:" + agent.getName());
				}

				@Override
				public void onStarted(Agent agent) {
					events.add("started:" + agent.getName());
				}

				@Override
				public void onStopped(Agent agent) {
					events.add("stopped:" + agent.getName());
				}

				@Override
				public void onDestroyed(Agent agent) {
					events.add("destroyed:" + agent.getName());
				}
			});

			// 创建Agent
			lifecycleManager.createAgents(def);
			assertEquals(1, registry.size());
			assertTrue(events.contains("created:lifecycle-agent"));

			// 启动Agent
			lifecycleManager.startAll();
			assertTrue(events.contains("started:lifecycle-agent"));

			// 停止Agent
			lifecycleManager.stopAll();
			assertTrue(events.contains("stopped:lifecycle-agent"));

			// 销毁Agent
			lifecycleManager.destroyAll();
			assertEquals(0, registry.size());
		}

	}

}
