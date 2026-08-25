package org.wall.im.quant.factor.liquidity;

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
 * Amihud 非流动性因子。
 *
 * <p>
 * Amihud(2002) 非流动性 = mean( |r_t| / Amount_t )，衡量单位成交额对应的价格冲击。 数值越大，流动性越差。常作为流动性溢价的代理变量。
 * </p>
 */
public class AmihudIlliquidityFactor extends AbstractFactor {

	public AmihudIlliquidityFactor() {
		super(new FactorDescriptor("amihud_illiq_20", "Amihud非流动性", FactorCategory.LIQUIDITY, "日均 |收益|/成交额",
				List.of(new FactorParameter("window", FactorParameter.ParameterType.INTEGER, 20, "回看窗口")), "1.0.0",
				"InvestmentMarket"));
	}

	@Override
	public FactorResult compute(FactorContext context) {
		int window = context.getParameter("window", Integer.class, 20);
		List<FactorValue> out = new ArrayList<>();
		for (String symbol : context.universe().symbols()) {
			List<Bar> bars = context.getHistory(symbol, window + 10);
			out.add(new FactorValue(symbol, amihud(bars, window)));
		}
		return new FactorResult(descriptor().name(), context.asOfDate(), out);
	}

	static double amihud(List<Bar> bars, int window) {
		if (bars == null || bars.size() <= window) {
			return Double.NaN;
		}
		int start = bars.size() - window;
		double sum = 0;
		int n = 0;
		for (int i = start; i < bars.size(); i++) {
			double prev = bars.get(i - 1).close();
			double cur = bars.get(i).close();
			double amount = bars.get(i).amount();
			if (prev <= 0 || amount <= 0) {
				continue;
			}
			sum += Math.abs(cur / prev - 1.0) / amount;
			n++;
		}
		if (n == 0) {
			return Double.NaN;
		}
		// 放大 1e9 倍便于读数
		return sum / n * 1e9;
	}

}
