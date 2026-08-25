package org.wall.im.ai.sandbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wall.im.ai.core.model.SandboxConfig;
import org.wall.im.ai.core.sandbox.Sandbox;
import org.wall.im.ai.core.sandbox.SandboxFactory;
import org.wall.im.ai.core.sandbox.SandboxType;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 沙盒工厂注册表
 * <p>
 * 管理所有 {@link SandboxFactory}，按 {@link SandboxType} 路由创建沙盒实例。 Spring 环境下通过构造器注入 factories
 * 优先；非 Spring 环境兜底用 {@link ServiceLoader} 加载
 * {@code META-INF/services/org.wall.im.ai.core.sandbox.SandboxFactory}。
 * </p>
 *
 * <h3>路由规则</h3>
 * <ul>
 * <li>同类型多 factory 时取 {@link SandboxFactory#priority()} 最大者</li>
 * <li>找不到匹配类型时 {@link #create(SandboxConfig)} 抛 {@link IllegalStateException}</li>
 * </ul>
 */
public class SandboxRegistry {

	private static final Logger log = LoggerFactory.getLogger(SandboxRegistry.class);

	/** 按 type 索引的 factory（已选 priority 最大者） */
	private final Map<SandboxType, SandboxFactory> byType = new ConcurrentHashMap<>();

	/**
	 * 默认构造器：仅用 ServiceLoader 加载内置 factory
	 */
	public SandboxRegistry() {
		this(Collections.emptyList());
	}

	/**
	 * Spring 注入优先 + ServiceLoader 兜底
	 */
	public SandboxRegistry(List<SandboxFactory> springFactories) {
		// 1. 先收集 Spring 注入的
		if (springFactories != null) {
			for (SandboxFactory f : springFactories) {
				if (f != null) {
					register(f);
				}
			}
		}
		// 2. ServiceLoader 兜底（去重：仅当某 type 未被 Spring 覆盖时才用 ServiceLoader 的）
		try {
			ServiceLoader<SandboxFactory> loader = ServiceLoader.load(SandboxFactory.class);
			for (SandboxFactory f : loader) {
				byType.putIfAbsent(f.supportedType(), f);
				log.debug("ServiceLoader loaded factory: {} for type {}", f.getClass().getName(), f.supportedType());
			}
		}
		catch (Exception e) {
			log.warn("ServiceLoader load SandboxFactory failed", e);
		}
	}

	/**
	 * 注册或覆盖某 type 的 factory（取 priority 最大者保留）
	 */
	public void register(SandboxFactory factory) {
		if (factory == null) {
			return;
		}
		SandboxType type = factory.supportedType();
		byType.compute(type, (t, existing) -> {
			if (existing == null || factory.priority() > existing.priority()) {
				return factory;
			}
			return existing;
		});
	}

	/**
	 * 查找某 type 的 factory（已选 priority 最大者）
	 */
	public Optional<SandboxFactory> getFactory(SandboxType type) {
		return Optional.ofNullable(byType.get(type));
	}

	/**
	 * 按 {@link SandboxConfig#getType()} 路由创建沙盒实例。
	 * @throws IllegalStateException 无匹配 factory 时
	 */
	public Sandbox create(SandboxConfig config) {
		SandboxType type = config != null ? config.getType() : SandboxType.LOCAL_PROCESS;
		SandboxFactory factory = byType.get(type);
		if (factory == null) {
			throw new IllegalStateException("No SandboxFactory registered for type: " + type);
		}
		return factory.create(config);
	}

	/**
	 * 返回所有已注册 factory
	 */
	public Collection<SandboxFactory> getAll() {
		return Collections.unmodifiableCollection(byType.values());
	}

}
