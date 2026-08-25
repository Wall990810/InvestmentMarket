package org.wall.im.quant.factor;

/**
 * 因子骨架基类。
 *
 * <p>
 * 封装 {@link #descriptor()} 字段持有，子类只需实现 {@link #compute(FactorContext)}，
 * 减少样板代码。同时提供常用横截面取值工具方法。
 * </p>
 */
public abstract class AbstractFactor implements Factor {

	private final FactorDescriptor descriptor;

	protected AbstractFactor(FactorDescriptor descriptor) {
		this.descriptor = java.util.Objects.requireNonNull(descriptor, "descriptor 不能为空");
	}

	@Override
	public final FactorDescriptor descriptor() {
		return this.descriptor;
	}

}
