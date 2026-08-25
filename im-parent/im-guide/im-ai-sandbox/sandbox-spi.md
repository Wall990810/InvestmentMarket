← [返回索引](../README.md)

# Sandbox SPI 全集

本文档系统梳理 im-ai-sandbox 模块的所有对外协议，包括来自 `im-ai-core` 的核心契约（Sandbox / SandboxResult / SandboxConfig / SandboxType / ResourceLimits / SandboxContext / SandboxFactory / SandboxLifecycleListener / CommandPolicy），以及本模块中编排与策略层的类型用法。

> 阅读建议：先读 [概述](overview.md) 把握层次，再按需阅读本页某几个接口。

---

## 1. 沙盒核心接口 `Sandbox`

源码：[Sandbox.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/sandbox/Sandbox.java)

`Sandbox` 是所有沙盒后端必须实现的统一五阶段接口：

```java
public interface Sandbox extends AutoCloseable {
    void initialize(SandboxConfig config, SandboxContext context);
    SandboxResult execute(String code, String language);
    SandboxResult executeCommand(String command, List<String> args);
    boolean isPathAllowed(String path);
    void destroy();
    @Override default void close() { destroy(); }
}
```

| 方法 | 调用时机 | 说明 |
| --- | --- | --- |
| `initialize` | 在任何 `execute*` 之前调用一次 | 完成工作目录、环境变量、容器启动、远端注册等准备；必须支持幂等重复调用 |
| `execute(code, language)` | 执行一段**代码** | `language` 用于区分执行器（目前 Process/Docker 沙盒仅要求 `"bash"`，远端沙盒可扩展多语言） |
| `executeCommand(command, args)` | 执行一条带数组参数的**命令** | 命令字符串由 `CommandPolicy` 先做安全预检，失败会被 SandboxManager 短路径返回 `SandboxResult.failure(...)` |
| `isPathAllowed` | 读/写文件路径校验 | 执行 `cat /etc/shadow` 前，SandboxManager 会显式调用本方法并与黑名单关键字结合判断 |
| `destroy` | 使用结束后销毁 | 对于 Docker：`docker rm -f`；对于 Remote：HTTP DELETE；对于 Process：空实现；实现必须支持重复调用不抛错 |
| `close()` | try-with-resources 默认 | 默认委托 `destroy()` |

---

## 2. 执行结果 `SandboxResult`

源码：[SandboxResult.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/sandbox/SandboxResult.java)

统一表示成功/失败与附加信息，三个字段均不可变：

| 字段 | 类型 | 成功语义 | 失败语义 |
| --- | --- | --- | --- |
| `success` | `boolean` | `true` | `false` |
| `output` | `String`（可为空） | stdout+stderr 合并 | 错误原因或空 |
| `exitCode` | `int` | 0 | 非 0（危险操作默认返回 1，远端 4xx/5xx 默认返回非 0） |

静态工厂方法：

```java
SandboxResult.ok(String output)          // success=true, exitCode=0
SandboxResult.ok()                        // success=true, exitCode=0, output=""
SandboxResult.failure(String output)      // success=false, exitCode=1
SandboxResult.failure(String output, int exitCode)
```

> SandboxManager 失败常量：`DANGEROUS_CODE_MSG` 与 `DANGEROUS_COMMAND_MSG`（详见 [sandbox-manager.md](sandbox-manager.md)）保持旧文本向后兼容。

---

## 3. 沙盒类型枚举 `SandboxType`

源码：[SandboxType.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/sandbox/SandboxType.java)

```java
public enum SandboxType {
    LOCAL_PROCESS,  // 本地进程（bash+ProcessBuilder）
    LOCAL_DOCKER,   // 本地 Docker 容器
    REMOTE_HTTP,    // 远端 HTTP 服务
    CUSTOM          // 第三方自定义扩展
}
```

路由规则见第 9 节 `SandboxRegistry`。

---

## 4. 资源限制 `ResourceLimits`

源码：[ResourceLimits.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/sandbox/ResourceLimits.java)

```java
public record ResourceLimits(
    Double cpuCores,            // e.g. 1.5 → docker --cpus=1.5
    Integer memoryMb,           // e.g. 1024 → docker --memory=1024m
    Integer maxExecutionTimeSec,// e.g. 60 → docker --stop-timeout 或 Process onExit().orTimeout()
    Integer diskMb,             // 预留：配额（目前未做磁盘配额校验）
    Integer maxProcesses        // e.g. 512 → docker --pids-limit=512
) {}
```

`ResourceLimits` 是可选的。当 `SandboxConfig.resourceLimits == null` 时，SandboxManager 会按以下回退规则映射到旧字段：
- `maxExecutionTimeSec` ← `SandboxConfig.getMaxExecutionTimeSec()`
- `memoryMb` ← `SandboxConfig.getMaxMemoryMb()`

