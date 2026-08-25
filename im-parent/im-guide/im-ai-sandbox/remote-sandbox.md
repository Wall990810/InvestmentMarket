← [返回索引](../README.md)

# RemoteSandbox · 远端 HTTP 沙盒

`REMOTE_HTTP` 类型把沙盒 **执行权委托给远端服务**，本地进程只做请求转发。本模块提供：
- 客户端 SPI：`RemoteSandboxClient`（5 方法）
- 内置默认实现：`HttpRemoteSandboxClient`（使用 JDK `java.net.http.HttpClient` + Jackson JSON 序列化）
- Sandbox 接口包装：`RemoteSandbox`（把 `RemoteSandboxClient` 适配为 `Sandbox` 接口，SandboxManager 可透明使用）
- 工厂：`RemoteSandboxFactory`

源码：
- [RemoteSandboxClient.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox/remote/RemoteSandboxClient.java)
- [HttpRemoteSandboxClient.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox/remote/HttpRemoteSandboxClient.java)
- [RemoteSandbox.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox/remote/RemoteSandbox.java)
- [RemoteSandboxFactory.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox/remote/RemoteSandboxFactory.java)
- DTO：[RemoteExecuteRequest.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox/remote/RemoteExecuteRequest.java) / [RemoteExecuteResponse.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-sandbox/src/main/java/org/wall/im/ai/sandbox/remote/RemoteExecuteResponse.java)

> **范围说明**：本模块不包含远端服务端实现。请自行部署 OpenCodeSandbox、e2b-code-interpreter 或自建 5 端点兼容服务。

---

## 协议约定（5 端点）

本地客户端使用的 HTTP 协议如下。所有请求 Content-Type 均为 `application/json`。

| 动作 | 方法 | 路径 | 请求体 | 成功响应（2xx） |
| --- | --- | --- | --- | --- |
| 1. 创建沙盒 | POST | `/sandboxes` | `{"endpoint": "...", "workDirectory": "...", "resourceLimits": {...}, "envVars": {...}, "image": "..."}` | `RemoteExecuteResponse`：`sandboxId=新建ID`，其余忽略 |
| 2. 执行代码 | POST | `/sandboxes/{id}/execute` | `{"code": "...", "language": "bash"}` | `RemoteExecuteResponse`：`sandboxId` + `success/output/exitCode` |
| 3. 执行命令 | POST | `/sandboxes/{id}/command` | `{"command": "ls", "args": ["-la"]}` | 同上 |
| 4. 路径权限 | GET | `/sandboxes/{id}/paths?path=<URL-encoded>` | （无） | `RemoteExecuteResponse.success` 字段为 true/false（output 可忽略） |
| 5. 销毁沙盒 | DELETE | `/sandboxes/{id}` | （无） | 空或任意 2xx |

### 通用请求头（SandboxContext 透传）

对于第 1-4 步的每次 HTTP 请求，客户端会自动从 `SandboxContext` 附加：

```
X-Agent-Id:   <agentId>
X-Session-Id: <sessionId>
X-Meta-<key>: <value>    // 对 metadata 中每个 entry 一行
```

> 若 context 的对应字段为 null，则省略对应 header。

---

## DTO 字段详解

### RemoteExecuteRequest

```java
public record RemoteExecuteRequest(
    String mode,                // "create" | "code" | "command"
    String code,                // mode=code 时
    String language,            // mode=code 时（约定 bash）
    String command,             // mode=command 时
    List<String> args,          // mode=command 时
    // -- 以下仅 create 请求使用 --
    String endpoint,            // 预留，可忽略
    String workDirectory,
    ResourceLimits resourceLimits,
    Map<String, String> envVars,
    String image
) {}
```

### RemoteExecuteResponse

```java
public record RemoteExecuteResponse(
    String sandboxId,
    boolean success,
    String output,
    int exitCode
) {}
```

所有响应字段都是 0/1 级 flat，避免嵌套解析。客户端在网络层面做了以下容错：

| 异常情况 | 客户端处理 |
| --- | --- |
| `IOException` / `InterruptedException` | 返回 `SandboxResult.failure("Remote sandbox error: " + ex.getMessage())` |
| HTTP 状态 ≥ 400 | 读响应体为 output，返回 `failure(output, statusCode)` |
| 空响应体 / JSON 反序列化失败 | `failure("Invalid remote response: ...")` |

---

## HttpRemoteSandboxClient 构造参数

```java
public HttpRemoteSandboxClient(String baseEndpoint,
                               int connectTimeoutSec,
                               ObjectMapper objectMapper)
```

