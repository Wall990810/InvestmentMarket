package org.wall.im.quant.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * 标的基本面快照。
 *
 * <p>
 * 价值/质量类因子的输入。所有字段使用"已披露最新值"，缺失以 {@link Double#NaN} 表示， 因子实现应通过
 * {@link Double#isNaN(double)} 判空。
 * </p>
 *
 * @param symbol 标的代码
 * @param asOfDate 数据截止日
 * @param pe 市盈率（TTM）
 * @param pb 市净率
 * @param ps 市销率
 * @param dividendYear 股息率（百分比，如 3.5 表示 3.5%）
 * @param roe 净资产收益率（百分比）
 * @param roa 总资产收益率（百分比）
 * @param debtRatio 资产负债率（百分比）
 * @param marketCap 总市值（元）
 * @param sharesOutstanding 总股本（股）
 */
public record FundamentalData(String symbol, LocalDate asOfDate, double pe, double pb, double ps, double dividendYear,
		double roe, double roa, double debtRatio, double marketCap, long sharesOutstanding) {

	public FundamentalData {
		Objects.requireNonNull(symbol, "symbol 不能为空");
		Objects.requireNonNull(asOfDate, "asOfDate 不能为空");
	}

}
