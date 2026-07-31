package org.wall.im.ai.agent.registry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.wall.im.ai.core.agent.Agent;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * AgentRegistry单元测试
 */
@DisplayName("AgentRegistry测试")
class AgentRegistryTest {

	private AgentRegistry registry;

	@BeforeEach
	void setUp() {
		registry = new AgentRegistry();
	}

	@Nested
	@DisplayName("注册测试")
	class RegisterTest {

		@Test
		@DisplayName("注册Agent后应能通过名称获取")
		void register_shouldBeRetrievable() {
			Agent agent = mock(Agent.class);
			when(agent.getName()).thenReturn("test-agent");

			registry.register(agent);

			assertTrue(registry.contains("test-agent"));
			assertEquals(1, registry.size());
		}

		@Test
		@DisplayName("重复注册同名Agent应替换旧实例并调用destroy")
		void registerDuplicate_shouldReplaceAndDestroyOld() {
			Agent oldAgent = mock(Agent.class);
			Agent newAgent = mock(Agent.class);
			when(oldAgent.getName()).thenReturn("agent");
			when(newAgent.getName()).thenReturn("agent");

			registry.register(oldAgent);
			registry.register(newAgent);

			assertEquals(1, registry.size());
			assertEquals(newAgent, registry.get("agent").orElse(null));
			verify(oldAgent).destroy();
		}

	}

	@Nested
	@DisplayName("获取测试")
	class GetTest {

		@Test
		@DisplayName("get() - 存在的Agent应返回Optional包装")
		void get_existingAgent_shouldReturnOptional() {
			Agent agent = mock(Agent.class);
			when(agent.getName()).thenReturn("my-agent");
			registry.register(agent);

			assertTrue(registry.get("my-agent").isPresent());
		}

		@Test
		@DisplayName("get() - 不存在的Agent应返回空Optional")
		void get_nonExisting_shouldReturnEmpty() {
			assertTrue(registry.get("non-existent").isEmpty());
		}

		@Test
		@DisplayName("getRequired() - 不存在的Agent应抛出异常")
		void getRequired_nonExisting_shouldThrow() {
			assertThrows(IllegalArgumentException.class, () -> registry.getRequired("missing"));
		}

	}

	@Nested
	@DisplayName("注销与销毁测试")
	class UnregisterAndDestroyTest {

		@Test
		@DisplayName("unregister应移除Agent并调用destroy")
		void unregister_shouldRemoveAndDestroy() {
			Agent agent = mock(Agent.class);
			when(agent.getName()).thenReturn("temp-agent");
			registry.register(agent);

			registry.unregister("temp-agent");

			assertFalse(registry.contains("temp-agent"));
			assertEquals(0, registry.size());
			verify(agent).destroy();
		}

		@Test
		@DisplayName("unregister不存在的Agent不应报错")
		void unregister_nonExisting_shouldNotThrow() {
			assertDoesNotThrow(() -> registry.unregister("ghost"));
		}

		@Test
		@DisplayName("destroyAll应销毁所有Agent")
		void destroyAll_shouldDestroyAllAgents() {
			Agent agent1 = mock(Agent.class);
			Agent agent2 = mock(Agent.class);
			when(agent1.getName()).thenReturn("a1");
			when(agent2.getName()).thenReturn("a2");

			registry.register(agent1);
			registry.register(agent2);
			assertEquals(2, registry.size());

			registry.destroyAll();

			assertEquals(0, registry.size());
			verify(agent1).destroy();
			verify(agent2).destroy();
		}

	}

	@Nested
	@DisplayName("getAll测试")
	class GetAllTest {

		@Test
		@DisplayName("getAll应返回不可修改的集合")
		void getAll_shouldReturnUnmodifiableCollection() {
			Agent agent = mock(Agent.class);
			when(agent.getName()).thenReturn("readonly-agent");
			registry.register(agent);

			var all = registry.getAll();
			assertEquals(1, all.size());
			assertThrows(UnsupportedOperationException.class, () -> all.clear());
		}

	}

}
