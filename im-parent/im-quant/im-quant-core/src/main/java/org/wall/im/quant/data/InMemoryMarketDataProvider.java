package org.wall.im.quant.data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.wall.im.quant.model.Bar;
import org.wall.im.quant.model.Frequency;

/**
 * 内存行情数据提供者（开发/测试用）。
 *
 * <p>
 * 双模式工作：若显式 {@link #register(String, List)} 注入 K 线则按注入数据返回；
 * 否则对未注入标的按确定性几何布朗运动生成合成日线，保证任意标的、任意区间均有数据可用、 且结果可复现（以 symbol 哈希为随机种子）。
 * </p>
 */
public class InMemoryMarketDataProvider implements MarketDataProvider {

	private final Map<String, List<Bar>> store = new ConcurrentHashMap<>();

	@Override
	public List<Bar> getBars(String symbol, LocalDate start, LocalDate end, Frequency frequency) {
		Objects.requireNonNull(symbol, "symbol 不能为空");
		Frequency freq = (frequency == null) ? Frequency.DAILY : frequency;
		List<Bar> all = this.store.computeIfAbsent(symbol, this::generate);
		List<Bar> filtered = new ArrayList<>();
		for (Bar bar : all) {
			if (!bar.date().isBefore(start) && !bar.date().isAfter(end)) {
				filtered.add(bar);
			}
		}
		return downsample(filtered, freq);
	}

	/**
	 * 注入某标的的 K 线序列（会覆盖既有数据）。用于精确控制测试数据。
	 */
	public void register(String symbol, List<Bar> bars) {
		Objects.requireNonNull(symbol, "symbol 不能为空");
		Objects.requireNonNull(bars, "bars 不能为空");
		List<Bar> sorted = bars.stream().sorted((a, b) -> a.date().compareTo(b.date())).toList();
		this.store.put(symbol, sorted);
	}

	@Override
	public String getProviderName() {
		return "in-memory-market";
	}

	private List<Bar> generate(String symbol) {
		long seed = symbol.hashCode() & 0xffffffffL;
		java.util.Random rng = new java.util.Random(seed);
		LocalDate end = LocalDate.now();
		LocalDate start = end.minusYears(3);
		double price = 10.0 + rng.nextDouble() * 40.0;
		List<Bar> bars = new ArrayList<>();
		LocalDate cursor = start;
		double adj = 1.0;
		while (!cursor.isAfter(end)) {
			if (isTradingDay(cursor)) {
				double drift = (rng.nextDouble() - 0.48) * 0.04;
				double open = price;
				double close = Math.max(0.5, open * (1.0 + drift));
				double high = Math.max(open, close) * (1.0 + rng.nextDouble() * 0.015);
				double low = Math.min(open, close) * (1.0 - rng.nextDouble() * 0.015);
				long volume = 500_000 + rng.nextLong(5_000_000);
				double amount = close * volume;
				bars.add(new Bar(symbol, cursor, round(open), round(high), round(low), round(close), volume,
						round(amount), adj));
				price = close;
			}
			cursor = cursor.plusDays(1);
		}
		return List.copyOf(bars);
	}

	private boolean isTradingDay(LocalDate date) {
		return date.getDayOfWeek().getValue() <= 5;
	}

	private List<Bar> downsample(List<Bar> daily, Frequency freq) {
		if (freq == Frequency.DAILY || daily.isEmpty()) {
			return daily;
		}
		// 简单周/月聚合：按周/月取最后一根
		List<Bar> out = new ArrayList<>();
		LocalDate bucket = null;
		Bar last = null;
		for (Bar bar : daily) {
			LocalDate key = bucketKey(bar.date(), freq);
			if (bucket == null || !bucket.equals(key)) {
				if (last != null) {
					out.add(last);
				}
				bucket = key;
			}
			last = bar;
		}
		if (last != null) {
			out.add(last);
		}
		return out;
	}

	private LocalDate bucketKey(LocalDate date, Frequency freq) {
		return switch (freq) {
			case WEEKLY -> date.with(java.time.DayOfWeek.MONDAY);
			case MONTHLY -> date.withDayOfMonth(1);
			default -> date;
		};
	}

	private double round(double v) {
		return Math.round(v * 100.0) / 100.0;
	}

}
