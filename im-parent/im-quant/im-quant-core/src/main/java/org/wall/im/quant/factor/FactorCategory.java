package org.wall.im.quant.factor;

/**
 * 因子分类。
 *
 * <p>
 * 用于 {@link FactorRegistry} 分组查询与监控打标。新增分类时在此枚举追加即可， 不影响既有因子实现。
 * </p>
 */
public enum FactorCategory {

	/** 动量/反转 */
	MOMENTUM,
	/** 价值 */
	VALUE,
	/** 波动率 */
	VOLATILITY,
	/** 流动性/量价 */
	LIQUIDITY,
	/** 质量 */
	QUALITY,
	/** 规模 */
	SIZE,
	/** 成长 */
	GROWTH,
	/** 技术/形态 */
	TECHNICAL,
	/** 自定义 */
	CUSTOM

}