---

## 5. 执行上下文 `SandboxContext`

源码：[SandboxContext.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/sandbox/SandboxContext.java)

```java
public record SandboxContext(
    String agentId,                     // 发起本次执行的 Agent 标识
    String sessionId,                   // 会话 ID（用于远端按 session 聚合）
    Map<String, String> metadata        // 自由附加信息
) {}
```

- 远端沙盒会以 HTTP 头的形式透传：
  - `X-Agent-Id: agentId`
  - `X-Session-Id: sessionId`
  - `X-Meta-<key>: <value>`（对 metadata 中每个键）
- 本地沙盒当前未使用 `SandboxContext`，但初始化参数仍然要求传入（`SandboxContext.EMPTY` 可用）。

---

## 6. 沙盒配置 `SandboxConfig`

源码：[SandboxConfig.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/model/SandboxConfig.java)

`SandboxConfig` 是所有沙盒后端共享的配置对象。共 **12 个字段**（6 个为向后兼容保留，6 个为新增扩展字段）：

| 字段 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `workDirectory` | `String` | 必需 | 允许的工作目录前缀，`isPathAllowed` 的根 |
| `maxExecutionTimeSec` | `int` | 60 | 旧字段：最大执行秒数（`resourceLimits` 为 null 时生效） |
| `maxMemoryMb` | `int` | 512 | 旧字段：最大内存 MB（`resourceLimits` 为 null 时生效） |
| `allowedPaths` | `List<String>` | `[]` | 旧字段：额外允许的绝对路径白名单 |
| `allowedCommands` | `List<String>` | `[]` | 旧字段：额外允许的命令白名单 |
| `enabled` | `boolean` | `true` | 旧字段：沙盒总开关（SandboxManager 在所有动作前先检查） |
| **type** | `SandboxType` | `LOCAL_PROCESS` | 新增：选择沙盒后端类型（SandboxRegistry 路由依据） |
| **resourceLimits** | `ResourceLimits` | `null` | 新增：5 维度细粒度资源限制（见第 4 节） |
| **commandPolicy** | `String` | `"default"` | 预留：策略名（未来可按名称从 Bean 工厂 pick） |
| **image** | `String` | `null` | 新增：LOCAL_DOCKER 使用的镜像名（null 时走 SandboxProperties.docker.image 全局默认） |
| **remoteEndpoint** | `String` | `null` | 新增：REMOTE_HTTP 使用的 base URL（null 时走 SandboxProperties.remote.endpoint） |
| **envVars** | `Map<String, String>` | `null` | 新增：传递给沙盒的额外环境变量（不覆盖系统 HOME/TMPDIR） |
| **context** | `SandboxContext` | `EMPTY` | 新增：执行上下文（若同时通过 initialize 传入，后者覆盖 config.context） |

全部 12 字段均提供 fluent setter（返回 `this`）以及无参构造器（字段取默认值）；同时提供一个 6 参数兼容构造器供旧代码直接调用（第 7-12 字段填默认值）。

---

## 7. 生命周期监听器 `SandboxLifecycleListener`

源码：[SandboxLifecycleListener.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/sandbox/SandboxLifecycleListener.java)

```java
public interface SandboxLifecycleListener {
    default void beforeExecute(String codeOrCommand, boolean isCommandMode) {}
    default void afterExecute(String codeOrCommand, SandboxResult result) {}
    default void onDestroy() {}
}
```

触发顺序在 [sandbox-manager.md](sandbox-manager.md) 中说明。多个监听器按添加顺序执行，SandboxManager 捕获并以 `log.warn` 忽略每个监听器抛出的异常，不影响执行结果。

---

## 8. 工厂 SPI `SandboxFactory`

源码：[SandboxFactory.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/sandbox/SandboxFactory.java)

```java
public interface SandboxFactory {
    SandboxType supportedType();
    Sandbox create(SandboxConfig config, SandboxContext context);
    default int priority() { return 0; }
}
```

| 方法 | 语义 |
| --- | --- |
| `supportedType` | 返回该工厂对应的 `SandboxType`，决定是否被路由 |
| `create(config, context)` | 新建并返回 `Sandbox` 实例（可以不 `initialize`，SandboxManager 负责首次 `initialize`） |
| `priority` | 若同一 `supportedType` 存在多个工厂，Registry 取 `priority()` 最大的；默认 0，返回值越大优先级越高 |

**注册方式**（二者任取其一）：
1. **Spring Bean 注入优先**：在 Spring Boot 应用中将自定义 `SandboxFactory` 声明为 `@Bean`，`SandboxRegistry(List<SandboxFactory>)` 构造器会收集并合入。
2. **ServiceLoader 兜底**：在 jar 的 `META-INF/services/org.wall.im.ai.core.sandbox.SandboxFactory` 中写一行实现类全限定名（默认内置 3 个工厂就是用此方式）。

