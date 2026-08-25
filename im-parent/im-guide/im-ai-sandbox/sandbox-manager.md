← [返回索引](../README.md)

# SandboxManager · 统一入口

`SandboxManager` 是业务方调用沙盒能力的唯一对外入口。它在具体 `Sandbox` 实现之外统一提供：

- 全局 `enabled` 开关；
- 危险关键字 + `CommandPolicy.isAllowed` 双重安全预检；
- `SandboxLifecycleListener` 生命周期钩子；
- 两条使用路径：直接传 `Sandbox` 实例，或传 `SandboxRegistry + SandboxType` 走路由。

源码：[SandboxManager.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox/SandboxManager.java)

---

## 构造器矩阵（4 个，向后兼容）

```
构造器 1（经典兼容 2 参）──┐
构造器 2（+ CommandPolicy） │
构造器 3（+ listeners）     │  直接传 Sandbox 实例，ownedByManager=false
构造器 4（Registry 路由）───┘  通过 SandboxRegistry.create()，ownedByManager=true
```

| # | 构造签名 | policy | listeners | ownedByManager | 说明 |
| --- | --- | --- | --- | --- | --- |
| 1 | `SandboxManager(Sandbox sandbox, SandboxConfig config)` | DefaultCommandPolicy(**restrict=false**) 宽松版（6 网络关键字不追加） | 空 | **false** | 旧 2 参，行为 1:1 回退 |
| 2 | `SandboxManager(Sandbox s, SandboxConfig c, CommandPolicy p)` | 参数 `p`（若为 null → DefaultCommandPolicy restrict=true 严格版） | 空 | **false** | 自定义策略 |
| 3 | `SandboxManager(Sandbox s, SandboxConfig c, CommandPolicy p, List<SandboxLifecycleListener> l)` | 参数 `p`（null 同 2） | 参数 `l`（null 视为空） | **false** | 自定义策略 + 生命周期链 |
| 4 | `SandboxManager(SandboxRegistry registry, SandboxConfig c, SandboxContext ctx)` | DefaultCommandPolicy restrict=true 严格版 | 空 | **true** | 通过 registry.create(config.type) 构建 Sandbox |

### ownedByManager 语义

这是影响 `destroy()` 行为的关键布尔量：

| ownedByManager | Sandbox 来源 | destroy() 时是否会调用 `sandbox.destroy()` |
| --- | --- | --- |
| `false`（构造器 1~3） | 外部传入，生命周期由调用者管理 | **否**（避免破坏调用者自己持有的 Sandbox） |
| `true`（构造器 4） | 由 registry 产出，Manager 持有所有权 | **是**（close()/destroy() 会真正销毁内部 Sandbox） |

> 无论哪种情况，`SandboxLifecycleListener.onDestroy()` 都会触发，便于上层做审计/指标。

---

## 4 个对外方法

| 方法 | 等价调用（示意） | 说明 |
| --- | --- | --- |
| `void initialize(SandboxContext context)` | `sandbox.initialize(config, context)`（自动触发 listeners 无，但 initialize 是幂等重复安全） | 显式初始化；execute* 首次执行时也会懒 initialize（使用 config.context 若 context 为 null） |
| `SandboxResult executeCode(String code, String language)` | 见下方详细流程 | 执行代码段 |
| `SandboxResult executeCommand(String command, List<String> args)` | 同 | 执行命令 |
| `void destroy()` | listeners.onDestroy()；若 ownedByManager 则 sandbox.destroy() | 释放资源（构造器 1~3 外部自行销毁 sandbox） |

另有 `close()`（AutoCloseable）委托 `destroy()`，所以 SandboxManager 支持：

```java
try (var mgr = new SandboxManager(registry, config, ctx)) {
    ...
}   // 自动 destroy，若 ownedByManager 会销毁容器/远端
```

---

## 执行流程（executeCode / executeCommand 共享）

```
 进入 executeCode(code, language) / executeCommand(cmd, args)
              │
              ▼
 1. 短路：if (!config.isEnabled())
          return SandboxResult.failure("Sandbox is disabled")
              │
              ▼
 2. 懒初始化（若首次调用）：if (!initialized) sandbox.initialize(config, ctx)
              │
              ▼
 3. 危险操作预检（分 mode）：
    ├─ 代码模式（executeCode）
    │    if code 命中 11 关键字 (DANGEROUS_CODE_SET)
    │       → failure(DANGEROUS_CODE_MSG, exitCode=1)
    │    扫描代码中"路径字符串字面量"（简单 "..." 识别），
    │       任一不通过 sandbox.isPathAllowed() 也失败
    │
    └─ 命令模式（executeCommand）
         if policy != null && !policy.isAllowed(command, config)
            → failure(DANGEROUS_COMMAND_MSG, exitCode=1)
              │
              ▼
 4. 调用 lifecycleListeners 每个的 beforeExecute(codeOrCommand, isCommandMode)
              │
              ▼
 5. 实际 sandbox.execute(code, lang) / sandbox.executeCommand(cmd, args)
              │
              ▼
 6. 调用 lifecycleListeners 每个的 afterExecute(codeOrCommand, result)
    （4、6 步单个 listener 抛错都被 catch + log.warn 忽略，不影响主流程）
              │
              ▼
 7. 返回 result
```

