← 返回索引

# Sandbox SPI 回顾

本模块实现自 [im-ai-core 的 Sandbox 接口](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/sandbox/Sandbox.java)：

```java
public interface Sandbox {
    void initialize();
    SandboxResult execute(String code, String workDir);
    SandboxResult executeCommand(String command);
    boolean isPathAllowed(String path);
    void destroy();
}
```

- `initialize()`：初始化沙盒环境（如创建工作目录）；
- `execute(code, workDir)`：在指定工作目录中执行一段脚本代码；
- `executeCommand(command)`：在沙盒中执行一条 shell 命令；
- `isPathAllowed(path)`：判断路径是否在允许范围内；
- `destroy()`：销毁沙盒并清理资源。

执行结果由 [SandboxResult](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/sandbox/SandboxResult.java) 承载，包含字段：`success`、`output`（标准输出）、`errorOutput`（标准错误）、`exitCode`、`executionTimeMs`。两个静态工厂方法：

- `SandboxResult.success(String output, long executionTimeMs)` —— `exitCode=0`；
- `SandboxResult.failure(String errorOutput, int exitCode, long executionTimeMs)` —— 失败时把错误信息写入 `errorOutput`。

配置由 [SandboxConfig](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/model/SandboxConfig.java) 提供，关键字段：

| 字段 | 默认值 | 含义 |
| --- | --- | --- |
| `enabled` | `true` | 是否启用沙盒限制；为 `false` 时路径检查直接放行 |
| `workDir` | `null` | 沙盒工作目录；为 `null` 时由实现创建临时目录 |
| `allowedPaths` | `null` | 额外允许访问的路径白名单 |
| `networkAccess` | `false` | 是否允许网络访问（配置项，当前实现未强制） |
| `maxExecutionTime` | `300` | 单次执行最大耗时（秒） |
| `maxMemoryMb` | `512` | 最大内存（MB，配置项） |