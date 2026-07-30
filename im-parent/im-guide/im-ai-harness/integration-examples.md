# im-ai-harness 典型使用示例

← [返回索引](../README.md)

## 六、典型使用示例

### 6.1 串行管道：过滤 + 路由，再串行运行 Agent A → Agent B

```java
import org.wall.im.ai.core.agent.Agent;
import org.wall.im.ai.core.model.Message;
import org.wall.im.ai.harness.component.MessageFilterComponent;
import org.wall.im.ai.harness.component.MessageRouterComponent;
import org.wall.im.ai.harness.pipeline.FunctionalStage;
import org.wall.im.ai.harness.pipeline.MessagePipeline;
import org.wall.im.ai.harness.runner.SequentialRunner;

import java.util.List;

// 1) 准备可复用组件
MessageFilterComponent filter = new MessageFilterComponent(8000, true);

MessageRouterComponent router = new MessageRouterComponent()
        .addRule(new MessageRouterComponent.RouteRule() {
            @Override
            public boolean matches(Message message) {
                return message.getContent() != null
                        && message.getContent().startsWith("/summary");
            }
            @Override
            public List<Message> process(Message message) {
                // 命中 /summary 指令时，改写为摘要任务消息
                return List.of(new Message("user",
                        "请摘要以下内容: " + message.getContent()));
            }
        });

// 2) 用 Pipeline 做消息预处理（过滤 → 路由）
MessagePipeline preprocess = new MessagePipeline("agent-a-preprocess")
        .addStage(new FunctionalStage("filter", filter::execute))
        .addStage(new FunctionalStage("route",  router::execute));

List<Message> messages = List.of(new Message("user", "/summary 长文本..."));
List<Message> prepared = preprocess.process(messages);

// 3) 串行运行 Agent A，再把其输出作为 Agent B 的输入
SequentialRunner runner = new SequentialRunner();
AgentResult ra = runner.run(agentA, prepared);

List<Message> nextInput = List.of(new Message("assistant", ra.getOutput()));
AgentResult rb = runner.run(agentB, nextInput);
```

> 说明：`FunctionalStage` 接收 `Function<List<Message>, List<Message>`。上例用方法引用 `filter::execute` / `router::execute` 直接把组件的 `execute(Agent, List<Message>)` 适配为"忽略 Agent"的函数——注意签名匹配：`MessageFilterComponent.execute` 的第一个参数是 `Agent`，因此更严谨的写法是 `msgs -> filter.execute(null, msgs)`。下面给出与签名严格一致的写法：

```java
.addStage(new FunctionalStage("filter", msgs -> filter.execute(null, msgs)))
.addStage(new FunctionalStage("route",  msgs -> router.execute(null, msgs)));
```

### 6.2 并行运行器：扇出 / 扇入

```java
import org.wall.im.ai.harness.runner.ParallelRunner;
import java.util.List;
import java.util.concurrent.Executors;

ParallelRunner parallel = new ParallelRunner(
        Executors.newFixedThreadPool(4));   // 限制并发 4

List<Agent> analysts = List.of(bullAgent, bearAgent, neutralAgent);
List<Message> query = List.of(new Message("user", "分析今日市场走势"));

// 扇出：三个 Agent 并发处理同一批消息；扇入：输出拼接、token 求和
AgentResult combined = parallel.runParallel(analysts, query);

System.out.println(combined.getOutput());
System.out.println("total tokens = " + combined.getTokenUsage()
        + ", cost ms = " + combined.getCostTimeMs());
```

- 单任务超时上限 60 秒（`future.get(60, TimeUnit.SECONDS)`）；任一 Agent 失败会在输出中体现为 `[ERROR] ...`，不会中断其他 Agent。
- 合并结果的 `success` 恒为 `true`（按源码实现），调用方需自行解析输出中是否存在 `[ERROR]`。

### 6.3 记忆增强 + 单 Agent 执行

```java
import org.wall.im.ai.harness.component.MemoryAugmentComponent;

MemoryAugmentComponent memoryAugment =
        new MemoryAugmentComponent(memoryStore, 10);   // 注入最近 10 条

List<Message> withMemory = memoryAugment.execute(agent, currentInput);
AgentResult result = new SequentialRunner().run(agent, withMemory);
```