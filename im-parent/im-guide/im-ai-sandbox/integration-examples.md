← [返回索引](../README.md)

# 使用示例

本节按 4 个典型场景给出端到端代码，覆盖向后兼容、Docker 强隔离、远端 HTTP、策略链 + 生命周期钩子四类。所有代码均可直接编译（只需 `im-ai-sandbox` compile 依赖）。

---

## 场景 1：向后兼容（旧 2 参构造，零改动运行）

**适用**：旧代码只依赖 `ProcessSandbox + SandboxManager 两参`。

```java
import org.wall.im.ai.core.model.SandboxConfig;
import org.wall.im.ai.core.sandbox.Sandbox;
import org.wall.im.ai.core.sandbox.SandboxResult;
import org.wall.im.ai.sandbox.SandboxManager;
import org.wall.im.ai.sandbox.local.ProcessSandbox;

public class LegacyMode {
    public static void main(String[] args) {
        // 兼容构造：6 字段传统 config
        SandboxConfig config = new SandboxConfig(
            "/tmp/work",
            30,                   // maxExecutionTimeSec
            512,                  // maxMemoryMb
            List.of("/var/tmp"),  // allowedPaths
            List.of("ls", "cat"), // allowedCommands
            true                  // enabled
        );

        Sandbox sandbox = new ProcessSandbox();

        // 经典 2 参构造 → ownedByManager=false；DefaultCommandPolicy(restrict=false) 宽松策略
        SandboxManager mgr = new SandboxManager(sandbox, config);

        // 可调用代码/命令，失败文案保持旧版
        SandboxResult r1 = mgr.executeCode("echo hello; ls $HOME", "bash");
        SandboxResult r2 = mgr.executeCommand("ls", List.of("-la", "/tmp/work"));

        System.out.println(r1.success() + " : " + r1.output());
        System.out.println(r2.success() + " : " + r2.output());

        // ownedByManager=false，sandbox 本身的 destroy 需要外部显式调用
        sandbox.destroy();
    }
}
```

---

## 场景 2：本地 Docker 强隔离（Registry 路由 + ResourceLimits）

**适用**：生产不可信脚本、要求资源强隔离。

```java
import org.wall.im.ai.core.model.SandboxConfig;
import org.wall.im.ai.core.sandbox.*;
import org.wall.im.ai.sandbox.SandboxManager;
import org.wall.im.ai.sandbox.SandboxRegistry;

public class DockerMode {
    public static void main(String[] args) {
        SandboxRegistry registry = new SandboxRegistry(); // ServiceLoader 默认 3 工厂

        SandboxConfig config = new SandboxConfig()
            .setType(SandboxType.LOCAL_DOCKER)
            .setWorkDirectory("/tmp/im-docker-work")       // 宿主机路径
            .setImage("python:3.13-slim")                  // 自定义镜像
            .setResourceLimits(new ResourceLimits(
                /*cpuCores*/         2.0,
                /*memoryMb*/         2048,
                /*maxExecutionTimeS*/ 90,
                /*diskMb*/           1024,                  // → tmpfs /tmp 1G
                /*maxProcesses*/     256))                  // pids-limit
            .setEnvVars(Map.of("TZ", "Asia/Shanghai"))
            .setEnabled(true);

        SandboxContext ctx = new SandboxContext("agent-7", "sess-42", Map.of("source", "example"));

        // 构造器 4：Registry 路由，ownedByManager=true
        try (SandboxManager mgr = new SandboxManager(registry, config, ctx)) {
            SandboxResult r = mgr.executeCommand("python3", List.of(
                "-c", "print(sum(range(101)))"));
            if (r.success()) {
                System.out.println("Output: " + r.output().trim());  // 5050
            } else {
                System.err.println("Fail: " + r.output());
            }
        }
        // try 退出：自动 destroy → docker rm -f 容器
    }
}
```

---

## 场景 3：远端 HTTP 沙盒 + 上下文透传

**适用**：多租户共享执行集群。

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import org.wall.im.ai.core.model.SandboxConfig;
import org.wall.im.ai.core.sandbox.*;
import org.wall.im.ai.sandbox.SandboxManager;
import org.wall.im.ai.sandbox.remote.HttpRemoteSandboxClient;
import org.wall.im.ai.sandbox.remote.RemoteSandbox;
import org.wall.im.ai.sandbox.remote.RemoteSandboxClient;

