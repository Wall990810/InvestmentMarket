package org.wall.im.quant.engine;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wall.im.quant.data.FundamentalDataProvider;
import org.wall.im.quant.data.MarketDataProvider;
import org.wall.im.quant.factor.Factor;
import org.wall.im.quant.factor.FactorContext;
import org.wall.im.quant.factor.FactorResult;
import org.wall.im.quant.model.Universe;
import org.wall.im.quant.process.FactorProcessorChain;
import org.wall.im.quant.registry.FactorRegistry;

/**
 * 因子计算引擎。
 *
 * <p>
 * 框架的调度中枢。一次计算请求的处理流程：
 * </p>
 * <ol>
 * <li>为每个因子从 {@link FactorRegistry} 解析实例；</li>
 * <li>构造共享的 {@link FactorContext}（数据源 / 标的池 / 截面日 / 参数）；</li>
 * <li>调用 {@link Factor#compute(FactorContext)} 得到原始截面值；</li>
 * <li>经 {@link FactorProcessorChain} 做后处理（去极值 → 标准化 → 中性化）；</li>
 * <li>汇总返回 {@link FactorCalculationResult}。</li>
 * </ol>
 */
public class FactorEngine {

	private static final Logger log = LoggerFactory.getLogger(FactorEngine.class);

	private final FactorRegistry registry;

	private final MarketDataProvider marketDataProvider;

	private final FundamentalDataProvider fundamentalDataProvider;

	public FactorEngine(FactorRegistry registry, MarketDataProvider marketDataProvider,
			FundamentalDataProvider fundamentalDataProvider) {
		this.registry = Objects.requireNonNull(registry, "registry 不能为空");
		this.marketDataProvider = Objects.requireNonNull(marketDataProvider, "marketDataProvider 不能为空");
		this.fundamentalDataProvider = Objects.requireNonNull(fundamentalDataProvider, "fundamentalDataProvider 不能为空");
	}

	public FactorCalculationResult calculate(FactorCalculationRequest request) {
		Objects.requireNonNull(request, "request 不能为空");
		long start = System.currentTimeMillis();
		Map<String, Object> params = request.parameters() == null ? Map.of() : request.parameters();
		FactorContext context = new FactorContext(this.marketDataProvider, this.fundamentalDataProvider,
				request.universe(), request.asOfDate(), params);
		FactorProcessorChain chain = request.processorChain() == null ? FactorProcessorChain.empty()
				: request.processorChain();
		Map<String, FactorResult> out = new LinkedHashMap<>();
		for (String name : request.factorNames()) {
			Factor factor = this.registry.getRequired(name);
			try {
				FactorResult raw = factor.compute(context);
				FactorResult processed = chain.apply(raw, context);
				out.put(name, processed);
			}
			catch (RuntimeException ex) {
				log.warn("因子 {} 计算失败: {}", name, ex.toString());
				out.put(name, new FactorResult(name, request.asOfDate(), Map.of()));
			}
		}
		return new FactorCalculationResult(out, System.currentTimeMillis() - start);
	}

	/** 便捷方法：单标的单因子取值，便于 AI Tool 调用。 */
	public double calculateValue(String factorName, String symbol, LocalDate asOfDate) {
		Universe single = new Universe("single", java.util.List.of(symbol));
		FactorCalculationRequest req = new FactorCalculationRequest(java.util.List.of(factorName), single, asOfDate,
				Map.of(), FactorProcessorChain.empty());
		FactorCalculationResult res = calculate(req);
		FactorResult fr = res.get(factorName);
		if (fr == null || fr.valueOf(symbol) == null) {
			return Double.NaN;
		}
		return fr.valueOf(symbol).value();
	}

	public FactorRegistry registry() {
		return this.registry;
	}

}
