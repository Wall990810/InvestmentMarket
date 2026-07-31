package org.wall.im.ai.harness.component;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.wall.im.ai.core.agent.Agent;
import org.wall.im.ai.core.model.Message;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * MessageFilterComponent单元测试
 */
@DisplayName("MessageFilterComponent测试")
class MessageFilterComponentTest {

	private final Agent mockAgent = mock(Agent.class);

	@Nested
	@DisplayName("空消息过滤测试")
	class EmptyMessageFilterTest {

		@Test
		@DisplayName("应移除content为null的消息")
		void shouldRemoveNullContentMessages() {
			MessageFilterComponent filter = new MessageFilterComponent(100, true);
			Message nullMsg = new Message("user", null);
			List<Message> result = filter.execute(mockAgent, List.of(nullMsg, Message.user("valid")));

			assertEquals(1, result.size());
			assertEquals("valid", result.get(0).getContent());
		}

		@Test
		@DisplayName("应移除content为空白的消息")
		void shouldRemoveBlankContentMessages() {
			MessageFilterComponent filter = new MessageFilterComponent(100, true);
			List<Message> input = List.of(new Message("user", "   "), Message.user("valid"));

			List<Message> result = filter.execute(mockAgent, input);

			assertEquals(1, result.size());
			assertEquals("valid", result.get(0).getContent());
		}

		@Test
		@DisplayName("removeEmpty=false时不应移除空消息")
		void removeEmptyFalse_shouldKeepEmptyMessages() {
			MessageFilterComponent filter = new MessageFilterComponent(100, false);
			List<Message> input = List.of(new Message("user", ""), Message.user("valid"));

			List<Message> result = filter.execute(mockAgent, input);

			assertEquals(2, result.size());
		}

	}

	@Nested
	@DisplayName("消息截断测试")
	class MessageTruncationTest {

		@Test
		@DisplayName("超长消息应被截断")
		void longMessage_shouldBeTruncated() {
			MessageFilterComponent filter = new MessageFilterComponent(10, true);
			String longContent = "This is a very long message that exceeds the limit";
			List<Message> input = List.of(new Message("user", longContent));

			List<Message> result = filter.execute(mockAgent, input);

			assertTrue(result.get(0).getContent().endsWith("...[truncated]"));
			assertTrue(result.get(0).getContent().length() < longContent.length());
		}

		@Test
		@DisplayName("短消息不应被截断")
		void shortMessage_shouldNotBeTruncated() {
			MessageFilterComponent filter = new MessageFilterComponent(100, true);
			List<Message> input = List.of(Message.user("Hi"));

			List<Message> result = filter.execute(mockAgent, input);

			assertEquals("Hi", result.get(0).getContent());
		}

	}

	@Test
	@DisplayName("组件名称和描述应正确返回")
	void nameAndDescription_shouldBeCorrect() {
		MessageFilterComponent filter = new MessageFilterComponent();
		assertEquals("message-filter", filter.getName());
		assertNotNull(filter.getDescription());
	}

}
