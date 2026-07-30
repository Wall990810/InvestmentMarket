# 自动装配与 Spring Boot 集成

← 返回 [索引](../README.md)

## 10. 自动装配与 Spring Boot 集成

### 10.1 AutoConfiguration.imports

模块通过标准 Spring Boot 3 自动装配机制注册，参见 [AutoConfiguration.imports](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-agent/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports)：

```text
org.wall.im.ai.agent.skill.MarkdownSkillAutoConfiguration
```

即仅 `MarkdownSkillAutoConfiguration` 一个类被自动装配。其它运行时组件（`AgentRegistry`、`SkillRegistry`、`ToolRegistry`、`MemoryStoreFactory`、`AgentFactory`、`AgentLifecycleManager`、`AgentConfigParser`）需要使用方在业务配置中自行声明为 Bean。

### 10.2 application.yml 配置

```yaml
im:
  ai:
    markdown-skills:
      enabled: true                       # 默认 true，可关闭
      classpath-dirs:
        - skills
        - custom-skills                   # 追加自定义目录
      file-system-dirs:
        - /opt/im/skills                  # 可选：外部目录
```

### 10.3 启用条件回顾

- 必须在容器中存在 `SkillRegistry` Bean，否则 `MarkdownSkillAutoConfiguration` 的 `@ConditionalOnClass` 仍成立但 `markdownSkillLoader` Bean 的入参无法满足（装配失败）。
- `im.ai.markdown-skills.enabled` 缺省视为 `true`。
- 加载时机：`ApplicationReadyEvent`，即所有 Bean 初始化完成后才扫描技能文件。