| 参数 | 说明 | 默认值（当由工厂装配时） |
| --- | --- | --- |
| `baseEndpoint` | 远端服务 base URL（**不含 /sandboxes 后缀**），如 `https://sb.example.com` | `SandboxConfig.remoteEndpoint` → 否则 `SandboxProperties.remote.endpoint` → 否则抛 `IllegalArgumentException` |
| `connectTimeoutSec` | 远端连接建立超时秒（TCP + TLS 握手） | `SandboxProperties.remote.connectTimeoutSec` 默认 `10` |
| `objectMapper` | Jackson ObjectMapper（用于 JSON 序列化） | 新实例，可注入下游自定义的（启用 Mixin、脱敏等） |

> 客户端使用 `HttpClient.newHttpClient()`，超时只在连接阶段。执行超时另由 SandboxManager 的 `resourceLimits.maxExecutionTimeSec` 控制（上层 `CompletableFuture.orTimeout` → 随后调用 destroy 通知远端释放资源）。

---

## RemoteSandbox（Sandbox 适配层）执行流程

```
RemoteSandbox.initialize(config, context)
  → client.initialize(config, context)
      ↓ POST {base}/sandboxes → 返回 sandboxId
        缓存 sandboxId 成员变量
        │
        ▼
RemoteSandbox.execute(code, language)
  → client.executeCode(sandboxId, code, language, context)
      ↓ POST {base}/sandboxes/{id}/execute
        RemoteExecuteResponse → SandboxResult
        │
        ▼
RemoteSandbox.executeCommand(command, args)
  → client.executeCommand(sandboxId, command, args, context)
      ↓ POST {base}/sandboxes/{id}/command
        │
        ▼
RemoteSandbox.isPathAllowed(path)
  → client.isPathAllowed(sandboxId, path, context)
      ↓ GET {base}/sandboxes/{id}/paths?path=<encoded>
        success=true/false → return boolean
        │
        ▼
RemoteSandbox.destroy()
  → client.destroy(sandboxId)      ← DELETE {base}/sandboxes/{id}
  清空 sandboxId（幂等）
```

- `destroy()` 前的 HTTP DELETE 失败会被捕获并 `log.warn`，本地 sandboxId 仍会清空（避免重复调用远端已经不存在的 ID）。
- 若 initialize 失败 → 抛异常（SandboxManager 负责上层捕获）。

---

## 安全与性能建议

### 必做
1. **HTTPS**：生产环境必须使用 TLS；baseEndpoint 必须是 `https://`。
2. **鉴权**：远端服务必须加鉴权（Bearer Token、mTLS、HMAC-SHA256 签名时间戳 + nonce 三选一）。实现方式：继承 `HttpRemoteSandboxClient`，重写 `buildRequest` 加 `Authorization: Bearer xxx`。
3. **速率限制 + 每租户配额**：远端务必按 `X-Agent-Id` / `X-Session-Id` 统计请求频率、最大并发沙盒数、累计 CPU 分钟。
4. **连接复用**：JDK HttpClient 默认使用连接池；同一 JVM 请复用单例 `HttpRemoteSandboxClient`（不要每次 create 都 new）。

### 推荐
1. **长沙盒 vs 一次性沙盒**：如果 Agent 一次任务内会连续 execute 20+ 次，建议保持同一 sandboxId（减少初始化开销）。若担心污染，可任务结束后 destroy + 重新 initialize。
2. **结果审计落库**：把每次 execute 的 stdout/stderr 落库并关联 X-Session-Id，便于事后审计。
3. **熔断/降级**：远端挂了时，可把 type 切回 LOCAL_DOCKER 作为降级路径（SandboxManager 本身不要求后端是固定类型）。

---

## 典型配置示例

### 编程式（纯 Java）

```java
SandboxConfig config = new SandboxConfig()
    .setWorkDirectory("/workspace")            // 语义由远端决定
    .setType(SandboxType.REMOTE_HTTP)
    .setRemoteEndpoint("https://sb.example.com")
    .setResourceLimits(new ResourceLimits(4.0, 8192, 300, null, 2048))
    .setEnvVars(Map.of("PYTHONDONTWRITEBYTECODE", "1"));

SandboxContext ctx = new SandboxContext("agent-001", "sess-42", Map.of("model", "gpt-5"));

RemoteSandboxClient client = new HttpRemoteSandboxClient(
    "https://sb.example.com",
    10,
    new ObjectMapper());
Sandbox sandbox = new RemoteSandbox(client);
SandboxManager mgr = new SandboxManager(sandbox, config);

mgr.initialize(ctx);
SandboxResult r = mgr.executeCode("python3 -c 'print(42)'", "python");
mgr.close();
```

### Spring Boot（通过 Registry 路由）

见 [configuration-guide.md](configuration-guide.md) `im.ai.sandbox.remote.endpoint` 示例。
