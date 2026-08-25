# ← 返回索引

# 后处理链

对因子截面值做变换：去极值 / 标准化 / 中性化 / 缺失填补等。后处理器实现应保持 `FactorResult.factorName()` 与 `asOfDate()` 不变。

---

## FactorProcessor —— 后处理器接口

源码：[FactorProcessor.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/process/FactorProcessor.java)

`@FunctionalInterface` 接口，可用 lambda 或类实现。

| 方法签名 | 说明 |
| --- | --- |
| `FactorResult process(FactorResult result, FactorContext context)` | 处理一个因子截面结果，返回处理后的结果（可为同一实例或新实例）。 |
| `default String name()` | 处理器名称，用于日志。默认返回类名。 |

---

## FactorProcessorChain —— 后处理器链

源码：[FactorProcessorChain.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/process/FactorProcessorChain.java)

有序应用一组 `FactorProcessor`，前一个输出作为后一个输入。空链等价于不处理。

| 方法签名 | 说明 |
| --- | --- |
| `FactorProcessorChain(FactorProcessor... processors)` | 变长参数构造。 |
| `FactorProcessorChain(List<FactorProcessor> processors)` | 列表构造。 |
| `FactorResult apply(FactorResult result, FactorContext context)` | 依次应用链中处理器。 |
| `boolean isEmpty()` | 是否为空链。 |
| `List<FactorProcessor> getProcessors()` | 获取处理器列表。 |
| `static Builder builder()` | 获取链式构造器。 |
| `static FactorProcessorChain empty()` | 空链快捷实例。 |

### Builder

| 方法签名 | 说明 |
| --- | --- |
| `Builder add(FactorProcessor processor)` | 添加自定义处理器。 |
| `Builder winsorize()` | 添加默认去极值（1%/99% 分位截断）。 |
| `Builder winsorize(double lowerPct, double upperPct)` | 添加自定义分位去极值。 |
| `Builder standardize()` | 添加 Z-Score 标准化。 |
| `Builder neutralize()` | 添加行业中性化。 |
| `FactorProcessorChain build()` | 构建链。 |

### 链式拼装示例

```java
// 去极值 → 标准化 → 中性化
FactorProcessorChain chain = FactorProcessorChain.builder()
        .winsorize()
        .standardize()
        .neutralize()
        .build();

// 自定义分位
FactorProcessorChain custom = FactorProcessorChain.builder()
        .winsorize(0.05, 0.95)
        .standardize()
        .build();

// 自定义处理器
FactorProcessor fillNaN = (result, ctx) -> result.withValues(
        result.values().entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().hasValue() ? e.getValue() : new FactorValue(e.getKey(), 0.0))));
FactorProcessorChain withFill = FactorProcessorChain.builder()
        .add(fillNaN)
        .standardize()
        .build();
```

---

## 内置处理器

### WinsorizeProcessor —— 去极值

源码：[WinsorizeProcessor.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/process/WinsorizeProcessor.java)

将截面值在指定分位处截断（缩尾），消除极端值对标准化与回归的影响。

| 构造函数 | 说明 |
| --- | --- |
| `WinsorizeProcessor()` | 默认 1%/99% 分位截断。 |
| `WinsorizeProcessor(double lowerPct, double upperPct)` | 自定义分位截断。 |

### StandardizeProcessor —— 标准化

源码：[StandardizeProcessor.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/process/StandardizeProcessor.java)

Z-Score 标准化：`(x - mean) / std`，使截面均值为 0、标准差为 1。

### NeutralizeProcessor —— 中性化

源码：[NeutralizeProcessor.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/process/NeutralizeProcessor.java)

行业中性化：对截面值按行业分组做去均值，消除行业偏差。当前以 universe 中标的代码前缀（如 `60`/`00`/`30`）做行业代理分组，可扩展为正式行业分类。
