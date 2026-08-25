← [返回索引](../README.md)

# 模块概述

`im-ai-sandbox` 是 InvestmentMarket AI 体系中的 **Agent 运行时沙盒模块**，用于限制 Agent 执行代码或命令时的工作目录、可访问路径、命令风险，并提供可插拔的隔离后端与策略扩展点。

本模块对 [im-ai-core](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-core) 中定义的 `Sandbox`/`SandboxFactory`/`CommandPolicy` SPI 提供 **三种开箱即用的沙盒实现**，外加一个 `CUSTOM` 扩展预留位：

| 沙盒类型 | 枚举 | 实现类 | 隔离强度 | 典型场景 |
| --- | --- | --- | --- | --- |
| 本地进程级 | `LOCAL_PROCESS`（默认） | [ProcessSandbox](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox/local/ProcessSandbox.java) | 弱（ProcessBuilder + bash，仅路径/超时/白名单） | 开发调试、受控环境、已知脚本 |
| 本地 Docker 容器 | `LOCAL_DOCKER` | [DockerLocalSandbox](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox/local/DockerLocalSandbox.java) | 强（容器级 CPU/内存/进程数/网络/tmpfs 隔离） | 生产级、不可信脚本、跨平台一致运行 |
| 远端 HTTP | `REMOTE_HTTP` | [RemoteSandbox](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox/remote/RemoteSandbox.java) + [HttpRemoteSandboxClient](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox/remote/HttpRemoteSandboxClient.java) | 强（远端执行服务自身的隔离） | 多租户共享执行集群、K8s 部署、统一资源管控 |
| 自定义扩展 | `CUSTOM` | 由第三方 `SandboxFactory` 提供 | — | 接入 Podman/LXC/OpenCodeSandbox 等后端 |

并提供以下横切能力：

- [SandboxManager](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox/SandboxManager.java) — 对外的统一入口：开关 + `CommandPolicy` 危险操作预检 + 生命周期钩子 + Registry 路由
- [SandboxRegistry](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox/SandboxRegistry.java) — `SandboxFactory` 注册表，Spring Bean 注入优先 + `ServiceLoader` 兜底
- [DefaultCommandPolicy](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox/policy/DefaultCommandPolicy.java) — 默认命令策略（迁移自旧硬编码黑名单，11 条关键字），基于 `CompositeCommandPolicy` 可链式叠加
- [SandboxAutoConfiguration](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox/config/SandboxAutoConfiguration.java) — Spring Boot 自动装配（可选依赖，纯 Java 环境不需要）

模块源码位于：
[im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox)

---

## Maven 坐标与依赖

模块坐标（继承自 `im-ai` 父模块，版本通过 `${revision}` 管理）：

```xml
<dependency>
    <groupId>org.wall.im</groupId>
    <artifactId>im-ai-sandbox</artifactId>
</dependency>
```

依据 [pom.xml](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/pom.xml)，本模块依赖关系如下：

| 依赖 | 说明 | scope | 是否可选 |
| --- | --- | --- | --- |
| `im-ai-core` | 提供 Sandbox/SandboxFactory/ResourceLimits/CommandPolicy 等核心抽象 | compile | 必选 |
| `slf4j-api` | 日志门面 | compile | 必选 |
| `jackson-databind` | 远端 HTTP 客户端序列化请求/响应 | compile | 必选 |
| `spring-boot-autoconfigure` | SandboxAutoConfiguration 装配支持 | compile | **optional**（下游不强传递） |
| `spring-boot-configuration-processor` | YAML 元数据补全 | — | **optional** |
| `junit-jupiter` | 单元测试 | test | — |
| `mockito-core` | 单元测试 mock（已升级至 5.23.0 + byte-buddy 1.18.x，兼容 JDK 26） | test | — |

> `LOCAL_PROCESS` 在 Windows 上需要 WSL/Git Bash 提供 `bash`；`LOCAL_DOCKER` 需要本地 Docker daemon + PATH 中的 `docker` CLI；`REMOTE_HTTP` 仅需网络到远端执行服务。
> 非 Spring 环境下本模块可独立作为纯 Java 库使用，`SandboxRegistry()` 默认构造器会通过 `ServiceLoader` 发现内置 3 个 `SandboxFactory`。

---

## 源码索引

### 核心编排

| 类 | 职责 | 源码 |
| --- | --- | --- |
| SandboxManager | 统一入口：enabled 开关、危险操作预检、策略注入、生命周期钩子、Registry 路由 | [SandboxManager.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox/SandboxManager.java) |
| SandboxRegistry | SandboxFactory 注册表（Spring 注入优先 + ServiceLoader 兜底，priority 取大） | [SandboxRegistry.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox/SandboxRegistry.java) |

### 本地沙盒（local 子包）

