# im-quant-factor 使用指南

## 模块概述

`im-quant-factor` 是 InvestmentMarket 项目中 **`im-quant` 量化子项目下的量化因子计算模块**。从命名约定看，它专注于金融量化领域中的"因子（Factor）"——即用于解释资产收益与风险的数值化指标，负责因子的定义、计算与输出，为上层的趋势预测、组合优化等场景提供量化输入。

该模块位于 [im-parent/im-quant/im-quant-factor](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-quant/im-quant-factor/)，是 `im-quant` 聚合 POM 当前唯一的子模块。

---

## 当前状态

**预留模块，目前仅包含 `pom.xml`，尚无源代码。** `src/main/java` 与 `src/test/java` 目录尚未创建，未实现任何 Java 类。

### pom.xml 关键信息（见 [pom.xml](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-quant/im-quant-factor/pom.xml)）

| 属性 | 值 |
| --- | --- |
| groupId | `org.wall.im` |
| artifactId | `im-quant-factor` |
| version | `${revision}`（由父 POM 定义为 `1.0.0-Beta0-SNAPSHOT`） |
| packaging | 默认 `jar`（未显式声明） |
| parent | `org.wall.im:im-quant:${revision}`（祖父 POM 为 `im-parent`） |
| maven.compiler.source / target | `26` |

> 模块层级：`im-parent` → `im-quant`（packaging=pom，聚合 `im-quant-factor`）→ `im-quant-factor`。

当前 `pom.xml` 未声明任何 `<dependencies>`，仅设置了编译器版本与编码（UTF-8）。

---

## 后续规划

依据项目 [README.md](file:///d:/IdeaProject/InvestmentMarket/README.md) 中"市场趋势预测、投资组合优化"的定位，以及 [im-ai/Design.md](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/Design.md) 的高内聚低耦合原则，`im-quant-factor` 预期承载：

- **因子定义**：技术因子（动量、波动率、换手率）、基本面因子（估值、成长性、质量因子）、风险因子（Barra 风格因子）等抽象与元数据
- **因子计算引擎**：基于行情/财务数据批量计算因子值，支持横截面与时序计算
- **因子库与注册表**：因子的注册、查找与版本管理
- **因子输出与导出**：以标准化结构（DataFrame/记录）输出因子矩阵，供组合优化、回测等下游使用
- **与 `im-core` 协作**：复用领域模型（标的、行情快照），将计算结果回写领域服务

预期依赖 `im-base`、`im-common` 与 `im-core`，但不依赖具体的 Web/应用层，保持计算内核的独立性。

---

## Maven 坐标

```xml
<dependency>
    <groupId>org.wall.im</groupId>
    <artifactId>im-quant-factor</artifactId>
    <version>1.0.0-Beta0-SNAPSHOT</version>
</dependency>
```

> 由于当前无源码，引入该依赖暂不会提供任何 API；待因子计算逻辑落地后方可使用。
