package org.wall.im.ai.sandbox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.wall.im.ai.core.model.SandboxConfig;
import org.wall.im.ai.core.sandbox.Sandbox;
import org.wall.im.ai.core.sandbox.SandboxFactory;
import org.wall.im.ai.core.sandbox.SandboxType;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SandboxRegistry 单元测试
 */
@DisplayName("SandboxRegistry测试")
class SandboxRegistryTest {

	@Test
	@DisplayName("register 后 getFactory 命中")
	void register_andGetFactory_shouldHit() {
		SandboxRegistry registry = new SandboxRegistry(Collections.emptyList());
		SandboxFactory factory = mock(SandboxFactory.class);
		when(factory.supportedType()).thenReturn(SandboxType.CUSTOM);
		when(factory.priority()).thenReturn(10);

		registry.register(factory);

		assertTrue(registry.getFactory(SandboxType.CUSTOM).isPresent());
	}

	@Test
	@DisplayName("同 type 两 factory priority 高的胜出")
	void register_sameTypeHigherPriorityWins() {
		SandboxRegistry registry = new SandboxRegistry(Collections.emptyList());
		SandboxFactory low = mock(SandboxFactory.class);
		when(low.supportedType()).thenReturn(SandboxType.CUSTOM);
		when(low.priority()).thenReturn(1);
		SandboxFactory high = mock(SandboxFactory.class);
		when(high.supportedType()).thenReturn(SandboxType.CUSTOM);
		when(high.priority()).thenReturn(100);

		registry.register(low);
		registry.register(high);

		assertEquals(high, registry.getFactory(SandboxType.CUSTOM).get());
	}

	@Test
	@DisplayName("create 按 config.type 路由到对应 factory")
	void create_shouldRouteByType() {
		SandboxRegistry registry = new SandboxRegistry(Collections.emptyList());
		SandboxFactory factory = mock(SandboxFactory.class);
		when(factory.supportedType()).thenReturn(SandboxType.CUSTOM);
		when(factory.priority()).thenReturn(100);
		Sandbox mockSandbox = mock(Sandbox.class);
		when(factory.create(any())).thenReturn(mockSandbox);
		registry.register(factory);

		SandboxConfig config = new SandboxConfig();
		config.setType(SandboxType.CUSTOM);
		Sandbox result = registry.create(config);

		assertSame(mockSandbox, result);
		verify(factory).create(config);
	}

	@Test
	@DisplayName("无匹配 type 时 create 抛 IllegalStateException")
	void create_noMatchingType_throws() {
		SandboxRegistry registry = new SandboxRegistry(Collections.emptyList());
		SandboxConfig config = new SandboxConfig();
		config.setType(SandboxType.CUSTOM); // 内置 3 个 factory 不含 CUSTOM

		assertThrows(IllegalStateException.class, () -> registry.create(config));
	}

	@Test
	@DisplayName("ServiceLoader 兜底加载内置 3 个 factory")
	void defaultConstructor_loadsBuiltinFactories() {
		SandboxRegistry registry = new SandboxRegistry();

		assertTrue(registry.getFactory(SandboxType.LOCAL_PROCESS).isPresent());
		assertTrue(registry.getFactory(SandboxType.LOCAL_DOCKER).isPresent());
		assertTrue(registry.getFactory(SandboxType.REMOTE_HTTP).isPresent());
	}

}
