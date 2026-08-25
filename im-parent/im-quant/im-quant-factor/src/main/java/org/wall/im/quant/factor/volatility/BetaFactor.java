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
 * Beta 因子（相对基准）。
 *
 * <p>
 * 以 benchmark（默认"000300.SH"沪深300）为市场基准，计算最近 window（默认 60）日 标的收益对基准收益的 OLS 斜率 β = Cov(r_i,
 * r_m) / Var(r_m)。
 * </p>
 */
public class BetaFactor extends AbstractFactor {

	public BetaFactor() {
		super(new FactorDescriptor("beta_60", "60日Beta", FactorCategory.VOLATILITY, "相对基准的60日OLS贝塔",
				List.of(new FactorParameter("window", FactorParameter.ParameterType.INTEGER, 60, "回看窗口"),
						new FactorParameter("benchmark", FactorParameter.ParameterType.STRING, "000300.SH", "基准标的代码")),
				"1.0.0", "InvestmentMarket"));
	}

	@Override
	public FactorResult compute(FactorContext context) {
		int window = context.getParameter("window", Integer.class, 60);
		String benchmark = context.getParameter("benchmark", String.class, "000300.SH");
		List<Bar> benchBars = context.getHistory(benchmark, window + 10);
		double[] benchRets = returns(benchBars, window);
		List<FactorValue> out = new ArrayList<>();
		for (String symbol : context.universe().symbols()) {
			List<Bar> bars = context.getHistory(symbol, window + 10);
			out.add(new FactorValue(symbol, beta(returns(bars, window), benchRets)));
		}
		return new FactorResult(descriptor().name(), context.asOfDate(), out);
	}

	private static double[] returns(List<Bar> bars, int window) {
		if (bars == null || bars.size() <= window) {
			return new double[0];
		}
		int start = bars.size() - window;
		double[] rets = new double[window];
		for (int i = 0; i < window; i++) {
			double prev = bars.get(start + i - 1).close();
			double cur = bars.get(start + i).close();
			rets[i] = (prev <= 0) ? 0 : cur / prev - 1.0;
		}
		return rets;
	}

	static double beta(double[] asset, double[] market) {
		if (asset.length < 2 || asset.length != market.length) {
			return Double.NaN;
		}
		double meanA = org.wall.im.quant.stats.StatUtils.mean(asset);
		double meanM = org.wall.im.quant.stats.StatUtils.mean(market);
		double cov = 0;
		double varM = 0;
		for (int i = 0; i < asset.length; i++) {
			cov += (asset[i] - meanA) * (market[i] - meanM);
			varM += (market[i] - meanM) * (market[i] - meanM);
		}
		return varM == 0 ? Double.NaN : cov / varM;
	}

}
