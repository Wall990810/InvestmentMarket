package org.wall.im.quant.factor.liquidity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.wall.im.quant.factor.AbstractFactor;
import org.wall.im.quant.factor.FactorCategory;
import org.wall.im.quant.factor.FactorContext;
import org.wall.im.quant.factor.FactorDescriptor;
import org.wall.im.quant.factor.FactorParameter;
import org.wall.im.quant.factor.FactorResult;
import org.wall.im.quant.factor.FactorValue;
import org.wall.im.quant.model.Bar;
import org.wall.im.quant.model.FundamentalData;

/**
 * 换手率因子。
 *
 * <p>
 * 取最近 window（默认 20）日均成交额与总市值之比，作为换手率代理。 高换手率通常对应高流动性、低个股特异风险。
 * </p>
 */
public class TurnoverFactor extends AbstractFactor {

	public TurnoverFactor() {
		super(new FactorDescriptor("turnover_20", "20日换手率", FactorCategory.LIQUIDITY, "成交额/总市值（均值）",
				List.of(new FactorParameter("window", FactorParameter.ParameterType.INTEGER, 20, "回看窗口")), "1.0.0",
				"InvestmentMarket"));
	}

	@Override
	public FactorResult compute(FactorContext context) {
		int window = context.getParameter("window", Integer.class, 20);
		Map<String, FundamentalData> fundamentals = context.getFundamentals();
		List<FactorValue> out = new ArrayList<>();
		for (String symbol : context.universe().symbols()) {
			List<Bar> bars = context.getHistory(symbol, window + 10);
			FundamentalData fd = fundamentals.get(symbol);
			out.add(new FactorValue(symbol, turnover(bars, window, fd)));
		}
		return new FactorResult(descriptor().name(), context.asOfDate(), out);
	}

	static double turnover(List<Bar> bars, int window, FundamentalData fd) {
		if (bars == null || bars.size() < window || fd == null || fd.marketCap() <= 0) {
			return Double.NaN;
		}
		int start = bars.size() - window;
		double sum = 0;
		for (int i = start; i < bars.size(); i++) {
			sum += bars.get(i).amount();
		}
		return sum / window / fd.marketCap();
	}

}
