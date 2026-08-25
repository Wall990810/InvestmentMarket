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
 * RSI 相对强弱因子。
 *
 * <p>
 * 计算周期 N（默认 14）日 RSI = 100 - 100/(1+RS)，RS = 平均涨幅 / 平均跌幅。 高 RSI 表示超买、低 RSI
 * 表示超卖，常作为反转/择时信号。
 * </p>
 */
public class RsiFactor extends AbstractFactor {

	public RsiFactor() {
		super(new FactorDescriptor("rsi_14", "RSI(14)", FactorCategory.MOMENTUM, "14日相对强弱指标",
				List.of(new FactorParameter("period", FactorParameter.ParameterType.INTEGER, 14, "RSI 周期")), "1.0.0",
				"InvestmentMarket"));
	}

	@Override
	public FactorResult compute(FactorContext context) {
		int period = context.getParameter("period", Integer.class, 14);
		int lookback = period + 20;
		List<FactorValue> out = new ArrayList<>();
		for (String symbol : context.universe().symbols()) {
			List<Bar> bars = context.getHistory(symbol, lookback);
			out.add(new FactorValue(symbol, rsi(bars, period)));
		}
		return new FactorResult(descriptor().name(), context.asOfDate(), out);
	}

	static double rsi(List<Bar> bars, int period) {
		if (bars == null || bars.size() <= period) {
			return Double.NaN;
		}
		double avgGain = 0;
		double avgLoss = 0;
		// 用最近 period+1 根收盘差分
		int start = bars.size() - period - 1;
		for (int i = start + 1; i < bars.size(); i++) {
			double diff = bars.get(i).close() - bars.get(i - 1).close();
			if (diff >= 0) {
				avgGain += diff;
			}
			else {
				avgLoss -= diff;
			}
		}
		avgGain /= period;
		avgLoss /= period;
		if (avgLoss == 0) {
			return 100.0;
		}
		double rs = avgGain / avgLoss;
		return 100.0 - 100.0 / (1.0 + rs);
	}

}
