# ← 返回索引

# InvestmentAgentService

[InvestmentAgentService.java](file:///d:/IdeaProject/InvestmentMarket/im-admin/src/main/java/org/wall/im/admin/agent/InvestmentAgentService.java) 是面向业务的服务层，演示如何通过 `AgentRegistry` 获取已注册的 Agent 并调用其能力。它持有一个常量 `AGENT_NAME = "investment-advisor"`，通过 `agentRegistry.getRequired(AGENT_NAME)` 拿到 `Agent` 实例。

## 公共 API

```java
@Service
public class InvestmentAgentService {

    public InvestmentAgentService(AgentRegistry agentRegistry) { ... }

    // 1. 投资咨询：自然语言问答
    public String consult(String question);

    // 2. 标的分析：对指定标的进行深度分析
    public String analyze(String symbolDescription);

    // 3. 投资组合推荐：根据用户偏好生成资产配置方案
    public AgentResult recommendPortfolio(String requirement);

    // 4. 重置 Agent 对话上下文（清除短期记忆）
    public void resetSession();
}
```

## 调用方式

- `consult(question)`：调用 `agent.chat(question)`，返回字符串回复。
- `analyze(symbolDescription)`：内部拼接为 `"请对以下标的进行深度分析并给出投资建议：" + symbolDescription`，再调用 `agent.chat(...)`。
- `recommendPortfolio(requirement)`：构造 `List<Message>`（含 system 提示与 user 提示），调用 `agent.execute(messages)` 返回 `AgentResult`。
- `resetSession()`：调用 `agent.reset()` 清除对话上下文。

## 使用示例

```java
// 1. 对话式咨询
String reply = investmentAgentService.consult("我有一笔10万元资金，风险偏好稳健，请给出投资建议");

// 2. 获取特定标的分析报告
String report = investmentAgentService.analyze("600519.SH 贵州茅台");

// 3. 生成投资组合方案
AgentResult portfolio = investmentAgentService.recommendPortfolio("稳健型，投资期限1年，资金50万");
```