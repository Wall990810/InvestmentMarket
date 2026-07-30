# ← 返回索引

# Sandbox / SandboxResult 详解

## Sandbox —— 沙盒接口

源码：[Sandbox.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/sandbox/Sandbox.java)

限制 Agent 运行时的工作路径与资源访问，用于安全地执行外部代码或命令。

| 方法签名 | 说明 |
| --- | --- |
| `void initialize()` | 初始化沙盒环境。 |
| `SandboxResult execute(String code, String workDir)` | 在指定工作目录下执行代码，返回 `SandboxResult`。 |
| `SandboxResult executeCommand(String command)` | 在沙盒中执行命令。 |
| `boolean isPathAllowed(String path)` | 检查路径是否在沙盒允许范围内。 |
| `void destroy()` | 销毁沙盒环境。 |

## SandboxResult —— 沙盒执行结果

源码：[SandboxResult.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/sandbox/SandboxResult.java)

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `success` | `boolean` | 是否成功。 |
| `output` | `String` | 标准输出。 |
| `errorOutput` | `String` | 错误输出。 |
| `exitCode` | `int` | 退出码。 |
| `executionTimeMs` | `long` | 执行耗时（毫秒）。 |

提供两个静态工厂方法：

```java
public static SandboxResult success(String output, long executionTimeMs)
public static SandboxResult failure(String errorOutput, int exitCode, long executionTimeMs)
```