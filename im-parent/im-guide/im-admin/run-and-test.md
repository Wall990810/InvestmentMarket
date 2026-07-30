# ← 返回索引

# 启动与运行

## 前置条件

1. JDK 26
2. 设置环境变量 `AI_DASHSCOPE_API_KEY`（从阿里云百炼控制台获取）
3. 已构建并安装 `im-ai-core` 与 `im-ai-agent` 到本地 Maven 仓库（`mvn install`）

## 构建与启动

```bash
# 在 im-admin 目录下
mvn clean package
# 设置 API Key 后启动
set AI_DASHSCOPE_API_KEY=sk-xxxxxxxxxxxxxxxx
mvn spring-boot:run
```

应用监听 `8080` 端口。启动成功后日志会输出 Agent 注册信息，可通过注入 `InvestmentAgentService` 调用投资咨询、标的分析、组合推荐能力。

> 说明：当前 Skill 与 Tool 的 `execute` 方法为演示桩实现，返回固定示例数据；接入真实行情/风控数据源后即可投入生产使用。