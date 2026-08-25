# ← 返回索引

# im-quant-factor 使用指南

> InvestmentMarket 项目量化因子实现模块使用文档

## 模块概述

`im-quant-factor` 是 InvestmentMarket 项目量化子项目（`im-quant`）的**因子实现层**。它在 [im-quant-core](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/factor/Factor.java) 定义的 SPI 基础上，提供：

- **8 个内置因子**：覆盖动量、价值、波动率、流动性四大分类，包含动量、RSI、PE、PB、已实现波动率、Beta、换手率、Amihud 非流动性。
- **AI Tool 桥接**：[FactorQueryTool](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-factor/src/main/java/org/wall/im/quant/factor/ai/FactorQueryTool.java) 实现 `Tool` 接口，供投资顾问 Agent 在 ReAct 循环中查询因子截面值。
- **Spring Boot 自动装配**：[QuantAutoConfiguration](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-factor/src/main/java/org/wall/im/quant/factor/ai/QuantAutoConfiguration.java) 一键装配数据源、注册表、引擎与 Tool。

## Maven 坐标与依赖

### 坐标信息

- **groupId**：`org.wall.im`
- **artifactId**：`im-quant-factor`
- **version**：`${revision}`（当前 `1.0.0-Beta0-SNAPSHOT`）

### 引入方式

```xml
<dependency>
    <groupId>org.wall.im</groupId>
    <artifactId>im-quant-factor</artifactId>
    <version>${revision}</version>
</dependency>
```

### 自身依赖

| 依赖 | 用途 |
| --- | --- |
| `org.wall.im:im-quant-core` | 因子核心抽象（Factor / FactorEngine / FactorRegistry 等） |
| `org.wall.im:im-ai-core` | AI Tool 接口定义（`Tool`） |
| `com.fasterxml.jackson.core:jackson-databind` | JSON 序列化 |
| `org.springframework.boot:spring-boot-autoconfigure`（optional） | 自动装配支持 |

## 源码索引

### 因子实现

| 因子 | 分类 | 源码 |
| --- | --- | --- |
| MomentumFactor | 动量 | [MomentumFactor.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-factor/src/main/java/org/wall/im/quant/factor/momentum/MomentumFactor.java) |
| RsiFactor | 动量 | [RsiFactor.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-factor/src/main/java/org/wall/im/quant/factor/momentum/RsiFactor.java) |
| PeRatioFactor | 价值 | [PeRatioFactor.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-factor/src/main/java/org/wall/im/quant/factor/value/PeRatioFactor.java) |
| PbRatioFactor | 价值 | [PbRatioFactor.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-factor/src/main/java/org/wall/im/quant/factor/value/PbRatioFactor.java) |
| RealizedVolatilityFactor | 波动率 | [RealizedVolatilityFactor.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-factor/src/main/java/org/wall/im/quant/factor/volatility/RealizedVolatilityFactor.java) |
| BetaFactor | 波动率 | [BetaFactor.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-factor/src/main/java/org/wall/im/quant/factor/volatility/BetaFactor.java) |
| TurnoverFactor | 流动性 | [TurnoverFactor.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-factor/src/main/java/org/wall/im/quant/factor/liquidity/TurnoverFactor.java) |
| AmihudIlliquidityFactor | 流动性 | [AmihudIlliquidityFactor.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-factor/src/main/java/org/wall/im/quant/factor/liquidity/AmihudIlliquidityFactor.java) |

### AI 集成

| 类 | 源码 |
| --- | --- |
| FactorQueryTool | [FactorQueryTool.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-factor/src/main/java/org/wall/im/quant/factor/ai/FactorQueryTool.java) |
| QuantAutoConfiguration | [QuantAutoConfiguration.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-factor/src/main/java/org/wall/im/quant/factor/ai/QuantAutoConfiguration.java) |

## 功能块文档索引

| 功能块 | 说明 | 文档 |
| --- | --- | --- |
| 内置因子 | 8 个因子的标识、分类、参数与计算逻辑 | [built-in-factors.md](built-in-factors.md) |
| AI Tool 集成 | FactorQueryTool 参数 schema 与 QuantAutoConfiguration | [ai-tool-integration.md](ai-tool-integration.md) |
| 因子扩展指南 | 新增因子的步骤与模板 | [extension-guide.md](extension-guide.md) |
