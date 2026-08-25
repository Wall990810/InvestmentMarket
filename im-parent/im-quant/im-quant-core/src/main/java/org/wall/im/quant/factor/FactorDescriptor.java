package org.wall.im.quant.factor;

import java.util.List;
import java.util.Objects;

/**
 * 因子元数据。
 *
 * <p>
 * 因子的自描述信息，由 {@link Factor#descriptor()} 提供。引擎、注册表、AI Tool 均依据此信息 进行发现、调用与展示。新增因子只需填好
 * descriptor 即可被全链路识别。
 * </p>
 *
 * @param name 因子唯一标识，如 "momentum_12_1"
 * @param displayName 展示名，如 "12-1月动量"
 * @param category 分类
 * @param description 计算逻辑说明
 * @param parameters 可调参数列表
 * @param version 版本
 * @param author 作者
 */
public record FactorDescriptor(String name, String displayName, FactorCategory category, String description,
		List<FactorParameter> parameters, String version, String author) {

	public FactorDescriptor {
		Objects.requireNonNull(name, "name 不能为空");
		Objects.requireNonNull(category, "category 不能为空");
		parameters = (parameters == null) ? List.of() : List.copyOf(parameters);
	}

	/** 无参数的便捷构造。 */
	public FactorDescriptor(String name, String displayName, FactorCategory category, String description) {
		this(name, displayName, category, description, List.of(), "1.0.0", "InvestmentMarket");
	}

}
