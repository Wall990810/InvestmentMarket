package org.wall.im.quant.model;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * 标的池。
 *
 * <p>
 * 一组参与因子计算的标的集合，如"沪深300""全A"。因子引擎在给定 universe 上输出截面值。
 * </p>
 *
 * @param name 标的池名称
 * @param symbols 标的代码列表（有序、去重）
 */
public record Universe(String name, List<String> symbols) {

	public Universe(String name, Collection<String> symbols) {
		this(name, symbols == null ? List.of() : symbols.stream().distinct().sorted().toList());
	}

	public Universe {
		Objects.requireNonNull(name, "name 不能为空");
		Objects.requireNonNull(symbols, "symbols 不能为空");
		symbols = List.copyOf(symbols);
	}

	public int size() {
		return this.symbols.size();
	}

	public boolean contains(String symbol) {
		return this.symbols.contains(symbol);
	}

}
