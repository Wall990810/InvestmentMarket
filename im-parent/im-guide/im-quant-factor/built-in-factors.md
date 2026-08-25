# ← 返回索引

# 内置因子清单

`im-quant-factor` 内置 8 个因子，覆盖动量、价值、波动率、流动性四大分类。所有因子继承 [AbstractFactor](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/factor/AbstractFactor.java)，通过 `FactorDescriptor` 声明元数据与可调参数。

## 因子总览

| 因子标识 | 展示名 | 分类 | 参数 | 数据源 |
| --- | --- | --- | --- | --- |
| `momentum_12_1` | 12-1月动量 | MOMENTUM | formationMonths=12, skipMonths=1 | 行情 |
| `rsi_14` | RSI(14) | MOMENTUM | period=14 | 行情 |
| `pe_ttm` | 市盈率(TTM) | VALUE | 无 | 基本面 |
| `pb` | 市净率 | VALUE | 无 | 基本面 |
| `realized_vol_20` | 20日已实现波动率 | VOLATILITY | window=20 | 行情 |
| `beta_60` | 60日Beta | VOLATILITY | window=60, benchmark=000300.SH | 行情 |
| `turnover_20` | 20日换手率 | LIQUIDITY | window=20 | 行情 + 基本面 |
| `amihud_illiq_20` | Amihud非流动性 | LIQUIDITY | window=20 | 行情 |

---

## 动量类

### MomentumFactor —— 12-1月动量

源码：[MomentumFactor.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-factor/src/main/java/org/wall/im/quant/factor/momentum/MomentumFactor.java)

经典 Jegadeesh-Titman 动量因子：跳过最近 `skipMonths` 个月，取此前 `formationMonths` 个月的累计收益。

| 参数 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `formationMonths` | INTEGER | 12 | 形成期月数 |
| `skipMonths` | INTEGER | 1 | 跳过月数（避免短期反转干扰） |

**计算逻辑**：取回看 K 线（约 `(formationMonths + skipMonths) * 21` 个交易日），计算 `endClose / startClose - 1`。

### RsiFactor —— RSI(14)

源码：[RsiFactor.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-factor/src/main/java/org/wall/im/quant/factor/momentum/RsiFactor.java)

Wilder 相对强弱指标，衡量超买超卖程度。

| 参数 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `period` | INTEGER | 14 | RSI 周期 |

**计算逻辑**：以 `period` 日内上涨日平均涨幅与下跌日平均跌幅计算 `100 - 100/(1+RS)`，值域 0~100。

---

## 价值类

### PeRatioFactor —— 市盈率(TTM)

源码：[PeRatioFactor.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-factor/src/main/java/org/wall/im/quant/factor/value/PeRatioFactor.java)

滚动市盈率，亏损（PE 为负）记为缺失（NaN）。

**计算逻辑**：直接取 `FundamentalData.pe`，若为负或 NaN 则返回 NaN。

### PbRatioFactor —— 市净率

源码：[PbRatioFactor.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-factor/src/main/java/org/wall/im/quant/factor/value/PbRatioFactor.java)

市净率，净资产为负记为缺失（NaN）。

**计算逻辑**：直接取 `FundamentalData.pb`，若为负或 NaN 则返回 NaN。

---

## 波动率类

### RealizedVolatilityFactor —— 20日已实现波动率

源码：[RealizedVolatilityFactor.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-factor/src/main/java/org/wall/im/quant/factor/volatility/RealizedVolatilityFactor.java)

日收益率标准差，年化处理。

| 参数 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `window` | INTEGER | 20 | 计算窗口（交易日） |

**计算逻辑**：取 `window` 日日线收益率序列，计算样本标准差，乘以 `sqrt(252)` 年化。

### BetaFactor —— 60日Beta

源码：[BetaFactor.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-factor/src/main/java/org/wall/im/quant/factor/volatility/BetaFactor.java)

相对基准指数的 OLS 贝塔系数。

| 参数 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `window` | INTEGER | 60 | 回归窗口（交易日） |
| `benchmark` | STRING | `000300.SH` | 基准指数代码 |

**计算逻辑**：取 `window` 日标 的与基准的收益率序列，OLS 回归 `R_stock = α + β * R_benchmark`，返回 `β`。

---

## 流动性类

### TurnoverFactor —— 20日换手率

源码：[TurnoverFactor.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-factor/src/main/java/org/wall/im/quant/factor/liquidity/TurnoverFactor.java)

成交额与总市值之比的均值，衡量交易活跃度。

| 参数 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `window` | INTEGER | 20 | 计算窗口（交易日） |

**计算逻辑**：取 `window` 日 `Bar.amount` 与 `FundamentalData.marketCap`，计算 `mean(amount / marketCap)`。

### AmihudIlliquidityFactor —— Amihud非流动性

源码：[AmihudIlliquidityFactor.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-factor/src/main/java/org/wall/im/quant/factor/liquidity/AmihudIlliquidityFactor.java)

Amihud (2002) 非流动性指标：日均绝对收益率与成交额之比。

| 参数 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `window` | INTEGER | 20 | 计算窗口（交易日） |

**计算逻辑**：取 `window` 日 `|returnRate| / amount` 的均值，值越大表示流动性越差。
