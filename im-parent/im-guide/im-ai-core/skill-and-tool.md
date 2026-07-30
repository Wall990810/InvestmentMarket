# ← 返回索引

# Skill / Tool 接口详解

## Skill —— 技能接口

源码：[Skill.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/skill/Skill.java)

技能是 Agent 可被调用的能力单元，一个 Agent 可拥有多个技能。技能以字符串作为输入输出，粒度较粗，适合表达"高层业务能力"。

| 方法签名 | 说明 |
| --- | --- |
| `String getName()` | 技能名称。 |
| `String getDescription()` | 技能描述。 |
| `String execute(String input)` | 执行技能，返回字符串结果。 |
| `default boolean canExecute(String input)` | 判断当前输入是否可由本技能处理，默认返回 `true`。 |

`canExecute` 为 `default` 方法，实现类可按需重写以实现技能路由 / 前置校验。

## Tool —— 工具接口

源码：[Tool.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/tool/Tool.java)

工具是 Agent 可调用的外部能力（如 API 调用、文件操作等）。与 `Skill` 相比，`Tool` 强调结构化参数，参数 schema 以 JSON Schema 形式描述，便于 LLM 进行 function calling。

| 方法签名 | 说明 |
| --- | --- |
| `String getName()` | 工具名称。 |
| `String getDescription()` | 工具描述。 |
| `Map<String, Object> getParameterSchema()` | 参数定义，遵循 JSON Schema 格式。 |
| `String execute(Map<String, Object> parameters)` | 以参数 Map 执行工具，返回字符串结果。 |