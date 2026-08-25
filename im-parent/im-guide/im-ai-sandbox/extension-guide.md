← [返回索引](../README.md)

# 扩展指南

`im-ai-sandbox` 从设计之初就为扩展而生。本节覆盖 4 类扩展场景，按 "需求 → 3~4 步落地 → 注册方式 → 最小测试建议" 的流程组织。

> 写作风格对齐 `im-quant-factor/extension-guide.md` 与 `im-ai-memory` 系列。

---

## 扩展场景速查表

| 需求 | 扩展点 | 所属包 | 实现难度 | 注册方式 |
| --- | --- | --- | --- | --- |
| 接入 Podman / LXC / 自定义容器 runtime | **SandboxFactory + Sandbox**（或继承 DockerLocalSandbox 替换 executor） | im-ai-core.sandbox / im-ai-sandbox.local | ⭐⭐⭐ | Spring Bean 或 `META-INF/services` |
| 接入另一种远端执行服务（gRPC / NATS / OpenCodeSandbox SDK） | **RemoteSandboxClient 接口** + 自定义工厂 | im-ai-sandbox.remote | ⭐⭐ | Spring Bean 或 `META-INF/services` |
| 新增企业合规检查：AST 级检测、YARA 规则、LLM 二次判定 | **CommandPolicy** + Composite 组合 | im-ai-core.sandbox.policy | ⭐ | 注册为 Spring Bean 覆盖 `CommandPolicy` 或 手动注入 SandboxManager 构造器 3 |
| 用自定义 DockerCommandExecutor 替换 Podman、远端 Docker Host、Dind | **DockerCommandExecutor** | im-ai-sandbox.local | ⭐ | 自定义工厂 setExecutor 后装配 |

---

## 扩展 1：自定义 SandboxFactory（3 步）

**目标**：新增一种沙盒后端（例如 Podman），让 `SandboxType.CUSTOM` 或其他 type 能路由到它。

### Step 1：实现 Sandbox

```java
package com.acme.im.sandbox.podman;

import org.wall.im.ai.core.model.SandboxConfig;
import org.wall.im.ai.core.sandbox.Sandbox;
import org.wall.im.ai.core.sandbox.SandboxContext;
import org.wall.im.ai.core.sandbox.SandboxResult;
import java.util.List;

public class PodmanSandbox implements Sandbox {

    private String containerId;

    @Override
    public void initialize(SandboxConfig config, SandboxContext context) {
        // podman run ... sleep infinity
        containerId = runPodmanCli(config);
    }

    @Override
    public SandboxResult execute(String code, String language) {
        return execBashStdin(containerId, code);
    }

    @Override
    public SandboxResult executeCommand(String command, List<String> args) {
        return execBashC(containerId, command, args);
    }

    @Override
    public boolean isPathAllowed(String path) {
        return "/workspace".equals(path) || path.startsWith("/workspace/");
    }

    @Override
    public void destroy() {
        if (containerId != null) {
            podmanRmF(containerId);
            containerId = null;
        }
    }

    // ... 私有方法略
}
```

### Step 2：实现 SandboxFactory

```java
package com.acme.im.sandbox.podman;

import org.wall.im.ai.core.model.SandboxConfig;
import org.wall.im.ai.core.sandbox.Sandbox;
import org.wall.im.ai.core.sandbox.SandboxContext;
import org.wall.im.ai.core.sandbox.SandboxFactory;
import org.wall.im.ai.core.sandbox.SandboxType;

public class PodmanSandboxFactory implements SandboxFactory {
    @Override
    public SandboxType supportedType() {
        return SandboxType.CUSTOM; // 或 LOCAL_DOCKER（priority > 0 即可覆盖内置）
    }

    @Override
    public Sandbox create(SandboxConfig config, SandboxContext context) {
        return new PodmanSandbox();
    }

    @Override
    public int priority() {
        return 10; // 若与其他工厂撞 supportedType，取大者；内置 LOCAL_DOCKER 工厂 priority=0
    }
}
```

### Step 3：注册（二选一）

**方案 A（Spring Boot 应用，推荐）**

```java
@Configuration
public class PodmanSandboxConfig {
    @Bean public SandboxFactory podmanSandboxFactory() {
        return new PodmanSandboxFactory();
    }
}
```

Spring Boot 自动装配的 `SandboxRegistry(List<SandboxFactory>)` 构造器会自动收进来。

**方案 B（纯 Java / 非 Spring）**

在 jar 中新建 `META-INF/services/org.wall.im.ai.core.sandbox.SandboxFactory`：

```
com.acme.im.sandbox.podman.PodmanSandboxFactory
```

> 如果你想覆盖**内置** LOCAL_DOCKER 的实现而不改变 type，只需让 `PodmanSandboxFactory.supportedType() = LOCAL_DOCKER` 且 `priority() > 0` 即可，Registry 会取 priority 最大者。

