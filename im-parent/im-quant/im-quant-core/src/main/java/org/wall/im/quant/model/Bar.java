package org.wall.im.quant.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * OHLCV K 线。
 *
 * <p>
 * 因子计算的最小行情单元，包含开高低收、成交量、成交额与前复权因子。
 * </p>
 *
 * @param symbol 标的代码
 * @param date 交易日
 * @param open 开盘价（前复权）
 * @param high 最高价（前复权）
 * @param low 最低价（前复权）
 * @param close 收盘价（前复权）
 * @param volume 成交量（股）
 * @param amount 成交额（元）
 * @param adjFactor 复权因子，缺省 1.0
 */
public record Bar(String symbol, LocalDate date, double open, double high, double low, double close, long volume,
		double amount, double adjFactor) {

	public Bar {
		Objects.requireNonNull(symbol, "symbol 不能为空");
		Objects.requireNonNull(date, "date 不能为空");
		if (adjFactor <= 0) {
			throw new IllegalArgumentException("adjFactor 必须为正数");
		}
	}

	public double typicalPrice() {
		return (this.high + this.low + this.close) / 3.0;
	}

	public double returnRate() {
		if (this.close == 0.0 || this.open == 0.0) {
			return 0.0;
		}
		return this.close / this.open - 1.0;
	}

}
