← 返回索引

# 安全注意事项

1. **依赖 `bash` 存在**：`ProcessSandbox` 硬编码使用 `bash` 解释脚本与命令。在 Windows 上需通过 WSL 或 Git Bash 提供 `bash`，否则执行会失败。部署环境应确保 `bash` 路径在 `PATH` 中。

2. **危险操作预检可被绕过**：`containsDangerousOperations` 是基于小写字符串包含的静态匹配，可被变量拼接、Base64 解码、引号变换等手段规避。请将其视为第一层防线，不要作为唯一安全边界。

3. **环境变量被清空**：子进程的 `environment()` 会被 `clear()`，仅注入 `HOME` 与 `TMPDIR`。若 Agent 依赖某些环境变量（如 `PATH`、`JAVA_HOME`），需自行扩展 `ProcessSandbox` 注入必要变量，否则脚本中调用的外部命令可能找不到。

4. **工作目录即边界**：路径准入以 `sandboxWorkDir` 与 `allowedPaths` 白名单为准。请谨慎配置 `allowedPaths`，避免将敏感系统目录加入白名单；`workDir` 应使用专用目录而非用户主目录。

5. **超时与资源**：`maxExecutionTime` 控制单次执行时长（默认 300 秒），超时后 `destroyForcibly()` 强制结束进程。`maxMemoryMb` 与 `networkAccess` 目前仅为配置项，`ProcessSandbox` 未对内存与网络做强制隔离；如需硬性限制，应结合 cgroups / 容器等 OS 级机制。

6. **销毁会删除目录**：`destroy()` 会递归删除整个 `sandboxWorkDir`。若 `workDir` 指向非临时目录（如共享数据目录），销毁会连带删除其中所有文件，请务必将 `workDir` 指向沙盒专用目录。

7. **以最小权限账户运行**：建议以非 root、仅具备沙盒目录读写权限的专用系统账户运行 Agent 进程，即便沙盒被绕过，也能限制损害范围。

8. **enabled=false 是逃生通道**：当 `enabled=false` 时，`SandboxManager` 会绕过预检与路径检查直接执行，仅适用于受控的开发调试环境，生产环境严禁关闭。