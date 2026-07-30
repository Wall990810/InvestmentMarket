# im-ai-harness Pipeline 详解

← [返回索引](../README.md)

## 四、Pipeline 详解

Pipeline 用于在不引入 Agent 的前提下，对 `List<Message>` 做链式加工。入口类：[MessagePipeline.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-harness/src/main/java/org/wall/im/ai/harness/pipeline/MessagePipeline.java)。

### 4.1 MessagePipeline

```java
public class MessagePipeline {
    public MessagePipeline(String name);
    public MessagePipeline addStage(PipelineStage stage);   // 链式构建
    public List<Message> process(List<Message> messages);   // 执行管道
    public String getName();
    public List<PipelineStage> getStages();
}
```

- `addStage` 返回 `this`，支持链式拼装。
- `process` 的消息流：先把输入拷贝为 `current`，然后**按顺序**遍历每个 `stage`，执行 `current = stage.execute(current)`，即上一阶段的输出整批作为下一阶段的输入。
- 阶段之间是**同步、串行**调用，所有阶段共享调用方线程。

### 4.2 PipelineStage

源码：[PipelineStage.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-harness/src/main/java/org/wall/im/ai/harness/pipeline/PipelineStage.java)

```java
public interface PipelineStage {
    String getName();
    List<Message> execute(List<Message> messages);
}
```

约定：`execute` 应返回一个新的（或同一）`List<Message>`，作为下一阶段输入。实现者可在此做过滤、转换、增强、路由等任意操作。

### 4.3 FunctionalStage（函数式阶段）

源码：[FunctionalStage.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-harness/src/main/java/org/wall/im/ai/harness/pipeline/FunctionalStage.java)

```java
public class FunctionalStage implements PipelineStage {
    public FunctionalStage(String name, Function<List<Message>, List<Message>> processor);
}
```

- 通过 Lambda 快速定义一个阶段，无需新建类：
  ```java
  new FunctionalStage("trim", msgs -> msgs.stream()
          .filter(m -> m.getContent() != null && !m.getContent().isBlank())
          .toList());
  ```
- `execute` 直接委托给传入的 `processor.apply(messages)`。

### 4.4 消息流示意

```
输入 List<Message>
      │
      ▼
 [Stage A].execute(...)  ──► List<Message>'
      │
      ▼
 [Stage B].execute(...)  ──► List<Message>''
      │
      ▼
 [Stage C].execute(...)  ──► List<Message>'''   ← 最终输出
```

每个 Stage 拿到的是上一个 Stage 的整批输出，而非单条消息。若需要"逐条路由"语义，可使用下文的 `MessageRouterComponent`，或自行在 `FunctionalStage` 中展开。