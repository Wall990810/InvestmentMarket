# ← 返回索引

# im-quant-core 使用指南

> InvestmentMarket 项目量化因子框架核心模块使用文档

## 模块概述

`im-quant-core` 是 InvestmentMarket 项目量化子项目（`im-quant`）的**核心抽象层**。它本身不提供任何具体因子实现，而是为整个量化因子框架定义统一的接口契约、数据模型、计算引擎与后处理链，相当于量化框架的"契约层 / SPI 层"。

该模块的职责包括：

- **定义因子核心契约**：[Factor](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/factor/Factor.java) 作为所有因子实现必须遵循的接口，仅需提供 `descriptor()` 与 `compute(FactorContext)` 即可被全链路识别。
- **定义因子元数据**：[FactorDescriptor](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/factor/FactorDescriptor.java)（自描述信息）、[FactorParameter](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/factor/FactorParameter.java)（可调参数）、[FactorCategory](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/factor/FactorCategory.java)（分类枚举）。
- **定义计算上下文**：[FactorContext](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/factor/FactorContext.java) 封装数据源、标的池、截面日、参数表与 K 线缓存。
- **定义数据提供者抽象**：[MarketDataProvider](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/data/MarketDataProvider.java)（行情）与 [FundamentalDataProvider](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/data/FundamentalDataProvider.java)（基本面），并附带内存实现供开发测试。
- **定义计算引擎**：[FactorEngine](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/engine/FactorEngine.java) 作为框架的调度中枢，串联注册表 → 上下文 → 因子计算 → 后处理链 → 结果汇总。
- **定义后处理链**：[FactorProcessor](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/process/FactorProcessor.java) 接口与 [FactorProcessorChain](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/process/FactorProcessorChain.java) 链式编排，内置去极值 / 标准化 / 中性化处理器。

凭借本模块定义的统一契约，上层模块（如 `im-quant-factor` 的因子实现）可以聚焦于具体因子逻辑，而无需重复定义数据结构与引擎；第三方也可通过实现 `Factor` 接口接入框架。

## Maven 坐标与依赖

### 坐标信息

`im-quant-core` 的 Maven 坐标定义于 [pom.xml](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/pom.xml)：

- **groupId**：`org.wall.im`（继承自父模块 `im-quant`）
- **artifactId**：`im-quant-core`
- **version**：`${revision}`（由顶层 `im-parent` 统一通过 `revision` 属性管理，当前 `1.0.0-Beta0-SNAPSHOT`）

### 引入方式

```xml
<dependency>
    <groupId>org.wall.im</groupId>
    <artifactId>im-quant-core</artifact>
    <version>${revision}</version>
</dependency>
```

### 自身依赖

本模块自身仅依赖如下库，保持轻量、无侵入：

| 依赖 | 用途 |
| --- | --- |
| `com.fasterxml.jackson.core:jackson-databind` | JSON 序列化与反序列化 |
| `org.slf4j:slf4j-api` | 日志门面 |
| `org.springframework.boot:spring-boot-autoconfigure`（optional） | 自动装配支持（仅编译期，不强制传递） |
| `org.junit.jupiter:junit-jupiter`（test） | 单元测试 |

> 注意：本模块不绑定任何具体数据源 SDK、数据库驱动或 Web 框架，数据源由下游模块/应用提供。

## 源码索引

### 因子核心抽象（`factor` 包）

| 类 | 源码 |
| --- | --- |
| Factor | [Factor.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/factor/Factor.java) |
| AbstractFactor | [AbstractFactor.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/factor/AbstractFactor.java) |
| FactorDescriptor | [FactorDescriptor.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/factor/FactorDescriptor.java) |
| FactorParameter | [FactorParameter.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/factor/FactorParameter.java) |
| FactorCategory | [FactorCategory.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/factor/FactorCategory.java) |
| FactorContext | [FactorContext.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/factor/FactorContext.java) |
| FactorResult | [FactorResult.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/factor/FactorResult.java) |
| FactorValue | [FactorValue.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/factor/FactorValue.java) |

### 计算引擎（`engine` 包）

| 类 | 源码 |
| --- | --- |
| FactorEngine | [FactorEngine.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/engine/FactorEngine.java) |
| FactorCalculationRequest | [FactorCalculationRequest.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/engine/FactorCalculationRequest.java) |
| FactorCalculationResult | [FactorCalculationResult.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/engine/FactorCalculationResult.java) |

### 因子注册表（`registry` 包）

| 类 | 源码 |
| --- | --- |
| FactorRegistry | [FactorRegistry.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/registry/FactorRegistry.java) |

### 后处理链（`process` 包）

| 类 | 源码 |
| --- | --- |
| FactorProcessor | [FactorProcessor.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/process/FactorProcessor.java) |
| FactorProcessorChain | [FactorProcessorChain.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/process/FactorProcessorChain.java) |
| WinsorizeProcessor | [WinsorizeProcessor.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/process/WinsorizeProcessor.java) |
| StandardizeProcessor | [StandardizeProcessor.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/process/StandardizeProcessor.java) |
| NeutralizeProcessor | [NeutralizeProcessor.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/process/NeutralizeProcessor.java) |

### 数据提供者（`data` 包）

| 类 | 源码 |
| --- | --- |
| MarketDataProvider | [MarketDataProvider.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/data/MarketDataProvider.java) |
| FundamentalDataProvider | [FundamentalDataProvider.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/data/FundamentalDataProvider.java) |
| InMemoryMarketDataProvider | [InMemoryMarketDataProvider.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/data/InMemoryMarketDataProvider.java) |
| InMemoryFundamentalDataProvider | [InMemoryFundamentalDataProvider.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/data/InMemoryFundamentalDataProvider.java) |

### 数据模型（`model` 包）

| 类 | 源码 |
| --- | --- |
| Bar | [Bar.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/model/Bar.java) |
| FundamentalData | [FundamentalData.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/model/FundamentalData.java) |
| Universe | [Universe.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/model/Universe.java) |
| Frequency | [Frequency.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/model/Frequency.java) |
| Instrument | [Instrument.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/model/Instrument.java) |

### 统计工具（`stats` 包）

| 类 | 源码 |
| --- | --- |
| StatUtils | [StatUtils.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/stats/StatUtils.java) |

## 功能块文档索引

| 功能块 | 说明 | 文档 |
| --- | --- | --- |
| 因子核心抽象 | Factor / AbstractFactor / Descriptor / Parameter / Context / Result / Value / Category | [factor-spi.md](factor-spi.md) |
| 数据提供者 | MarketDataProvider / FundamentalDataProvider + 内存实现 | [data-provider.md](data-provider.md) |
| 计算引擎 | FactorEngine / FactorCalculationRequest / FactorCalculationResult | [calculation-engine.md](calculation-engine.md) |
| 因子注册表 | FactorRegistry 注册 / 查询 / 批量管理 | [factor-registry.md](factor-registry.md) |
| 后处理链 | FactorProcessor / FactorProcessorChain / Winsorize / Standardize / Neutralize | [processor-chain.md](processor-chain.md) |
| 数据模型 | Bar / FundamentalData / Universe / Frequency / Instrument | [data-models.md](data-models.md) |
| 使用示例 | 因子注册、引擎调用、后处理、自定义因子扩展 | [integration-examples.md](integration-examples.md) |
