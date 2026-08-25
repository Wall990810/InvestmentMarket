package org.wall.im.quant.factor;

import java.util.Objects;

/**
 * 因子参数定义。
 *
 * <p>
 * 声明因子可调参数（如动量窗口、分位阈值），使因子可在不改动代码的前提下被引擎/配置覆盖。 默认值在
 * {@link FactorContext#getParameter(String)} 中取用。
 * </p>
 *
 * @param name 参数名
 * @param type 参数类型
 * @param defaultValue 默认值
 * @param description 说明
 */
public record FactorParameter(String name, ParameterType type, Object defaultValue, String description) {

	public FactorParameter {
		Objects.requireNonNull(name, "name 不能为空");
		Objects.requireNonNull(type, "type 不能为空");
		if (defaultValue == null) {
			throw new IllegalArgumentException("defaultValue 不能为空");
		}
	}

	/** 参数类型 */
	public enum ParameterType {

		/** 整数 */
		INTEGER,
		/** 浮点 */
		DOUBLE,
		/** 布尔 */
		BOOLEAN,
		/** 字符串 */
		STRING

	}

}
