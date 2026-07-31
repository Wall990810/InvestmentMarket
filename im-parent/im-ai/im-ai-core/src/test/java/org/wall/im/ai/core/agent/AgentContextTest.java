package org.wall.im.ai.core.agent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.wall.im.ai.core.memory.MemoryStore;
import org.wall.im.ai.core.skill.Skill;
import org.wall.im.ai.core.tool.Tool;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * AgentContext单元测试
 */
@DisplayName("AgentContext测试")
class AgentContextTest {

	private Agent mockAgent;

	private AgentContext context;

	@BeforeEach
	void setUp() {
		mockAgent = mock(Agent.class);
		context = new AgentContext(mockAgent);
	}

	@Nested
	@DisplayName("Skill注册测试")
	class SkillRegistrationTest {

		@Test
		@DisplayName("注册Skill后应能通过名称获取")
		void registerSkill_shouldBeRetrievable() {
			Skill skill = mock(Skill.class);
			when(skill.getName()).thenReturn("qa-skill");

			context.registerSkill(skill);

			assertEquals(skill, context.getSkill("qa-skill"));
			assertEquals(1, context.getSkills().size());
		}

		@Test
		@DisplayName("获取未注册的Skill应返回null")
		void getUnregisteredSkill_shouldReturnNull() {
			assertNull(context.getSkill("non-existent"));
		}

	}

	@Nested
	@DisplayName("Tool注册测试")
	class ToolRegistrationTest {

		@Test
		@DisplayName("注册Tool后应能通过名称获取")
		void registerTool_shouldBeRetrievable() {
			Tool tool = mock(Tool.class);
			when(tool.getName()).thenReturn("calculator");

			context.registerTool(tool);

			assertEquals(tool, context.getTool("calculator"));
			assertEquals(1, context.getTools().size());
		}

	}

	@Nested
	@DisplayName("变量管理测试")
	class VariableTest {

		@Test
		@DisplayName("设置和获取变量")
		void setAndGetVariable() {
			context.setVariable("key1", "value1");
			context.setVariable("key2", 42);

			assertEquals("value1", context.<String>getVariable("key1"));
			assertEquals(42, (int) context.<Integer>getVariable("key2"));
		}

		@Test
		@DisplayName("获取不存在的变量应返回null")
		void getNonExistentVariable_shouldReturnNull() {
			assertNull(context.getVariable("missing"));
		}

	}

	@Nested
	@DisplayName("Memory设置测试")
	class MemoryTest {

		@Test
		@DisplayName("应能设置和获取短期/长期记忆")
		void shouldSetAndGetMemoryStores() {
			MemoryStore shortTerm = mock(MemoryStore.class);
			MemoryStore longTerm = mock(MemoryStore.class);

			context.setShortTermMemory(shortTerm);
			context.setLongTermMemory(longTerm);

			assertEquals(shortTerm, context.getShortTermMemory());
			assertEquals(longTerm, context.getLongTermMemory());
		}

	}

}
