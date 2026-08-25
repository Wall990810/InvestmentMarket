← [返回索引](../README.md)

# ProcessSandbox · 本地进程级沙盒

`ProcessSandbox` 是最基础的沙盒实现：基于 JDK `ProcessBuilder` + 宿主系统的 `bash` 解释器执行命令和代码，仅提供路径白名单、超时、关键字黑名单三级防护，**未做文件系统/资源隔离**。

源码：[ProcessSandbox.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox/local/ProcessSandbox.java)
工厂：[LocalProcessSandboxFactory.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox/local/LocalProcessSandboxFactory.java)

---

## 适用场景

| 场景 | 是否推荐 | 备注 |
| --- | --- | --- |
| 开发调试、可信脚本 | ✅ | 启动开销最小（毫秒级） |
| 生产不可信 Agent 脚本 | ❌ | 无法阻止进程逃逸、磁盘爆炸、root 写入 |
| 跨平台一致性执行 | ⚠️ | 依赖宿主机 bash + 环境变量，不同机器结果可能不同 |
| 旧代码 0 改动迁移 | ✅ | 完全兼容老 2 参构造签名（见 [sandbox-manager.md](sandbox-manager.md)） |

对比：[DockerLocalSandbox](docker-sandbox.md) 与 [RemoteSandbox](remote-sandbox.md) 的隔离更强。

---

## 执行机制

```
SandboxConfig + SandboxContext
          │
          ▼
  ProcessSandbox.initialize()
   - 校验 workDirectory 为绝对路径
   - 校验 PATH 中存在 bash
          │
          ▼
  execute(code, language=bash)
   - ProcessBuilder("bash", "-c", "cd <workDirectory> && <code>")
   - 继承 HOME、TMPDIR、PATH
   - 追加 config.envVars（若有）
   - CompletableFuture + orTimeout(maxExecutionTimeSec)
   - waitFor() → 取 stdout、stderr、exitCode
          │
          ▼
  executeCommand(cmd, args)
   - ProcessBuilder(command, args...)
   - directory(config.workDirectory)
   - 环境变量处理同上
          │
          ▼
  isPathAllowed(path)
   - path.startsWith(workDirectory)
   - 或 path ∈ allowedPaths
   - 两者任一成立返回 true
          │
          ▼
  destroy()       → 空操作（进程沙盒无持久资源）
```

### 执行超时与中断

`ProcessSandbox` 使用 `process.onExit().orTimeout(maxExecutionTimeSec, SECONDS)` 实现超时；超时抛 `TimeoutException`：
- 捕获后对 Process 调用 `destroyForcibly()`
- 返回 `SandboxResult.failure("Execution timed out after <N> seconds")`

---

## 环境变量注入

`ProcessSandbox.initialize()` 读取 `config.getEnvVars()`：

1. 先从宿主机 `System.getenv()` 保留 **HOME、TMPDIR、PATH、LANG** 四个键（保持 OS 可执行搜索/用户家目录默认）
2. 再把 `envVars` 里的 key-value 追加到 process builder（**不覆盖 1 中已有键**，避免破坏 bash 启动）
3. 若 `envVars == null`，不做任何覆盖；此时等价旧行为

> 若希望沙盒里能看到的环境变量为 **完全自定义的封闭集合**（不继承宿主），请改用 `DockerLocalSandbox` 并在创建镜像时只注入 ENV。

---

## Windows 兼容性说明

`ProcessSandbox` 依赖 `bash` 解释器，在 Windows 上需要：
- 安装 WSL2 的 Ubuntu 发行版（推荐）并把 `<wsl>/bin` 放进 PATH；或
- 安装 Git for Windows，使用 Git Bash。

若 `bash --version` 在 cmd/PowerShell 中执行失败，ProcessSandbox 会在 `initialize` 时报 `bash not found`。需要强 Windows 支持请使用 `DockerLocalSandbox`（基于 Linux 容器镜像，与平台无关）。

---

## 已知风险（与 Docker/Remote 的对比）

下表对比三种实现的隔离维度：

| 防护维度 | ProcessSandbox | DockerLocalSandbox | RemoteSandbox |
| --- | --- | --- | --- |
| 路径访问白名单 | ✅ 粗粒度前缀 | ✅ 通过容器挂载目录限制 | ✅ 由远端服务负责 |
| 命令关键字黑名单 | ✅（SandboxManager 层） | ✅（SandboxManager 层） | ✅（SandboxManager 层 + 远端双保险） |
| CPU 限制 | ❌ 仅靠超时 | ✅ `--cpus` | ✅ 由远端负责 |
| 内存限制 | ❌ 仅靠旧字段 | ✅ `--memory` / `--memory-swap` | ✅ 由远端负责 |
| 进程数限制 | ❌ | ✅ `--pids-limit` | ✅ 由远端负责 |
| 网络隔离 | ❌（策略层关键字） | ✅ 默认 `--network=none` | ✅ 由远端负责 |
| 文件系统隔离 | ❌（宿主磁盘共享） | ✅ `tmpfs` 临时 + 只读卷 | ✅ 由远端负责 |
| 环境变量隔离 | ⚠️ 继承宿主 4 个核心键 | ✅ 容器内独立 | ✅ 远端环境独立 |
| 启动开销 | ~5-50 ms | ~500-3000 ms | 一次网络 RTT（创建） |

---

## 与旧版迁移注意事项

| 旧版（单实现时期） | 新版（本实现） |
| --- | --- |
| 包路径 `org.wall.im.ai.sandbox.ProcessSandbox` | 包路径已改为 `org.wall.im.ai.sandbox.local.ProcessSandbox` |
| `SandboxManager(Sandbox, SandboxConfig)` 两参构造内部为硬编码黑名单 | 构造器语义保持不变，内部改为注入 DefaultCommandPolicy(restrict=false) + 空 listeners + ownedByManager=false |
| 无 envVars 支持 | 新增 envVars 字段，非空时会追加 4 个默认环境变量 |

直接把源码 import 改成新包路径即可，无其他 API 变化。

---

## 快速上手

```java
SandboxConfig config = new SandboxConfig()
    .setWorkDirectory("/tmp/im-work")
    .setMaxExecutionTimeSec(30)
    .setMaxMemoryMb(512)
    .setAllowedPaths(List.of("/home/me/data"));

Sandbox sandbox = new ProcessSandbox();
SandboxManager mgr = new SandboxManager(sandbox, config); // 兼容 2 参

SandboxResult r1 = mgr.executeCode("echo hello", "bash");
SandboxResult r2 = mgr.executeCommand("ls", List.of("-la"));
mgr.close();
```

更多示例见 [integration-examples.md](integration-examples.md)。
