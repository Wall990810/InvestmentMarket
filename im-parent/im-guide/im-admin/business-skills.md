# ← 返回索引

# 技能 (Skill)

技能实现自 [Skill](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/) 接口（`org.wall.im.ai.core.skill.Skill`），需实现 `getName()`、`getDescription()`、`execute(String input)` 与 `canExecute(String input)` 四个方法。

## InvestmentAnalysisSkill

[InvestmentAnalysisSkill.java](file:///d:/IdeaProject/InvestmentMarket/im-admin/src/main/java/org/wall/im/admin/agent/skill/InvestmentAnalysisSkill.java) 负责分析市场行情、技术面/基本面数据，输出投资分析报告。

- `getName()`：返回 `"investment-analysis-skill"`
- `getDescription()`：返回 `"投资分析技能：对指定标的进行技术面和基本面分析，输出投资建议报告"`
- `canExecute(input)`：输入非空即返回 `true`
- `execute(input)`：返回格式化的"投资分析报告"，包含技术面（均线、MACD、RSI）、基本面（市盈率、营收、行业景气度）、综合评级与操作建议。当前为演示桩实现，注释中说明实际应调用行情数据接口与指标计算。

## PortfolioRecommendSkill

[PortfolioRecommendSkill.java](file:///d:/IdeaProject/InvestmentMarket/im-admin/src/main/java/org/wall/im/admin/agent/skill/PortfolioRecommendSkill.java) 根据用户风险偏好生成个性化资产配置方案。

- `getName()`：返回 `"portfolio-recommend-skill"`
- `getDescription()`：返回 `"投资组合推荐技能：根据风险偏好生成个性化资产配置方案"`
- `canExecute(input)`：输入非空即返回 `true`
- `execute(input)`：返回格式化的"投资组合建议"，包含稳健型资产配置比例（A股宽基指数基金 40%、国债基金 25%、黄金ETF 10%、货币基金 15%、港股科技ETF 10%）、预期年化收益、最大回撤预估与再平衡周期。当前为演示桩实现。