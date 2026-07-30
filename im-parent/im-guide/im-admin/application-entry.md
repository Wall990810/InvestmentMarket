# ← 返回索引

# 应用入口

[AdminApplication.java](file:///d:/IdeaProject/InvestmentMarket/im-admin/src/main/java/org/wall/im/admin/AdminApplication.java) 是标准的 Spring Boot 启动类：

```java
@SpringBootApplication
public class AdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdminApplication.class, args);
    }
}
```

启动后即触发 [AiAgentConfig](file:///d:/IdeaProject/InvestmentMarket/im-admin/src/main/java/org/wall/im/admin/config/AiAgentConfig.java) 中的 Agent 装配流程。

---

# 配置类 AiAgentConfig

[AiAgentConfig.java](file:///d:/IdeaProject/InvestmentMarket/im-admin/src/main/java/org/wall/im/admin/config/AiAgentConfig.java) 是整个智能体的装配中枢，负责加载 YAML 配置、注册 Skill/Tool、创建并管理 Agent 实例。它声明了以下 Bean：

| Bean | 作用 |
| --- | --- |
| `agentConfigParser()` | `AgentConfigParser` 实例，用于解析 `agents/investment-advisor.yml` |
| `agentRegistry()` | `AgentRegistry`，Agent 注册表（按 name 查找 Agent） |
| `skillRegistry(...)` | `SkillRegistry`，注册 Java 实现（`InvestmentAnalysisSkill`、`PortfolioRecommendSkill`），并通过 `MarkdownSkillLoader.loadFromClasspath("skills")` 从 classpath 加载 Markdown 技能 |
| `markdownSkillLoader(...)` | `MarkdownSkillLoader`，从 classpath/文件系统加载 `.md` 技能 |
| `toolRegistry()` | `ToolRegistry`，注册 `MarketDataTool` 与 `RiskAssessmentTool` |
| `memoryStoreFactory()` | `MemoryStoreFactory`，默认返回基于 `ConcurrentHashMap` 的内存存储实现（可替换为 Redis/DB） |
| `dashScopeApi(...)` | `DashScopeApi` 客户端，API Key 取自 `spring.ai.dashscope.api-key` 或环境变量 `AI_DASHSCOPE_API_KEY` |
| `chatModel(...)` | `DashScopeChatModel`（通义千问），作为 Agent 的"大脑"提供推理能力；默认模型 `qwen-plus`、温度 `0.7`、`maxTokens 4096` |
| `agentFactory(...)` | `AgentFactory`，注入 Skill/Tool/Memory/ChatModel，用于创建基于 ReactAgent 的智能体实例 |
| `agentLifecycleManager(...)` | `AgentLifecycleManager`，管理 Agent 生命周期 |
| `investmentAgentsDefinition(...)` | **核心入口**：调用 `parser.parseFromClasspath("agents/investment-advisor.yml")` 加载配置，再调用 `lifecycleManager.createAgents(definition)` 创建并注册所有 Agent |

启动日志会输出："成功加载投资建议Agent配置，共 N 个Agent" 以及 "成功创建并注册投资建议Agent实例"。