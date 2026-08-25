package org.wall.im.quant.engine;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.wall.im.quant.factor.FactorResult;

/**
 * 因子计算结果集合。
 *
 * @param results 因子标识 -> 截面结果（保序）
 * @param costTimeMs 总耗时（毫秒）
 */
public record FactorCalculationResult(Map<String, FactorResult> results, long costTimeMs) {

	public FactorCalculationResult {
		Objects.requireNonNull(results, "results 不能为空");
		results = new LinkedHashMap<>(results);
	}

	public FactorResult get(String factorName) {
		return this.results.get(factorName);
	}

	public int size() {
		return this.results.size();
	}

}
