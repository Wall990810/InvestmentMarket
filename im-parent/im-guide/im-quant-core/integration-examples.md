# ← 返回索引

# 使用示例

## 1. 编程式调用（无 Spring）

```java
// 1. 创建注册表并注册因子
FactorRegistry registry = new FactorRegistry();
registry.registerAll(new MomentumFactor(), new PeRatioFactor(), new RsiFactor(),
        new RealizedVolatilityFactor(), new BetaFactor(), new TurnoverFactor());

// 2. 构造引擎（使用内存数据源）
FactorEngine engine = new FactorEngine(registry,
        new InMemoryMarketDataProvider(),
        new InMemoryFundamentalDataProvider());

// 3. 构造计算请求
Universe universe = new Universe("portfolio",
        List.of("000001.SZ", "600000.SH", "000300.SH", "002415.SZ", "300750.SZ"));
FactorProcessorChain chain = FactorProcessorChain.builder()
        .winsorize()
        .standardize()
        .build();

FactorCalculationRequest req = new FactorCalculationRequest(
        List.of("momentum_12_1", "pe_ttm", "rsi_14", "realized_vol_20"),
        universe, LocalDate.now(), Map.of(), chain);

// 4. 执行计算
FactorCalculationResult res = engine.calculate(req);

// 5. 读取结果
for (String factorName : List.of("momentum_12_1", "pe_ttm")) {
    FactorResult fr = res.get(factorName);
    for (String symbol : universe.symbols()) {
        FactorValue fv = fr.valueOf(symbol);
        if (fv != null && fv.hasValue()) {
            System.out.printf("%s %s = %.4f%n", factorName, symbol, fv.value());
        }
    }
}
System.out.printf("总耗时: %dms%n", res.costTimeMs());
```

---

## 2. 单标的单因子查询

```java
double pe = engine.calculateValue("pe_ttm", "000001.SZ", LocalDate.now());
if (Double.isNaN(pe)) {
    System.out.println("该标的无有效 PE 数据");
} else {
    System.out.printf("PE(TTM) = %.2f%n", pe);
}
```

---

## 3. 参数注入

因子可声明可调参数，计算请求通过 `parameters` 注入：

```java
// 使用自定义形成期（6 个月）计算动量
FactorCalculationRequest req = new FactorCalculationRequest(
        List.of("momentum_12_1"),
        universe, LocalDate.now(),
        Map.of("formationMonths", 6, "skipMonths", 1),  // 覆盖默认 12/1
        FactorProcessorChain.empty());

FactorCalculationResult res = engine.calculate(req);
```

---

## 4. 自定义后处理器

```java
// 缺失值填补为截面均值
FactorProcessor fillMissing = (result, ctx) -> {
    double mean = StatUtils.mean(
            result.values().values().stream()
                    .mapToDouble(FactorValue::value)
                    .filter(v -> !Double.isNaN(v))
                    .toArray());
    Map<String, FactorValue> filled = new LinkedHashMap<>();
    for (var e : result.values().entrySet()) {
        double v = e.getValue().hasValue() ? e.getValue().value() : mean;
        filled.put(e.getKey(), new FactorValue(e.getKey(), v));
    }
    return result.withValues(filled);
};

FactorProcessorChain chain = FactorProcessorChain.builder()
        .add(fillMissing)
        .winsorize()
        .standardize()
        .build();
```

---

## 5. 自定义因子扩展

新增因子只需继承 `AbstractFactor` 并注册，无需改动引擎或既有代码：

```java
package org.wall.im.quant.factor.quality;

import java.util.ArrayList;
import java.util.List;
import org.wall.im.quant.factor.*;
import org.wall.im.quant.model.Bar;

public class SharpeRatioFactor extends AbstractFactor {

    public SharpeRatioFactor() {
        super(new FactorDescriptor(
                "sharpe_60", "60日夏普比率", FactorCategory.QUALITY,
                "60日风险调整收益（均值/标准差）",
                List.of(new FactorParameter(
                        "windowDays", FactorParameter.ParameterType.INTEGER, 60, "计算窗口")),
                "1.0.0", "InvestmentMarket"));
    }

    @Override
    public FactorResult compute(FactorContext context) {
        int window = context.getParameter("windowDays", Integer.class, 60);
        List<FactorValue> out = new ArrayList<>();
        for (String symbol : context.universe().symbols()) {
            List<Bar> bars = context.getHistory(symbol, window);
            out.add(new FactorValue(symbol, calcSharpe(bars)));
        }
        return new FactorResult(descriptor().name(), context.asOfDate(), out);
    }

    private static double calcSharpe(List<Bar> bars) {
        if (bars == null || bars.size() < 2) return Double.NaN;
        double[] returns = new double[bars.size() - 1];
        for (int i = 1; i < bars.size(); i++) {
            returns[i - 1] = bars.get(i).close() / bars.get(i - 1).close() - 1;
        }
        double mean = StatUtils.mean(returns);
        double std = StatUtils.std(returns);
        return (Double.isNaN(std) || std == 0) ? 0 : mean / std;
    }
}
```

注册并使用：

```java
registry.register(new SharpeRatioFactor());
// 引擎通过因子名 "sharpe_60" 即可调用
```

---

## 6. Spring Boot 自动装配

引入 `im-quant-factor` 依赖后，`QuantAutoConfiguration` 自动装配：

- 内存数据源（`InMemoryMarketDataProvider` / `InMemoryFundamentalDataProvider`）
- 注册全部内置因子的 `FactorRegistry`
- `FactorEngine`
- `FactorQueryTool`（暴露给 AI Agent）

```java
@Service
public class QuantService {

    private final FactorEngine engine;

    public QuantService(FactorEngine engine) {
        this.engine = engine;
    }

    public double getMomentum(String symbol) {
        return engine.calculateValue("momentum_12_1", symbol, LocalDate.now());
    }
}
```

可通过 `im.quant.enabled=false` 关闭自动装配。接入真实数据源时，定义自己的 `MarketDataProvider` / `FundamentalDataProvider` Bean 即可覆盖内存实现。
