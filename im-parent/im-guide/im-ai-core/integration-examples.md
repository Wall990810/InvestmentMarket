# ← 返回索引

# 典型使用方式

下面通过若干小示例展示如何基于本模块的接口与配置模型进行开发。示例仅演示 API 用法，不包含具体 LLM 调用实现。

## 1. 实现 Tool

```java
public class MarketDataTool implements Tool {

    @Override
    public String getName() {
        return "market-data-api";
    }

    @Override
    public String getDescription() {
        return "查询股票/指数实时行情数据";
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        // 遵循 JSON Schema 格式描述参数
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        Map<String, Object> properties = new HashMap<>();
        properties.put("symbol", Map.of("type", "string", "description", "股票代码"));
        properties.put("market", Map.of("type", "string", "description", "市场"));
        schema.put("properties", properties);
        schema.put("required", List.of("symbol"));
        return schema;
    }

    @Override
    public String execute(Map<String, Object> parameters) {
        String symbol = (String) parameters.get("symbol");
        // 实际调用行情接口...
        return "{\"symbol\":\"" + symbol + "\",\"price\":102.5}";
    }
}
```

## 2. 实现 Skill

```java
public class StockLookupSkill implements Skill {

    @Override
    public String getName() {
        return "stock-lookup";
    }

    @Override
    public String getDescription() {
        return "根据用户输入查询股票信息";
    }

    @Override
    public boolean canExecute(String input) {
        // 简单的关键词路由判断
        return input != null && input.contains("股票");
    }

    @Override
    public String execute(String input) {
        // 实际技能逻辑...
        return "已为查询：" + input;
    }
}
```

## 3. 实现 MemoryStore（以内存版为例）

```java
public class InMemoryStore implements MemoryStore {

    private final Map<String, List<MemoryEntry>> store = new ConcurrentHashMap<>();

    @Override
    public void store(String key, MemoryEntry entry) {
        store.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(entry);
    }

    @Override
    public void storeAll(String key, List<MemoryEntry> entries) {
        store.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).addAll(entries);
    }

    @Override
    public List<MemoryEntry> retrieve(String key) {
        return store.getOrDefault(key, List.of());
    }

    @Override
    public List<MemoryEntry> retrieveRecent(String key, int count) {
        List<MemoryEntry> all = retrieve(key);
        int from = Math.max(0, all.size() - count);
        return new ArrayList<>(all.subList(from, all.size()));
    }

    @Override
    public List<MemoryEntry> search(String key, String query) {
        return retrieve(key).stream()
                .filter(e -> e.getContent() != null && e.getContent().contains(query))
                .collect(Collectors.toList());
    }

    @Override
    public void clear(String key) {
        store.remove(key);
    }

    @Override
    public String getStoreType() {
        return "memory";
    }
}
```

## 4. 组装 AgentConfig 与 AgentContext

```java
public AgentConfig buildConfig() {
    AgentConfig config = new AgentConfig();
    config.setName("market-analyst");
    config.setDescription("投资市场分析助手");
    config.setType("chat");

    ModelConfig model = new ModelConfig();
    model.setProvider("openai");
    model.setName("gpt-4");
    model.setEndpoint("https://api.example.com/v1");
    model.setTemperature(0.3);
    model.setMaxTokens(2048);
    config.setModel(model);

    config.setSkills(List.of("stock-lookup"));
    config.setTools(List.of("market-data-api"));

    MemoryConfig memory = new MemoryConfig();
    memory.setShortTermStore("memory");
    memory.setLongTermStore("redis");
    memory.setShortTermMaxEntries(50);
    memory.setTtlSeconds(3600);
    config.setMemory(memory);

    ExecutionConfig exec = new ExecutionConfig();
    exec.setTimeoutSeconds(30);
    exec.setRetryCount(2);
    config.setExecution(exec);

    return config;
}

public AgentContext buildContext(Agent agent) {
    AgentContext ctx = new AgentContext(agent);
    ctx.registerSkill(new StockLookupSkill());
    ctx.registerTool(new MarketDataTool());
    ctx.setShortTermMemory(new InMemoryStore());
    ctx.setVariable("tenant", "default");
    return ctx;
}
```

## 5. 实现 Agent 接口（骨架）

```java
public class MarketAnalystAgent implements Agent {

    private final AgentConfig config;
    private AgentContext context;

    public MarketAnalystAgent(AgentConfig config) {
        this.config = config;
    }

    @Override
    public String getName() {
        return config.getName();
    }

    @Override
    public AgentConfig getConfig() {
        return config;
    }

    @Override
    public void initialize() {
        this.context = new AgentContext(this);
        // 注册技能 / 工具 / 记忆...
        context.registerTool(new MarketDataTool());
        context.setShortTermMemory(new InMemoryStore());
    }

    @Override
    public String chat(String input) {
        // 组装消息并调用 execute
        List<Message> messages = List.of(
                Message.system("你是一个投资市场分析助手。"),
                Message.user(input)
        );
        return execute(messages).getOutput();
    }

    @Override
    public AgentResult execute(List<Message> messages) {
        long start = System.currentTimeMillis();
        try {
            // 实际调用 LLM 与工具...
            String output = "分析结果";
            AgentResult result = AgentResult.success(output);
            result.setCostTimeMs(System.currentTimeMillis() - start);
            result.setMessageChain(messages);
            return result;
        } catch (Exception e) {
            AgentResult failure = AgentResult.failure(e.getMessage());
            failure.setCostTimeMs(System.currentTimeMillis() - start);
            return failure;
        }
    }

    @Override
    public void reset() {
        if (context != null && context.getShortTermMemory() != null) {
            context.getShortTermMemory().clear(getName());
        }
    }

    @Override
    public void destroy() {
        // 释放资源...
    }
}
```

