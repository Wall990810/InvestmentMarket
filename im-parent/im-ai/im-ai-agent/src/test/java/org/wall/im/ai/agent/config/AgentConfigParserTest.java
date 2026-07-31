package org.wall.im.ai.agent.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.wall.im.ai.core.config.AgentsDefinition;
import org.wall.im.ai.core.model.AgentConfig;
import org.wall.im.ai.core.model.MemoryConfig;
import org.wall.im.ai.core.model.ModelConfig;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AgentConfigParser单元测试
 */
@DisplayName("AgentConfigParser测试")
class AgentConfigParserTest {

	private AgentConfigParser parser;

	@BeforeEach
	void setUp() {
		parser = new AgentConfigParser();
	}

	@Nested
	@DisplayName("YAML字符串解析测试")
	class ParseFromStringTest {

		@Test
		@DisplayName("应正确解析基本YAML配置")
		void shouldParseBasicYamlConfig() {
			String yaml = """
					agents:
					  - name: test-agent
					    description: "A test agent"
					    type: chat
					    skills:
					      - skill-a
					    tools:
					      - tool-x
					""";

			AgentsDefinition definition = parser.parseFromString(yaml);

			assertNotNull(definition);
			assertEquals(1, definition.getAgents().size());

			AgentConfig agent = definition.getAgents().get(0);
			assertEquals("test-agent", agent.getName());
			assertEquals("A test agent", agent.getDescription());
			assertEquals("chat", agent.getType());
			assertEquals(1, agent.getSkills().size());
			assertEquals("skill-a", agent.getSkills().get(0));
			assertEquals(1, agent.getTools().size());
			assertEquals("tool-x", agent.getTools().get(0));
		}

		@Test
		@DisplayName("应正确解析包含model配置的YAML")
		void shouldParseYamlWithModelConfig() {
			String yaml = """
					agents:
					  - name: llm-agent
					    model:
					      provider: openai
					      name: gpt-4
					      temperature: 0.5
					      maxTokens: 2048
					""";

			AgentsDefinition def = parser.parseFromString(yaml);
			AgentConfig agent = def.getAgents().get(0);

			assertNotNull(agent.getModel());
			assertEquals("openai", agent.getModel().getProvider());
			assertEquals("gpt-4", agent.getModel().getName());
			assertEquals(0.5, agent.getModel().getTemperature());
			assertEquals(2048, agent.getModel().getMaxTokens());
		}

		@Test
		@DisplayName("应正确解析包含defaults的配置")
		void shouldParseYamlWithDefaults() {
			String yaml = """
					defaults:
					  model:
					    provider: azure
					    name: gpt-4-azure
					agents:
					  - name: agent-1
					  - name: agent-2
					    model:
					      provider: openai
					      name: gpt-3.5
					""";

			AgentsDefinition def = parser.parseFromString(yaml);

			assertNotNull(def.getDefaults());
			assertEquals("azure", def.getDefaults().getModel().getProvider());
			assertEquals(2, def.getAgents().size());
		}

		@Test
		@DisplayName("应正确解析多个Agent的配置")
		void shouldParseMultipleAgents() {
			String yaml = """
					agents:
					  - name: agent-a
					    type: chat
					  - name: agent-b
					    type: task
					  - name: agent-c
					    type: workflow
					""";

			AgentsDefinition def = parser.parseFromString(yaml);
			assertEquals(3, def.getAgents().size());
			assertEquals("agent-a", def.getAgents().get(0).getName());
			assertEquals("agent-b", def.getAgents().get(1).getName());
			assertEquals("agent-c", def.getAgents().get(2).getName());
		}

		@Test
		@DisplayName("无效YAML应抛出异常")
		void invalidYaml_shouldThrowException() {
			String invalidYaml = "{{{{not valid yaml";
			assertThrows(RuntimeException.class, () -> parser.parseFromString(invalidYaml));
		}

	}

	@Nested
	@DisplayName("默认配置合并测试")
	class ApplyDefaultsTest {

		@Test
		@DisplayName("应将默认model配置合并到没有model的Agent")
		void shouldApplyDefaultModel() {
			String yaml = """
					defaults:
					  model:
					    provider: openai
					    name: gpt-4
					agents:
					  - name: agent-1
					  - name: agent-2
					    model:
					      provider: azure
					      name: gpt-4-azure
					""";

			AgentsDefinition def = parser.parseFromString(yaml);
			AgentConfigParser.applyDefaults(def);

			// agent-1没有model，应继承defaults
			assertEquals("openai", def.getAgents().get(0).getModel().getProvider());
			// agent-2有自己的model，不应被覆盖
			assertEquals("azure", def.getAgents().get(1).getModel().getProvider());
		}

		@Test
		@DisplayName("无defaults时不应报错")
		void noDefaults_shouldNotThrow() {
			String yaml = """
					agents:
					  - name: agent-1
					""";

			AgentsDefinition def = parser.parseFromString(yaml);
			assertDoesNotThrow(() -> AgentConfigParser.applyDefaults(def));
		}

		@Test
		@DisplayName("应将默认memory配置合并到Agent")
		void shouldApplyDefaultMemory() {
			String yaml = """
					defaults:
					  memory:
					    shortTermStore: redis
					    longTermStore: db
					agents:
					  - name: agent-1
					""";

			AgentsDefinition def = parser.parseFromString(yaml);
			AgentConfigParser.applyDefaults(def);

			AgentConfig agent = def.getAgents().get(0);
			assertNotNull(agent.getMemory());
			assertEquals("redis", agent.getMemory().getShortTermStore());
			assertEquals("db", agent.getMemory().getLongTermStore());
		}

	}

	@Nested
	@DisplayName("classpath资源解析测试")
	class ClasspathTest {

		@Test
		@DisplayName("不存在的资源应抛出异常")
		void nonExistentResource_shouldThrowException() {
			assertThrows(IllegalArgumentException.class, () -> parser.parseFromClasspath("non-existent.yml"));
		}

	}

}
