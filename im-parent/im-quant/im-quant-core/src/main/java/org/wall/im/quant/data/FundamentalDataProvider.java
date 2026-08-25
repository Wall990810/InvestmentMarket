package org.wall.im.quant.data;

import java.time.LocalDate;
import java.util.Map;

import org.wall.im.quant.model.FundamentalData;

/**
 * 基本面数据提供者。
 *
 * <p>
 * 价值/质量类因子的数据来源。按截面日期返回一批标的的最新基本面快照。
 * </p>
 */
public interface FundamentalDataProvider {

	/**
	 * 获取截面日期的最新基本面快照。
	 * @param symbols 标的代码集合
	 * @param asOfDate 截面日期
	 * @return symbol -> 基本面快照；缺失项以不包含该 key 表示
	 */
	Map<String, FundamentalData> getFundamentals(java.util.Collection<String> symbols, LocalDate asOfDate);

	/**
	 * 数据源标识。
	 */
	String getProviderName();

}
