package org.wall.im.quant.process;

import java.util.ArrayList;
import java.util.List;

import org.wall.im.quant.factor.FactorContext;
import org.wall.im.quant.factor.FactorResult;

/**
 * 后处理器链。
 *
 * <p>
 * 有序应用一组 {@link FactorProcessor}，前一个输出作为后一个输入。空链等价于不处理。
 * </p>
 */
public class FactorProcessorChain {

	private final List<FactorProcessor> chain;

	public FactorProcessorChain(FactorProcessor... processors) {
		this.chain = new ArrayList<>();
		for (FactorProcessor p : processors) {
			if (p != null) {
				this.chain.add(p);
			}
		}
	}

	public FactorProcessorChain(List<FactorProcessor> processors) {
		this.chain = (processors == null) ? List.of() : new ArrayList<>(processors);
	}

	public FactorResult apply(FactorResult result, FactorContext context) {
		FactorResult current = result;
		for (FactorProcessor p : this.chain) {
			current = p.process(current, context);
		}
		return current;
	}

	public boolean isEmpty() {
		return this.chain.isEmpty();
	}

	public List<FactorProcessor> getProcessors() {
		return List.copyOf(this.chain);
	}

	/** 构造器，便于链式拼装。 */
	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private final List<FactorProcessor> list = new ArrayList<>();

		public Builder add(FactorProcessor processor) {
			if (processor != null) {
				this.list.add(processor);
			}
			return this;
		}

		public Builder winsorize(double lowerPct, double upperPct) {
			return add(new WinsorizeProcessor(lowerPct, upperPct));
		}

		public Builder winsorize() {
			return add(new WinsorizeProcessor());
		}

		public Builder standardize() {
			return add(new StandardizeProcessor());
		}

		public Builder neutralize() {
			return add(new NeutralizeProcessor());
		}

		public FactorProcessorChain build() {
			return new FactorProcessorChain(this.list);
		}

	}

	/** 空链快捷实例。 */
	public static FactorProcessorChain empty() {
		return new FactorProcessorChain();
	}

	@Override
	public String toString() {
		return "FactorProcessorChain" + this.chain.stream().map(Object::toString).toList();
	}

}
