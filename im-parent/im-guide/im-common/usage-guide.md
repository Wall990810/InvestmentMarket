# im-common 使用指南

## 模块概述

`im-common` 是 InvestmentMarket 项目中的**跨模块通用工具与常量模块**。与 `im-base` 侧重底层支撑不同，`im-common` 定位为业务侧的通用共享层，沉淀跨业务模块复用的工具类、常量、通用 DTO/异常等，避免各业务模块重复造轮子。

---

## 当前状态

**预留模块，目前仅包含 `pom.xml`，尚无源代码。** `src/main/java` 与 `src/test/java` 目录尚未创建，未实现任何 Java 类。

### pom.xml 关键信息（见 [pom.xml](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-common/pom.xml)）

| 属性 | 值 |
| --- | --- |
| groupId | `org.wall.im` |
| artifactId | `im-common` |
| version | `${revision}`（由父 POM 定义为 `1.0.0-Beta0-SNAPSHOT`） |
| packaging | 默认 `jar`（未显式声明） |
| parent | `org.wall.im:im-parent:${revision}` |
| maven.compiler.source / target | `26` |

当前 `pom.xml` 未声明任何 `<dependencies>` 与 `<modules>`，仅设置了编译器版本与编码（UTF-8）。

---

## 后续规划

依据项目 [README.md](file:///d:/IdeaProject/InvestmentMarket/README.md) 中"智能投资应用"的定位，以及 [im-ai/Design.md](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/Design.md) 强调的高内聚低耦合原则，`im-common` 预期承载：

- **业务通用常量**：市场标识（A股/港股/美股）、资产类别、风险等级等枚举
- **通用 DTO / VO**：跨模块传输对象，如统一的分页响应、API 返回包装
- **通用异常与错误码**：业务异常基类与统一错误码定义
- **跨模块工具**：金融数据格式化、日期处理（交易日/交易日历）、金额计算等与业务相关但跨模块复用的工具
- **通用注解与切面**：如日志、参数校验等横切关注点

`im-common` 可依赖 `im-base`，但应避免依赖具体业务实现模块，以保持通用性。

---

## Maven 坐标

```xml
<dependency>
    <groupId>org.wall.im</groupId>
    <artifactId>im-common</artifactId>
    <version>1.0.0-Beta0-SNAPSHOT</version>
</dependency>
```

> 由于当前无源码，引入该依赖暂不会提供任何 API；待源码落地后方可使用。