---

## 扩展 2：自定义 CommandPolicy 链（3 步）

**目标**：在默认 11 关键字 + 6 网络关键字之外，再叠一层 AST 级 Python import 扫描、敏感路径检查。

### Step 1：实现 CommandPolicy

```java
package com.acme.im.sandbox.policy;

import com.github.javaparser.StaticJavaParser;
import org.wall.im.ai.core.model.SandboxConfig;
import org.wall.im.ai.core.sandbox.policy.CommandPolicy;
import java.util.Set;

public class NoSystemExitPolicy implements CommandPolicy {
    private static final Set<String> FORBIDDEN_METHODS = Set.of(
        "System.exit", "Runtime.getRuntime().halt", "Runtime.halt");

    @Override
    public boolean isAllowed(String command, SandboxConfig config) {
        // 仅演示：直接按字符串 contains；生产建议 AST 解析
        return FORBIDDEN_METHODS.stream().noneMatch(command::contains);
    }

    @Override public String getPolicyName() { return "no-system-exit"; }
}
```

### Step 2：链组合

```java
import org.wall.im.ai.core.sandbox.policy.CompositeCommandPolicy;
import org.wall.im.ai.sandbox.policy.DefaultCommandPolicy;

CompositeCommandPolicy chain = new DefaultCommandPolicy(true)
    .add(new NoSystemExitPolicy())
    .add(new YaraSignaturePolicy(...))
    .add(new LlmJudgePolicy(...));
```

`CompositeCommandPolicy` 语义：**全部返回 true 才放行**。

### Step 3：接入（二选一）

| 接入方式 | 适合 | 代码 |
| --- | --- | --- |
| 构造器 3 直接注入 | 单任务 | `new SandboxManager(sb, cfg, chain, List.of(...))` |
| Spring 覆盖 Bean | 全局生效 | `@Bean public CommandPolicy commandPolicy() { return chain; }`（`@ConditionalOnMissingBean` 会让默认策略失效） |

---

## 扩展 3：替换 DockerCommandExecutor（2 步）

**目标**：把 docker CLI 调用替换为 `podman` 或 `ssh user@docker-host docker`（远端 Docker Host），而不重写 DockerLocalSandbox 的主流程。

### Step 1：实现 DockerCommandExecutor

```java
package com.acme.im.sandbox.podman;

import org.wall.im.ai.sandbox.local.DockerCommandExecutor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PodmanCommandExecutor implements DockerCommandExecutor {

    @Override
    public ProcessResult exec(List<String> args) throws IOException, InterruptedException {
        List<String> full = new ArrayList<>();
        full.add("podman");   // 替换 "docker"
        full.addAll(args);
        return runProcess(full, null);
    }

    @Override
    public ProcessResult execWithStdin(List<String> args, String stdin) throws IOException, InterruptedException {
        List<String> full = new ArrayList<>();
        full.add("podman");
        full.addAll(args);
        return runProcess(full, stdin);
    }

    private ProcessResult runProcess(List<String> cmd, String stdin) { /* 参考 ProcessDockerCommandExecutor */ throw new Error(); }
}
```

### Step 2：在自定义 Factory 里注入

```java
public class PodmanBasedFactory implements SandboxFactory {
    @Override public SandboxType supportedType() { return SandboxType.LOCAL_DOCKER; }
    @Override public int priority() { return 20; }  // 覆盖内置 factory

    @Override
    public Sandbox create(SandboxConfig config, SandboxContext context) {
        DockerLocalSandbox s = new DockerLocalSandbox();
        s.setDockerCommandExecutor(new PodmanCommandExecutor()); // ← 替换 CLI
        return s;
    }
}
```

> 同样按扩展 1 的 Step 3 注册此 Factory（Spring Bean 或 ServiceLoader）。

---

## 扩展 4：自定义 RemoteSandboxClient（gRPC 为例，4 步）

**目标**：用 gRPC 代替内置的 HTTP 协议，调用自家远端执行服务。

### Step 1：实现 RemoteSandboxClient

