# im-ai-sandbox 本地+远端双沙盒架构实现计划

## Context（背景与目标）

`im-ai-sandbox` 当前仅有一种本地进程级沙盒 `ProcessSandbox`（基于 `ProcessBuilder + bash`，弱隔离），且 `SandboxManager` 把危险命令黑名单**硬编码**在内部（`rm -rf /`、`mkfs`、`wget` 等 11 条），无法扩展。主人需要：

1. **本地沙盒**：在现有进程级沙盒之外，新增基于 Docker 容器的强隔离本地沙盒。
2. **远端沙盒**：新增基于 REST/HTTP 的远端执行沙盒，对接外部执行服务。
3. **命令限制可扩展**：把硬编码黑名单抽出为 `CommandPolicy` 接口，支持链式组合与第三方自定义。
4. **扩展接口**：提供 `SandboxFactory` SPI + `SandboxRegistry` 注册表，让第三方可以注册自定义沙盒类型（如 gVisor、Firecracker、K8s Pod）。

已确认决策：
- ProcessSandbox **迁移**至 `org.wall.im.ai.sandbox.local` 子包。
- 远端沙盒本次**仅实现客户端 + REST 协议约定**，不实现服务端。
- im-guide 文档本次不更新，后续按需补充。

## 设计原则

- **零新增运行时第三方依赖**：仅用 JDK 26（`java.net.http.HttpClient`、record）+ Spring Boot 3.5.15 + slf4j + junit5 + mockito；JSON 用 jackson-databind（经 im-ai-core 传递获得）。
- **向后兼容三条红线**：
  1. `Sandbox` SPI 五方法签名不变。
  2. `SandboxConfig` 旧字段与无参构造器保留。
  3. `SandboxManager(Sandbox, SandboxConfig)` 旧构造器签名不变，旧行为等价（`DefaultCommandPolicy` 命中黑名单时失败信息保留 `"Dangerous operations detected and blocked"` / `"Dangerous command blocked"` 字样，旧测试 `verifyNoInteractions(sandbox)` 仍成立）。

## 目录结构

### im-ai-core 新增（包 `org.wall.im.ai.core.sandbox` 及子包）

```
im-ai-core/src/main/java/org/wall/im/ai/core/sandbox/
├── Sandbox.java                      # 已有，不改签名
├── SandboxResult.java                # 已有，不改签名
├── SandboxType.java                  # 新增 枚举 LOCAL_PROCESS/LOCAL_DOCKER/REMOTE_HTTP/CUSTOM
├── ResourceLimits.java               # 新增 值对象（cpuCores/memoryMb/maxExecutionTimeSec/diskMb/maxProcesses）
├── SandboxContext.java               # 新增 值对象（agentId/sessionId/metadata，远端租户隔离）
├── SandboxLifecycleListener.java     # 新增 全 default 方法接口（onInitialize/onPreExecute/onPostExecute/onDestroy）
├── SandboxFactory.java               # 新增 SPI（create/supportedType/priority）
└── policy/
    ├── CommandPolicy.java            # 新增 接口 isAllowed/getPolicyName
    ├── CompositeCommandPolicy.java   # 新增 链式组合，any deny => deny
    ├── KeywordBlacklistPolicy.java   # 新增 关键字黑名单
    └── RegexWhitelistPolicy.java     # 新增 正则白名单

im-ai-core/src/main/java/org/wall/im/ai/core/model/
└── SandboxConfig.java                # 扩展字段（见下）
```

### im-ai-sandbox 新增/迁移

