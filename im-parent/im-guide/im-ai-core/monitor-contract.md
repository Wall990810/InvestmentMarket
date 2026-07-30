# ← 返回索引

# AgentMonitor / CustomMetricRegistry 详解

## AgentMonitor —— 监控接口

源码：[AgentMonitor.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/monitor/AgentMonitor.java)

统一监控抽象，可对接 Micrometer / Zipkin / Langfuse 等后端。以 `traceId` 串联一次完整的 Agent 调用链路。

| 方法签名 | 说明 |
| --- | --- |
| `String traceStart(String agentName, String input)` | 记录调用开始，返回 `traceId`。 |
| `void traceEnd(String traceId, String agentName, String output, long costTimeMs, int tokenUsage)` | 记录调用正常结束。 |
| `void traceError(String traceId, String agentName, String error)` | 记录调用异常。 |
| `void traceToolCall(String traceId, String toolName, Map<String, Object> parameters, String result, long costTimeMs)` | 记录一次工具调用。 |
| `void recordMetric(String metricName, double value, Map<String, String> tags)` | 记录自定义指标。 |
| `CustomMetricRegistry getCustomMetricRegistry()` | 获取自定义指标注册器。 |

典型使用流程：调用前 `traceStart` 拿到 `traceId` → 执行过程中通过 `traceToolCall` 记录工具调用 → 结束时 `traceEnd` 或异常时 `traceError`。

## CustomMetricRegistry —— 自定义指标注册器

源码：[CustomMetricRegistry.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/monitor/CustomMetricRegistry.java)

允许开发者注册和更新自定义监控指标，提供三类指标：计数器（Counter）、仪表盘（Gauge）、定时器（Timer）。

| 方法签名 | 说明 |
| --- | --- |
| `void registerCounter(String name, String description)` | 注册计数器。 |
| `void incrementCounter(String name)` | 递增计数器。 |
| `void registerGauge(String name, String description)` | 注册仪表盘。 |
| `void setGaugeValue(String name, double value)` | 设置仪表盘当前值。 |
| `void registerTimer(String name, String description)` | 注册定时器。 |
| `void recordTimer(String name, long durationMs)` | 记录一次耗时（毫秒）。 |

使用模式为"先注册、后记录"：先调用 `registerXxx` 注册指标，再通过 `incrementCounter` / `setGaugeValue` / `recordTimer` 持续上报。