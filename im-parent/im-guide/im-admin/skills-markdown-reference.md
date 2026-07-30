# ← 返回索引

# skills/*.md 技能文件说明

`src/main/resources/skills/` 下存放 Markdown 形式的技能定义，由 `MarkdownSkillLoader.loadFromClasspath("skills")` 加载。每个文件由 YAML front-matter（`name`、`description`、`tools`）与正文 Prompt 组成，正文通过 `{{input}}` 占位符接收用户输入。

## investment-analysis.md

[investment-analysis.md](file:///d:/IdeaProject/InvestmentMarket/im-admin/src/main/resources/skills/investment-analysis.md)：

- front-matter：`name: investment-analysis-skill`，关联工具 `market-data-tool`、`risk-assessment-tool`
- 角色：专业投资分析师
- 要求：技术面分析（趋势、信号）、基本面分析（估值、成长性）、风险评估（等级、风险点）、投资建议（综合评级、操作建议含入场价与止损位）
- 输出格式：固定结构的"投资分析报告"

## portfolio-recommend.md

[portfolio-recommend.md](file:///d:/IdeaProject/InvestmentMarket/im-admin/src/main/resources/skills/portfolio-recommend.md)：

- front-matter：`name: portfolio-recommend-skill`，关联工具 `market-data-tool`、`risk-assessment-tool`
- 角色：资产配置顾问
- 配置原则：风险匹配、分散投资、流动性管理、再平衡策略
- 输出格式：固定结构的"投资组合建议"（含各类资产占比、预期收益、最大回撤、再平衡周期、风险提示）

> Java 实现的 Skill 与 Markdown Skill 同名（`investment-analysis-skill`、`portfolio-recommend-skill`）。Java 实现提供确定性桩逻辑，Markdown 文件提供 LLM Prompt 模板，二者互补。