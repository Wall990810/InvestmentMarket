# im-ai-harness Harness 组件详解

← [返回索引](../README.md)

## 五、Harness 组件

### 5.1 HarnessComponent SPI

源码：[HarnessComponent.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-harness/src/main/java/org/wall/im/ai/harness/component/HarnessComponent.java)

```java
public interface HarnessComponent {
    String getName();
    String getDescription();
    void initialize();
    List<Message> execute(Agent agent, List<Message> input);
    void destroy();
}
```

- 生命周期：`initialize()`（初始化）→ 多次 `execute()`（业务调用）→ `destroy()`（销毁）。
- `execute` 携带关联的 `Agent` 上下文，可读取 `agent.getName()` 等信息（如 `MemoryAugmentComponent` 用 Agent 名作为记忆键）。
- 与 `PipelineStage` 的区别：组件面向"Agent 协作"语义，带有生命周期与 Agent 关联；Pipeline 面向"纯消息加工"。

### 5.2 内置组件一览

| 组件 | getName() | 作用 |
| --- | --- | --- |
| `MessageRouterComponent` | `message-router` | 按规则把消息路由到不同处理逻辑 |
| `MessageFilterComponent` | `message-filter` | 去空、截断过长内容 |
| `MemoryAugmentComponent` | `memory-augment` | 在输入前注入历史记忆上下文 |

### 5.3 MessageRouterComponent

源码：[MessageRouterComponent.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-harness/src/main/java/org/wall/im/ai/harness/component/MessageRouterComponent.java)

- 通过 `addRule(RouteRule rule)` 链式注册路由规则。`RouteRule` 是组件内部的嵌套接口：

  ```java
  public interface RouteRule {
      boolean matches(Message message);
      List<Message> process(Message message);
  }
  ```

- `execute` 逻辑：**双层遍历**——外层遍历规则，内层遍历输入消息；一旦某条规则对某条消息 `matches` 返回 `true`，立即调用 `rule.process(msg)` 并返回其结果，**不再继续匹配**。
- 若所有规则都未命中，原样返回 `input`。
- 配置要点：规则有优先级（先注册先匹配）；`process` 返回的是该单条消息处理后的结果列表（可拆分/替换）。

### 5.4 MessageFilterComponent

源码：[MessageFilterComponent.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-harness/src/main/java/org/wall/im/ai/harness/component/MessageFilterComponent.java)

- 构造器：
  ```java
  public MessageFilterComponent();                                  // 默认 maxMessageLength=10000, removeEmpty=true
  public MessageFilterComponent(int maxMessageLength, boolean removeEmpty);
  ```
- `execute` 逻辑：
  - `removeEmpty=true` 时，跳过 `content` 为 `null` 或空白（`isBlank()`）的消息。
  - 当 `content.length() > maxMessageLength` 时，截断为前 `maxMessageLength` 个字符并追加 `...[truncated]`。
  - 返回过滤后的新列表。
- 适用场景：防止超长 prompt、过滤噪声空消息。

### 5.5 MemoryAugmentComponent

源码：[MemoryAugmentComponent.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-harness/src/main/java/org/wall/im/ai/harness/component/MemoryAugmentComponent.java)

- 构造器：
  ```java
  public MemoryAugmentComponent(MemoryStore memoryStore, int contextWindowSize);
  ```
- `execute` 逻辑：
  1. 当 `memoryStore != null` 时，调用 `memoryStore.retrieveRecent(agent.getName() + ":conversation", contextWindowSize)` 检索最近 `contextWindowSize` 条记忆。
  2. 将每条 `MemoryEntry` 转换为 `new Message(entry.getRole(), entry.getContent())`，按检索顺序放在结果列表前面。
  3. 追加当前 `input` 消息，形成"历史记忆 + 当前输入"的合并列表。
- 配置要点：
  - 记忆键约定为 `"<agentName>:conversation"`，存储侧需使用同一键写入才能被检索到。
  - `contextWindowSize` 控制注入的历史条数，直接影响 token 预算，需结合模型上下文窗口合理设置。
  - `memoryStore` 为 `null` 时等价于不增强（仅返回当前输入）。