| 类 | 职责 | 源码 |
| --- | --- | --- |
| ProcessSandbox | 进程级沙盒（bash + ProcessBuilder） | [ProcessSandbox.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox/local/ProcessSandbox.java) |
| LocalProcessSandboxFactory | `LOCAL_PROCESS` 对应的工厂 | [LocalProcessSandboxFactory.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox/local/LocalProcessSandboxFactory.java) |
| DockerLocalSandbox | Docker 容器级沙盒（docker CLI + `sleep infinity` 保活） | [DockerLocalSandbox.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox/local/DockerLocalSandbox.java) |
| DockerLocalSandboxFactory | `LOCAL_DOCKER` 对应的工厂 | [DockerLocalSandboxFactory.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox/local/DockerLocalSandboxFactory.java) |
| DockerCommandExecutor | docker CLI 调用抽象（便于 mock 与替换 Podman） | [DockerCommandExecutor.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox/local/DockerCommandExecutor.java) |
| ProcessDockerCommandExecutor | 基于 ProcessBuilder 的 docker CLI 默认实现 | [ProcessDockerCommandExecutor.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox/local/ProcessDockerCommandExecutor.java) |

### 远端沙盒（remote 子包）

| 类 | 职责 | 源码 |
| --- | --- | --- |
| RemoteSandboxClient | 远端执行客户端 SPI（5 个 REST 动作） | [RemoteSandboxClient.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox/remote/RemoteSandboxClient.java) |
| HttpRemoteSandboxClient | JDK HttpClient 默认实现，附协议约定 | [HttpRemoteSandboxClient.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox/remote/HttpRemoteSandboxClient.java) |
| RemoteSandbox | 把 RemoteSandboxClient 包装为 `Sandbox` 接口，SandboxManager 可透明使用 | [RemoteSandbox.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox/remote/RemoteSandbox.java) |
| RemoteSandboxFactory | `REMOTE_HTTP` 对应的工厂 | [RemoteSandboxFactory.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox/remote/RemoteSandboxFactory.java) |
| RemoteExecuteRequest | 创建/执行请求 DTO（command 模式 + code 模式） | [RemoteExecuteRequest.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox/remote/RemoteExecuteRequest.java) |
| RemoteExecuteResponse | 响应 DTO（sandboxId + SandboxResult 字段） | [RemoteExecuteResponse.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox/remote/RemoteExecuteResponse.java) |

### 策略（policy 子包）

| 类 | 职责 | 源码 |
| --- | --- | --- |
| DefaultCommandPolicy | 默认策略：`CompositeCommandPolicy` + 默认 11 关键字黑名单 + 可选网络关键字扩展 | [DefaultCommandPolicy.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox/policy/DefaultCommandPolicy.java) |

### Spring Boot 自动装配（config 子包）

| 类 | 职责 | 源码 |
| --- | --- | --- |
| SandboxProperties | `@ConfigurationProperties("im.ai.sandbox")`，含 `docker` 与 `remote` 嵌套属性 | [SandboxProperties.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox/config/SandboxProperties.java) |
| SandboxAutoConfiguration | 自动装配：SandboxRegistry + DefaultCommandPolicy（`@ConditionalOnMissingBean`，可被覆盖） | [SandboxAutoConfiguration.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox/config/SandboxAutoConfiguration.java) |

---

## 功能块文档索引

| 功能块 | 说明 | 文档 |
| --- | --- | --- |
| Sandbox SPI 全集 | Sandbox 接口、类型系统、资源限制、上下文、生命周期、工厂、策略契约 | [sandbox-spi.md](sandbox-spi.md) |
| ProcessSandbox | 进程级沙盒详解（bash + ProcessBuilder） | [process-sandbox.md](process-sandbox.md) |
| DockerLocalSandbox | Docker 容器级沙盒详解（资源限制 + tmpfs + 网络隔离） | [docker-sandbox.md](docker-sandbox.md) |
| RemoteSandbox | 远端 HTTP 沙盒客户端 + REST 5 端点协议 | [remote-sandbox.md](remote-sandbox.md) |
| SandboxManager | 统一入口与兼容构造器、策略注入、生命周期钩子、ownedByManager 语义 | [sandbox-manager.md](sandbox-manager.md) |
| 配置指南 | YAML + Java Bean + Spring Boot 自动装配 三种配置方式 | [configuration-guide.md](configuration-guide.md) |
| 使用示例 | 4 场景端到端代码 | [integration-examples.md](integration-examples.md) |
| 扩展指南 | SandboxFactory 扩展、CommandPolicy 链扩展、DockerCommandExecutor 替换 | [extension-guide.md](extension-guide.md) |
| 安全注意事项 | 12 条安全要点与已知风险 | [security-notes.md](security-notes.md) |
