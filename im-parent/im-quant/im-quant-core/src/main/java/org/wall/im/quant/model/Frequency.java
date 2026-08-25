package org.wall.im.quant.model;

/**
 * K 线频率。
 *
 * <p>
 * 当前以日线为主，预留周线 / 月线以支持低频因子。
 * </p>
 */
public enum Frequency {

	/** 日线 */
	DAILY(252),
	/** 周线 */
	WEEKLY(52),
	/** 月线 */
	MONTHLY(12);

	/** 每年交易日/周期数，用于年化波动率等计算 */
	private final int periodsPerYear;

	Frequency(int periodsPerYear) {
		this.periodsPerYear = periodsPerYear;
	}

	public int getPeriodsPerYear() {
		return this.periodsPerYear;
	}

}
