# im-base 使用指南

## 模块概述

`im-base` 是 InvestmentMarket 项目中的**基础工具类与底层支撑模块**。从命名与项目分层约定来看，它承担跨模块复用的底层工具能力，为上层业务模块（如 `im-core`、`im-quant` 等）提供通用的基础类库支撑。

---

## 当前状态

**预留模块，目前仅包含 `pom.xml`，尚无源代码。** 该模块的 `src/main/java` 与 `src/test/java` 目录尚未创建，未实现任何 Java 类。

### pom.xml 关键信息（见 [pom.xml](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-base/pom.xml)）

| 属性 | 值 |
| --- | --- |
| groupId | `org.wall.im` |
| artifactId | `im-base` |
| version | `${revision}`（由父 POM 定义为 `1.0.0-Beta0-SNAPSHOT`） |
| packaging | 默认 `jar`（未显式声明） |
| parent | `org.wall.im:im-parent:${revision}` |
| maven.compiler.source / target | `26` |

当前 `pom.xml` 未声明任何 `<dependencies>` 与 `<modules>`，仅设置了编译器版本与编码（UTF-8）。

---

## 后续规划

依据项目 [README.md](file:///d:/IdeaProject/InvestmentMarket/README.md) 对"开源金融数据分析平台"的定位，以及 [im-ai/Design.md](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/Design.md) 中"高内聚、低耦合"的模块设计原则，`im-base` 预期承载：

- **通用工具类**：字符串、日期、数值、集合等基础工具方法
- **基础常量与枚举**：全项目共享的业务无关常量
- **底层支撑类**：异常基类、通用校验工具、IO/资源处理辅助类等
- **类型转换与序列化基础**：供上层模块统一复用

作为最底层的依赖，`im-base` 不应依赖业务模块，保持轻量与稳定。

---

## Maven 坐标

```xml
<dependency>
    <groupId>org.wall.im</groupId>
    <artifactId>im-base</artifactId>
    <version>1.0.0-Beta0-SNAPSHOT</version>
</dependency>
```

> 由于当前无源码，引入该依赖暂不会提供任何 API；待源码落地后方可使用。
