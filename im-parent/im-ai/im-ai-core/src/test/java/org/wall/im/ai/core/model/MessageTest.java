package org.wall.im.ai.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Message模型单元测试
 */
@DisplayName("Message模型测试")
class MessageTest {

	@Nested
	@DisplayName("构造方法测试")
	class ConstructorTest {

		@Test
		@DisplayName("默认构造 - 应设置当前时间戳")
		void defaultConstructor_shouldSetTimestamp() {
			Instant before = Instant.now();
			Message msg = new Message();
			Instant after = Instant.now();

			assertNotNull(msg.getTimestamp());
			assertFalse(msg.getTimestamp().isBefore(before));
			assertFalse(msg.getTimestamp().isAfter(after));
		}

		@Test
		@DisplayName("参数构造 - 应正确设置role和content")
		void parameterizedConstructor_shouldSetRoleAndContent() {
			Message msg = new Message("user", "hello");

			assertEquals("user", msg.getRole());
			assertEquals("hello", msg.getContent());
			assertNotNull(msg.getTimestamp());
		}

	}

	@Nested
	@DisplayName("工厂方法测试")
	class FactoryMethodTest {

		@Test
		@DisplayName("system() - 应创建system角色消息")
		void system_shouldCreateSystemMessage() {
			Message msg = Message.system("You are a helpful assistant");
			assertEquals("system", msg.getRole());
			assertEquals("You are a helpful assistant", msg.getContent());
		}

		@Test
		@DisplayName("user() - 应创建user角色消息")
		void user_shouldCreateUserMessage() {
			Message msg = Message.user("What is AI?");
			assertEquals("user", msg.getRole());
			assertEquals("What is AI?", msg.getContent());
		}

		@Test
		@DisplayName("assistant() - 应创建assistant角色消息")
		void assistant_shouldCreateAssistantMessage() {
			Message msg = Message.assistant("AI is...");
			assertEquals("assistant", msg.getRole());
			assertEquals("AI is...", msg.getContent());
		}

		@Test
		@DisplayName("tool() - 应创建tool角色消息并设置name")
		void tool_shouldCreateToolMessageWithName() {
			Message msg = Message.tool("calculator", "42");
			assertEquals("tool", msg.getRole());
			assertEquals("42", msg.getContent());
			assertEquals("calculator", msg.getName());
		}

	}

	@Nested
	@DisplayName("元数据测试")
	class MetadataTest {

		@Test
		@DisplayName("metadata默认不为null")
		void metadata_shouldNotBeNullByDefault() {
			Message msg = new Message();
			assertNotNull(msg.getMetadata());
			assertTrue(msg.getMetadata().isEmpty());
		}

		@Test
		@DisplayName("traceId可以正确设置和获取")
		void traceId_shouldSetAndGet() {
			Message msg = new Message("user", "test");
			msg.setTraceId("trace-123");
			assertEquals("trace-123", msg.getTraceId());
		}

	}

}
