package org.wall.im.quant.factor;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 因子计算截面结果。
 *
 * <p>
 * 持有某因子在指定截面日、指定标的池上的全部取值，支持按 symbol 查询、缺失返回 null。 后处理器链以此对象为输入输出做原地或新实例转换。
 * </p>
 *
 * @param factorName 因子标识
 * @param asOfDate 截面日
 * @param values symbol -> 因子值（保序）
 */
public record FactorResult(String factorName, LocalDate asOfDate, Map<String, FactorValue> values) {

	public FactorResult(String factorName, LocalDate asOfDate, Collection<FactorValue> factorValues) {
		this(factorName, asOfDate, toMap(factorValues));
	}

	public FactorResult {
		Objects.requireNonNull(factorName, "factorName 不能为空");
		Objects.requireNonNull(asOfDate, "asOfDate 不能为空");
		values = (values == null) ? Map.of() : new LinkedHashMap<>(values);
	}

	private static Map<String, FactorValue> toMap(Collection<FactorValue> factorValues) {
		Map<String, FactorValue> map = new LinkedHashMap<>();
		if (factorValues != null) {
			for (FactorValue v : factorValues) {
				map.put(v.symbol(), v);
			}
		}
		return map;
	}

	public FactorValue valueOf(String symbol) {
		return this.values.get(symbol);
	}

	public int size() {
		return this.values.size();
	}

	/** 以新值集合返回一个新结果（后处理器使用）。 */
	public FactorResult withValues(Map<String, FactorValue> newValues) {
		return new FactorResult(this.factorName, this.asOfDate, newValues);
	}

}
