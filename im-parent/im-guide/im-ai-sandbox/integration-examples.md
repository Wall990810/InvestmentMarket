← 返回索引

# 典型使用示例

以下示例展示一个 Agent 在沙盒中执行用户提交的脚本，并获取结果：

```java
// 1. 初始化沙盒
SandboxConfig config = new SandboxConfig();
config.setEnabled(true);
config.setWorkDir("/var/lib/im/ai-sandbox");
config.setMaxExecutionTime(30);

ProcessSandbox sandbox = new ProcessSandbox(config);
sandbox.initialize();

SandboxManager manager = new SandboxManager(sandbox, config);

try {
    // 2. 执行用户脚本（已通过危险操作预检）
    String script = "echo hello\n"
                  + "date\n"
                  + "ls -l";
    SandboxResult result = manager.safeExecute(script);

    if (result.isSuccess()) {
        System.out.println("输出:\n" + result.getOutput());
    } else {
        System.err.println("失败 exitCode=" + result.getExitCode()
                + " 耗时=" + result.getExecutionTimeMs() + "ms");
        System.err.println("stderr:\n" + result.getErrorOutput());
    }

    // 3. 执行单条命令
    SandboxResult cmdResult = manager.safeExecuteCommand("whoami");
    System.out.println("whoami => " + cmdResult.getOutput());

    // 4. 路径访问预检
    boolean ok = manager.canAccess("/var/lib/im/ai-sandbox/output.txt");
    System.out.println("canAccess = " + ok);

} finally {
    // 5. 销毁沙盒，清理工作目录
    sandbox.destroy();
}
```

执行流程时序：

1. `SandboxManager.safeExecute` 检查 `enabled`、扫描危险模式；
2. 通过后委托 `ProcessSandbox.execute(code, config.getWorkDir())`；
3. `ProcessSandbox` 将脚本写入 `script_<timestamp>.sh`，以 `bash` 启动子进程；
4. 清空子进程环境变量，仅保留 `HOME` / `TMPDIR` 指向沙盒目录；
5. 在 `maxExecutionTime` 内等待进程结束，超时则强制销毁；
6. 收集 stdout / stderr / exitCode 封装为 `SandboxResult` 返回，并删除临时脚本。