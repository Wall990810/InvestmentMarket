package org.wall.im.quant.factor;

/**
 * 因子核心抽象。
 *
 * <p>
 * 所有因子的统一契约。实现类只需提供 {@link #descriptor()} 与 {@link #compute(FactorContext)}， 即可被
 * {@link org.wall.im.quant.registry.FactorRegistry} 注册、被
 * {@link org.wall.im.quant.engine.FactorEngine} 调用、被后处理器链加工。
 * </p>
 *
 * <p>
 * 扩展指南：实现本接口（或继承 {@link AbstractFactor} 减少样板）→ 填充 descriptor → 在注册表注册。 无需改动引擎、Tool 或既有因子。
 * </p>
 */
public interface Factor {

	/** 因子元数据。 */
	FactorDescriptor descriptor();

	/**
	 * 在给定上下文上计算因子截面值。
	 * <p>
	 * 实现应遍历 {@link FactorContext#universe()} 的标的，逐个取值并返回 {@link FactorResult}；
	 * 个别标的数据缺失时应以 {@link Double#NaN} 占位而非抛异常，以保证截面完整性。
	 * </p>
	 */
	FactorResult compute(FactorContext context);

}
