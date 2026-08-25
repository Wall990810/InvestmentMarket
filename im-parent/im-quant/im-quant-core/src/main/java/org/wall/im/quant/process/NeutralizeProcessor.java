package org.wall.im.quant.process;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.wall.im.quant.factor.FactorContext;
import org.wall.im.quant.factor.FactorResult;
import org.wall.im.quant.factor.FactorValue;
import org.wall.im.quant.stats.StatUtils;

/**
 * 行业中性化处理器。
 *
 * <p>
 * 按标的所属行业分组，在每个行业内部做 z-score，消除行业间的系统性差异。 行业信息取自
 * {@link org.wall.im.quant.model.Instrument#getSector()} —— 当前以
 * {@link FactorContext#getFundamentals()} 不可用时退化为整体标准化。
 * </p>
 *
 * <p>
 * 这是中性化的占位实现；生产可替换为按行业哑变量回归取残差。预留扩展点： 实现新的 {@link FactorProcessor} 注入链即可。
 * </p>
 */
public class NeutralizeProcessor implements FactorProcessor {

	@Override
	public FactorResult process(FactorResult result, FactorContext context) {
		Map<String, String> sectorOf = loadSectorMapping(context);
		if (sectorOf.isEmpty()) {
			// 无行业信息则退化为整体标准化
			return new StandardizeProcessor().process(result, context);
		}
		Map<String, List<String>> groups = new HashMap<>();
		for (String symbol : result.values().keySet()) {
			String sector = sectorOf.getOrDefault(symbol, "UNKNOWN");
			groups.computeIfAbsent(sector, k -> new ArrayList<>()).add(symbol);
		}
		Map<String, FactorValue> out = new LinkedHashMap<>();
		for (List<String> members : groups.values()) {
			double[] values = members.stream().mapToDouble(s -> result.valueOf(s).value()).toArray();
			double mean = StatUtils.mean(values);
			double std = StatUtils.std(values);
			for (String symbol : members) {
				FactorValue v = result.valueOf(symbol);
				out.put(symbol, new FactorValue(symbol, StatUtils.zscore(v.value(), mean, std), v.rank()));
			}
		}
		return result.withValues(out);
	}

	private Map<String, String> loadSectorMapping(FactorContext context) {
		// 当前上下文未直接携带 Instrument 元数据，留作扩展点：
		// 子类可覆盖本方法接入证券主数据。这里返回空以触发退化逻辑。
		return Map.of();
	}

}