---

## 9. 注册表 `SandboxRegistry`

源码：[SandboxRegistry.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox/SandboxRegistry.java)

```java
SandboxRegistry registry = new SandboxRegistry();         // 纯 Java：仅 ServiceLoader 扫描
SandboxRegistry registry = new SandboxRegistry(beanFactories); // Spring：传入 Bean 工厂，再合并 ServiceLoader
```

路由算法（`Sandbox create(SandboxType type, SandboxConfig config, SandboxContext context)`）：

1. 过滤 `factory.supportedType() == type`
2. 取 `priority()` 最大的 `SandboxFactory`（相等时取先加入的）
3. 找不到对应类型的工厂 → 抛 `IllegalStateException("No SandboxFactory for type: " + type)`
4. 返回 `factory.create(config, context)`（此时未调用 initialize）

---

## 10. 命令策略 `CommandPolicy` 体系

### 10.1 接口 `CommandPolicy`

源码：[CommandPolicy.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/sandbox/policy/CommandPolicy.java)

```java
public interface CommandPolicy {
    boolean isAllowed(String command, SandboxConfig config);
    String getPolicyName();
}
```

- `isAllowed` 只做 "命令" 维度的风险判断（代码 `execute` 走关键字黑名单 + `isPathAllowed` 组合判断，不在此接口内）。
- `getPolicyName` 用于日志 / 未来按名查询。

### 10.2 `CompositeCommandPolicy` 组合链

源码：[CompositeCommandPolicy.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/sandbox/policy/CompositeCommandPolicy.java)

```java
public class CompositeCommandPolicy implements CommandPolicy {
    public CompositeCommandPolicy(CommandPolicy... policies) { ... }
    public CompositeCommandPolicy add(CommandPolicy policy) { ... }
    public boolean isAllowed(String command, SandboxConfig config) {
        return policies.stream().allMatch(p -> p.isAllowed(command, config));
    }
}
```

**短路语义：全部策略都返回 true 才允许**（等价 AND）；任一策略返回 false → 命令被拒绝。

### 10.3 `KeywordBlacklistPolicy` 基础实现

源码：[KeywordBlacklistPolicy.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/sandbox/policy/KeywordBlacklistPolicy.java)

```java
public class KeywordBlacklistPolicy implements CommandPolicy {
    // 以 contains 匹配任意 token（非正则，大小写敏感）
    public KeywordBlacklistPolicy(Set<String> keywords) { ... }
}
```

### 10.4 `DefaultCommandPolicy` 默认策略

源码：[DefaultCommandPolicy.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox/policy/DefaultCommandPolicy.java)

继承 `CompositeCommandPolicy`，**1:1 迁移旧 SandboxManager 硬编码黑名单**：

| 构造方式 | 默认关键字（11 条 always 命中） | 可选网络关键字（6 条，`restrictNetworkCommands=true` 追加） |
| --- | --- | --- |
| 无参构造 | `rm -rf` `chmod 777` `/dev/null` `/dev/` `sda` `passwd` `shadow` `systemctl` `mount` `dd if` `>/dev/sd` | `curl` `wget` `nc` `ncat` `ssh` `scp` |
| `DefaultCommandPolicy(restrictNetworkCommands=false)` | 同上 11 条 | 不追加（等价旧 `LOCAL_PROCESS` 宽松策略） |

- 对于 `LOCAL_DOCKER` / `REMOTE_HTTP`，SandboxManager 默认使用 **restrictNetworkCommands=true**（无参工厂返回的就是该值）。
- `SandboxManager(Sandbox, SandboxConfig)` 兼容 2 参构造器：内部 `restrictNetworkCommands=false`，行为与旧版完全一致。

---

## 11. 类型关系总览

```
                        Sandbox (interface)
                              ▲
       ┌──────────────────────┼───────────────────────┐
       │                      │                       │
ProcessSandbox        DockerLocalSandbox        RemoteSandbox
   (local)                 (local)                  (remote)
       ▲                      ▲                       ▲
       │                      │                       │
LocalProcessSandboxF.  DockerLocalSandboxF.   RemoteSandboxFactory
       └──────────────────────┼───────────────────────┘
                              │
                       SandboxRegistry
                      (按 SandboxType 路由)
                              │
                   SandboxManager (统一入口)
                          │    │
         CommandPolicy────┘    └─── SandboxLifecycleListener[]
```

各实现细节见：
- [process-sandbox.md](process-sandbox.md)
- [docker-sandbox.md](docker-sandbox.md)
- [remote-sandbox.md](remote-sandbox.md)
- [sandbox-manager.md](sandbox-manager.md)
- [extension-guide.md](extension-guide.md)