```java
package com.acme.im.sandbox.grpc;

import io.grpc.ManagedChannel;
import org.wall.im.ai.core.model.SandboxConfig;
import org.wall.im.ai.core.sandbox.SandboxContext;
import org.wall.im.ai.core.sandbox.SandboxResult;
import org.wall.im.ai.sandbox.remote.RemoteSandboxClient;
import java.util.List;

public class GrpcRemoteSandboxClient implements RemoteSandboxClient {

    private final SandboxRuntimeStub stub;

    public GrpcRemoteSandboxClient(ManagedChannel channel) {
        this.stub = SandboxRuntimeGrpc.newStub(channel);
    }

    @Override
    public String initialize(SandboxConfig config, SandboxContext context) {
        var req = CreateRequest.newBuilder()
            .setWorkDir(config.getWorkDirectory())
            .setAgentId(nullToEmpty(context.agentId()))
            .setSessionId(nullToEmpty(context.sessionId()))
            .build();
        return stub.create(req).getSandboxId();
    }

    @Override
    public SandboxResult executeCode(String sandboxId, String code, String language, SandboxContext context) {
        ExecResponse r = stub.execCode(ExecCodeRequest.newBuilder()
            .setSandboxId(sandboxId).setCode(code).setLanguage(language).build());
        return r.getSuccess() ? SandboxResult.ok(r.getOutput())
                              : SandboxResult.failure(r.getOutput(), r.getExitCode());
    }

    @Override
    public SandboxResult executeCommand(String sandboxId, String cmd, List<String> args, SandboxContext context) {
        // ...
        throw new Error();
    }

    @Override
    public boolean isPathAllowed(String sandboxId, String path, SandboxContext context) { /* ... */ throw new Error(); }

    @Override
    public void destroy(String sandboxId) { stub.destroy(DestroyRequest.newBuilder().setSandboxId(sandboxId).build()); }
}
```

### Step 2：自定义 SandboxFactory

```java
public class GrpcSandboxFactory implements SandboxFactory {
    private final ManagedChannel channel;
    public GrpcSandboxFactory(ManagedChannel ch) { this.channel = ch; }

    @Override public SandboxType supportedType() { return SandboxType.REMOTE_HTTP; /* 或自定义 CUSTOM */ }
    @Override public int priority() { return 50; }   // 覆盖 HttpRemote 默认工厂

    @Override
    public Sandbox create(SandboxConfig config, SandboxContext context) {
        return new RemoteSandbox(new GrpcRemoteSandboxClient(channel));
    }
}
```

### Step 3：注入 SandboxConfig.remoteEndpoint 的兼容

由于 gRPC 不走 HTTP baseEndpoint，`config.remoteEndpoint` 可以留空，gRPC 通道由 Factory 构造时传入即可。也可以定义一个 custom prefix YAML：

```yaml
acme:
  sandbox:
    grpc-endpoint: sandbox-grpc.internal:50051
```

### Step 4：注册并测试

同扩展 1 Step 3。测试时优先验证 `initialize → execute → destroy` 的完整闭环 + 超时断连重试。

---

## 编写规范（必看）

1. **destroy 幂等**：Sandbox 实现重复调用 destroy 绝不能抛异常（参考 DockerLocalSandbox 中 `if (containerId==null) return` 模式）。
2. **initialize 幂等**：重复调用应直接 return，避免重复起容器或重复 create 远端。
3. **不吞关键异常**：initialize/create/createClient 阶段失败必须抛（非 0/空字符串覆盖）。execute 阶段可把异常转为 `SandboxResult.failure(msg)`，但要把 exception message 放进 output 以便排障。
4. **资源关闭友好**：`Sandbox` 已 `extends AutoCloseable`，实现类如果持有 Process/Stream/Channel，务必在 destroy 里显式关闭并在 close 有默认语义。
5. **不要强依赖 Spring**：`SandboxFactory`、`CommandPolicy` 可在纯 Java 环境使用，不要在实现中 import `org.springframework.*`；如果必须用 Spring 类，请放在专属 `*-spring` 子 jar 中。
6. **priority > 0 时写注释说明"覆盖目的"**：例如 `priority=10  // 覆盖内置 HTTP，改用 gRPC`。
7. **Sandbox 不跨线程共享**：Sandbox 是有状态对象（远端 ID、容器 ID）。文档使用者请避免把同一 SandboxManager 暴露给多个并发任务。
8. **日志使用 SLF4J**：不要 System.out 直接打印。

---

## 最小测试模板

```java
class MySandboxFactoryTest {

    @Test
    void factory_creates_and_runs_end_to_end() {
        SandboxFactory factory = new MySandboxFactory();
        SandboxConfig cfg = new SandboxConfig()
            .setWorkDirectory("/tmp/test")
            .setEnabled(true);
        SandboxContext ctx = SandboxContext.EMPTY;

        Sandbox s = factory.create(cfg, ctx);
        s.initialize(cfg, ctx);
        SandboxResult r = s.executeCommand("echo", List.of("ok"));
        assertTrue(r.success(), r.output());
        assertEquals("ok\n", r.output());

        // 幂等 destroy × 2
        s.destroy();
        s.destroy();
    }
}
```

更多扩展参考：
- 工厂注册示例（内置 3 个）：`LocalProcessSandboxFactory`、`DockerLocalSandboxFactory`、`RemoteSandboxFactory`
- 策略链示例：`DefaultCommandPolicy`
- 接口源码见 [sandbox-spi.md](sandbox-spi.md)
- 安全提示见 [security-notes.md](security-notes.md)
