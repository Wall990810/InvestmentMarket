← 返回索引

# 与 im-ai-observation 模块的关系

`im-ai-observation` 是位于 [im-parent/im-ai/im-ai-observation](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-observation/) 的库模块，提供 AI 智能体的可观测性实现，包含以下组件（基于 `org.wall.im.ai.monitor` 包）：

| 组件 | 作用 |
| --- | --- |
| `composite.CompositeAgentMonitor` | 组合多个 Monitor，统一分发监控事件 |
| `micrometer.MicrometerAgentMonitor` | 基于 Micrometer 的指标采集，对接 Prometheus / Datadog / Influx 等 registry |
| `micrometer.MicrometerCustomMetricRegistry` | 自定义指标注册接口 |
| `zipkin.ZipkinAgentTracer` | 基于 Zipkin Brave 的链路追踪，上报 span |
| `langfuse.LangfuseMonitor` / `LangfuseMonitorFactory` | 接入 Langfuse 平台，记录 Agent 调用 trace |

`im-observation` 作为 Spring Boot 应用，预期未来将这些监控组件以可独立运行的进程形式暴露（例如对外提供 HTTP 接口查询监控数据、聚合 trace 上报端点等），从而成为 `im-ai-observation` 技术栈的"运行壳"。

> 当前阶段 `im-observation` 尚未在 `pom.xml` 中声明对 `im-ai-observation` 的依赖，仅保留启动骨架。