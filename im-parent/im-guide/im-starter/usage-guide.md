# im-starter 使用指南

## 模块概述

`im-starter` 是 InvestmentMarket 项目中的 **Spring Boot starter 聚合模块**。其设计目标是为业务应用提供"一键引入"的能力——通过聚合项目内核心业务依赖（如 `im-core`、`im-common`、`im-base` 及相关自动配置），让接入方仅需声明一个依赖即可获得 InvestmentMarket 业务所需的全套能力，而无需逐个罗列模块。

---

## 当前状态

**预留模块，目前仅包含 `pom.xml`，尚无源代码。** `src/main/java` 目录尚未创建，未实现任何 Java 类或自动配置。

### pom.xml 关键信息（见 [pom.xml](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-starter/pom.xml)）

| 属性 | 值 |
| --- | --- |
| groupId | `org.wall.im` |
| artifactId | `im-starter` |
| version | `${revision}`（由父 POM 定义为 `1.0.0-Beta0-SNAPSHOT`） |
| packaging | **`pom`**（聚合模块，不产出 jar） |
| parent | `org.wall.im:im-parent:${revision}` |
| maven.compiler.source / target | `26` |

值得注意的两点：

1. `<packaging>pom</packaging>`：表明该模块本身不产出可执行 jar，而是作为聚合/依赖管理容器。
2. `<modules></modules>` 列表为空：当前未聚合任何子模块。

当前 `pom.xml` 未声明任何 `<dependencies>`，仅设置了编译器版本与编码（UTF-8）。

---

## 后续规划

依据项目 [README.md](file:///d:/IdeaProject/InvestmentMarket/README.md) 与 [im-ai/Design.md](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/Design.md) 的分层约定，`im-starter` 预期承载：

- **依赖聚合**：通过 `<dependencies>` 或 `dependencyManagement` 汇集 `im-base`、`im-common`、`im-core` 等业务模块，外部应用只需引入 `im-starter` 即可传递获得全部依赖
- **自动配置（Auto-Configuration）**：若后续从 `pom` 切换为 `jar` 打包，可放置 Spring Boot 自动配置类与 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`，实现条件化 Bean 装配
- **默认属性**：提供合理的默认配置（如默认数据源、默认监控端点），允许接入方通过属性覆盖
- **子模块聚合**：未来可在 `<modules>` 中纳入按场景拆分的 starter（如 `im-starter-web`、`im-starter-quant`）

> 当前 `packaging=pom` 且无源码，意味着它暂时仅充当占位符，待依赖与自动配置规划清晰后再补全。

---

## Maven 坐标

```xml
<dependency>
    <groupId>org.wall.im</groupId>
    <artifactId>im-starter</artifactId>
    <version>1.0.0-Beta0-SNAPSHOT</version>
    <type>pom</type>
</dependency>
```

> 由于当前为空聚合 POM 且无源码，引入该依赖暂不会传递任何业务依赖或提供自动配置；待源码与依赖聚合落地后方可使用。
