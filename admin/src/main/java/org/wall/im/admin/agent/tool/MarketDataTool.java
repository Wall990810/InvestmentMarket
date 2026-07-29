package org.wall.im.admin.agent.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wall.im.ai.core.tool.Tool;

import java.time.LocalDate;
import java.util.Map;

/**
 * 行情数据查询工具
 * 提供股票、基金、债券等标的的实时/历史行情数据查询能力
 */
public class MarketDataTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(MarketDataTool.class);

    @Override
    public String getName() {
        return "market-data-tool";
    }

    @Override
    public String getDescription() {
        return "行情数据查询工具：支持查询A股/港股/美股股票、基金、债券等标的的实时行情与历史数据";
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "symbol", Map.of("type", "string", "description", "标的代码，如 600519.SH"),
                        "market", Map.of("type", "string", "enum", new String[]{"A股", "港股", "美股"}, "description", "市场"),
                        "dataType", Map.of("type", "string", "enum", new String[]{"realtime", "daily", "weekly"}, "description", "数据类型"),
                        "startDate", Map.of("type", "string", "description", "开始日期 yyyy-MM-dd"),
                        "endDate", Map.of("type", "string", "description", "结束日期 yyyy-MM-dd")
                ),
                "required", new String[]{"symbol", "market"}
        );
    }

    @Override
    public String execute(Map<String, Object> parameters) {
        String symbol = (String) parameters.getOrDefault("symbol", "unknown");
        String market = (String) parameters.getOrDefault("market", "A股");
        log.info("查询行情数据: symbol={}, market={}", symbol, market);

        // 实际实现中应调用行情数据API（如Tushare、Wind等）
        return String.format(
                "{\"symbol\":\"%s\",\"market\":\"%s\",\"date\":\"%s\"," +
                "\"open\":1850.00,\"high\":1872.50,\"low\":1845.20,\"close\":1865.80," +
                "\"volume\":3256000,\"amount\":6045000000," +
                "\"change\":1.25,\"changePercent\":0.067}",
                symbol, market, LocalDate.now()
        );
    }
}
