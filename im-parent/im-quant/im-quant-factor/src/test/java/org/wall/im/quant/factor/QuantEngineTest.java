package org.wall.im.quant.factor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.wall.im.quant.data.InMemoryFundamentalDataProvider;
import org.wall.im.quant.data.InMemoryMarketDataProvider;
import org.wall.im.quant.engine.FactorCalculationRequest;
import org.wall.im.quant.engine.FactorCalculationResult;
import org.wall.im.quant.engine.FactorEngine;
import org.wall.im.quant.factor.liquidity.AmihudIlliquidityFactor;
import org.wall.im.quant.factor.liquidity.TurnoverFactor;
import org.wall.im.quant.factor.momentum.MomentumFactor;
import org.wall.im.quant.factor.momentum.RsiFactor;
import org.wall.im.quant.factor.value.PeRatioFactor;
import org.wall.im.quant.factor.value.PbRatioFactor;
import org.wall.im.quant.factor.volatility.BetaFactor;
import org.wall.im.quant.factor.volatility.RealizedVolatilityFactor;
import org.wall.im.quant.model.Universe;
import org.wall.im.quant.process.FactorProcessorChain;
import org.wall.im.quant.registry.FactorRegistry;

/**
 * 量化引擎端到端测试，兼作使用示例。
 */
class QuantEngineTest {

	@Test
	void calculatesAllFactorsAndProcesses() {
		FactorRegistry registry = new FactorRegistry();
		registry.registerAll(new MomentumFactor(), new RsiFactor(), new PeRatioFactor(), new PbRatioFactor(),
				new RealizedVolatilityFactor(), new BetaFactor(), new TurnoverFactor(), new AmihudIlliquidityFactor());
		assertEquals(8, registry.size());

		FactorEngine engine = new FactorEngine(registry, new InMemoryMarketDataProvider(),
				new InMemoryFundamentalDataProvider());

		Universe universe = new Universe("test",
				List.of("000001.SZ", "600000.SH", "000300.SH", "002415.SZ", "300750.SZ"));
		FactorProcessorChain chain = FactorProcessorChain.builder().winsorize().standardize().build();

		FactorCalculationRequest req = new FactorCalculationRequest(
				List.of("momentum_12_1", "rsi_14", "pe_ttm", "realized_vol_20", "beta_60", "turnover_20"), universe,
				LocalDate.now(), Map.of(), chain);
		FactorCalculationResult res = engine.calculate(req);

		assertEquals(6, res.size());
		for (String name : req.factorNames()) {
			assertFalse(res.get(name).values().isEmpty(), name + " 应有结果");
		}
		// 动量与 RSI 应有有效值
		FactorResult momentum = res.get("momentum_12_1");
		long valid = momentum.values().values().stream().filter(FactorValue::hasValue).count();
		assertTrue(valid >= 4, "动量有效值应>=4，实际 " + valid);
		assertTrue(res.costTimeMs() >= 0);
	}

	@Test
	void singleValueQueryWorks() {
		FactorRegistry registry = new FactorRegistry();
		registry.register(new PeRatioFactor());
		FactorEngine engine = new FactorEngine(registry, new InMemoryMarketDataProvider(),
				new InMemoryFundamentalDataProvider());
		double v = engine.calculateValue("pe_ttm", "000001.SZ", LocalDate.now());
		assertTrue(v > 0, "PE 应为正，实际 " + v);
	}

}
