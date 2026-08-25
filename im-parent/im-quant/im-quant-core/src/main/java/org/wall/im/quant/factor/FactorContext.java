package org.wall.im.quant.factor;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.wall.im.quant.data.FundamentalDataProvider;
import org.wall.im.quant.data.MarketDataProvider;
import org.wall.im.quant.model.Bar;
import org.wall.im.quant.model.Frequency;
import org.wall.im.quant.model.FundamentalData;
import org.wall.im.quant.model.Universe;

/**
 * 因子计算上下文。
 *
 * <p>
 * 一次截面计算的环境载体：数据源、标的池、截面日、参数表与 K 线缓存。 因子实现通过本对象获取行情与基本面，并以
 * {@link #getParameter(String, Class)} 读取可调参数。
 * </p>
 */
public class FactorContext {

	private final MarketDataProvider marketDataProvider;

	private final FundamentalDataProvider fundamentalDataProvider;

	private final Universe universe;

	private final LocalDate asOfDate;

	private final Map<String, Object> parameters;

	private final Map<String, List<Bar>> barCache = new HashMap<>();

	private Map<String, FundamentalData> fundamentalsCache;

	public FactorContext(MarketDataProvider marketDataProvider, FundamentalDataProvider fundamentalDataProvider,
			Universe universe, LocalDate asOfDate, Map<String, Object> parameters) {
		this.marketDataProvider = Objects.requireNonNull(marketDataProvider, "marketDataProvider 不能为空");
		this.fundamentalDataProvider = Objects.requireNonNull(fundamentalDataProvider, "fundamentalDataProvider 不能为空");
		this.universe = Objects.requireNonNull(universe, "universe 不能为空");
		this.asOfDate = Objects.requireNonNull(asOfDate, "asOfDate 不能为空");
		this.parameters = (parameters == null) ? Collections.emptyMap() : new HashMap<>(parameters);
	}

	/** 获取某标的截至截面日的回看 K 线（含截面日），按日升序。 */
	public List<Bar> getHistory(String symbol, int lookbackDays) {
		return getHistory(symbol, lookbackDays, Frequency.DAILY);
	}

	public List<Bar> getHistory(String symbol, int lookbackDays, Frequency frequency) {
		String key = symbol + "|" + lookbackDays + "|" + frequency.name();
		// lookbackDays 以交易日计，需放大为日历日以覆盖周末与节假日（约 7/5 倍 + 缓冲）
		int calendarDays = (int) Math.ceil(lookbackDays * 1.5) + 20;
		return this.barCache.computeIfAbsent(key, k -> this.marketDataProvider.getBars(symbol,
				this.asOfDate.minusDays(calendarDays), this.asOfDate, frequency));
	}

	/** 获取截面基本面快照。 */
	public Map<String, FundamentalData> getFundamentals() {
		if (this.fundamentalsCache == null) {
			this.fundamentalsCache = this.fundamentalDataProvider.getFundamentals(this.universe.symbols(),
					this.asOfDate);
		}
		return this.fundamentalsCache;
	}

	/** 读取参数并按目标类型转换，缺失则抛异常。 */
	public <T> T getParameter(String name, Class<T> type) {
		Object raw = this.parameters.get(name);
		if (raw == null) {
			throw new IllegalArgumentException("缺少参数: " + name);
		}
		return convert(raw, type);
	}

	/** 读取参数，缺失返回默认值。 */
	@SuppressWarnings("unchecked")
	public <T> T getParameter(String name, Class<T> type, T defaultValue) {
		Object raw = this.parameters.get(name);
		if (raw == null) {
			return defaultValue;
		}
		try {
			return convert(raw, type);
		}
		catch (Exception ex) {
			return defaultValue;
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> T convert(Object raw, Class<T> type) {
		if (type.isInstance(raw)) {
			return (T) raw;
		}
		String s = raw.toString();
		if (type == Integer.class || type == int.class) {
			return (T) Integer.valueOf(s);
		}
		if (type == Double.class || type == double.class) {
			return (T) Double.valueOf(s);
		}
		if (type == Boolean.class || type == boolean.class) {
			return (T) Boolean.valueOf(s);
		}
		return (T) s;
	}

	public Universe universe() {
		return this.universe;
	}

	public LocalDate asOfDate() {
		return this.asOfDate;
	}

}
