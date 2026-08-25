# ← 返回索引

# 数据模型

量化框架的核心数据结构，均为不可变 `record`，位于 `model` 包。

---

## Bar —— OHLCV K 线

源码：[Bar.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/model/Bar.java)

因子计算的最小行情单元，包含开高低收、成交量、成交额与前复权因子。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `symbol` | `String` | 标的代码 |
| `date` | `LocalDate` | 交易日 |
| `open` | `double` | 开盘价（前复权） |
| `high` | `double` | 最高价（前复权） |
| `low` | `double` | 最低价（前复权） |
| `close` | `double` | 收盘价（前复权） |
| `volume` | `long` | 成交量（股） |
| `amount` | `double` | 成交额（元） |
| `adjFactor` | `double` | 复权因子，缺省 1.0 |

| 方法签名 | 说明 |
| --- | --- |
| `double typicalPrice()` | 典型价：(high + low + close) / 3 |
| `double returnRate()` | 日收益率：close / open - 1（除零保护） |

---

## FundamentalData —— 基本面快照

源码：[FundamentalData.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/model/FundamentalData.java)

价值/质量类因子的输入。所有字段使用"已披露最新值"，缺失以 `Double.NaN` 表示。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `symbol` | `String` | 标的代码 |
| `asOfDate` | `LocalDate` | 数据截止日 |
| `pe` | `double` | 市盈率（TTM） |
| `pb` | `double` | 市净率 |
| `ps` | `double` | 市销率 |
| `dividendYear` | `double` | 股息率（百分比，如 3.5 表示 3.5%） |
| `roe` | `double` | 净资产收益率（百分比） |
| `roa` | `double` | 总资产收益率（百分比） |
| `debtRatio` | `double` | 资产负债率（百分比） |
| `marketCap` | `double` | 总市值（元） |
| `sharesOutstanding` | `long` | 总股本（股） |

---

## Universe —— 标的池

源码：[Universe.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/model/Universe.java)

一组参与因子计算的标的集合，如"沪深300""全A"。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `name` | `String` | 标的池名称 |
| `symbols` | `List<String>` | 标的代码列表（有序、去重） |

| 方法签名 | 说明 |
| --- | --- |
| `int size()` | 标的数量。 |
| `boolean contains(String symbol)` | 是否包含某标的。 |

---

## Frequency —— K 线频率

源码：[Frequency.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/model/Frequency.java)

| 枚举值 | periodsPerYear | 说明 |
| --- | --- | --- |
| `DAILY` | 252 | 日线 |
| `WEEKLY` | 52 | 周线 |
| `MONTHLY` | 12 | 月线 |

| 方法签名 | 说明 |
| --- | --- |
| `int getPeriodsPerYear()` | 每年交易日/周期数，用于年化波动率等计算。 |

---

## Instrument —— 标的基础信息

源码：[Instrument.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/model/Instrument.java)

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `symbol` | `String` | 标的代码，如 `"000001.SZ"` |
| `name` | `String` | 标的名称，如 `"平安银行"` |
| `exchange` | `String` | 交易所，如 `"SZSE"` / `"SSE"` |
| `type` | `String` | 标的类型，如 `"STOCK"` / `"ETF"` / `"INDEX"` |
| `sector` | `String` | 所属行业/板块，用于行业中性化 |
| `listDate` | `LocalDate` | 上市日期 |

---

## StatUtils —— 统计工具

源码：[StatUtils.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/stats/StatUtils.java)

无外部依赖的轻量统计工具，供后处理器使用。仅处理有限值，自动剔除 NaN。

| 方法签名 | 说明 |
| --- | --- |
| `static double mean(double[] values)` | 均值（自动剔除 NaN）。 |
| `static double std(double[] values)` | 样本标准差（n-1，自动剔除 NaN）。 |
| `static double percentile(double[] values, double p)` | 线性插值分位（p 取 0~100）。 |
| `static double zscore(double value, double mean, double std)` | Z-Score 标准化，标准差为 0 时返回 0。 |
