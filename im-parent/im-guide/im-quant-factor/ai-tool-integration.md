# ← 返回索引

# AI Tool 集成

`im-quant-factor` 通过 [FactorQueryTool](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-factor/src/main/java/org/wall/im/quant/factor/ai/FactorQueryTool.java) 将因子计算能力暴露给 AI Agent，使投资顾问 Agent 能在 ReAct 循环中按"因子名 + 标的 + 截面日"查询截面值。

---

## FactorQueryTool

源码：[FactorQueryTool.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-factor/src/main/java/org/wall/im/quant/factor/ai/FactorQueryTool.java)

实现 [Tool](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/tool/Tool.java) 接口，工具名 `factor-query`。

| 方法签名 | 说明 |
| --- | --- |
| `String getName()` | 返回 `"factor-query"`。 |
| `String getDescription()` | 返回工具描述。 |
| `Map<String, Object> getParameterSchema()` | 返回 JSON Schema 参数定义。 |
| `String execute(Map<String, Object> parameters)` | 执行查询，返回 JSON 字符串。 |

### 参数 Schema

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `factors` | array&lt;string&gt; | 是 | 因子名列表，如 `["momentum_12_1","pe_ttm"]` |
| `symbols` | array&lt;string&gt; | 是 | 标的代码列表，如 `["000001.SZ","600000.SH"]` |
| `asOfDate` | string | 否 | 截面日期 `yyyy-MM-dd`，缺省为当天 |
| `processors` | array&lt;string&gt; | 否 | 后处理器，可选值 `winsorize` / `standardize` / `neutralize` |

### Agent 调用示例参数

```json
{
  "factors": ["momentum_12_1", "pe_ttm"],
  "symbols": ["000001.SZ", "600000.SH"],
  "asOfDate": "2026-08-24",
  "processors": ["winsorize", "standardize"]
}
```

### 返回 JSON 结构

```json
{
  "asOfDate": "2026-08-24",
  "costTimeMs": 12,
  "factors": {
    "momentum_12_1": {
      "values": { "000001.SZ": 0.1523, "600000.SH": -0.0841 },
      "missing": []
    },
    "pe_ttm": {
      "values": { "000001.SZ": 8.52 },
      "missing": ["600000.SH"]
    }
  }
}
```

每个因子下含 `values`（有效值映射）与 `missing`（缺失标的列表）。计算异常时返回 `{"error":"..."}`。

---

## QuantAutoConfiguration

源码：[QuantAutoConfiguration.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-factor/src/main/java/org/wall/im/quant/factor/ai/QuantAutoConfiguration.java)

`@AutoConfiguration` 自动装配类。引入 `im-quant-factor` 依赖后自动生效。

### 装配条件

| 条件 | 说明 |
| --- | --- |
| `@ConditionalOnClass(FactorEngine.class)` | classpath 存在 `FactorEngine`（即 `im-quant-core` 已引入） |
| `@ConditionalOnProperty(name = "im.quant.enabled", matchIfMissing = true)` | 默认启用，可通过 `im.quant.enabled=false` 关闭 |

### 装配的 Bean

| Bean | 条件 | 说明 |
| --- | --- | --- |
| `InMemoryMarketDataProvider` | `@ConditionalOnMissingBean(MarketDataProvider.class)` | 内存行情数据源，可被自定义 Bean 覆盖 |
| `InMemoryFundamentalDataProvider` | `@ConditionalOnMissingBean(FundamentalDataProvider.class)` | 内存基本面数据源，可被自定义 Bean 覆盖 |
| `FactorRegistry` | 无条件 | 注册全部 8 个内置因子 |
| `FactorEngine` | `@ConditionalOnMissingBean` | 因子计算引擎 |
| `FactorQueryTool` | `@ConditionalOnMissingBean` | AI 因子查询工具 |

### 在 im-admin 中的集成

`im-admin` 已在 [AiAgentConfig](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-admin/src/main/java/org/wall/im/admin/config/AiAgentConfig.java) 中将 `FactorQueryTool` 注入 `ToolRegistry`，并在 [investment-advisor.yml](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-admin/src/main/resources/agents/investment-advisor.yml) 的 `tools` 列表中声明 `factor-query`，使投资顾问 Agent 可直接调用。

### 接入真实数据源

```java
@Bean
public MarketDataProvider tushareProvider() {
    return new TushareMarketDataProvider("your-token");
}

@Bean
public FundamentalDataProvider windProvider() {
    return new WindFundamentalDataProvider();
}
```

定义上述 Bean 后，`QuantAutoConfiguration` 的内存实现自动让位，因子与引擎自动复用真实数据源。自定义因子可通过定义自己的 `FactorRegistry` Bean 覆盖内置注册表，或在自动装配后手动追加注册。
