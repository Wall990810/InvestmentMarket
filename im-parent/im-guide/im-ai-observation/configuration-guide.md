[← 返回索引](../README.md)

> 本文档为 [`usage-guide.md`](./usage-guide.md) 的拆分章节之一。

# 配置示例

## 9.1 YAML 形式（Spring Boot 风格，绑定到 `MonitorConfig`）

```yaml
im:
  ai:
    monitor:
      enabled: true
      zipkin-endpoint: http://localhost:9411/api/v2/spans
      langfuse:
        enabled: true
        host: https://cloud.langfuse.com
        public-key: pk-lf-xxxxxxxx
        secret-key: sk-lf-xxxxxxxx
        debug: false
        flush-interval-ms: 5000
        max-batch-size: 50
      custom-metrics:
        ai.rag.hit: "RAG 命中次数"
        ai.context.tokens: "当前上下文 token 数"
```

> `MonitorConfig` 顶层字段为 `enabled`、`zipkinEndpoint`、`langfuse`、`customMetrics`。Langfuse 子段对应 `MonitorConfig.LangfuseConfig`。配置绑定后，调用 `LangfuseMonitorFactory.create(config.getLangfuse(), delegate)` 即可得到启用的 Langfuse 监控器。

## 9.2 仅启用部分后端

```yaml
# 只用 Micrometer + Prometheus，关闭 Langfuse
im:
  ai:
    monitor:
      enabled: true
      langfuse:
        enabled: false
```

```yaml
# 只用 Zipkin + Langfuse，关闭整体指标聚合（仅示例，Micrometer 仍可独立构造）
im:
  ai:
    monitor:
      enabled: true
      zipkin-endpoint: http://zipkin:9411/api/v2/spans
      langfuse:
        enabled: true
        host: http://langfuse:3000
        public-key: ${LANGFUSE_PUBLIC_KEY}
        secret-key: ${LANGFUSE_SECRET_KEY}
```

## 9.3 环境变量方式（Langfuse）

```bash
export LANGFUSE_PUBLIC_KEY=pk-lf-xxxxxxxx
export LANGFUSE_SECRET_KEY=sk-lf-xxxxxxxx
export LANGFUSE_HOST=https://cloud.langfuse.com   # 可选，默认 http://localhost:3000
```

对应代码：`LangfuseMonitorFactory.fromEnvironment(delegate)`。