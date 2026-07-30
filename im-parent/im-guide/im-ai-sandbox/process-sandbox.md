← 返回索引

# ProcessSandbox 详解

源码：[ProcessSandbox.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox/ProcessSandbox.java)

`ProcessSandbox` 是基于 OS 进程的隔离实现，是 `Sandbox` SPI 的唯一具体实现。

## 初始化（initialize）

- 幂等：通过 `volatile boolean initialized` 保证只初始化一次；
- 工作目录确定规则：
  - 若 `config.getWorkDir()` 非空，则将其解析为绝对路径并 `normalize()`；
  - 否则调用 `Files.createTempDirectory("ai-sandbox-")` 创建临时目录；
- 随后 `Files.createDirectories(...)` 确保目录存在，标记 `initialized=true` 并记录日志。

> 未初始化即调用 `execute` / `executeCommand` 会抛 `IllegalStateException("Sandbox not initialized")`（由 `ensureInitialized()` 触发）。

## 工作目录与路径限制（isPathAllowed）

路径准入逻辑：

1. 若 `config.isEnabled() == false`，直接返回 `true`（沙盒关闭，全部放行）；
2. 将目标路径 `toAbsolutePath().normalize()`；
3. 若路径以 `sandboxWorkDir` 为前缀，允许；
4. 否则遍历 `config.getAllowedPaths()`，命中任一白名单前缀即允许；
5. 都未命中则返回 `false`，异常时也返回 `false` 并打印告警日志。

## 执行脚本（execute）

`execute(String code, String workDir)` 的执行流程：

1. `ensureInitialized()`；
2. `targetDir = workDir != null ? workDir : sandboxWorkDir.toString()`；若该目录未通过 `isPathAllowed`，直接返回 `failure("Path not allowed in sandbox: ...")`；
3. 将 `code` 写入临时脚本文件 `script_<timestamp>.sh`（位于 `sandboxWorkDir`）；
4. 构造 `ProcessBuilder("bash", scriptFile)`，`.directory(targetDir)`，`redirectErrorStream(false)`；
5. **清空环境变量**：`pb.environment().clear()`，仅注入 `HOME` 与 `TMPDIR`，均指向 `sandboxWorkDir`；
6. 启动进程，`process.waitFor(config.getMaxExecutionTime(), TimeUnit.SECONDS)`：
   - 超时则 `destroyForcibly()` 并返回 `failure("Execution timed out", -1, elapsed)`；
7. 读取 stdout / stderr，获取 `exitCode`；
8. 删除临时脚本文件；
9. `exitCode == 0` 返回 `success(stdout, elapsed)`，否则返回 `failure(stderr, exitCode, elapsed)`；
10. 任何异常都被捕获并转为 `failure(e.getMessage(), -1, elapsed)`。

## 执行命令（executeCommand）

`executeCommand(String command)` 与 `execute` 类似，区别在于：

- 直接使用 `ProcessBuilder("bash", "-c", command)`，不写入脚本文件；
- 工作目录固定为 `sandboxWorkDir`（不接受外部 `workDir` 参数）；
- 同样清空环境变量、仅保留 `HOME` / `TMPDIR`、应用 `maxExecutionTime` 超时控制。

## 销毁（destroy）

- 递归遍历 `sandboxWorkDir`，按逆序删除所有文件与子目录；
- 将 `initialized` 置为 `false`，允许再次 `initialize()` 重建；
- 删除失败仅打印告警日志，不抛异常。