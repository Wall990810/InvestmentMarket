# 典型使用流程

← 返回 [索引](../README.md)

## 11. 典型使用流程

下面示例展示一条端到端的使用路径：注册自定义 Tool/Skill → 从 `agents.yml` 引导 Agent → 调用 Agent。

```java
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.wall.im.ai.agent.config.AgentConfigParser;
import org.wall.im.ai.agent.lifecycle.AgentFactory;
import org.wall.im.ai.agent.lifecycle.AgentLifecycleManager;
import org.wall.im.ai.agent.lifecycle.MemoryStoreFactory;
import org.wall.im.ai.agent.lifecycle.SkillRegistry;
import org.wall.im.ai.agent.lifecycle.ToolRegistry;
import org.wall.im.ai.agent.registry.AgentRegistry;
import org.wall.im.ai.agent.skill.MarkdownSkillLoader;
import org.wall.im.ai.core.config.AgentsDefinition;
import org.wall.im.ai.core.memory.MemoryStore;
import org.wall.im.ai.core.tool.Tool;

import java.util.List;
import java.util.Map;

@Configuration
public class AgentBootstrapConfig {

    /** 1. 注册中心 Bean（MarkdownSkillAutoConfiguration 依赖 SkillRegistry） */
    @Bean
    public SkillRegistry skillRegistry() {
        return new SkillRegistry();
    }

    @Bean
    public ToolRegistry toolRegistry() {
        return new ToolRegistry();
    }

    @Bean
    public AgentRegistry agentRegistry() {
        return new AgentRegistry();
    }

    /** 2. MemoryStoreFactory：示例仅支持内存型 */
    @Bean
    public MemoryStoreFactory memoryStoreFactory() {
        return storeType -> {
            // 真实场景按 storeType 选择 memory/redis/db 实现
            return new InMemoryMemoryStore();
        };
    }

    /** 3. 自定义工具：实现 org.wall.im.ai.core.tool.Tool */
    public static class MarketDataTool implements Tool {
        @Override public String getName() { return "market-data-tool"; }
        @Override public String getDescription() { return "查询市场行情数据"; }
        @Override public String execute(Map<String, Object> parameters) {
            return "{ \"price\": 100.0 }";
        }
    }

    /** 4. 把自定义工具注册到 ToolRegistry */
    @Bean
    public Tool marketDataTool(ToolRegistry toolRegistry) {
        Tool tool = new MarketDataTool();
        toolRegistry.register(tool);
        return tool;
    }

    /** 5. AgentFactory + 生命周期管理器 */
    @Bean
    public AgentFactory agentFactory(SkillRegistry skillRegistry,
                                     ToolRegistry toolRegistry,
                                     MemoryStoreFactory memoryStoreFactory,
                                     ChatModel chatModel) {
        return new AgentFactory(skillRegistry, toolRegistry, memoryStoreFactory, chatModel);
    }

    @Bean
    public AgentLifecycleManager agentLifecycleManager(AgentRegistry agentRegistry,
                                                       AgentFactory agentFactory) {
        return new AgentLifecycleManager(agentRegistry, agentFactory);
    }

    /**
     * 6. 启动入口：解析 agents.yml，应用 defaults，批量创建并初始化 Agent。
     *    注意：Markdown 技能由 MarkdownSkillLoaderInitializer 在 ApplicationReadyEvent 加载，
     *    若希望此处 createAgents 时技能已就绪，可改为在 ApplicationReadyEvent 之后触发，
     *    或显式调用 markdownSkillLoader.loadFromClasspath("skills")。
     */
    @Bean
    public AgentBootstrap agentBootstrap(AgentLifecycleManager lifecycleManager,
                                         MarkdownSkillLoader markdownSkillLoader) {
        return new AgentBootstrap(lifecycleManager, markdownSkillLoader);
    }

    public static class AgentBootstrap {
        private final AgentLifecycleManager lifecycleManager;
        private final MarkdownSkillLoader markdownSkillLoader;

        public AgentBootstrap(AgentLifecycleManager lifecycleManager,
                              MarkdownSkillLoader markdownSkillLoader) {
            this.lifecycleManager = lifecycleManager;
            this.markdownSkillLoader = markdownSkillLoader;
        }

        public void start() {
            // 先加载 Markdown 技能（确保 AgentFactory.create 时能解析到 skills）
            markdownSkillLoader.loadFromClasspath("skills");

            // 解析 agents.yml 并应用默认配置
            AgentConfigParser parser = new AgentConfigParser();
            AgentsDefinition definition = parser.parseFromClasspath("agents.yml");
            AgentConfigParser.applyDefaults(definition);

            // 批量创建并初始化（同时注册到 AgentRegistry）
            lifecycleManager.createAgents(definition);
            lifecycleManager.startAll();
        }
    }
}
```

业务侧调用：

```java
@Autowired
private AgentRegistry agentRegistry;

public String chat(String agentName, String userInput) {
    return agentRegistry.getRequired(agentName).chat(userInput);
}
```

### 11.1 注意事项

1. **`ChatModel` Bean**：模块不自动注册 `ChatModel`，需通过 `spring-ai-alibaba-starter-dashscope` 等方式提供。
2. **技能加载时序**：`MarkdownSkillLoaderInitializer` 在 `ApplicationReadyEvent` 触发；若你的 `createAgents` 也监听同一事件且需要技能已就绪，请显式调用 `markdownSkillLoader.loadFromClasspath(...)`，或保证监听器顺序。
3. **工具未注册**：`AgentFactory` 找不到 tool/skill 时仅打印 warn 日志，不会失败；请检查 `agents.yml` 中的 name 与注册中心的 name 是否一致。
4. **同名 Agent 替换**：`AgentRegistry.register` 会先 `destroy()` 同名旧实例，注意避免在旧实例仍在使用时重复注册。
5. **`maxConcurrency` 含义**：在 `DefaultAgent` 中被复用为 `ReactAgent` 的 `recursionLimit`（封顶 20），并非传统意义的并发度。