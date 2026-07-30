# im-ai-harness 扩展指南

← [返回索引](../README.md)

## 七、扩展自定义组件

### 7.1 自定义 HarnessComponent

实现 `HarnessComponent` 接口即可，注意完整实现生命周期方法：

```java
public class PromptGuardComponent implements HarnessComponent {

    private final List<String> blockedWords;

    public PromptGuardComponent(List<String> blockedWords) {
        this.blockedWords = blockedWords;
    }

    @Override
    public String getName() { return "prompt-guard"; }

    @Override
    public String getDescription() { return "拦截包含敏感词的输入"; }

    @Override
    public void initialize() { /* 可空实现：预热/校验配置 */ }

    @Override
    public List<Message> execute(Agent agent, List<Message> input) {
        List<Message> safe = new ArrayList<>();
        for (Message msg : input) {
            String c = msg.getContent();
            if (c == null) { safe.add(msg); continue; }
            boolean hit = blockedWords.stream().anyMatch(c::contains);
            if (!hit) safe.add(msg);
        }
        return safe;
    }

    @Override
    public void destroy() { /* 可空实现：释放资源 */ }
}
```

使用：

```java
PromptGuardComponent guard = new PromptGuardComponent(List.of("secret", "password"));
List<Message> safe = guard.execute(agent, rawInput);
```

### 7.2 自定义 PipelineStage

除 `FunctionalStage` 外，也可直接实现 `PipelineStage`：

```java
public class DedupStage implements PipelineStage {
    @Override public String getName() { return "dedup"; }
    @Override
    public List<Message> execute(List<Message> messages) {
        Map<String, Message> uniq = new LinkedHashMap<>();
        for (Message m : messages) {
            uniq.putIfAbsent(m.getContent(), m);
        }
        return new ArrayList<>(uniq.values());
    }
}
```

### 7.3 自定义 AgentRunner

实现 `AgentRunner` 接口，提供 `run` 与 `getType`，即可接入调度层。例如带重试的运行器：

```java
public class RetryRunner implements AgentRunner {
    private final int maxRetries;
    public RetryRunner(int maxRetries) { this.maxRetries = maxRetries; }

    @Override
    public AgentResult run(Agent agent, List<Message> messages) {
        AgentResult result = null;
        for (int i = 0; i <= maxRetries; i++) {
            result = agent.execute(messages);
            if (result.isSuccess()) return result;
        }
        return result;
    }

    @Override
    public String getType() { return "retry"; }
}
```