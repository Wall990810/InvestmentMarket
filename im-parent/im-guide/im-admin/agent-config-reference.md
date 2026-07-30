# ← 返回索引

# agents/investment-advisor.yml 配置说明

配置文件位于 [investment-advisor.yml](file:///d:/IdeaProject/InvestmentMarket/im-admin/src/main/resources/agents/investment-advisor.yml)，由 `AgentConfigParser.parseFromClasspath("agents/investment-advisor.yml")` 加载。文件分为 `defaults` 与 `agents` 两部分。

## defaults 段（全局默认值）

```yaml
defaults:
  model:        { provider: openai, name: gpt-4, temperature: 0.7, maxTokens: 4096 }
  memory:       { shortTermStore: memory, longTermStore: memory,
                  shortTermMaxEntries: 100, longTermMaxEntries: 10000, ttlSeconds: 0 }
  sandbox:      { enabled: true, networkAccess: false, maxExecutionTime: 300, maxMemoryMb: 512 }
  monitor:      { enabled: true, zipkinEndpoint: http://localhost:9411/api/v2/spans,
                  langfuse: { enabled: false, host: http://localhost:3000, publicKey: pk-lf-xxx, secretKey: sk-lf-xxx } }
  execution:    { maxConcurrency: 10, timeoutSeconds: 60, retryCount: 3 }
```

> 注意：`defaults.model` 声明的是 `openai/gpt-4`，但本模块实际注入的 `ChatModel` Bean 是 `DashScopeChatModel`（通义千问 `qwen-plus`）。YAML 中的模型配置为声明式约定，运行时由 [AiAgentConfig](file:///d:/IdeaProject/InvestmentMarket/im-admin/src/main/java/org/wall/im/admin/config/AiAgentConfig.java) 提供的真实 `ChatModel` Bean 替代。

## agents 段（投资顾问 Agent 定义）

```yaml
agents:
  - name: investment-advisor
    description: "投资建议智能体，负责分析市场行情、评估风险并生成个性化投资组合建议"
    type: task
    model:        { provider: openai, name: gpt-4, temperature: 0.3, maxTokens: 8192 }
    skills:       [investment-analysis-skill, portfolio-recommend-skill]
    tools:        [market-data-tool, risk-assessment-tool]
    memory:       { shortTermStore: memory, longTermStore: memory,
                    shortTermMaxEntries: 200, longTermMaxEntries: 50000, ttlSeconds: 86400 }
    sandbox:      { enabled: true, workDir: /tmp/ai-sandbox/investment,
                    networkAccess: true, maxExecutionTime: 600 }
    monitor:      { enabled: true,
                    langfuse: { enabled: true, host: http://localhost:3000,
                                publicKey: pk-lf-investment, secretKey: sk-lf-investment } }
    execution:    { maxConcurrency: 5, timeoutSeconds: 120, retryCount: 2 }
    properties:
      riskLevels: [conservative, balanced, aggressive]
      defaultRiskLevel: balanced
      supportedMarkets: [A股, 港股, 美股]
      assetCategories: [股票, 债券, 基金, 货币基金, 黄金]
```

关键配置解读：

- `name: investment-advisor`：与 `InvestmentAgentService.AGENT_NAME` 一致，是 `AgentRegistry.getRequired(...)` 的查找键。
- `type: task`：任务型 Agent。
- `model.temperature: 0.3`：投资建议需较低随机性，保证输出稳定（低于默认 0.7）。
- `skills` / `tools`：引用已注册的 Skill 与 Tool 名称。
- `memory.ttlSeconds: 86400`：24 小时过期。
- `sandbox.networkAccess: true`：需访问行情数据接口，故放开网络。
- `properties`：投资建议专属扩展属性（风险等级、支持市场、资产类别）。