public class RemoteMode {
    public static void main(String[] args) {
        String endpoint = "https://sb.example.com:8443";

        SandboxConfig config = new SandboxConfig()
            .setType(SandboxType.REMOTE_HTTP)
            .setRemoteEndpoint(endpoint)
            .setWorkDirectory("/workspace")
            .setResourceLimits(new ResourceLimits(4.0, 8192, 300, null, 4096))
            .setEnvVars(Map.of("JAVA_TOOL_OPTIONS", "-Xmx4g"));

        SandboxContext ctx = new SandboxContext(
            "agent-99",                // X-Agent-Id
            "sess-20260825a",          // X-Session-Id
            Map.of("tenant", "corp-A", "model", "gpt-5"));  // → X-Meta-tenant: corp-A 等

        RemoteSandboxClient client = new HttpRemoteSandboxClient(
            endpoint, 10, new ObjectMapper());
        Sandbox sandbox = new RemoteSandbox(client);

        try (SandboxManager mgr = new SandboxManager(sandbox, config); // 构造器 1，这里 sandbox 外部持有
             var _ = sandbox) {                    // 双 try：先 close Manager 再 close Sandbox
            mgr.initialize(ctx);
            SandboxResult r = mgr.executeCode("""
                #!/usr/bin/env python3
                import platform
                print(f'Python={platform.python_version()}')
                """, "bash");
            System.out.println(r.output());
        }
    }
}
```

> 实际生产建议用 Registry（type=REMOTE_HTTP 会走 RemoteSandboxFactory 自动装配 client，避免手动 new）。

---

## 场景 4：复合策略链 + 审计监听器

**适用**：金融合规等严格场景，除默认关键字黑名单外，叠加 AST 级安全扫描和落库审计。

```java
import org.wall.im.ai.core.model.SandboxConfig;
import org.wall.im.ai.core.sandbox.*;
import org.wall.im.ai.core.sandbox.policy.CommandPolicy;
import org.wall.im.ai.core.sandbox.policy.CompositeCommandPolicy;
import org.wall.im.ai.sandbox.SandboxManager;
import org.wall.im.ai.sandbox.SandboxRegistry;
import org.wall.im.ai.sandbox.policy.DefaultCommandPolicy;

public class StrictComplianceMode {

    // 自定义策略：禁止访问 /etc/shadow、/proc/kcore 等敏感对象的显式路径
    private static final CommandPolicy extraBlacklistPolicy = new CommandPolicy() {
        private final Set<String> FORBIDDEN = Set.of(
            "/etc/shadow", "/proc/kcore", "/proc/sysrq-trigger");
        @Override public boolean isAllowed(String cmd, SandboxConfig c) {
            return FORBIDDEN.stream().noneMatch(cmd::contains);
        }
        @Override public String getPolicyName() { return "extra-blacklist"; }
    };

    // 自定义生命周期：把每次执行结果写入审计表（示例仅 println）
    private static final class AuditLogger implements SandboxLifecycleListener {
        @Override public void beforeExecute(String cmd, boolean isCommand) {
            System.out.println("[AUDIT-BEFORE] mode=" + (isCommand ? "CMD" : "CODE") + " body=" + cmd);
        }
        @Override public void afterExecute(String cmd, SandboxResult result) {
            System.out.println("[AUDIT-AFTER ] ok=" + result.success()
                + " exit=" + result.exitCode()
                + " len(out)=" + (result.output() == null ? 0 : result.output().length()));
        }
        @Override public void onDestroy() { System.out.println("[AUDIT-DESTROY]"); }
    }

    public static void main(String[] args) {
        // 复合策略：默认严格（11 条 + 6 条网络关键字） + 自定义黑名单
        CompositeCommandPolicy chain = new DefaultCommandPolicy(true)
            .add(extraBlacklistPolicy);

        SandboxConfig config = new SandboxConfig()
            .setType(SandboxType.LOCAL_DOCKER)
            .setWorkDirectory("/tmp/strict-work")
            .setResourceLimits(new ResourceLimits(1.0, 512, 30, 256, 128));

        SandboxContext ctx = new SandboxContext("agent-compliance", "sess-x", Map.of());

        SandboxRegistry registry = new SandboxRegistry();
        Sandbox sandbox = registry.create(config.getType(), config, ctx);

        // 构造器 3：sandbox + config + policy + listeners（ownedByManager=false，外部自己 destroy）
        try (var mgr = new SandboxManager(
                 sandbox, config, chain, List.of(new AuditLogger()));
             var _ = sandbox) {

            SandboxResult rSafe   = mgr.executeCommand("echo", List.of("hi"));
            SandboxResult rRisky  = mgr.executeCommand("cat", List.of("/etc/shadow"));  // 被 extraBlacklistPolicy 拦下

            System.out.println("safe exit="  + rSafe.exitCode());   // 0
            System.out.println("risky exit=" + rRisky.exitCode());  // 1
        }
    }
}
```

---

## 运行前检查清单

| 场景 | Local Process | Local Docker | Remote HTTP |
| --- | --- | --- | --- |
| 基础检查 | `bash --version` | `docker info` | `curl -k <endpoint>/healthz` |
| 磁盘 | workDirectory 必须为绝对路径且已创建，权限 rwx | 同上 + 空间 ≥ memoryMb + workdir 大小 | 远端服务管理磁盘 |
| 网络 | 无额外要求（策略关键字才管） | 默认 `--network=none`；需要联网请自定义 Factory | 本机 443/6443 等端口可达 |
| 镜像 | — | 要么本地存在镜像，要么 `auto-pull=true` 且可访问 Registry | — |

失败排查见各实现文档：[process-sandbox.md](process-sandbox.md) / [docker-sandbox.md](docker-sandbox.md) / [remote-sandbox.md](remote-sandbox.md)。
