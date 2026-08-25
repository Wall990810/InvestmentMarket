package org.wall.im.quant.process;

import java.util.LinkedHashMap;
import java.util.Map;

import org.wall.im.quant.factor.FactorContext;
import org.wall.im.quant.factor.FactorResult;
import org.wall.im.quant.factor.FactorValue;
import org.wall.im.quant.stats.StatUtils;

/**
 * 缩尾处理器（去极值）。
 *
 * <p>
 * 将超出分位阈值的值截断到阈值。默认下界 1% / 上界 99%，常用于剔除极端异常值后再标准化。
 * </p>
 *
 * @param lowerPct 下界分位（0~100）
 * @param upperPct 上界分位（0~100）
 */
public class WinsorizeProcessor implements FactorProcessor {

	private final double lowerPct;

	private final double upperPct;

	public WinsorizeProcessor(double lowerPct, double upperPct) {
		if (lowerPct < 0 || lowerPct > 100 || upperPct < 0 || upperPct > 100 || lowerPct >= upperPct) {
			throw new IllegalArgumentException("分位参数非法: lower=" + lowerPct + ", upper=" + upperPct);
		}
		this.lowerPct = lowerPct;
		this.upperPct = upperPct;
	}

	public WinsorizeProcessor() {
		this(1.0, 99.0);
	}

	@Override
	public FactorResult process(FactorResult result, FactorContext context) {
		double[] values = toArray(result);
		double lower = StatUtils.percentile(values, this.lowerPct);
		double upper = StatUtils.percentile(values, this.upperPct);
		Map<String, FactorValue> out = new LinkedHashMap<>();
		for (Map.Entry<String, FactorValue> e : result.values().entrySet()) {
			FactorValue v = e.getValue();
			double clipped = v.value();
			if (v.hasValue()) {
				clipped = Math.min(Math.max(v.value(), lower), upper);
			}
			out.put(e.getKey(), new FactorValue(e.getKey(), clipped, v.rank()));
		}
		return result.withValues(out);
	}

	private static double[] toArray(FactorResult result) {
		return result.values().values().stream().mapToDouble(FactorValue::value).toArray();
	}

}
