← 返回索引

# 模块概述

`im-ai-sandbox` 是 InvestmentMarket AI 体系中的**Agent 运行时沙盒模块**，用于限制 Agent 执行代码或命令时的工作目录、可访问路径以及危险操作，防止 Agent 产生的脚本越权访问宿主文件系统或执行破坏性命令。

本模块对 [im-ai-core](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core) 中的 `Sandbox` SPI 提供：

- `ProcessSandbox` —— 基于 OS 进程隔离的沙盒实现，通过 `bash` 执行脚本/命令，限制工作目录、清空环境变量、设置超时与路径白名单；
- `SandboxManager` —— 对外的统一入口，在调用 `Sandbox` 之前叠加一层"危险操作预检查"，并支持通过 `SandboxConfig.enabled` 一键开关沙盒。

模块源码位于：
[im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox)

---

# Maven 坐标与依赖

模块坐标（继承自 `im-ai` 父模块，版本通过 `${revision}` 管理）：

```xml
<dependency>
    <groupId>org.wall.im</groupId>
    <artifactId>im-ai-sandbox</artifactId>
</dependency>
```

依据 [pom.xml](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/pom.xml)，本模块依赖非常轻量：

| 依赖 | 说明 | scope |
| --- | --- | --- |
| `im-ai-core` | 提供 `Sandbox` / `SandboxResult` / `SandboxConfig` 抽象 | compile |
| `slf4j-api` | 日志门面 | compile |
| `junit-jupiter` | 单元测试 | test |
| `mockito-core` | 单元测试 mock | test |

> 本模块**不**依赖 Spring，可独立作为纯 Java 库使用。运行时需要宿主环境提供 `bash` 可执行文件（Linux/macOS 原生支持；Windows 需通过 WSL/Git Bash 等提供）。