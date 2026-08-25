package org.wall.im.quant.process;

import org.wall.im.quant.factor.FactorContext;
import org.wall.im.quant.factor.FactorResult;

/**
 * 因子后处理器。
 *
 * <p>
 * 对因子截面值做变换：去极值 / 标准化 / 中性化 / 缺失填补等。实现应保持 {@link FactorResult#factorName()} 与
 * {@link FactorResult#asOfDate()} 不变。
 * </p>
 */
@FunctionalInterface
public interface FactorProcessor {

	/**
	 * 处理一个因子截面结果。
	 * @param result 原始结果
	 * @param context 计算上下文（可取行业、universe 等元信息做中性化）
	 * @return 处理后的结果（可为同一实例或新实例）
	 */
	FactorResult process(FactorResult result, FactorContext context);

	/** 处理器名称，用于日志。 */
	default String name() {
		return getClass().getSimpleName();
	}

}
