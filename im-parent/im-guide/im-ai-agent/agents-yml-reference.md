# agents.yml 配置详解

← 返回 [索引](../README.md)

## 8. agents.yml 配置示例

以下示例对应模块内置的 [agents.yml](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/resources/agents.yml)，字段含义见注释：

```yaml
# 全局默认配置：被 agents 列表中对应字段为 null 的 Agent 继承
defaults:
  model:
    provider: openai
    name: gpt-4
    temperature: 0.7
    maxTokens: 4096
  memory:
    shortTermStore: memory
    longTermStore: memory
    shortTermMaxEntries: 100
    longTermMaxEntries: 10000
    ttlSeconds: 0
  sandbox:
    enabled: true
    networkAccess: false
    maxExecutionTime: 300
    maxMemoryMb: 512
  monitor:
    enabled: true
    zipkinEndpoint: http://localhost:9411/api/v2/spans
    langfuse:
      enabled: false
      host: http://localhost:3000
      publicKey: pk-xxx
      secretKey: sk-xxx
  execution:
    maxConcurrency: 10     # DefaultAgent 会用其推导 ReactAgent 的 recursionLimit
    timeoutSeconds: 60
    retryCount: 3

# Agent 列表
agents:
  - name: chat-agent
    description: "通用对话智能体，支持多轮对话和上下文记忆"
    type: chat
    skills:
      - qa-skill
    tools:
      - web-search
      - calculator

  - name: data-analysis-agent
    description: "数据分析智能体，擅长数据处理和统计分析"
    type: task
    model:                       # 自身设置了 model，不会被 defaults 覆盖
      provider: openai
      name: gpt-4
      temperature: 0.3
    skills:
      - data-analysis-skill
    tools:
      - python-executor
      - sql-query
    memory:                      # 自身设置了 memory
      shortTermStore: redis
      longTermStore: db
      shortTermMaxEntries: 200
      longTermMaxEntries: 50000
      ttlSeconds: 86400
    sandbox:
      enabled: true
      workDir: /tmp/ai-sandbox/data-analysis
      networkAccess: true
      maxExecutionTime: 600
    execution:
      maxConcurrency: 5
      timeoutSeconds: 120
```

调用 `AgentConfigParser.applyDefaults(definition)` 后，`chat-agent` 将继承 `defaults` 中的 `model`/`memory`/`sandbox`/`monitor`/`execution`；`data-analysis-agent` 因已显式设置这些字段而保持不变。