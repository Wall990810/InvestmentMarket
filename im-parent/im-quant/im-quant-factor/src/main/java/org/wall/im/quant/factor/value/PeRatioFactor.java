package org.wall.im.quant.factor.value;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.wall.im.quant.factor.AbstractFactor;
import org.wall.im.quant.factor.FactorCategory;
import org.wall.im.quant.factor.FactorContext;
import org.wall.im.quant.factor.FactorDescriptor;
import org.wall.im.quant.factor.FactorResult;
import org.wall.im.quant.factor.FactorValue;
import org.wall.im.quant.model.FundamentalData;

/**
 * 市盈率（PE TTM）因子。
 *
 * <p>
 * 经典价值因子。低 PE 表示估值便宜。本实现直接输出 PE 原值；如需"越便宜越大"的方向， 可在后处理或自定义子类取负值（{@code -pe}）。负 PE（亏损）以
 * NaN 占位。
 * </p>
 */
public class PeRatioFactor extends AbstractFactor {

	public PeRatioFactor() {
		super(new FactorDescriptor("pe_ttm", "市盈率(TTM)", FactorCategory.VALUE, "滚动市盈率，亏损记为缺失", List.of(), "1.0.0",
				"InvestmentMarket"));
	}

	@Override
	public FactorResult compute(FactorContext context) {
		Map<String, FundamentalData> fundamentals = context.getFundamentals();
		List<FactorValue> out = new ArrayList<>();
		for (String symbol : context.universe().symbols()) {
			FundamentalData fd = fundamentals.get(symbol);
			double value = Double.NaN;
			if (fd != null && fd.pe() > 0) {
				value = fd.pe();
			}
			out.add(new FactorValue(symbol, value));
		}
		return new FactorResult(descriptor().name(), context.asOfDate(), out);
	}

}