```
im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox/
├── SandboxManager.java               # 升级（保留根包，统一入口）
├── SandboxRegistry.java              # 新增 工厂注册表 + 路由
├── local/
│   ├── ProcessSandbox.java           # 迁移自根包（原 org.wall.im.ai.sandbox.ProcessSandbox 删除）
│   ├── LocalProcessSandboxFactory.java
│   ├── DockerCommandExecutor.java   # 新增 接口（便于 mock）
│   ├── ProcessDockerCommandExecutor.java
│   ├── DockerLocalSandbox.java
│   └── DockerLocalSandboxFactory.java
├── remote/
│   ├── RemoteSandbox.java            # 适配 Sandbox SPI，委托 client
│   ├── RemoteSandboxClient.java     # 新增 接口
│   ├── HttpRemoteSandboxClient.java  # JDK HttpClient 实现
│   ├── RemoteExecuteRequest.java     # DTO
│   ├── RemoteExecuteResponse.java    # DTO
│   └── RemoteSandboxFactory.java
├── policy/
│   └── DefaultCommandPolicy.java     # 继承 CompositeCommandPolicy，迁移旧黑名单
└── config/
    ├── SandboxProperties.java         # @ConfigurationProperties(prefix="im.ai.sandbox")
    └── SandboxAutoConfiguration.java  # @AutoConfiguration

im-ai-sandbox/src/main/resources/META-INF/
├── spring/
│   └── org.springframework.boot.autoconfigure.AutoConfiguration.imports   # 新增
└── services/
    └── org.wall.im.ai.core.sandbox.SandboxFactory                         # 新增 ServiceLoader 兜底

im-ai-sandbox/src/test/java/org/wall/im/ai/sandbox/
├── SandboxManagerTest.java           # 升级：保留旧 3 @Nested + 新增 PolicyInjection/RegistryRoute/Lifecycle @Nested
├── policy/DefaultCommandPolicyTest.java
├── local/DockerLocalSandboxTest.java
├── remote/HttpRemoteSandboxClientTest.java
└── SandboxRegistryTest.java
```

## 关键接口/类签名

### im-ai-core 新增

```java
public enum SandboxType { LOCAL_PROCESS, LOCAL_DOCKER, REMOTE_HTTP, CUSTOM }

public class ResourceLimits {
    private int cpuCores = 1;
    private long memoryMb = 512;
    private int maxExecutionTimeSec = 300;
    private long diskMb = 1024;
    private int maxProcesses = 64;
    // getter/setter + 静态工厂 from(SandboxConfig) 合并旧 maxExecutionTime/maxMemoryMb
}

public class SandboxContext {
    private String agentId;
    private String sessionId;
    private Map<String, String> metadata;  // 透传远端 X-Meta-*
    // 静态工厂 of(agentId, sessionId)
}

public interface SandboxLifecycleListener {
    default void onInitialize(Sandbox sandbox, SandboxContext ctx) {}
    default void onPreExecute(Sandbox sandbox, String code, SandboxContext ctx) {}
    default void onPostExecute(Sandbox sandbox, SandboxResult result, SandboxContext ctx) {}
    default void onDestroy(Sandbox sandbox, SandboxContext ctx) {}
}

public interface SandboxFactory {
    Sandbox create(SandboxConfig config);
    SandboxType supportedType();
    default int priority() { return 0; }  // 同 type 取最大者
}

public interface CommandPolicy {
    boolean isAllowed(String command);
    String getPolicyName();
}

// CompositeCommandPolicy: 链式 add()，any deny => deny
// KeywordBlacklistPolicy(String... keywords): 命中即 false，大小写不敏感
// RegexWhitelistPolicy(String... regexes): 不匹配任一即 false
```

### SandboxConfig 扩展（向后兼容）

在 [SandboxConfig.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/model/SandboxConfig.java) 现有字段基础上新增：

```java
private SandboxType type = SandboxType.LOCAL_PROCESS;  // 默认保证旧行为
private ResourceLimits resourceLimits;
private CommandPolicy commandPolicy;                    // null => SandboxManager 用 DefaultCommandPolicy
private String image;                                  // 容器/远端镜像
private String remoteEndpoint;                         // 远端基地址
private Map<String, String> envVars;
private SandboxContext context;
// 旧无参构造器与旧 getter/setter 全部保留
```

### im-ai-sandbox 关键类

```java
// SandboxManager 升级（核心改造点）
public class SandboxManager {
    // 旧构造器保留，行为等价
    public SandboxManager(Sandbox sandbox, SandboxConfig config) {}
    // 注入策略
    public SandboxManager(Sandbox sandbox, SandboxConfig config, CommandPolicy commandPolicy) {}
    // 注入策略 + 生命周期监听
    public SandboxManager(Sandbox sandbox, SandboxConfig config, CommandPolicy commandPolicy,
                          List<SandboxLifecycleListener> listeners) {}
    // Registry 路由（Agent 框架推荐路径）
    public SandboxManager(SandboxConfig config, SandboxRegistry registry) {}
    public SandboxManager(SandboxConfig config, SandboxRegistry registry, CommandPolicy policy) {}

    public SandboxResult safeExecute(String code) {}
    public SandboxResult safeExecuteCommand(String command) {}
    public boolean canAccess(String path) {}
    public void destroy() {}  // 新增：销毁 sandbox + 触发 onDestroy
}
```

