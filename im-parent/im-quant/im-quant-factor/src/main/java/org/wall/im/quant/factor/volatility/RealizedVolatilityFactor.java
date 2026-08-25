package org.wall.im.quant.factor.volatility;

import java.util.ArrayList;
import java.util.List;

import org.wall.im.quant.factor.AbstractFactor;
import org.wall.im.quant.factor.FactorCategory;
import org.wall.im.quant.factor.FactorContext;
import org.wall.im.quant.factor.FactorDescriptor;
import org.wall.im.quant.factor.FactorParameter;
import org.wall.im.quant.factor.FactorResult;
import org.wall.im.quant.factor.FactorValue;
import org.wall.im.quant.model.Bar;

/**
 * 已实现波动率因子。
 *
 * <p>
 * 取最近 window（默认 20）日日收益序列，计算样本标准差并年化（×√252）。 衡量标的近期风险，高波动因子常对应高风险溢价。
 * </p>
 */
public class RealizedVolatilityFactor extends AbstractFactor {

	public RealizedVolatilityFactor() {
		super(new FactorDescriptor("realized_vol_20", "20日已实现波动率", FactorCategory.VOLATILITY, "日收益标准差年化",
				List.of(new FactorParameter("window", FactorParameter.ParameterType.INTEGER, 20, "回看窗口")), "1.0.0",
				"InvestmentMarket"));
	}

	@Override
	public FactorResult compute(FactorContext context) {
		int window = context.getParameter("window", Integer.class, 20);
		List<FactorValue> out = new ArrayList<>();
		for (String symbol : context.universe().symbols()) {
			List<Bar> bars = context.getHistory(symbol, window + 10);
			out.add(new FactorValue(symbol, realizedVol(bars, window)));
		}
		return new FactorResult(descriptor().name(), context.asOfDate(), out);
	}

	static double realizedVol(List<Bar> bars, int window) {
		if (bars == null || bars.size() <= window) {
			return Double.NaN;
		}
		int start = bars.size() - window;
		double[] rets = new double[window];
		for (int i = 0; i < window; i++) {
			double prev = bars.get(start + i - 1).close();
			double cur = bars.get(start + i).close();
			rets[i] = (prev <= 0) ? 0 : cur / prev - 1.0;
		}
		double mean = org.wall.im.quant.stats.StatUtils.mean(rets);
		double std = org.wall.im.quant.stats.StatUtils.std(rets);
		return Double.isNaN(std) ? Double.NaN : std * Math.sqrt(252);
	}

}
