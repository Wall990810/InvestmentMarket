package org.wall.im.quant.data;

import java.time.LocalDate;
import java.util.List;

import org.wall.im.quant.model.Bar;
import org.wall.im.quant.model.Frequency;

/**
 * 行情数据提供者。
 *
 * <p>
 * 抽象行情数据来源（Tushare / Wind / 聚宽 / 本地数据库）。因子计算通过此接口获取 K 线， 与具体数据源解耦。实现应保证返回的 K 线按日期升序、已前复权。
 * </p>
 */
public interface MarketDataProvider {

	/**
	 * 获取指定标的在时间区间内的 K 线序列。
	 * @param symbol 标的代码
	 * @param start 起始日（含）
	 * @param end 截止日（含）
	 * @param frequency K 线频率，缺省为 {@link Frequency#DAILY}
	 * @return 升序 K 线列表，无数据时返回空列表
	 */
	List<Bar> getBars(String symbol, LocalDate start, LocalDate end, Frequency frequency);

	/**
	 * 获取指定标的最新一根 K 线。
	 */
	default Bar getLatestBar(String symbol, LocalDate asOfDate) {
		List<Bar> bars = getBars(symbol, asOfDate.minusDays(10), asOfDate, Frequency.DAILY);
		return bars.isEmpty() ? null : bars.getLast();
	}

	/**
	 * 数据源标识，用于日志与监控。
	 */
	String getProviderName();

}
