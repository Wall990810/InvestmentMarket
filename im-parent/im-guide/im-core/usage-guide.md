# im-core 使用指南

## 模块概述

`im-core` 是 InvestmentMarket 项目中的**业务领域核心模型与领域服务模块**。从命名与分层约定看，它承载金融投资领域的核心领域模型（实体、值对象、领域服务），是上层业务应用（如 `im-admin`、`im-observation`）与下层量化模块（`im-quant`）共享的领域内核。

---

## 当前状态

**预留模块，目前仅包含 `pom.xml`，尚无源代码。** `src/main/java` 与 `src/test/java` 目录尚未创建，未实现任何 Java 类。

### pom.xml 关键信息（见 [pom.xml](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-core/pom.xml)）

| 属性 | 值 |
| --- | --- |
| groupId | `org.wall.im` |
| artifactId | `im-core` |
| version | `${revision}`（由父 POM 定义为 `1.0.0-Beta0-SNAPSHOT`） |
| packaging | 默认 `jar`（未显式声明） |
| parent | `org.wall.im:im-parent:${revision}` |
| maven.compiler.source / target | `26` |

当前 `pom.xml` 未声明任何 `<dependencies>` 与 `<modules>`，仅设置了编译器版本与编码（UTF-8）。

---

## 后续规划

依据项目 [README.md](file:///d:/IdeaProject/InvestmentMarket/README.md) 中"市场趋势预测、投资组合优化"的定位，以及 [im-ai/Design.md](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/Design.md) 的模块划分原则，`im-core` 预期承载：

- **领域模型**：标的（股票/基金/债券）、投资组合、持仓、行情快照等核心实体与值对象
- **领域服务**：组合构建、风险评级、资产配置等核心业务逻辑
- **领域事件**：组合再平衡、标的入池/出池等事件定义
- **仓储接口**：与持久化无关的 Repository 接口契约（具体实现在应用层）
- **业务规则与策略**：风险偏好匹配策略、再平衡阈值规则等

`im-core` 作为领域内核，预期依赖 `im-base` 与 `im-common`，但不依赖 Web/持久化等基础设施，保持领域纯净。

---

## Maven 坐标

```xml
<dependency>
    <groupId>org.wall.im</groupId>
    <artifactId>im-core</artifactId>
    <version>1.0.0-Beta0-SNAPSHOT</version>
</dependency>
```

> 由于当前无源码，引入该依赖暂不会提供任何 API；待源码落地后方可使用。
