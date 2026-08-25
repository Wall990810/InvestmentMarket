package org.wall.im.quant.factor.ai;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import org.wall.im.quant.data.FundamentalDataProvider;
import org.wall.im.quant.data.InMemoryFundamentalDataProvider;
import org.wall.im.quant.data.InMemoryMarketDataProvider;
import org.wall.im.quant.data.MarketDataProvider;
import org.wall.im.quant.engine.FactorEngine;
import org.wall.im.quant.factor.liquidity.AmihudIlliquidityFactor;
import org.wall.im.quant.factor.liquidity.TurnoverFactor;
import org.wall.im.quant.factor.momentum.MomentumFactor;
import org.wall.im.quant.factor.momentum.RsiFactor;
import org.wall.im.quant.factor.value.PeRatioFactor;
import org.wall.im.quant.factor.value.PbRatioFactor;
import org.wall.im.quant.factor.volatility.BetaFactor;
import org.wall.im.quant.factor.volatility.RealizedVolatilityFactor;
import org.wall.im.quant.registry.FactorRegistry;

/**
 * 量化因子栈自动装配。
 *
 * <p>
 * 引入 im-quant-factor 依赖后即自动装配：内存数据源（可被真实数据源 Bean 覆盖）、 注册全部内置因子的
 * {@link FactorRegistry}、{@link FactorEngine}，以及暴露给 AI Agent 的
 * {@link FactorQueryTool}。可通过 {@code im.quant.enabled=false} 关闭。
 * </p>
 *
 * <p>
 * 接入真实数据源：仅需在应用中定义 {@link MarketDataProvider} / {@link FundamentalDataProvider}
 * Bean（自动以 @ConditionalOnMissingBean 让位），因子与引擎自动复用。
 * </p>
 */
@AutoConfiguration
@ConditionalOnClass(FactorEngine.class)
@ConditionalOnProperty(name = "im.quant.enabled", matchIfMissing = true)
public class QuantAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(MarketDataProvider.class)
	public MarketDataProvider inMemoryMarketDataProvider() {
		return new InMemoryMarketDataProvider();
	}

	@Bean
	@ConditionalOnMissingBean(FundamentalDataProvider.class)
	public FundamentalDataProvider inMemoryFundamentalDataProvider() {
		return new InMemoryFundamentalDataProvider();
	}

	@Bean
	public FactorRegistry factorRegistry() {
		FactorRegistry registry = new FactorRegistry();
		registry.registerAll(new MomentumFactor(), new RsiFactor(), new PeRatioFactor(), new PbRatioFactor(),
				new RealizedVolatilityFactor(), new BetaFactor(), new TurnoverFactor(), new AmihudIlliquidityFactor());
		return registry;
	}

	@Bean
	@ConditionalOnMissingBean
	public FactorEngine factorEngine(FactorRegistry registry, MarketDataProvider market,
			FundamentalDataProvider fundamental) {
		return new FactorEngine(registry, market, fundamental);
	}

	@Bean
	@ConditionalOnMissingBean
	public FactorQueryTool factorQueryTool(FactorEngine engine) {
		return new FactorQueryTool(engine);
	}

}
