package org.wall.im.quant.factor;

import java.util.Objects;

/**
 * 单标的因子值。
 *
 * @param symbol 标的代码
 * @param value 因子原始值；缺失以 {@link Double#NaN} 表示
 * @param rank 截面排名（从大到小，1 表示最大），缺省 0 表示未排名
 */
public record FactorValue(String symbol, double value, int rank) {

	public FactorValue(String symbol, double value) {
		this(symbol, value, 0);
	}

	public FactorValue {
		Objects.requireNonNull(symbol, "symbol 不能为空");
	}

	public boolean hasValue() {
		return !Double.isNaN(this.value);
	}

}