```java
// DefaultCommandPolicy：组合 + 迁移硬编码黑名单
public class DefaultCommandPolicy extends CompositeCommandPolicy {
    public DefaultCommandPolicy() { this(true); }
    public DefaultCommandPolicy(boolean networkRestricted) {}
    // 内部 KeywordBlacklistPolicy(旧 11 条) + RegexWhitelistPolicy(常见安全命令) + 路径/网络检查
    @Override public String getPolicyName() { return "default"; }
    // 命中黑名单返回 failure 文案沿用旧字符串，保留 "Dangerous" 字样
}
```

```java
// SandboxRegistry
public class SandboxRegistry {
    public SandboxRegistry() {}                                   // ServiceLoader 兜底
    public SandboxRegistry(List<SandboxFactory> springFactories) {} // Spring 注入优先 + ServiceLoader 补充
    public void register(SandboxFactory factory) {}
    public Optional<SandboxFactory> getFactory(SandboxType type) {} // 同 type 取 priority 最大
    public Sandbox create(SandboxConfig config) {}                // 找不到抛 IllegalStateException
    public Collection<SandboxFactory> getAll() {}
}
```

```java
// DockerCommandExecutor（抽象便于 mock）
public interface DockerCommandExecutor {
    DockerExecResult run(List<String> command, int timeoutSec);
    record DockerExecResult(int exitCode, String stdout, String stderr) {}
}

// DockerLocalSandbox
public class DockerLocalSandbox implements Sandbox {
    public DockerLocalSandbox(SandboxConfig config, DockerCommandExecutor executor) {}
    public DockerLocalSandbox(SandboxConfig config) { this(config, new ProcessDockerCommandExecutor()); }
    // initialize: docker run -d --name ... --cpus --memory --pids-limit --network --tmpfs -v -w {image} sleep infinity
    // executeCommand: docker exec -w {workDir} {name} bash -c {command}
    // execute: docker exec -i -w {workDir} {name} bash -s  (stdin 流式写 code)
    // isPathAllowed: 容器内只判断 workDir + /tmp 前缀
    // destroy: docker rm -f {name}（失败仅 warn）
    // 容器名: sandbox-{agentId}-{sessionId}-{ts}，全小写
}

// RemoteSandboxClient
public interface RemoteSandboxClient {
    String initialize(SandboxConfig config);                       // 返回 sandboxId
    SandboxResult execute(String sandboxId, String code, String workDir);
    SandboxResult executeCommand(String sandboxId, String command);
    boolean isPathAllowed(String sandboxId, String path);
    void destroy(String sandboxId);
}

// HttpRemoteSandboxClient：JDK HttpClient + jackson ObjectMapper
// RemoteSandbox: 实现 Sandbox，initialize 后持有 sandboxId，委托 client
// RemoteSandboxFactory: 校验 remoteEndpoint 非空，构造 HttpRemoteSandboxClient + RemoteSandbox
```

## SandboxManager 改造兼容策略

1. **策略选择优先级**：构造器显式传入 > `config.getCommandPolicy()` > `new DefaultCommandPolicy()`。
2. **旧构造器等价**：`SandboxManager(sandbox, config)` 内部等价 `this(sandbox, config, config.getCommandPolicy() != null ? ... : new DefaultCommandPolicy())`。因 `SandboxConfig.commandPolicy` 默认 null，自动落到 `DefaultCommandPolicy`（黑名单与旧硬编码 1:1 迁移），旧测试全绿。
3. **黑名单原样迁移**：旧 `List.of("rm -rf /","rm -rf ~","mkfs","dd if=",":(){:|:&};:","chmod -R 777 /","wget","curl -o","nc -l","> /dev/sda","format c:")` + `lowerCode.contains` 大小写不敏感逻辑原样迁入 `KeywordBlacklistPolicy`。
4. **失败文案保留**：`DefaultCommandPolicy` 拒绝时返回的 `SandboxResult.failure` 文案沿用旧字符串 `"Dangerous operations detected and blocked"` / `"Dangerous command blocked"`。
5. **生命周期钩子接入**：`safeExecute` 内 `onPreExecute` → 策略检查 → `sandbox.execute` → `onPostExecute`；listeners 为空列表时零开销；旧构造器 listeners 默认 `List.of()`，行为零变化。

## Docker CLI 命令模板

参数按 `List<String>` 逐元素传给 ProcessBuilder，避免 shell 注入。

