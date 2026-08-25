package org.wall.im.quant.stats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 描述性统计工具。
 *
 * <p>
 * 无外部依赖的轻量实现，供后处理器（缩尾/标准化）使用。仅处理有限值，自动剔除 NaN。
 * </p>
 */
public final class StatUtils {

	private StatUtils() {
	}

	public static double mean(double[] values) {
		if (values.length == 0) {
			return Double.NaN;
		}
		double sum = 0;
		int n = 0;
		for (double v : values) {
			if (!Double.isNaN(v)) {
				sum += v;
				n++;
			}
		}
		return n == 0 ? Double.NaN : sum / n;
	}

	public static double std(double[] values) {
		double mean = mean(values);
		if (Double.isNaN(mean)) {
			return Double.NaN;
		}
		double sum = 0;
		int n = 0;
		for (double v : values) {
			if (!Double.isNaN(v)) {
				sum += (v - mean) * (v - mean);
				n++;
			}
		}
		return n <= 1 ? Double.NaN : Math.sqrt(sum / (n - 1));
	}

	/** 线性插值分位（p 取 0~100）。 */
	public static double percentile(double[] values, double p) {
		List<Double> valid = new ArrayList<>();
		for (double v : values) {
			if (!Double.isNaN(v)) {
				valid.add(v);
			}
		}
		if (valid.isEmpty()) {
			return Double.NaN;
		}
		Collections.sort(valid);
		if (valid.size() == 1) {
			return valid.get(0);
		}
		double pos = p / 100.0 * (valid.size() - 1);
		int lower = (int) Math.floor(pos);
		int upper = Math.min(lower + 1, valid.size() - 1);
		double frac = pos - lower;
		return valid.get(lower) * (1 - frac) + valid.get(upper) * frac;
	}

	/** z-score 标准化，输入标准差为 0 时返回 0。 */
	public static double zscore(double value, double mean, double std) {
		if (Double.isNaN(value) || Double.isNaN(std) || std == 0) {
			return Double.isNaN(value) ? Double.NaN : 0.0;
		}
		return (value - mean) / std;
	}

}
