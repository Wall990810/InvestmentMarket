# ← 返回索引

# 计算引擎

## FactorEngine —— 因子计算引擎

源码：[FactorEngine.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/engine/FactorEngine.java)

框架的调度中枢。一次计算请求的处理流程：

1. 为每个因子从 `FactorRegistry` 解析实例；
2. 构造共享的 `FactorContext`（数据源 / 标的池 / 截面日 / 参数）；
3. 调用 `Factor.compute(FactorContext)` 得到原始截面值；
4. 经 `FactorProcessorChain` 做后处理（去极值 → 标准化 → 中性化）；
5. 汇总返回 `FactorCalculationResult`。

| 方法签名 | 说明 |
| --- | --- |
| `FactorEngine(FactorRegistry registry, MarketDataProvider marketDataProvider, FundamentalDataProvider fundamentalDataProvider)` | 构造函数，注入注册表与数据源。 |
| `FactorCalculationResult calculate(FactorCalculationRequest request)` | 批量计算：遍历因子名列表，逐个计算 + 后处理，返回结果集合（含耗时）。 |
| `double calculateValue(String factorName, String symbol, LocalDate asOfDate)` | 便捷方法：单标的单因子取值，便于 AI Tool 调用。返回 NaN 表示缺失。 |
| `FactorRegistry registry()` | 获取注册表。 |

> **容错**：单个因子计算失败时记录警告日志并以空结果占位，不影响其他因子。

### 调用示例

```java
FactorRegistry registry = new FactorRegistry();
registry.registerAll(new MomentumFactor(), new PeRatioFactor());

FactorEngine engine = new FactorEngine(registry, new InMemoryMarketDataProvider(),
        new InMemoryFundamentalDataProvider());

Universe universe = new Universe("hs300", List.of("000001.SZ", "600000.SH", "000300.SH"));
FactorProcessorChain chain = FactorProcessorChain.builder().winsorize().standardize().build();

FactorCalculationRequest req = new FactorCalculationRequest(
        List.of("momentum_12_1", "pe_ttm"), universe, LocalDate.now(), Map.of(), chain);

FactorCalculationResult res = engine.calculate(req);
// res.get("momentum_12_1").valueOf("000001.SZ").value()
```

---

## FactorCalculationRequest —— 计算请求

源码：[FactorCalculationRequest.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/engine/FactorCalculationRequest.java)

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `factorNames` | `List<String>` | 待计算的因子标识列表 |
| `universe` | `Universe` | 标的池 |
| `asOfDate` | `LocalDate` | 截面日 |
| `parameters` | `Map<String, Object>` | 全局参数表，注入到每个因子的 `FactorContext` |
| `processorChain` | `FactorProcessorChain` | 可选的后处理器链，null 表示不做后处理 |

---

## FactorCalculationResult —— 计算结果集合

源码：[FactorCalculationResult.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/engine/FactorCalculationResult.java)

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `results` | `Map<String, FactorResult>` | 因子标识 → 截面结果（保序） |
| `costTimeMs` | `long` | 总耗时（毫秒） |

| 方法签名 | 说明 |
| --- | --- |
| `FactorResult get(String factorName)` | 按因子名获取截面结果。 |
| `int size()` | 因子数量。 |