```text
# 初始化（create + start）
docker run -d --name {name} --cpus={cpu} --memory={mem}m --pids-limit={pids}
  --network={networkAccess ? "bridge" : "none"}
  --tmpfs /tmp:size={disk}m -v {hostWorkDir}:{containerWorkDir} -w {containerWorkDir}
  {image} sleep infinity

# 镜像预检
docker image inspect {image}    # exitCode=0 才继续
# 探活
docker version
# 就绪轮询
docker inspect -f '{{.State.Running}}' {name}

# 执行命令
docker exec -w {workDir} {name} bash -c {command}
# 执行代码（stdin 流式）
docker exec -i -w {workDir} {name} bash -s   # process.getOutputStream().write(code.getBytes())
# 销毁
docker rm -f {name}
```

跨平台：所有 bash 在 Linux 容器内执行，宿主 Windows + Docker Desktop 也可用（对 ProcessSandbox 依赖宿主 bash 的改进）。`docker` CLI 必须在宿主 PATH 中。

## 远端 REST 协议约定

基地址 = `SandboxConfig.remoteEndpoint`，`Content-Type: application/json`，请求头 `X-Agent-Id`、`X-Session-Id`（取自 `SandboxContext`，无则省略）。

| 方法 | 路径 | 请求 Body | 响应 |
|---|---|---|---|
| POST | `/sandboxes` | workDir/allowedPaths/networkAccess/image/resourceLimits/envVars | 201 `{sandboxId, status}` |
| POST | `/sandboxes/{id}/execute` | code/workDir/envVars/timeoutSec | 200 RemoteExecuteResponse |
| POST | `/sandboxes/{id}/command` | command/envVars/timeoutSec | 200 RemoteExecuteResponse |
| GET | `/sandboxes/{id}/paths?path=...` | — | 200 `{allowed:bool}` |
| DELETE | `/sandboxes/{id}` | — | 204 |

错误：非 2xx 返回 `{"error":"...","code":"SANDBOX_NOT_FOUND|TIMEOUT|POLICY_VIOLATION|INTERNAL"}`，`HttpRemoteSandboxClient` 映射为 `SandboxResult.failure`，**不抛异常**（与本地沙盒行为一致，避免 Agent 工具调用炸裂）。连接异常/超时同样返回 failure。

`RemoteExecuteRequest/Response` 是无注解 POJO，用 `com.fasterxml.jackson.databind.ObjectMapper`（经 im-ai-core 传递依赖获得）序列化，字段名 camelCase。

## Spring Boot 自动装配

### pom 依赖变更（im-ai-sandbox/pom.xml，均 optional）

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-autoconfigure</artifactId>
    <optional>true</optional>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-configuration-processor</artifactId>
    <optional>true</optional>
