package org.wall.im.quant.process;

import java.util.LinkedHashMap;
import java.util.Map;

import org.wall.im.quant.factor.FactorContext;
import org.wall.im.quant.factor.FactorResult;
import org.wall.im.quant.factor.FactorValue;
import org.wall.im.quant.stats.StatUtils;

/**
 * 标准化处理器（z-score）。
 *
 * <p>
 * 将截面值转换为均值 0、标准差 1 的分布，消除因子量纲差异。缺失值保持 NaN。
 * </p>
 */
public class StandardizeProcessor implements FactorProcessor {

	@Override
	public FactorResult process(FactorResult result, FactorContext context) {
		double[] values = result.values().values().stream().mapToDouble(FactorValue::value).toArray();
		double mean = StatUtils.mean(values);
		double std = StatUtils.std(values);
		Map<String, FactorValue> out = new LinkedHashMap<>();
		for (Map.Entry<String, FactorValue> e : result.values().entrySet()) {
			FactorValue v = e.getValue();
			out.put(e.getKey(), new FactorValue(e.getKey(), StatUtils.zscore(v.value(), mean, std), v.rank()));
		}
		return result.withValues(out);
	}

}
