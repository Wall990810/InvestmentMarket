package org.wall.im.quant.factor.ai;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.wall.im.ai.core.tool.Tool;
import org.wall.im.quant.engine.FactorCalculationRequest;
import org.wall.im.quant.engine.FactorCalculationResult;
import org.wall.im.quant.engine.FactorEngine;
import org.wall.im.quant.factor.FactorResult;
import org.wall.im.quant.factor.FactorValue;
import org.wall.im.quant.model.Universe;
import org.wall.im.quant.process.FactorProcessorChain;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 因子查询工具（暴露给 AI Agent 调用）。
 *
 * <p>
 * 实现 {@link Tool}，供投资顾问 Agent 在 ReAct 循环中按"因子名+标的+截面日"查询截面值。 返回 JSON：每个因子下含 symbol->value
 * 与缺失项。可选 processors（winsorize/standardize/neutralize）。
 * </p>
 *
 * <p>
 * Agent 调用示例参数： <pre>
 * { "factors": ["momentum_12_1","pe_ttm"], "symbols": ["000001.SZ","600000.SH"], "asOfDate": "2026-08-24" }
 * </pre>
 * </p>
 */
public class FactorQueryTool implements Tool {

	private final FactorEngine engine;

	private final ObjectMapper objectMapper = new ObjectMapper();

	public FactorQueryTool(FactorEngine engine) {
		this.engine = Objects.requireNonNull(engine, "engine 不能为空");
	}

	@Override
	public String getName() {
		return "factor-query";
	}

	@Override
	public String getDescription() {
		return "查询量化因子截面值。输入因子名列表、标的代码列表与截面日期，返回各因子在各标的上的取值。" + "可用因子见注册表，可选 processors 做去极值/标准化。";
	}

	@Override
	public Map<String, Object> getParameterSchema() {
		Map<String, Object> factors = Map.of("type", "array", "description", "因子名列表", "items",
				Map.of("type", "string"));
		Map<String, Object> symbols = Map.of("type", "array", "description", "标的代码列表", "items",
				Map.of("type", "string"));
		Map<String, Object> asOfDate = Map.of("type", "string", "description", "截面日期 yyyy-MM-dd");
		Map<String, Object> processors = Map.of("type", "array", "description", "后处理器，可选", "items",
				Map.of("type", "string", "enum", List.of("winsorize", "standardize", "neutralize")));
		return Map.of("type", "object", "properties",
				Map.of("factors", factors, "symbols", symbols, "asOfDate", asOfDate, "processors", processors),
				"required", List.of("factors", "symbols"));
	}

	@Override
	public String execute(Map<String, Object> parameters) {
		try {
			@SuppressWarnings("unchecked")
			List<String> factorNames = (List<String>) parameters.getOrDefault("factors", List.of());
			@SuppressWarnings("unchecked")
			List<String> symbols = (List<String>) parameters.getOrDefault("symbols", List.of());
			String dateStr = (String) parameters.get("asOfDate");
			LocalDate asOfDate = (dateStr == null || dateStr.isBlank()) ? LocalDate.now() : LocalDate.parse(dateStr);
			@SuppressWarnings("unchecked")
			List<String> processorNames = (List<String>) parameters.getOrDefault("processors", List.of());
			FactorProcessorChain chain = buildChain(processorNames);
			Universe universe = new Universe("query", symbols);
			FactorCalculationRequest req = new FactorCalculationRequest(factorNames, universe, asOfDate, Map.of(),
					chain);
			FactorCalculationResult res = this.engine.calculate(req);
			return this.objectMapper.writeValueAsString(toJson(res));
		}
		catch (RuntimeException ex) {
			return "{\"error\":\"" + escape(ex.getMessage()) + "\"}";
		}
		catch (Exception ex) {
			return "{\"error\":\"序列化失败: " + escape(ex.getMessage()) + "\"}";
		}
	}

	private Map<String, Object> toJson(FactorCalculationResult res) {
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("asOfDate", res.results().values().stream().findFirst().map(FactorResult::asOfDate).orElse(null));
		out.put("costTimeMs", res.costTimeMs());
		Map<String, Object> factors = new LinkedHashMap<>();
		for (Map.Entry<String, FactorResult> e : res.results().entrySet()) {
			Map<String, Object> values = new LinkedHashMap<>();
			List<String> missing = new ArrayList<>();
			for (FactorValue v : e.getValue().values().values()) {
				if (v.hasValue()) {
					values.put(v.symbol(), v.value());
				}
				else {
					missing.add(v.symbol());
				}
			}
			Map<String, Object> entry = new LinkedHashMap<>();
			entry.put("values", values);
			entry.put("missing", missing);
			factors.put(e.getKey(), entry);
		}
		out.put("factors", factors);
		return out;
	}

	private FactorProcessorChain buildChain(List<String> names) {
		FactorProcessorChain.Builder b = FactorProcessorChain.builder();
		if (names != null) {
			for (String n : names) {
				switch (n) {
					case "winsorize" -> b.winsorize();
					case "standardize" -> b.standardize();
					case "neutralize" -> b.neutralize();
					default -> {
					}
				}
			}
		}
		return b.build();
	}

	private static String escape(String s) {
		return s == null ? "" : s.replace("\"", "'");
	}

}
