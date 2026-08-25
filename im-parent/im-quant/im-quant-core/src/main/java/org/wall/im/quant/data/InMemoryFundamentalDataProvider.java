package org.wall.im.quant.data;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.wall.im.quant.model.FundamentalData;

/**
 * 内存基本面数据提供者（开发/测试用）。
 *
 * <p>
 * 按标的代码哈希生成确定性的基本面快照，覆盖 PE/PB/ROE/市值等字段， 便于价值/质量类因子在无真实数据源时跑通计算与测试。
 * </p>
 */
public class InMemoryFundamentalDataProvider implements FundamentalDataProvider {

	private final Map<String, FundamentalData> override = new HashMap<>();

	@Override
	public Map<String, FundamentalData> getFundamentals(Collection<String> symbols, LocalDate asOfDate) {
		Objects.requireNonNull(symbols, "symbols 不能为空");
		Map<String, FundamentalData> result = new HashMap<>();
		for (String symbol : symbols) {
			if (this.override.containsKey(symbol)) {
				result.put(symbol, this.override.get(symbol));
				continue;
			}
			long seed = symbol.hashCode() & 0xffffffffL;
			java.util.Random rng = new java.util.Random(seed);
			double pe = round(5.0 + rng.nextDouble() * 80.0);
			double pb = round(0.5 + rng.nextDouble() * 8.0);
			double ps = round(0.2 + rng.nextDouble() * 12.0);
			double dividend = round(rng.nextDouble() * 6.0);
			double roe = round(-5.0 + rng.nextDouble() * 35.0);
			double roa = round(-3.0 + rng.nextDouble() * 18.0);
			double debt = round(10.0 + rng.nextDouble() * 70.0);
			double marketCap = Math.round((5e8 + rng.nextDouble() * 5e10));
			long shares = Math.round(marketCap / (5.0 + rng.nextDouble() * 45.0));
			result.put(symbol,
					new FundamentalData(symbol, asOfDate, pe, pb, ps, dividend, roe, roa, debt, marketCap, shares));
		}
		return result;
	}

	/**
	 * 精确注入某标的基本面快照，覆盖合成值。
	 */
	public void register(String symbol, FundamentalData data) {
		this.override.put(Objects.requireNonNull(symbol), data);
	}

	@Override
	public String getProviderName() {
		return "in-memory-fundamental";
	}

	private double round(double v) {
		return Math.round(v * 100.0) / 100.0;
	}

}
