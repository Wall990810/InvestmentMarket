package org.wall.im.quant.engine;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.wall.im.quant.model.Universe;
import org.wall.im.quant.process.FactorProcessorChain;

/**
 * 因子计算请求。
 *
 * @param factorNames 待计算的因子标识列表
 * @param universe 标的池
 * @param asOfDate 截面日
 * @param parameters 全局参数表，会注入到每个因子的 {@link org.wall.im.quant.factor.FactorContext}
 * @param processorChain 可选的后处理器链，null 表示不做后处理
 */
public record FactorCalculationRequest(List<String> factorNames, Universe universe, LocalDate asOfDate,
		Map<String, Object> parameters, FactorProcessorChain processorChain) {

	public FactorCalculationRequest {
		Objects.requireNonNull(factorNames, "factorNames 不能为空");
		Objects.requireNonNull(universe, "universe 不能为空");
		Objects.requireNonNull(asOfDate, "asOfDate 不能为空");
		factorNames = List.copyOf(factorNames);
	}

}
