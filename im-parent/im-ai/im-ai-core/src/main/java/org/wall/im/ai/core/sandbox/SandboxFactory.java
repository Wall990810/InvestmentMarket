package org.wall.im.ai.core.sandbox;

import org.wall.im.ai.core.model.SandboxConfig;

/**
 * 沙盒工厂 SPI
 * <p>
 * 第三方扩展的核心入口：实现该接口并声明 {@link #supportedType()}， 在 Spring 环境注册为 Bean，或非 Spring 环境写
 * {@code META-INF/services/org.wall.im.ai.core.sandbox.SandboxFactory} 文件， 即可被
 * {@code SandboxRegistry} 自动收集与路由。
 * </p>
 */
public interface SandboxFactory {

	/**
	 * 根据配置创建沙盒实例
	 * @param config 沙盒配置
	 * @return 沙盒实例（未 initialize）
	 */
	Sandbox create(SandboxConfig config);

	/**
	 * 该工厂支持的沙盒类型，用于注册路由
	 */
	SandboxType supportedType();

	/**
	 * 同类型多工厂时的优先级，数值越大越优先。默认 0。
	 */
	default int priority() {
		return 0;
	}

}
