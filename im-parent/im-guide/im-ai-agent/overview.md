# im-ai-agent 模块概述

← 返回 [索引](../README.md)

> 适用版本：`1.0.0-Beta0-SNAPSHOT`
> 源码位置：`im-parent/im-ai/im-ai-agent`
> 父模块：`org.wall.im:im-ai`

## 1. 模块概述

`im-ai-agent` 是 InvestmentMarket AI 体系中的 **默认 Agent 运行时实现模块**。它在 `im-ai-core` 定义的抽象接口（`Agent`、`Tool`、`Skill`、`MemoryStore`）之上，提供了一整套可开箱即用的运行时组件：

| 职责 | 实现类 |
| --- | --- |
| 默认 Agent 实现（ReAct 范式） | [DefaultAgent.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/java/org/wall/im/ai/agent/lifecycle/DefaultAgent.java) |
| Agent 生命周期管理 | [AgentLifecycleManager.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/java/org/wall/im/ai/agent/lifecycle/AgentLifecycleManager.java)、[AgentLifecycleListener.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/java/org/wall/im/ai/agent/lifecycle/AgentLifecycleListener.java) |
| Agent 装配工厂 | [AgentFactory.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/java/org/wall/im/ai/agent/lifecycle/AgentFactory.java) |
| 注册中心 | [AgentRegistry.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/java/org/wall/im/ai/agent/registry/AgentRegistry.java)、[SkillRegistry.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/java/org/wall/im/ai/agent/lifecycle/SkillRegistry.java)、[ToolRegistry.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/java/org/wall/im/ai/agent/lifecycle/ToolRegistry.java)、[MemoryStoreFactory.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/java/org/wall/im/ai/agent/lifecycle/MemoryStoreFactory.java) |
| 配置解析器 | [AgentConfigParser.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/java/org/wall/im/ai/agent/config/AgentConfigParser.java) |
| Spring AI 工具适配器 | [SpringAiToolAdapter.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/java/org/wall/im/ai/agent/adapter/SpringAiToolAdapter.java) |
| Markdown 技能加载器 | [MarkdownSkill.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/java/org/wall/im/ai/agent/skill/MarkdownSkill.java)、[MarkdownSkillLoader.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/java/org/wall/im/ai/agent/skill/MarkdownSkillLoader.java)、[MarkdownSkillAutoConfiguration.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/java/org/wall/im/ai/agent/skill/MarkdownSkillAutoConfiguration.java)、[MarkdownSkillProperties.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/java/org/wall/im/ai/agent/skill/MarkdownSkillProperties.java) |

底层推理引擎采用 **Spring AI Alibaba** 的 `ReactAgent`（ReAct：Reasoning + Acting），通过 `SpringAiToolAdapter` 将自定义 `Tool` 接口桥接为 Spring AI 的 `ToolCallback`，从而让本模块的工具体系与 Alibaba Agent 框架无缝协作。

## 2. Maven 坐标与依赖

### 2.1 引入依赖

在业务模块的 `pom.xml` 中加入：

```xml
<dependency>
    <groupId>org.wall.im</groupId>
    <artifactId>im-ai-agent</artifactId>
    <version>1.0.0-Beta0-SNAPSHOT</version>
</dependency>
```

### 2.2 传递依赖

`im-ai-agent` 自身已声明的关键依赖（参见 [pom.xml](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-agent/pom.xml)）：

| 依赖 | 版本 | 作用 |
| --- | --- | --- |
| `org.wall.im:im-ai-core` | `1.0.0-Beta0-SNAPSHOT` | 提供 `Agent`/`Tool`/`Skill`/`MemoryStore` 等核心接口与配置模型 |
| `org.springframework.boot:spring-boot-autoconfigure` | `3.5.15` | 支持自动装配 |
| `com.alibaba.cloud.ai:spring-ai-alibaba-agent-framework` | `1.1.2.0` | 提供 `ReactAgent` ReAct 推理引擎 |
| `com.alibaba.cloud.ai:spring-ai-alibaba-starter-dashscope` | `1.1.2.0` | 提供阿里云 DashScope `ChatModel` |
| `org.slf4j:slf4j-api` | `2.0.13` | 日志门面 |

> 说明：模块未直接传递 `ChatModel` 的具体实现 Bean。实际使用时需确保 Spring 上下文中存在一个 `ChatModel` Bean（如 DashScope starter 自动注册的 `DashScopeChatModel`）。