## 6. 使用 AgentMonitor 记录链路

```java
public AgentResult executeWithMonitor(AgentMonitor monitor, String input) {
    String traceId = monitor.traceStart(getName(), input);
    long start = System.currentTimeMillis();
    try {
        String output = chat(input);
        long cost = System.currentTimeMillis() - start;
        monitor.traceEnd(traceId, getName(), output, cost, 0);
        return AgentResult.success(output);
    } catch (Exception e) {
        monitor.traceError(traceId, getName(), e.getMessage());
        return AgentResult.failure(e.getMessage());
    }
}
```

## 7. 使用 CustomMetricRegistry 上报自定义指标

```java
public void initMetrics(CustomMetricRegistry registry) {
    registry.registerCounter("agent.chat.total", "对话总次数");
    registry.registerTimer("agent.chat.latency", "对话耗时");
    registry.registerGauge("agent.active.sessions", "活跃会话数");
}

public void onChatComplete(CustomMetricRegistry registry, long latencyMs) {
    registry.incrementCounter("agent.chat.total");
    registry.recordTimer("agent.chat.latency", latencyMs);
    registry.setGaugeValue("agent.active.sessions", 12.0);
}
```

## 8. 使用 Sandbox 执行受限代码

```java
public void runInSandbox(Sandbox sandbox) {
    sandbox.initialize();
    try {
        if (!sandbox.isPathAllowed("/data/scripts")) {
            throw new IllegalStateException("路径不被沙盒允许");
        }
        SandboxResult result = sandbox.execute("print('hello')", "/data/scripts");
        if (result.isSuccess()) {
            System.out.println(result.getOutput());
        } else {
            System.err.println(result.getErrorOutput() + " (exit=" + result.getExitCode() + ")");
        }
    } finally {
        sandbox.destroy();
    }
}
```

## 扩展点

本模块作为框架的契约层，提供了丰富的扩展点供第三方接入。所有扩展均通过实现接口或继承配置模型完成，无需修改核心代码。

### 1. 实现自定义 Agent

实现 [Agent](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/agent/Agent.java) 接口，按需实现 `chat` / `execute` / 生命周期方法，并配合 `AgentConfig` 进行配置驱动。这是接入框架的最高层级扩展点。

### 2. 扩展能力：Skill 与 Tool

- **Skill**：实现 [Skill](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/skill/Skill.java)，适合粗粒度、字符串输入输出的业务能力；可重写 `canExecute` 参与技能路由。
- **Tool**：实现 [Tool](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/tool/Tool.java)，适合需要结构化参数（JSON Schema）的外部能力，便于 LLM function calling。

扩展后通过 `AgentContext.registerSkill` / `registerTool` 注册，并在 `AgentConfig.skills` / `tools` 中以名称引用。

### 3. 扩展存储：MemoryStore

实现 [MemoryStore](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/memory/MemoryStore.java) 可接入任意后端（Redis、关系数据库、向量库等）。`getStoreType()` 返回的类型字符串需与 `MemoryConfig.shortTermStore` / `longTermStore` 的取值匹配，框架据此完成实现与配置的绑定。`MemoryEntry.importance` 字段可用于实现基于重要性的记忆淘汰策略。

### 4. 扩展监控：AgentMonitor 与 CustomMetricRegistry

- 实现 [AgentMonitor](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/monitor/AgentMonitor.java) 可对接任意 Tracing / 可观测后端（Zipkin、Langfuse、SkyWalking 等），实现 `traceStart` / `traceEnd` / `traceError` / `traceToolCall` 完整链路埋点。
- 实现 [CustomMetricRegistry](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/monitor/CustomMetricRegistry.java) 可将计数器 / 仪表盘 / 定时器三类指标桥接到 Micrometer、Prometheus 等指标系统。
- `MonitorConfig.customMetrics` 与 `LangfuseConfig` 为配置层入口，供实现方读取后端连接信息。

### 5. 扩展安全沙盒：Sandbox

实现 [Sandbox](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/sandbox/Sandbox.java) 可对接 Docker、Firecracker、nsjail 等隔离运行时。约束条件由 [SandboxConfig](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/model/SandboxConfig.java) 提供：`workDir` / `allowedPaths` 限制路径，`networkAccess` 控制网络，`maxExecutionTime` / `maxMemoryMb` 限制资源。实现方应在 `execute` / `executeCommand` 中强制校验这些约束，并通过 [SandboxResult](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/sandbox/SandboxResult.java) 的静态工厂方法返回结果。

### 6. 扩展配置：properties 与 AgentsDefinition

- `AgentConfig.properties`（`Map<String, Object>`）为开放式扩展属性，下游实现可在不修改核心模型的前提下注入自定义配置。
- [AgentsDefinition](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/config/AgentsDefinition.java) 的 `defaults` 字段支持全局默认配置继承，第三方启动器可据此实现"一份 YAML 定义多 Agent"的加载机制。