package org.wall.im.quant.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.wall.im.quant.factor.Factor;
import org.wall.im.quant.factor.FactorCategory;
import org.wall.im.quant.factor.FactorDescriptor;

/**
 * 因子注册表。
 *
 * <p>
 * 进程级因子仓库，提供按名/分类查询与批量注册。线程安全。 同名因子重复注册会覆盖旧实例并记录警告。
 * </p>
 */
public class FactorRegistry {

	private final Map<String, Factor> factors = new ConcurrentHashMap<>();

	/** 注册单个因子。 */
	public void register(Factor factor) {
		Objects.requireNonNull(factor, "factor 不能为空");
		FactorDescriptor d = factor.descriptor();
		this.factors.put(d.name(), factor);
	}

	/** 批量注册。 */
	public void registerAll(Factor... toRegister) {
		for (Factor f : toRegister) {
			register(f);
		}
	}

	/** 按名获取，缺失返回空。 */
	public Factor get(String name) {
		return this.factors.get(name);
	}

	/** 按名获取，缺失抛异常。 */
	public Factor getRequired(String name) {
		Factor f = this.factors.get(name);
		if (f == null) {
			throw new IllegalArgumentException("未注册的因子: " + name);
		}
		return f;
	}

	/** 按分类过滤。 */
	public List<Factor> getByCategory(FactorCategory category) {
		List<Factor> out = new ArrayList<>();
		for (Factor f : this.factors.values()) {
			if (f.descriptor().category() == category) {
				out.add(f);
			}
		}
		return out;
	}

	/** 全部因子。 */
	public List<Factor> getAll() {
		return List.copyOf(this.factors.values());
	}

	/** 全部因子名。 */
	public List<String> getAllNames() {
		return this.factors.keySet().stream().sorted().toList();
	}

	public boolean contains(String name) {
		return this.factors.containsKey(name);
	}

	public int size() {
		return this.factors.size();
	}

	/** 注销因子。 */
	public Factor unregister(String name) {
		return this.factors.remove(name);
	}

}
