← 返回索引

# 模块概述

`im-observation` 是 InvestmentMarket 项目中的一个 **Spring Boot 应用模块**，定位为聚合观测/监控能力的独立运行器（standalone runner）。它是 `im-ai-observation` 监控技术栈的承载入口，旨在将 AI 智能体运行过程中的链路追踪、指标采集与可观测性能力以独立进程的方式对外暴露。

当前阶段，该模块仅包含最小化的 Spring Boot 启动骨架（启动类 + 基础配置 + 上下文加载测试），后续将逐步接入 `im-ai-observation` 提供的各类监控实现。

---

## Maven 坐标与依赖

模块位于 [im-parent/im-observation](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-observation/)，继承自 `spring-boot-starter-parent 4.1.0`。

| 属性 | 值 |
| --- | --- |
| groupId | `org.wall.im` |
| artifactId | `im-observation` |
| version | `0.0.1-SNAPSHOT` |
| parent | `org.springframework.boot:spring-boot-starter-parent:4.1.0` |
| java.version | `26` |
| name | `im-observation` |

### 关键依赖

当前依赖非常精简（见 [pom.xml](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-observation/pom.xml)）：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

构建插件仅包含 `spring-boot-maven-plugin`，用于打包可执行 jar。`im-ai-observation` 及相关监控依赖尚未引入，留待后续接入。

---

## 源码索引

| 类型 | 路径 |
| --- | --- |
| 启动类 | [ImObservationApplication.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-observation/src/main/java/org/wall/im/imobservation/ImObservationApplication.java) |
| 测试类 | [ImObservationApplicationTests.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-observation/src/test/java/org/wall/im/imobservation/ImObservationApplicationTests.java) |
| 配置文件 | [application.properties](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-observation/src/main/resources/application.properties) |
| Maven POM | [pom.xml](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-observation/pom.xml) |