# Spring AI 工具适配器详解

← 返回 [索引](../README.md)

## 6. Spring AI 工具适配器

[SpringAiToolAdapter.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/java/org/wall/im/ai/agent/adapter/SpringAiToolAdapter.java) 把自定义 `org.wall.im.ai.core.tool.Tool` 桥接为 Spring AI 的 `org.springframework.ai.tool.ToolCallback`，使 Alibaba `ReactAgent` 能够识别并调用本模块定义的工具。

### 6.1 适配过程

`toToolCallback(Tool tool)` 流程：

1. 以 `tool.getName()` 作为函数名构造 `FunctionToolCallback.builder(name, function)`。
2. `function` 接收一个 JSON 字符串入参，用 `ObjectMapper` 反序列化为 `Map<String, Object>`，再调用 `tool.execute(parameters)`。
3. 入参解析失败或工具执行抛异常时，返回形如 `"Error: ..."` 的字符串（不抛出，避免打断 Agent 循环）。
4. `.description(tool.getDescription())` 设置工具描述。
5. `.inputType(String.class)` 声明入参类型。
6. `.build()` 返回 `ToolCallback`。

### 6.2 批量转换

```java
public static ToolCallback[] toToolCallbacks(Tool... tools)
```

`DefaultAgent.initialize()` 即通过该方法一次性把所有自定义工具注册到 `ReactAgent`。

### 6.3 单独使用示例

```java
Tool myTool = new MyCustomTool();
ToolCallback callback = SpringAiToolAdapter.toToolCallback(myTool);
// 可直接喂给任意接受 ToolCallback 的 Spring AI 组件
```