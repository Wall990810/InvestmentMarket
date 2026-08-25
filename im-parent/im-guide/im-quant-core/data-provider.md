# ← 返回索引

# 数据提供者

因子计算与具体数据源（Tushare / Wind / 聚宽 / 本地数据库）解耦，通过两个 SPI 接口抽象行情与基本面数据来源。实现应保证返回数据按日期升序、已前复权。

---

## MarketDataProvider —— 行情数据提供者

源码：[MarketDataProvider.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/data/MarketDataProvider.java)

抽象行情数据来源，因子计算通过此接口获取 K 线。

| 方法签名 | 说明 |
| --- | --- |
| `List<Bar> getBars(String symbol, LocalDate start, LocalDate end, Frequency frequency)` | 获取指定标的在时间区间内的 K 线序列（升序、前复权），无数据返回空列表。 |
| `default Bar getLatestBar(String symbol, LocalDate asOfDate)` | 获取指定标的最新一根 K 线（默认实现取截止日前 10 日区间最后一根）。 |
| `String getProviderName()` | 数据源标识，用于日志与监控。 |

---

## FundamentalDataProvider —— 基本面数据提供者

源码：[FundamentalDataProvider.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/data/FundamentalDataProvider.java)

价值/质量类因子的数据来源。按截面日期返回一批标的的最新基本面快照。

| 方法签名 | 说明 |
| --- | --- |
| `Map<String, FundamentalData> getFundamentals(Collection<String> symbols, LocalDate asOfDate)` | 获取截面日期的最新基本面快照；缺失项以不包含该 key 表示。 |
| `String getProviderName()` | 数据源标识。 |

---

## InMemoryMarketDataProvider —— 内存行情实现

源码：[InMemoryMarketDataProvider.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/data/InMemoryMarketDataProvider.java)

面向开发与测试的内存行情数据源。按需生成 3 年模拟日 K 线（随机游走 + 轻微漂移），覆盖调用当日往前 3 年。`getProviderName()` 返回 `"in-memory"`。

> 在 Spring Boot 应用中，若未定义任何 `MarketDataProvider` Bean，`QuantAutoConfiguration` 会自动注册此实例（`@ConditionalOnMissingBean`）。接入真实数据源时仅需定义自己的 `MarketDataProvider` Bean 即可覆盖。

---

## InMemoryFundamentalDataProvider —— 内存基本面实现

源码：[InMemoryFundamentalDataProvider.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/data/InMemoryFundamentalDataProvider.java)

面向开发与测试的内存基本面数据源。为每个标的生成一组合理的模拟基本面快照（PE、PB、ROE 等）。`getProviderName()` 返回 `"in-memory"`。

> 同样可被 `@ConditionalOnMissingBean` 自动注册，定义自己的 `FundamentalDataProvider` Bean 即可覆盖。

---

## 接入真实数据源

```java
@Bean
public MarketDataProvider tushareMarketDataProvider() {
    return new TushareMarketDataProvider("your-token");
}

@Bean
public FundamentalDataProvider tushareFundamentalDataProvider() {
    return new TushareFundamentalDataProvider("your-token");
}
```

定义上述 Bean 后，`QuantAutoConfiguration` 的内存实现自动让位，因子与引擎自动复用真实数据源。
