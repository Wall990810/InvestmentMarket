# im-ai-harness AgentRunner 详解

← [返回索引](../README.md)

## 三、Runner 详解

Runner 负责把"调用一个 Agent"这件事抽象出来，便于在调度层替换执行策略。SPI 定义见 [AgentRunner.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-harness/src/main/java/org/wall/im/ai/harness/runner/AgentRunner.java)。

### 3.1 AgentRunner SPI

```java
public interface AgentRunner {
    AgentResult run(Agent agent, List<Message> messages);
    String getType();
}
```

- `run`：执行单个 `Agent`，入参为消息列表，返回 `AgentResult`。
- `getType`：返回运行器类型字符串，用于在工厂/配置场景下识别实现（例如 `"sequential"`、`"parallel"`）。

### 3.2 SequentialRunner（串行运行器）

源码：[SequentialRunner.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-harness/src/main/java/org/wall/im/ai/harness/runner/SequentialRunner.java)

```java
public class SequentialRunner implements AgentRunner {
    @Override
    public AgentResult run(Agent agent, List<Message> messages) {
        return agent.execute(messages);
    }
    @Override
    public String getType() { return "sequential"; }
}
```

- **行为**：直接委托给 `agent.execute(messages)`，调用线程同步执行。
- **线程模型**：单线程、阻塞，调用方线程即执行线程，无额外线程池开销。
- **适用场景**：单 Agent 调用；多 Agent 需要严格前后依赖（在前一个结果上构造下一个输入时，由调用方自行循环调用）。
- **类型标识**：`"sequential"`。

### 3.3 ParallelRunner（并行运行器）

源码：[ParallelRunner.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-harness/src/main/java/org/wall/im/ai/harness/runner/ParallelRunner.java)

`ParallelRunner` 提供两个构造器：

```java
public ParallelRunner() {
    this.executor = Executors.newCachedThreadPool();
}
public ParallelRunner(ExecutorService executor) {
    this.executor = executor;
}
```

- 默认使用 `Executors.newCachedThreadPool()`；若需控制并发上限或共享线程池，请传入自定义 `ExecutorService`。

**核心方法 `runParallel`（扇出 + 汇总）：**

```java
public AgentResult runParallel(List<Agent> agents, List<Message> messages) { ... }
```

执行流程（基于源码）：

1. 记录 `startTime`。
2. 为每个 `Agent` 提交 `executor.submit(() -> agent.execute(messages))` 任务，收集 `Future<AgentResult>`。
3. 逐个 `future.get(60, TimeUnit.SECONDS)` 获取结果：
   - 成功：把 `result.getOutput()` 追加到 `combinedOutput`，并把 `result.getTokenUsage()` 累加到 `totalTokens`。
   - 异常（含超时）：追加 `[ERROR] <message>` 到输出，不中断其他任务。
4. 构造合并后的 `AgentResult`：`success=true`、`output` 为去尾空白的拼接文本、`costTimeMs` 为总耗时、`tokenUsage` 为各 Agent token 之和。

**单 Agent 入口 `run`：** 仅委托 `agent.execute(messages)`，与 `SequentialRunner` 一致，便于统一通过 `AgentRunner` 接口调用。

- **类型标识**：`"parallel"`。
- **线程模型**：多线程并发，每个 Agent 一个任务；汇总阶段在调用方线程阻塞等待，单任务超时上限 60 秒。
- **适用场景**：多个独立 Agent 对同一批消息做扇出处理（如多视角分析、多模型投票），最终合并文本与 token。

### 3.4 何时使用哪种 Runner

| 场景 | 推荐 | 理由 |
| --- | --- | --- |
| 单 Agent 调用 | `SequentialRunner` | 无额外开销，语义最简单 |
| 多 Agent 有严格顺序依赖 | `SequentialRunner` + 外部循环 | Runner 不内置链式结果传递，需调用方手动衔接 |
| 多 Agent 互相独立、可并发 | `ParallelRunner.runParallel` | 并发执行，自动汇总输出与 token |
| 需要限制并发度 | `new ParallelRunner(customExecutor)` | 传入有界线程池控制资源 |