### 兼容常量说明

以下字符串常量与旧版完全一致（仅从内联硬编码抽到静态字段）：

```java
public static final String DANGEROUS_CODE_MSG
    = "The code contains dangerous operations and is blocked from running.";
public static final String DANGEROUS_COMMAND_MSG
    = "The command is blocked by security policy.";
```

`DANGEROUS_CODE_SET` 包含 11 条关键字：`rm -rf` `chmod 777` `/dev/null` `/dev/` `sda` `passwd` `shadow` `systemctl` `mount` `dd if` `>/dev/sd`。
旧代码若用字符串 `==` 比较返回值，会与旧结果一致（常量池引用相同）。

---

## 命令策略注入示例

```java
// 自定义策略：禁止访问任何 /var/lib/** 目录
var enterprisePolicy = new CommandPolicy() {
    @Override public boolean isAllowed(String cmd, SandboxConfig c) {
        return !cmd.contains("/var/lib");
    }
    @Override public String getPolicyName() { return "enterprise-varlib"; }
};

// 叠加默认关键字黑名单（11 条 + 6 条网络严格）
CompositeCommandPolicy chain = new DefaultCommandPolicy(true)
    .add(enterprisePolicy);

SandboxManager mgr = new SandboxManager(
    new DockerLocalSandbox(),
    config,
    chain,                              // 3 参注入 policy
    List.of(new AuditLifecycleListener()));  // 加审计 listener
```

---

## Registry 路由路径示例（构造器 4）

```java
SandboxRegistry registry = ...;  // 从 Spring 注入或 new SandboxRegistry()

SandboxConfig config = new SandboxConfig()
    .setType(SandboxType.LOCAL_DOCKER)
    .setWorkDirectory("/srv/work")
    .setResourceLimits(new ResourceLimits(2.0, 2048, 60, 256, 1024));
SandboxContext ctx = new SandboxContext("agent-7", "sess-1", Map.of());

try (var mgr = new SandboxManager(registry, config, ctx)) {
    SandboxResult r = mgr.executeCommand("python3", List.of("./calc.py"));
}
// 自动 destroy → docker rm -f（因为 ownedByManager=true）
```

对比：如果用构造器 1 直接传 DockerLocalSandbox，则 close 不会自动 `docker rm`，需要外部调用 `sandbox.destroy()`。

---

## 典型生命周期钩子示例

```java
public class MetricListener implements SandboxLifecycleListener {
    private final Timer execTimer = Metrics.timer("sandbox.exec");

    @Override public void beforeExecute(String code, boolean cmd) {
        MDC.put("sandbox_exec_start", String.valueOf(System.nanoTime()));
    }

    @Override public void afterExecute(String code, SandboxResult result) {
        long start = Long.parseLong(MDC.get("sandbox_exec_start"));
        execTimer.record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        if (!result.success()) {
            Metrics.counter("sandbox.exec.fail").increment();
        }
    }

    @Override public void onDestroy() {
        Metrics.counter("sandbox.destroy").increment();
    }
}
```

---

## 易错点说明

| 误用 | 后果 | 正确做法 |
| --- | --- | --- |
| 用构造器 1~3 传入 ProcessSandbox 但忘记外部 destroy() | 对 ProcessSandbox 无害（destroy 空操作），对 Docker 会**遗留容器** | 要么用 try-with-resources 包住 `Sandbox` 自身，要么用构造器 4 走 ownedByManager=true |
| config.enabled=false 却希望执行 | 所有 execute 返回 "Sandbox is disabled" | 上层开关逻辑；enabled=false 是紧急逃生通道 |
| executeCode 传入 language="python" 但沙盒只实现了 bash | DockerLocalSandbox 目前**只按 bash -s 处理**，不会自动调用 python 解释器 | 代码开头加 shebang；或 executeCommand("python3", List.of(file)) |
| 重写 DefaultCommandPolicy 忘了 super 构造 | 11 关键字黑名单全部丢失，危险命令放行 | 先 `new DefaultCommandPolicy(true)` 再加自定义策略链 |
| 路径字符串不经过 isPathAllowed 的语义 | executeCode 预检里只会扫 `"/"` 开始的双引号包串；复杂拼路径（如 `$HOME/data`）可能漏检 | 强烈建议命令走 `executeCommand` + CommandPolicy 规则，或把危险路径放入策略链 AST 级扫描 |
