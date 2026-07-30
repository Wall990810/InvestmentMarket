# ← 返回索引

# 模块概述

`im-admin` 是 InvestmentMarket 项目中的一个**独立 Spring Boot 演示应用**（standalone demo app），它不在 `im-parent` 聚合模块之下，而是直接继承自 `spring-boot-starter-parent 4.1.0`。该模块演示了如何基于项目自研的 AI 智能体框架（`im-ai-core` + `im-ai-agent`）构建一个"投资顾问智能体"（Investment Advisor Agent），对外提供：

- 投资咨询问答（自然语言对话）
- 标的深度分析报告
- 个性化投资组合推荐

智能体在 Spring 容器启动时根据 YAML 配置自动创建，并装配了两个技能（Skill）和两个工具（Tool），通过 `AgentRegistry` 统一注册与查找。

---

## Maven 坐标与依赖

### 坐标

| 属性 | 值 |
| --- | --- |
| groupId | `org.wall.im` |
| artifactId | `im-admin` |
| version | `0.0.1-SNAPSHOT` |
| parent | `org.springframework.boot:spring-boot-starter-parent:4.1.0` |
| java.version | `26` |

### 关键依赖

`im-admin` 直接继承 `spring-boot-starter-parent 4.1.0`（**注意**：与 `im-parent` 下使用 Spring Boot 3.5.x 的模块不同，这里使用 4.1.0），主要依赖包括：

- **AI 智能体框架**（自研）：
  - [im-ai-core](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/)（`${ai.framework.version}` = `1.0.0-Beta0-SNAPSHOT`）
  - [im-ai-agent](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-agent/)（同版本）
- **Spring AI Alibaba（通义千问）**：
  - `com.alibaba.cloud.ai:spring-ai-alibaba-agent-framework`（`1.1.2.0`）
  - `com.alibaba.cloud.ai:spring-ai-alibaba-starter-dashscope`（`1.1.2.0`）
- **Spring Boot starters**：`webflux`、`webmvc`、`actuator`、`security`、`jdbc`、`mongodb`、`elasticsearch`、`session-data-redis`、`session-jdbc`、`zipkin`
- **Spring Cloud**：`spring-cloud-starter`、`loadbalancer`、`zookeeper-config`、`zookeeper-discovery`（`spring-cloud.version` = `2025.1.2`）
- **监控**：`micrometer-registry-prometheus` / `datadog` / `influx`
- 其他：`mysql-connector-j`、`lombok`

---

## 源码索引

| 类别 | 文件 |
| --- | --- |
| 应用入口 | [AdminApplication.java](file:///d:/IdeaProject/InvestmentMarket/im-admin/src/main/java/org/wall/im/admin/AdminApplication.java) |
| 配置类 | [AiAgentConfig.java](file:///d:/IdeaProject/InvestmentMarket/im-admin/src/main/java/org/wall/im/admin/config/AiAgentConfig.java) |
| 业务服务 | [InvestmentAgentService.java](file:///d:/IdeaProject/InvestmentMarket/im-admin/src/main/java/org/wall/im/admin/agent/InvestmentAgentService.java) |
| 技能 | [InvestmentAnalysisSkill.java](file:///d:/IdeaProject/InvestmentMarket/im-admin/src/main/java/org/wall/im/admin/agent/skill/InvestmentAnalysisSkill.java)、[PortfolioRecommendSkill.java](file:///d:/IdeaProject/InvestmentMarket/im-admin/src/main/java/org/wall/im/admin/agent/skill/PortfolioRecommendSkill.java) |
| 工具 | [MarketDataTool.java](file:///d:/IdeaProject/InvestmentMarket/im-admin/src/main/java/org/wall/im/admin/agent/tool/MarketDataTool.java)、[RiskAssessmentTool.java](file:///d:/IdeaProject/InvestmentMarket/im-admin/src/main/java/org/wall/im/admin/agent/tool/RiskAssessmentTool.java) |
| Agent 配置 | [investment-advisor.yml](file:///d:/IdeaProject/InvestmentMarket/im-admin/src/main/resources/agents/investment-advisor.yml) |
| Markdown 技能 | [investment-analysis.md](file:///d:/IdeaProject/InvestmentMarket/im-admin/src/main/resources/skills/investment-analysis.md)、[portfolio-recommend.md](file:///d:/IdeaProject/InvestmentMarket/im-admin/src/main/resources/skills/portfolio-recommend.md) |
| 应用配置 | [application.yml](file:///d:/IdeaProject/InvestmentMarket/im-admin/src/main/resources/application.yml) |