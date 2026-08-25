# ← 返回索引

# 因子核心抽象

本模块（`im-quant-core` 的 `factor` 包）定义量化因子的核心 SPI。所有因子——无论是内置的还是自定义的——都遵循同一套契约，确保可被 [FactorRegistry](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/registry/FactorRegistry.java) 注册、被 [FactorEngine](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/engine/FactorEngine.java) 调用、被后处理器链加工。

---

## Factor —— 因子接口

源码：[Factor.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/factor/Factor.java)

因子是量化框架的统一契约。实现类只需提供 `descriptor()` 与 `compute(FactorContext)`，即可被全链路识别。

| 方法签名 | 说明 |
| --- | --- |
| `FactorDescriptor descriptor()` | 返回因子元数据（名称、分类、参数等）。 |
| `FactorResult compute(FactorContext context)` | 在给定上下文上计算因子截面值，遍历 universe 标的逐个取值。 |

> **约定**：个别标的数据缺失时应以 `Double.NaN` 占位而非抛异常，以保证截面完整性。

---

## AbstractFactor —— 因子骨架基类

源码：[AbstractFactor.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/factor/AbstractFactor.java)

封装 `descriptor()` 字段持有，子类只需实现 `compute(FactorContext)`，减少样板代码。

| 方法签名 | 说明 |
| --- | --- |
| `protected AbstractFactor(FactorDescriptor descriptor)` | 构造函数，持有 descriptor（不可变）。 |
| `final FactorDescriptor descriptor()` | 返回因子元数据（final，不可覆盖）。 |

---

## FactorDescriptor —— 因子元数据

源码：[FactorDescriptor.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/factor/FactorDescriptor.java)

因子的自描述信息。引擎、注册表、AI Tool 均依据此信息进行发现、调用与展示。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `name` | `String` | 因子唯一标识，如 `"momentum_12_1"` |
| `displayName` | `String` | 展示名，如 `"12-1月动量"` |
| `category` | `FactorCategory` | 分类 |
| `description` | `String` | 计算逻辑说明 |
| `parameters` | `List<FactorParameter>` | 可调参数列表 |
| `version` | `String` | 版本 |
| `author` | `String` | 作者 |

便捷构造（无参数、默认版本与作者）：

```java
new FactorDescriptor("momentum_12_1", "12-1月动量", FactorCategory.MOMENTUM, "跳过最近1个月、取此前12个月累计收益");
```

---

## FactorParameter —— 因子参数定义

源码：[FactorParameter.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/factor/FactorParameter.java)

声明因子可调参数（如动量窗口、分位阈值），使因子可在不改动代码的前提下被引擎/配置覆盖。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `name` | `String` | 参数名 |
| `type` | `ParameterType` | 参数类型（INTEGER / DOUBLE / BOOLEAN / STRING） |
| `defaultValue` | `Object` | 默认值 |
| `description` | `String` | 说明 |

```java
new FactorParameter("formationMonths", FactorParameter.ParameterType.INTEGER, 12, "形成期月数")
```

---

## FactorCategory —— 因子分类

源码：[FactorCategory.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/factor/FactorCategory.java)

用于 `FactorRegistry` 分组查询与监控打标。新增分类时在此枚举追加即可，不影响既有因子实现。

| 枚举值 | 说明 |
| --- | --- |
| `MOMENTUM` | 动量/反转 |
| `VALUE` | 价值 |
| `VOLATILITY` | 波动率 |
| `LIQUIDITY` | 流动性/量价 |
| `QUALITY` | 质量 |
| `SIZE` | 规模 |
| `GROWTH` | 成长 |
| `TECHNICAL` | 技术/形态 |
| `CUSTOM` | 自定义 |

---

## FactorContext —— 因子计算上下文

源码：[FactorContext.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/factor/FactorContext.java)

一次截面计算的环境载体：数据源、标的池、截面日、参数表与 K 线缓存。因子实现通过本对象获取行情与基本面，并以 `getParameter` 读取可调参数。

| 方法签名 | 说明 |
| --- | --- |
| `List<Bar> getHistory(String symbol, int lookbackDays)` | 获取某标的截至截面日的回看 K 线（含截面日），按日升序，频率为 DAILY。 |
| `List<Bar> getHistory(String symbol, int lookbackDays, Frequency frequency)` | 同上，指定 K 线频率。 |
| `Map<String, FundamentalData> getFundamentals()` | 获取截面基本面快照（按 universe 批量查询，内部缓存）。 |
| `<T> T getParameter(String name, Class<T> type)` | 读取参数并按目标类型转换，缺失则抛异常。 |
| `<T> T getParameter(String name, Class<T> type, T defaultValue)` | 读取参数，缺失返回默认值。 |
| `Universe universe()` | 获取标的池。 |
| `LocalDate asOfDate()` | 获取截面日。 |

> **K 线缓存**：`getHistory` 内部以 `symbol|lookbackDays|frequency` 为 key 缓存，同一标的同一窗口的多次调用只请求一次数据源。`lookbackDays` 以交易日计，内部自动放大为日历日（约 1.5 倍 + 20 天缓冲）以覆盖周末与节假日。

---

## FactorResult —— 因子截面结果

源码：[FactorResult.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/factor/FactorResult.java)

持有某因子在指定截面日、指定标的池上的全部取值。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `factorName` | `String` | 因子标识 |
| `asOfDate` | `LocalDate` | 截面日 |
| `values` | `Map<String, FactorValue>` | symbol → 因子值（保序） |

| 方法签名 | 说明 |
| --- | --- |
| `FactorValue valueOf(String symbol)` | 按标的查询因子值，缺失返回 null。 |
| `int size()` | 标的数量。 |
| `FactorResult withValues(Map<String, FactorValue> newValues)` | 以新值集合返回一个新结果（后处理器使用）。 |

---

## FactorValue —— 单标的因子值

源码：[FactorValue.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/factor/FactorValue.java)

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `symbol` | `String` | 标的代码 |
| `value` | `double` | 因子原始值；缺失以 `Double.NaN` 表示 |
| `rank` | `int` | 截面排名（从大到小，1 表示最大），缺省 0 表示未排名 |

| 方法签名 | 说明 |
| --- | --- |
| `boolean hasValue()` | 是否有有效值（非 NaN）。 |