</dependency>
```

`<optional>true</optional>` 确保下游模块不被迫传递引入，纯 Java 使用方不受影响。项目根 pom 已 import `spring-boot-dependencies BOM`，不算新增第三方依赖。

### SandboxProperties

```java
@ConfigurationProperties(prefix = "im.ai.sandbox")
public class SandboxProperties {
    private boolean enabled = true;
    private SandboxType defaultType = SandboxType.LOCAL_PROCESS;
    private DockerProperties docker = new DockerProperties();  // image/openjdk:26-slim, autoPull=false
    private RemoteProperties remote = new RemoteProperties(); // endpoint, connectTimeoutSec=10
}
```

### SandboxAutoConfiguration

```java
@AutoConfiguration
@ConditionalOnProperty(prefix = "im.ai.sandbox", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SandboxAutoConfiguration {
    @Bean @ConditionalOnMissingBean
    public SandboxRegistry sandboxRegistry(List<SandboxFactory> factories) { return new SandboxRegistry(factories); }
    @Bean @ConditionalOnMissingBean
    public CommandPolicy defaultCommandPolicy() { return new DefaultCommandPolicy(); }
    @Bean @ConditionalOnMissingBean(SandboxManager.class)
    public SandboxManager sandboxManager(SandboxRegistry registry, CommandPolicy policy,
                                         ObjectProvider<List<SandboxLifecycleListener>> listeners) { ... }
}
```

### 注册文件

- `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`：
  ```
  org.wall.im.ai.sandbox.config.SandboxAutoConfiguration
  ```
- `META-INF/services/org.wall.im.ai.core.sandbox.SandboxFactory`（ServiceLoader 兜底）：
  ```
  org.wall.im.ai.sandbox.local.LocalProcessSandboxFactory
  org.wall.im.ai.sandbox.local.DockerLocalSandboxFactory
  org.wall.im.ai.sandbox.remote.RemoteSandboxFactory
  ```

`SandboxRegistry` 构造：先收集 Spring 注入 factories，再用 `ServiceLoader.load(SandboxFactory.class)` 补充去重。

### 配置示例

```yaml
im:
  ai:
    sandbox:
      enabled: true
      default-type: local_process   # local_process | local_docker | remote_http
      docker:
        image: openjdk:26-slim
        auto-pull: false
      remote:
        endpoint: https://sb.example.com
        connect-timeout-sec: 10
```

## 关键扩展点（给第三方扩展用）

| 扩展点 | 位置 | 扩展方式 | 用途 |
|---|---|---|---|
| `SandboxFactory` SPI | im-ai-core | Spring @Bean / 非 Spring 写 services 文件 | 自定义沙盒后端（gVisor/Firecracker/K8s Pod） |
| `CommandPolicy` | im-ai-core policy 子包 | 实现接口或继承 CompositeCommandPolicy | 自定义命令过滤（AST 解析、频率限制） |
| `RemoteSandboxClient` | im-ai-sandbox remote | 实现接口替换 HttpRemoteSandboxClient | 换协议（gRPC/WebSocket）或加鉴权/重试中间件 |
| `DockerCommandExecutor` | im-ai-sandbox local | 实现接口 | 替换 docker 调用后端（Podman/远端 docker host） |
| `SandboxLifecycleListener` | im-ai-core | 实现 default 方法接口 | 审计日志/指标采集/资源预热/trace 上报 |
| `SandboxType.CUSTOM` | 枚举 | 配合自定义 SandboxFactory | 兜底类型，避免枚举膨胀 |

扩展示例（第三方新增 K8s 沙盒）：实现 `KubeSandboxFactory implements SandboxFactory`，`supportedType()` 返回 `CUSTOM`，Spring 环境注册为 Bean，`SandboxAutoConfiguration` 自动收集，`SandboxRegistry.create` 按 type 路由。

## 测试方案

| 测试类 | 目的 | 关键用例 |
|---|---|---|
| `DefaultCommandPolicyTest` | 策略链黑/白名单+正则 | `rm -rf /` 被拒且含 "Dangerous"；`wget http://x` 被拒；`echo hello` 通过；正则白名单 `git status` 通过 `git push` 被拒；`add()` 链式生效 |
| `DockerLocalSandboxTest` | Docker 命令编排与资源限制参数 | mock `DockerCommandExecutor`：initialize 调 `docker run -d --cpus --memory --pids-limit --network none`；executeCommand 调 `docker exec ... bash -c`；超时返回 failure；destroy 调 `docker rm -f`；image 不存在返回 failure 不抛；`networkAccess=true` 时 network=bridge |
| `HttpRemoteSandboxClientTest` | REST 调用与 JSON 编解码 | mock `HttpClient`（Mockito 5 可 mock）：POST /sandboxes 解析 sandboxId；execute 组装 RemoteExecuteRequest JSON 正确；5xx/网络异常映射为 failure 不抛；X-Agent-Id/X-Session-Id 头携带 |
| `SandboxRegistryTest` | 注册/路由/优先级 | register 后 getFactory 命中；同 type 两 factory priority 高的胜出；create 按 config.type 路由；无匹配抛 IllegalStateException；非 Spring 构造时 ServiceLoader 加载内置 3 factory |
| `SandboxManagerTest`（升级） | 兼容回归 + 新路径 | **保留旧 3 @Nested 全部用例不变（用旧构造器）**；新增 `PolicyInjectionTest`（mock CommandPolicy 返回 false → `verifyNoInteractions(sandbox)`）；`RegistryRouteTest`（mock Registry.create）；`LifecycleListenerTest`（mock listener 验证 onPreExecute/onPostExecute） |

测试约束：全部纯单元测试，不依赖真实 docker/bash/远端服务，可跨平台（含 Windows）运行。

## 实施顺序

1. **im-ai-core 抽象层**：`SandboxType` → `ResourceLimits` → `SandboxContext` → `SandboxLifecycleListener` → `CommandPolicy`+三个策略实现 → `SandboxFactory` → 扩展 `SandboxConfig`（仅加字段）。
2. **im-ai-sandbox policy 层**：`DefaultCommandPolicy`（迁移黑名单）。
3. **迁移 + 升级**：`ProcessSandbox` 迁至 local 子包（原文件删除）→ `SandboxManager` 升级（旧构造器兼容、注入策略、Registry 路由、listener）→ 升级 `SandboxManagerTest` 旧用例调整 import。
4. **本地 Docker**：`DockerCommandExecutor` 接口 → `ProcessDockerCommandExecutor` → `DockerLocalSandbox` → `DockerLocalSandboxFactory` → `LocalProcessSandboxFactory`。
5. **远端**：DTO → `RemoteSandboxClient` 接口 → `HttpRemoteSandboxClient` → `RemoteSandbox` → `RemoteSandboxFactory`。
6. **Registry + 自动装配**：`SandboxRegistry` → `SandboxProperties` → `SandboxAutoConfiguration` → imports/services 注册文件 → pom 依赖变更。
7. **测试**：按上表逐个补齐，每完成一层立即跑对应测试。

## 验证方法

### 编译
```powershell
mvn -pl im-ai-core,im-ai-sandbox -am compile -f c:\Users\钟世超\IdeaProjects\InvestmentMarket\im-parent\im-ai\pom.xml -q
```

### 单元测试
```powershell
mvn -pl im-ai-core,im-ai-sandbox -am test -f c:\Users\钟世超\IdeaProjects\InvestmentMarket\im-parent\im-ai\pom.xml
```

### 仅跑沙盒相关测试
```powershell
mvn -pl im-ai-sandbox test -f c:\Users\钟世超\IdeaProjects\InvestmentMarket\im-parent\im-ai\pom.xml -Dtest="SandboxManagerTest,DefaultCommandPolicyTest,DockerLocalSandboxTest,HttpRemoteSandboxClientTest,SandboxRegistryTest"
```

### 全量回归
```powershell
mvn -f c:\Users\钟世超\IdeaProjects\InvestmentMarket\im-parent\im-ai\pom.xml clean test
```

### 兼容性专项检查
- `SandboxManagerTest` 旧 `@Nested`（DangerousOperationTest/SandboxDisabledTest/PathAccessTest）必须 0 失败。
- `im-ai-core` 现有测试（AgentContextTest/AgentResultTest/MessageTest/MemoryEntryTest）不受影响。

## 关键文件清单（待修改/新增）

修改：
- [SandboxConfig.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/model/SandboxConfig.java) — 扩展字段
- [SandboxManager.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox/SandboxManager.java) — 升级
- [ProcessSandbox.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox/ProcessSandbox.java) — 迁移至 local 子包后删除原文件
- [SandboxManagerTest.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/test/java/org/wall/im/ai/sandbox/SandboxManagerTest.java) — 升级
- [im-ai-sandbox/pom.xml](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/pom.xml) — 新增 optional 依赖

不变（契约基线）：
- [Sandbox.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/sandbox/Sandbox.java)
- [SandboxResult.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/sandbox/SandboxResult.java)

新增：im-ai-core 8 个类（SandboxType/ResourceLimits/SandboxContext/SandboxLifecycleListener/SandboxFactory + policy 子包 4 个）+ im-ai-sandbox 约 17 个类/接口/DTO + 2 个 META-INF 注册文件 + 4 个新测试类。

## 风险与对策

| 风险 | 对策 |
|---|---|
| Windows 无 bash 导致旧 ProcessSandbox 真实执行测试无法跑 | 旧 ProcessSandbox 测试只 mock 或跑 `safeExecute` 路径，不在 CI 强制真实执行；DockerLocalSandbox 跨平台 |
| `SandboxConfig.commandPolicy` 字段类型为接口，Spring @ConfigurationProperties 无法直接绑定 | SandboxProperties 不绑定 policy，仅绑字符串配置；`SandboxAutoConfiguration` 用 Bean 注入；编程式用户可直接 `config.setCommandPolicy(...)` |
| ServiceLoader 在模块化/打包后失效 | 内置 3 factory 同时由 `@Bean` 注册（Spring 环境）+ services 文件（非 Spring）双保险 |
| 远端沙盒网络抖动 | `HttpRemoteSandboxClient` 失败一律返回 `SandboxResult.failure` 不抛；重试留作 `RemoteSandboxClient` 包装扩展点 |
| Docker 容器残留 | `destroy` 用 `docker rm -f`；`initialize` 前可选清理同名旧容器；超时容器由 `--pids-limit` + 进程超时双保险 |
