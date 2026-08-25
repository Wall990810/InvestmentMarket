# ← 返回索引

# 因子扩展指南

量化框架的核心设计目标：**新增因子无需改动引擎、注册表、Tool 或既有因子代码**。本指南说明扩展流程。

---

## 新增因子的 3 步流程

### 第 1 步：创建因子类

在 `im-quant-factor` 对应分类包下创建因子类，继承 [AbstractFactor](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/factor/AbstractFactor.java)：

```java
package org.wall.im.quant.factor.quality;

import java.util.ArrayList;
import java.util.List;
import org.wall.im.quant.factor.*;
import org.wall.im.quant.model.Bar;
import org.wall.im.quant.stats.StatUtils;

public class SharpeRatioFactor extends AbstractFactor {

    public SharpeRatioFactor() {
        super(new FactorDescriptor(
                "sharpe_60",                        // 因子唯一标识
                "60日夏普比率",                      // 展示名
                FactorCategory.QUALITY,              // 分类
                "60日风险调整收益（均值/标准差）",     // 计算逻辑说明
                List.of(new FactorParameter(         // 可调参数
                        "windowDays",
                        FactorParameter.ParameterType.INTEGER,
                        60,
                        "计算窗口")),
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
        return (Double.isNaN(std) || std == 0) ? 0.0 : mean / std;
    }
}
```

### 第 2 步：注册因子

**编程式**：

```java
FactorRegistry registry = new FactorRegistry();
registry.registerAll(new MomentumFactor(), new SharpeRatioFactor());
```

**Spring Boot**：在 `QuantAutoConfiguration` 的 `factorRegistry()` 中追加，或定义自己的 `FactorRegistry` Bean。

### 第 3 步：调用

无需改动引擎或 Tool 代码：

```java
FactorCalculationRequest req = new FactorCalculationRequest(
        List.of("momentum_12_1", "sharpe_60"),  // 新因子名
        universe, LocalDate.now(),
        Map.of("windowDays", 60),               // 参数注入
        FactorProcessorChain.builder()
                .winsorize().standardize().build());

FactorCalculationResult res = engine.calculate(req);
```

注册后 `factor-query` Tool 也可自动使用新因子，Agent 只需在 `factors` 参数中传入 `"sharpe_60"`。

---

## 扩展场景速查

| 扩展场景 | 做法 | 是否需改框架 |
| --- | --- | --- |
| 同类因子不同窗口 | 传参覆盖默认值，如 `formationMonths=6` | 否 |
| 新增因子逻辑 | 继承 `AbstractFactor` → 填 descriptor → 注册 | 否 |
| 新增因子分类 | `FactorCategory` 枚举追加一项 | 仅加枚举 |
| 自定义后处理器 | 实现 `FactorProcessor`（lambda 或类）→ 加入链 | 否 |
| 接入真实数据源 | 实现 `MarketDataProvider` / `FundamentalDataProvider` → 定义 Bean | 否 |
| Agent 调用新因子 | 因子注册后自动可用，Tool 无需改动 | 否 |
| 因子依赖声明 | `FactorDescriptor` 无 dependencies 字段，当前不支持 | — |

---

## 编写规范

1. **因子标识**：使用小写下划线，格式 `类别_窗口` 或 `类别_参数`，如 `momentum_12_1`、`rsi_14`、`pe_ttm`。
2. **缺失值**：标的数据不足或计算异常时返回 `Double.NaN`，不要抛异常。
3. **参数**：所有可调参数通过 `FactorParameter` 声明，在 `compute` 中以 `context.getParameter(name, type, defaultValue)` 读取。
4. **数据获取**：通过 `context.getHistory(symbol, lookbackDays)` 获取 K 线（内部缓存），通过 `context.getFundamentals()` 获取基本面快照。
5. **包结构**：按分类建子包，如 `factor/momentum/`、`factor/value/`、`factor/quality/`。
6. **测试**：在 `im-quant-factor/src/test/java` 下编写因子测试，参考 [QuantEngineTest](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-factor/src/test/java/org/wall/im/quant/factor/QuantEngineTest.java) 的端到端测试模式。
