← 返回索引

# SandboxManager 详解

源码：[SandboxManager.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox/SandboxManager.java)

`SandboxManager` 是模块对外的**统一入口**，封装了"开关 + 危险操作预检查 + 委托执行"的完整流程。构造参数为 `Sandbox sandbox` 与 `SandboxConfig config`。

## 开关：enabled

- `safeExecute(code)` 与 `safeExecuteCommand(command)` 在执行前都会检查 `config.isEnabled()`：
  - 为 `false` 时，打印 `warn` 日志后**绕过所有限制**直接调用 `sandbox.execute` / `sandbox.executeCommand`；
- 这意味着 `enabled=false` 等价于"裸执行"，仅作为开发/调试逃生通道，生产环境应保持 `true`。

## 危险操作预检（containsDangerousOperations）

在调用底层 `Sandbox` 之前，`SandboxManager` 会先对代码/命令做**小写化包含匹配**，命中以下任一模式即直接拦截并返回 `failure`，不进入进程执行：

| 模式 | 说明 |
| --- | --- |
| `rm -rf /` | 递归删除根目录 |
| `rm -rf ~` | 递归删除用户主目录 |
| `mkfs` | 格式化文件系统 |
| `dd if=` | 块设备底层写入 |
| `:(){:|:&};:` | fork 炸弹 |
| `chmod -R 777 /` | 全盘权限放开 |
| `wget` | 网络下载 |
| `curl -o` | 网络下载到文件 |
| `nc -l` | 监听网络端口 |
| `> /dev/sda` | 直接写块设备 |
| `format c:` | Windows 格式化 |

- 命中时 `safeExecute` 返回 `failure("Dangerous operations detected and blocked", -1, 0)`；
- `safeExecuteCommand` 返回 `failure("Dangerous command blocked", -1, 0)`。

> 该预检基于简单字符串匹配，属于"纵深防御"中的一层，不能替代操作系统级别的权限隔离。规避方式（如变量拼接、编码）可能绕过匹配，因此仍需结合最小权限账户运行 Agent。

## 路径访问查询

`canAccess(String path)` 直接委托给 `sandbox.isPathAllowed(path)`，供上层在执行前自行判断是否允许访问某路径，而不真正启动进程。

## 调用入口一览

| 方法 | 行为 |
| --- | --- |
| `safeExecute(code)` | enabled 关闭→裸执行；命中危险模式→拦截；否则 `sandbox.execute(code, config.getWorkDir())` |
| `safeExecuteCommand(command)` | enabled 关闭→裸执行；命中危险模式→拦截；否则 `sandbox.executeCommand(command)` |
| `canAccess(path)` | 委托 `sandbox.isPathAllowed(path)` |