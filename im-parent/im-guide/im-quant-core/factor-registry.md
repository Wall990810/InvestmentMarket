# ← 返回索引

# 因子注册表

## FactorRegistry

源码：[FactorRegistry.java](file:///c:/Users/钟世超/IdeaProjects/InvestmentMarket/im-parent/im-quant/im-quant-core/src/main/java/org/wall/im/quant/registry/FactorRegistry.java)

进程级因子仓库，提供按名/分类查询与批量注册。线程安全（基于 `ConcurrentHashMap`）。同名因子重复注册会覆盖旧实例。

| 方法签名 | 说明 |
| --- | --- |
| `void register(Factor factor)` | 注册单个因子（按 `descriptor().name()` 索引）。 |
| `void registerAll(Factor... toRegister)` | 批量注册。 |
| `Factor get(String name)` | 按名获取，缺失返回 null。 |
| `Factor getRequired(String name)` | 按名获取，缺失抛 `IllegalArgumentException`。 |
| `List<Factor> getByCategory(FactorCategory category)` | 按分类过滤。 |
| `List<Factor> getAll()` | 全部因子。 |
| `List<String> getAllNames()` | 全部因子名（排序）。 |
| `boolean contains(String name)` | 是否包含。 |
| `int size()` | 因子数量。 |
| `Factor unregister(String name)` | 注销因子，返回被移除的实例。 |

### 注册示例

```java
FactorRegistry registry = new FactorRegistry();
registry.registerAll(new MomentumFactor(), new PeRatioFactor(), new RsiFactor());

// 按分类查询
List<Factor> valueFactors = registry.getByCategory(FactorCategory.VALUE);

// 引擎通过 getRequired 解析
Factor f = registry.getRequired("momentum_12_1");
```

> 在 Spring Boot 应用中，`QuantAutoConfiguration` 会自动注册全部内置因子的 `FactorRegistry` Bean。自定义因子可通过定义自己的 `FactorRegistry` Bean 覆盖，或在自动装配后手动追加注册。
