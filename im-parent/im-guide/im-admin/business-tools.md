# ← 返回索引

# 工具 (Tool)

工具实现自 `Tool` 接口（`org.wall.im.ai.core.tool.Tool`），需实现 `getName()`、`getDescription()`、`getParameterSchema()` 与 `execute(Map<String, Object> parameters)`。工具提供结构化参数与 JSON 返回，供 Agent 在推理过程中调用。

## MarketDataTool

[MarketDataTool.java](file:///d:/IdeaProject/InvestmentMarket/im-admin/src/main/java/org/wall/im/admin/agent/tool/MarketDataTool.java) 提供股票、基金、债券等标的的实时/历史行情数据查询能力。

- `getName()`：`"market-data-tool"`
- 参数 schema（`required`：`symbol`、`market`）：
  - `symbol`（string）：标的代码，如 `600519.SH`
  - `market`（string，enum：`A股`/`港股`/`美股`）：市场
  - `dataType`（string，enum：`realtime`/`daily`/`weekly`）：数据类型
  - `startDate`（string）：`yyyy-MM-dd`
  - `endDate`（string）：`yyyy-MM-dd`
- `execute(...)` 返回 JSON 字符串，包含 `symbol`、`market`、`date`、`open`、`high`、`low`、`close`、`volume`、`amount`、`change`、`changePercent`。当前为演示桩，注释中说明实际应调用 Tushare/Wind 等行情 API。

## RiskAssessmentTool

[RiskAssessmentTool.java](file:///d:/IdeaProject/InvestmentMarket/im-admin/src/main/java/org/wall/im/admin/agent/tool/RiskAssessmentTool.java) 对投资组合或单一标的进行风险指标计算与评估。

- `getName()`：`"risk-assessment-tool"`
- 参数 schema（`required`：`portfolioId`）：
  - `portfolioId`（string）：投资组合 ID
  - `riskLevel`（string，enum：`conservative`/`balanced`/`aggressive`）：风险偏好
  - `timeHorizon`（integer）：投资期限（天）
- `execute(...)` 返回 JSON 字符串，包含 `var95`、`var99`、`maxDrawdown`、`sharpeRatio`、`sortinoRatio`、`volatility`、`beta`、`alpha`、`riskScore`、`riskLabel`。当前为演示桩，注释中说明实际应基于历史数据做蒙特卡洛模拟或参数法计算。