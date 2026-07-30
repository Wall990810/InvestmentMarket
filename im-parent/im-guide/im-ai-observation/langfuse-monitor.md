[← 返回索引](../README.md)

> 本文档为 [`usage-guide.md`](./usage-guide.md) 的拆分章节之一。

# LangfuseMonitor + LangfuseMonitorFactory

源码：[LangfuseMonitor.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-observation/src/main/java/org/wall/im/ai/monitor/langfuse/LangfuseMonitor.java)、[LangfuseMonitorFactory.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-observation/src/main/java/org/wall/im/ai/monitor/langfuse/LangfuseMonitorFactory.java)

`LangfuseMonitor` 同样是**装饰器**：所有方法先调用 `delegate`，再在配置启用时把事件批量上报到 Langfuse。它通过 langfuse-java SDK 的 Ingestion API 异步发送事件。

## 7.1 构造与工厂

```java
public LangfuseMonitor(MonitorConfig.LangfuseConfig config,
                       LangfuseClient langfuseClient,
                       AgentMonitor delegate);

public static LangfuseMonitor create(MonitorConfig.LangfuseConfig config, AgentMonitor delegate);
```

直接使用构造器需要自行构建 `LangfuseClient`。推荐使用 `LangfuseMonitorFactory`：

| 工厂方法 | 行为 |
| --- | --- |
| `createDefault(delegate)` | 默认连接 `http://localhost:3000`，但 `enabled=false`（未启用） |
| `create(config, delegate)` | 按指定 `LangfuseConfig` 创建，`config` 不能为 `null` |
| `fromEnvironment(delegate)` | 读取环境变量 `LANGFUSE_PUBLIC_KEY` / `LANGFUSE_SECRET_KEY` / `LANGFUSE_HOST`（可选），两 key 均非空才 `enabled=true` |
| `fromClient(client, config, delegate)` | 复用已构建的 `LangfuseClient`（用于自定义 HTTP 客户端/连接池） |
| `defaultConfig()` | 返回 `enabled=false`、host 为默认值的配置 |

工厂常量：

```java
public static final String DEFAULT_HOST = "http://localhost:3000";
public static final String ENV_PUBLIC_KEY = "LANGFUSE_PUBLIC_KEY";
public static final String ENV_SECRET_KEY = "LANGFUSE_SECRET_KEY";
public static final String ENV_HOST = "LANGFUSE_HOST";
```

## 7.2 上报的内容

所有上报都受 `config.isEnabled() && config.isConfigured()` 双重门控；`isConfigured()` 要求 `publicKey` 与 `secretKey` 均非空。内部维护 `traceMapping`（`ConcurrentHashMap`），把 delegate 返回的 `traceId` 映射到 Langfuse 自身的 `langfuseTraceId`（UUID）。

| 方法 | Langfuse 事件 | 关键字段 |
| --- | --- | --- |
| `traceStart` | `TraceEvent`（traceCreate） | `name="agent.<agentName>"`，`input={input: <input>}`，`metadata={framework: wall-ai}` |
| `traceEnd` | `CreateObservationEvent`（observationCreate，type=GENERATION） | `name="agent.<agentName>.generation"`，`input={agent:<agentName>}`，`output={output:<output>}`，`usage={input:0, output:0, total:<tokenUsage>}`，`startTime=now-costMs`，`endTime=now`，`metadata={cost_ms:<costTimeMs>}` |
| `traceError` | `CreateObservationEvent`（type=SPAN，level=ERROR） | `name="agent.<agentName>.error"`，`output={error:<error>}`，`level=ERROR` |
| `traceToolCall` | `CreateObservationEvent`（type=SPAN） | `name="tool.<toolName>"`，`input={tool:<toolName>, parameters:<parameters>}`，`output={result:<result>, cost_ms:<costTimeMs>}` |
| `recordMetric` | 不上报 Langfuse | 仅委托 delegate |
| `getCustomMetricRegistry` | 不上报 Langfuse | 返回 delegate 的注册器 |

要点：

- **trace 映射生命周期**：`traceStart` 时写入映射；`traceEnd` / `traceError` 时 `remove` 取出并使用；`traceToolCall` 时仅 `get`（不删除），允许同一 trace 下多次工具调用。
- **时间戳**：均使用 `OffsetDateTime.now(ZoneOffset.UTC)` 并以 `ISO_OFFSET_DATE_TIME` 格式化；Generation/Span 的 `startTime` 通过 `now.minus(Duration.ofMillis(costTimeMs))` 反推。
- **Usage 字段**：当前 `input` 与 `output` 固定为 0，仅 `total` 携带 `tokenUsage`。
- **错误隔离**：所有上报逻辑包裹在 try/catch 中，失败仅 `log.warn`，不影响主流程；delegate 已经先执行过，因此 Langfuse 失败不会丢失 delegate 的监控数据。
- **flush**：`LangfuseMonitor.flush()` 目前仅打印 debug 日志，预留用于后续显式刷新；SDK 的批量上报由 `IngestionRequest` 触发。
- **调试**：`config.isDebug()=true` 时，每次上报都会 `log.debug` 输出 `langfuseTraceId` 等信息。

## 7.3 配置对象

`MonitorConfig.LangfuseConfig`（位于 [MonitorConfig.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/model/MonitorConfig.java)）字段：

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `enabled` | `false` | 是否启用 Langfuse 上报 |
| `host` | `http://localhost:3000` | Langfuse 服务地址 |
| `publicKey` | `null` | 公开 API Key（必需） |
| `secretKey` | `null` | 私有 API Key（必需） |
| `debug` | `false` | 是否打印调试日志 |
| `flushIntervalMs` | `5000` | 预留刷新间隔，0 表示不自动刷新 |
| `maxBatchSize` | `50` | 预留批量上报最大条数 |

`isConfigured()` 当且仅当 `publicKey` 与 `secretKey` 均非空时返回 `true`。