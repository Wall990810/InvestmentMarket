package org.wall.im.ai.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AgentResult模型单元测试
 */
@DisplayName("AgentResult模型测试")
class AgentResultTest {

	@Nested
	@DisplayName("工厂方法测试")
	class FactoryMethodTest {

		@Test
		@DisplayName("success() - 应创建成功结果")
		void success_shouldCreateSuccessResult() {
			AgentResult result = AgentResult.success("output data");

			assertTrue(result.isSuccess());
			assertEquals("output data", result.getOutput());
			assertNull(result.getErrorMessage());
		}

		@Test
		@DisplayName("failure() - 应创建失败结果")
		void failure_shouldCreateFailureResult() {
			AgentResult result = AgentResult.failure("something went wrong");

			assertFalse(result.isSuccess());
			assertEquals("something went wrong", result.getErrorMessage());
			assertNull(result.getOutput());
		}

	}

	@Nested
	@DisplayName("属性设置测试")
	class PropertyTest {

		@Test
		@DisplayName("应正确设置和获取所有属性")
		void shouldSetAndGetAllProperties() {
			AgentResult result = new AgentResult();
			result.setSuccess(true);
			result.setOutput("result");
			result.setCostTimeMs(1500);
			result.setTokenUsage(256);
			result.setTraceId("trace-abc");

			assertTrue(result.isSuccess());
			assertEquals("result", result.getOutput());
			assertEquals(1500, result.getCostTimeMs());
			assertEquals(256, result.getTokenUsage());
			assertEquals("trace-abc", result.getTraceId());
			assertNotNull(result.getMessageChain());
			assertTrue(result.getMessageChain().isEmpty());
		}

	}

}
