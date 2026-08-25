package org.wall.im.quant.factor.momentum;

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
 * 12-1 月动量因子（Jegadeesh-Titman 经典动量）。
 *
 * <p>
 * 计算逻辑：跳过最近 1 个月，取此前 12 个月的累计收益 = P_{t-skip}/P_{t-skip-formation} - 1。 剔除最近一月以避免短期反转噪声。
 * </p>
 *
 * <p>
 * <b>扩展示意</b>：通过 formationMonths / skipMonths 参数即可衍生 6-1、3-1 等动量窗口， 无需新写类。如需 EMA
 * 动量，新增子类实现即可。
 * </p>
 */
public class MomentumFactor extends AbstractFactor {

	/** 每月约 21 个交易日 */
	private static final int TRADING_DAYS_PER_MONTH = 21;

	public MomentumFactor() {
		super(new FactorDescriptor("momentum_12_1", "12-1月动量", FactorCategory.MOMENTUM, "跳过最近1个月、取此前12个月累计收益",
				List.of(new FactorParameter("formationMonths", FactorParameter.ParameterType.INTEGER, 12, "动量形成期（月）"),
						new FactorParameter("skipMonths", FactorParameter.ParameterType.INTEGER, 1, "跳过月数")),
				"1.0.0", "InvestmentMarket"));
	}

	@Override
	public FactorResult compute(FactorContext context) {
		int formation = context.getParameter("formationMonths", Integer.class, 12);
		int skip = context.getParameter("skipMonths", Integer.class, 1);
		int lookbackDays = (formation + skip) * TRADING_DAYS_PER_MONTH + 15;
		List<FactorValue> out = new ArrayList<>();
		for (String symbol : context.universe().symbols()) {
			List<Bar> bars = context.getHistory(symbol, lookbackDays);
			out.add(new FactorValue(symbol, momentum(bars, formation, skip)));
		}
		return new FactorResult(descriptor().name(), context.asOfDate(), out);
	}

	static double momentum(List<Bar> bars, int formationMonths, int skipMonths) {
		if (bars == null || bars.size() < (formationMonths + skipMonths) * TRADING_DAYS_PER_MONTH) {
			return Double.NaN;
		}
		int endIdx = bars.size() - 1 - skipMonths * TRADING_DAYS_PER_MONTH;
		int startIdx = endIdx - formationMonths * TRADING_DAYS_PER_MONTH;
		if (startIdx < 0 || endIdx < 0) {
			return Double.NaN;
		}
		double startPrice = bars.get(startIdx).close();
		double endPrice = bars.get(endIdx).close();
		if (startPrice <= 0) {
			return Double.NaN;
		}
		return endPrice / startPrice - 1.0;
	}

}
