# Markdown 技能加载器详解

← 返回 [索引](../README.md)

## 7. Markdown 技能加载器

本模块支持通过编写 `.md` 文件快速声明一个 `Skill`，无需编写 Java 类。该机制由四个类协作完成。

### 7.1 MarkdownSkill

[MarkdownSkill.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/java/org/wall/im/ai/agent/skill/MarkdownSkill.java) 实现 `Skill` 接口，内部维护：`name`、`description`、`template`（正文模板）、`tools`（关联工具名列表）、`metadata`（front-matter 中的额外字段）。

**模板变量渲染：** 使用正则 `\{\{(\w+)\}\}` 匹配占位符，在 `execute(input)` 时按以下规则替换：

| 占位符 | 替换值 |
| --- | --- |
| `{{input}}` | 调用 `execute(input)` 时传入的 `input` |
| `{{skillName}}` | 技能 `name` |
| `{{description}}` | 技能 `description` |
| `{{其他}}` | 优先从 `metadata` 取值；不存在则保留原占位符文本 |

**`canExecute(String input)`**：当 `input` 非 null 且非空白时返回 `true`。

### 7.2 MarkdownSkillLoader

[MarkdownSkillLoader.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/java/org/wall/im/ai/agent/skill/MarkdownSkillLoader.java) 负责扫描、解析、注册 Markdown 技能。

**构造方法：**

```java
public MarkdownSkillLoader(SkillRegistry skillRegistry)
```

**加载入口：**

| 方法 | 说明 |
| --- | --- |
| `loadFromClasspath(String classpathDir)` | 扫描 classpath 下的指定目录，支持 `file:` 与 `jar:` 两种协议；逐个解析 `.md`/`.MD` 文件并注册到 `SkillRegistry`；返回加载数量 |
| `loadFromClasspath(String... classpathDirs)` | 多目录批量扫描 |
| `loadFromFileSystem(String dirPath)` | 递归遍历文件系统目录（`Files.walkFileTree`），加载其中的 `.md`/`.MD` 文件 |
| `parseSkill(String mdContent, String source)` | 解析单个 MD 文件内容，返回 `MarkdownSkill`；解析失败返回 `null` |

**`parseSkill` 解析规则：**

1. 文件必须以 `---` 开头，且存在第二个 `---` 作为 front-matter 结束分隔；否则视为无 front-matter，返回 `null`。
2. 用 YAML 解析 front-matter 段。
3. `name` 字段必填（缺失返回 `null` 并告警）。
4. `description` 可选，缺省为 `"{name} skill"`。
5. `tools` 为可选的字符串数组。
6. 除 `name`/`description`/`tools` 外的其它 front-matter 字段会被收入 `metadata`（文本节点取文本值，其它节点取 JSON 字符串形式）。
7. 正文（第二个 `---` 之后的内容，去除首尾空白）作为 `template`。

### 7.3 MarkdownSkillProperties

[MarkdownSkillProperties.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/java/org/wall/im/ai/agent/skill/MarkdownSkillProperties.java) 绑定配置前缀 `im.ai.markdown-skills`：

| 属性 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `enabled` | boolean | `true` | 是否启用 Markdown 技能加载 |
| `classpathDirs` | `List<String>` | `["skills"]` | classpath 下需扫描的目录列表 |
| `fileSystemDirs` | `List<String>` | `[]` | 文件系统中需扫描的目录列表 |

### 7.4 自动配置与触发

[MarkdownSkillAutoConfiguration.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/java/org/wall/im/ai/agent/skill/MarkdownSkillAutoConfiguration.java) 是一个 `@AutoConfiguration`，装配条件：

- `@ConditionalOnClass(SkillRegistry.class)`：类路径存在 `SkillRegistry`。
- `@ConditionalOnProperty(prefix = "im.ai.markdown-skills", name = "enabled", havingValue = "true", matchIfMissing = true)`：默认开启，可通过 `im.ai.markdown-skills.enabled=false` 关闭。
- `@EnableConfigurationProperties(MarkdownSkillProperties.class)`。

注册的 Bean：

| Bean | 条件 |
| --- | --- |
| `MarkdownSkillLoader markdownSkillLoader(SkillRegistry)` | `@ConditionalOnMissingBean` |
| `MarkdownSkillLoaderInitializer markdownSkillLoaderInitializer(loader, properties)` | `@ConditionalOnBean(MarkdownSkillLoader.class)` |

[MarkdownSkillLoaderInitializer.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/java/org/wall/im/ai/agent/skill/MarkdownSkillLoaderInitializer.java) 通过 `@EventListener(ApplicationReadyEvent.class)` 监听应用就绪事件：在 Spring 上下文完全启动后，依次遍历 `classpathDirs` 调用 `loadFromClasspath`、遍历 `fileSystemDirs` 调用 `loadFromFileSystem`，汇总加载日志。

> 重要：自动配置只创建 `MarkdownSkillLoader` 与初始化器，**不会自动创建 `SkillRegistry` Bean**。使用方需自行向 Spring 容器注册一个 `SkillRegistry`（以及 `ToolRegistry`、`MemoryStoreFactory`、`AgentFactory`、`AgentLifecycleManager` 等运行时 Bean，按需）。

## 9. Markdown 技能文件格式示例

模块内置两个技能文件：[investment-analysis.md](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/resources/skills/investment-analysis.md) 与 [portfolio-recommend.md](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/resources/skills/portfolio-recommend.md)。以下为通用格式与一个具体示例。

### 9.1 通用格式

```markdown
---
name: skill-name
description: 技能描述（可省略，默认为 "{name} skill"）
tools:
  - tool-a
  - tool-b
---

# 技能指令

在此编写技能的 prompt 模板，支持以下变量占位符：
- {{input}}        用户输入
- {{skillName}}    技能名称
- {{description}}  技能描述
- {{customKey}}    front-matter 中声明的任意额外字段
```

### 9.2 内置示例：投资分析技能

```markdown
---
name: investment-analysis-skill
description: 投资分析技能：对指定标的进行技术面和基本面分析，输出投资建议报告
tools:
  - market-data-tool
  - risk-assessment-tool
---

# 投资分析技能

你是一位专业的投资分析师。请根据用户输入的标的进行全面的投资分析。

## 分析要求

1. **技术面分析**：分析短期均线、MACD、RSI等技术指标，判断趋势和动量
2. **基本面分析**：评估市盈率、营收增速、行业景气度等关键指标
3. **风险评估**：识别主要风险因素，给出风险等级
4. **投资建议**：基于以上分析，给出综合评级和操作建议

## 用户输入

{{input}}

## 输出格式

请按以下格式输出分析报告：

【投资分析报告】
分析标的: {{input}}
─────────────────────────────
技术面分析:
  - 趋势判断:
  - 关键信号:
基本面分析:
  - 估值水平:
  - 成长性:
风险评估:
  - 风险等级:
  - 主要风险点:
综合评级: 推荐关注/中性/回避
建议操作: 具体操作建议（含入场价、止损位）
```

文件放在 classpath 的 `skills/` 目录下，应用启动后 `MarkdownSkillAutoConfiguration` 会自动加载并注册到 `SkillRegistry`。在 `agents.yml` 中通过 `skills: [investment-analysis-skill]` 引用即可。