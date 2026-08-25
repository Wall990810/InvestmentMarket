package org.wall.im.quant.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * 标的基础信息。
 *
 * <p>
 * 描述一只可交易标的（股票 / 基金 / 指数等）的静态元数据，供因子计算与标的池管理使用。
 * </p>
 *
 * @param symbol 标的代码，如 "000001.SZ"
 * @param name 标的名称，如 "平安银行"
 * @param exchange 交易所，如 "SZSE"/"SSE"
 * @param type 标的类型，如 "STOCK"/"ETF"/"INDEX"
 * @param sector 所属行业/板块，用于行业中性化
 * @param listDate 上市日期
 */
public record Instrument(String symbol, String name, String exchange, String type, String sector, LocalDate listDate) {

	public Instrument {
		Objects.requireNonNull(symbol, "symbol 不能为空");
		if (symbol.isBlank()) {
			throw new IllegalArgumentException("symbol 不能为空白");
		}
	}

}
