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
 * 市净率（PB）因子。
 *
 * <p>
 * 价值因子。低 PB 表示估值便宜，破净（PB&lt;1）常用于银行等行业的底部信号。 负净资产以 NaN 占位。
 * </p>
 */
public class PbRatioFactor extends AbstractFactor {

	public PbRatioFactor() {
		super(new FactorDescriptor("pb", "市净率", FactorCategory.VALUE, "市净率，净资产为负记为缺失", List.of(), "1.0.0",
				"InvestmentMarket"));
	}

	@Override
	public FactorResult compute(FactorContext context) {
		Map<String, FundamentalData> fundamentals = context.getFundamentals();
		List<FactorValue> out = new ArrayList<>();
		for (String symbol : context.universe().symbols()) {
			FundamentalData fd = fundamentals.get(symbol);
			double value = Double.NaN;
			if (fd != null && fd.pb() > 0) {
				value = fd.pb();
			}
			out.add(new FactorValue(symbol, value));
		}
		return new FactorResult(descriptor().name(), context.asOfDate(), out);
	}

}
