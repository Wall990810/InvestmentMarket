package org.wall.im.imobservation;

import com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeAgentAutoConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.wall.im.ai.agent.lifecycle.DefaultAgent;
import org.wall.im.ai.core.model.AgentConfig;
import org.wall.im.ai.core.model.MonitorConfig;
import org.wall.im.ai.core.tool.Tool;
import org.wall.im.ai.monitor.langfuse.LangfuseMonitor;
import org.wall.im.ai.monitor.langfuse.LangfuseMonitorFactory;
import org.wall.im.ai.monitor.micrometer.MicrometerAgentMonitor;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.util.List;
import java.util.Map;

/**
 * ReActAgent Langfuse Trace 集成测试
 * <p>
 * 演示如何使用 im-observation 模块记录 ReActAgent 的 trace 并上送至 Langfuse 平台。
 * </p>
 *
 * <h3>测试场景</h3>
 * <ul>
 * <li>基础 trace 记录：记录 Agent 调用的完整生命周期（start → end）</li>
 * <li>Tool 调用 trace：记录 Agent 调用工具的过程</li>
 * <li>异常 trace：记录 Agent 执行异常的情况</li>
 * </ul>
 *
 * <h3>环境要求</h3>
 * <ul>
 * <li>DashScope API Key: 设置环境变量 AI_DASHSCOPE_API_KEY</li>
 * <li>Langfuse 配置: 设置环境变量 LANGFUSE_PUBLIC_KEY, LANGFUSE_SECRET_KEY, LANGFUSE_HOST(可选)</li>
 * </ul>
 *
 * <h3>运行方式</h3>
 * <pre>{@code
 * # 设置环境变量
 * export AI_DASHSCOPE_API_KEY=your-dashscope-api-key
 * export LANGFUSE_PUBLIC_KEY=pk-lf-xxx
 * export LANGFUSE_SECRET_KEY=sk-lf-xxx
 * export LANGFUSE_HOST=http://localhost:3000  # 可选
 *
 * # 运行测试
 * mvn test -Dtest=ReActAgentLangfuseTraceTest
 * }</pre>
 */
@SpringBootTest(exclude = {DashScopeAgentAutoConfiguration.class})
@ActiveProfiles("test")
@DisplayName("ReActAgent Langfuse Trace 集成测试")
@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+",
    disabledReason = "需要设置环境变量 AI_DASHSCOPE_API_KEY 才能运行测试")
class ReActAgentLangfuseTraceTest {

    @Autowired
    private ChatModel chatModel;

    private LangfuseMonitor langfuseMonitor;

    private DefaultAgent agent;

    @BeforeEach
    void setUp() {
        // 1. 创建 Langfuse 配置
        MonitorConfig.LangfuseConfig langfuseConfig = createLangfuseConfig();

        // 2. 创建委托监控器（Micrometer）
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        MicrometerAgentMonitor micrometerMonitor = new MicrometerAgentMonitor(meterRegistry);

        // 3. 创建 LangfuseMonitor（包装 MicrometerMonitor）
        langfuseMonitor = LangfuseMonitorFactory.create(langfuseConfig, micrometerMonitor);

        // 4. 创建 ReActAgent（DefaultAgent 内部封装 ReactAgent）
        AgentConfig agentConfig = createAgentConfig();
        agent = new DefaultAgent(agentConfig, chatModel, createSampleTools());
        agent.initialize();
    }

    /**
     * 测试场景1：基础 Trace 记录
     * <p>
     * 演示如何记录 ReActAgent 的完整调用链路并上送至 Langfuse
     * </p>
     */
    @Test
    @DisplayName("基础 Trace 记录 - 记录 Agent 调用并上送 Langfuse")
    void testBasicTraceRecording() {
        String agentName = agent.getName();
        String input = "请帮我分析一下当前的投资市场情况";

        System.out.println("=== 开始基础 Trace 记录测试 ===");
        System.out.println("Agent: " + agentName);
        System.out.println("Input: " + input);

        // 1. 记录 Trace 开始
        String traceId = langfuseMonitor.traceStart(agentName, input);
        System.out.println("Trace 开始: traceId=" + traceId);

        long startTime = System.currentTimeMillis();
        String output;
        try {
            // 2. 执行 ReActAgent 推理循环
            // ReactAgent 会自动进行: 推理(Reason) → 行动(Act) → 观察(Observe) → 循环
            output = agent.chat(input);
            long costTimeMs = System.currentTimeMillis() - startTime;

            System.out.println("Agent 响应: " + output);
            System.out.println("耗时: " + costTimeMs + "ms");

            // 3. 记录 Trace 结束
            langfuseMonitor.traceEnd(traceId, agentName, output, costTimeMs, estimateTokens(input + output));
            System.out.println("Trace 结束: 已上送至 Langfuse");

        } catch (Exception e) {
            long costTimeMs = System.currentTimeMillis() - startTime;
            // 异常时记录错误信息
            langfuseMonitor.traceError(traceId, agentName, e.getMessage());
            System.err.println("Agent 执行异常: " + e.getMessage());
            throw e;
        }

        // 4. 刷新数据到 Langfuse
        langfuseMonitor.flush();
        System.out.println("=== 基础 Trace 记录测试完成 ===\n");
    }

    /**
     * 测试场景2：Tool 调用 Trace 记录
     * <p>
     * 演示如何记录 ReActAgent 调用工具的 trace 并上送至 Langfuse。
     * ReActAgent 在推理过程中会调用工具获取信息，这些调用都会被记录。
     * </p>
     */
    @Test
    @DisplayName("Tool 调用 Trace 记录 - 记录 Agent 工具调用并上送 Langfuse")
    void testToolCallTraceRecording() {
        String agentName = agent.getName();
        String input = "查询一下贵州茅台(600519)的股票价格";

        System.out.println("=== 开始 Tool 调用 Trace 记录测试 ===");
        System.out.println("Agent: " + agentName);
        System.out.println("Input: " + input);

        // 1. 记录 Trace 开始
        String traceId = langfuseMonitor.traceStart(agentName, input);
        System.out.println("Trace 开始: traceId=" + traceId);

        long startTime = System.currentTimeMillis();
        String output;
        try {
            // 2. 模拟工具调用记录
            // 在实际场景中，ReActAgent 会自动调用工具，这里演示如何手动记录工具调用
            String toolName = "stock_price_query";
            Map<String, Object> parameters = Map.of("stock_code", "600519", "stock_name", "贵州茅台");

            System.out.println("记录工具调用: tool=" + toolName + ", params=" + parameters);
            long toolStartTime = System.currentTimeMillis();

            // 模拟工具执行
            String toolResult = simulateToolExecution(toolName, parameters);
            long toolCostTimeMs = System.currentTimeMillis() - toolStartTime;

            // 3. 记录工具调用 Trace
            langfuseMonitor.traceToolCall(traceId, toolName, parameters, toolResult, toolCostTimeMs);
            System.out.println("工具调用已记录: result=" + toolResult + ", cost=" + toolCostTimeMs + "ms");

            // 4. 继续 Agent 推理（使用工具结果）
            output = agent.chat(input);
            long costTimeMs = System.currentTimeMillis() - startTime;

            System.out.println("Agent 最终响应: " + output);
            System.out.println("总耗时: " + costTimeMs + "ms");

            // 5. 记录 Trace 结束
            langfuseMonitor.traceEnd(traceId, agentName, output, costTimeMs, estimateTokens(input + output));
            System.out.println("Trace 结束: 已上送至 Langfuse");

        } catch (Exception e) {
            langfuseMonitor.traceError(traceId, agentName, e.getMessage());
            System.err.println("执行异常: " + e.getMessage());
            throw e;
        }

        langfuseMonitor.flush();
        System.out.println("=== Tool 调用 Trace 记录测试完成 ===\n");
    }

    /**
     * 测试场景3：多次调用 Trace 记录
     * <p>
     * 演示如何记录多次 Agent 调用的 trace，每次调用都会生成独立的 trace 记录
     * </p>
     */
    @Test
    @DisplayName("多次调用 Trace 记录 - 记录多次 Agent 调用并上送 Langfuse")
    void testMultipleTraceRecording() {
        String agentName = agent.getName();
        String[] inputs = {
            "什么是价值投资？",
            "如何评估一只股票的投资价值？",
            "请给出一个简单的投资组合建议"
        };

        System.out.println("=== 开始多次调用 Trace 记录测试 ===");
        System.out.println("Agent: " + agentName);
        System.out.println("调用次数: " + inputs.length);

        for (int i = 0; i < inputs.length; i++) {
            String input = inputs[i];
            System.out.println("\n--- 第 " + (i + 1) + " 次调用 ---");
            System.out.println("Input: " + input);

            // 每次调用生成独立的 trace
            String traceId = langfuseMonitor.traceStart(agentName, input);
            long startTime = System.currentTimeMillis();

            try {
                String output = agent.chat(input);
                long costTimeMs = System.currentTimeMillis() - startTime;

                langfuseMonitor.traceEnd(traceId, agentName, output, costTimeMs, estimateTokens(input + output));

                System.out.println("Trace 已记录: traceId=" + traceId + ", cost=" + costTimeMs + "ms");
                System.out.println("响应摘要: " + truncate(output, 100));

            } catch (Exception e) {
                langfuseMonitor.traceError(traceId, agentName, e.getMessage());
                System.err.println("调用异常: " + e.getMessage());
            }
        }

        langfuseMonitor.flush();
        System.out.println("\n=== 多次调用 Trace 记录测试完成 ===");
    }

    /**
     * 创建 Langfuse 配置
     * <p>
     * 从环境变量读取配置，支持以下环境变量：
     * <ul>
     * <li>LANGFUSE_PUBLIC_KEY - Langfuse 公钥（必需）</li>
     * <li>LANGFUSE_SECRET_KEY - Langfuse 私钥（必需）</li>
     * <li>LANGFUSE_HOST - Langfuse 服务地址（可选，默认 http://localhost:3000）</li>
     * </ul>
     * </p>
     */
    private MonitorConfig.LangfuseConfig createLangfuseConfig() {
        MonitorConfig.LangfuseConfig config = new MonitorConfig.LangfuseConfig();
        config.setEnabled(true);
        config.setDebug(true);

        // 从环境变量读取配置
        String publicKey = System.getenv("LANGFUSE_PUBLIC_KEY");
        String secretKey = System.getenv("LANGFUSE_SECRET_KEY");
        String host = System.getenv("LANGFUSE_HOST");

        if (publicKey != null && !publicKey.isEmpty()) {
            config.setPublicKey(publicKey);
        } else {
            // 使用默认测试值（实际使用时请替换为真实值）
            config.setPublicKey("pk-lf-test-key");
            System.out.println("警告: 使用默认 LANGFUSE_PUBLIC_KEY，请设置环境变量");
        }

        if (secretKey != null && !secretKey.isEmpty()) {
            config.setSecretKey(secretKey);
        } else {
            config.setSecretKey("sk-lf-test-key");
            System.out.println("警告: 使用默认 LANGFUSE_SECRET_KEY，请设置环境变量");
        }

        config.setHost(host != null ? host : "http://localhost:3000");

        System.out.println("Langfuse 配置: host=" + config.getHost() + ", enabled=" + config.isEnabled());
        return config;
    }

    /**
     * 创建 Agent 配置
     */
    private AgentConfig createAgentConfig() {
        AgentConfig config = new AgentConfig();
        config.setName("InvestmentAdvisor");
        config.setDescription("你是一个专业的投资顾问助手，擅长分析投资市场、评估股票价值，并给出专业的投资建议。");
        config.setTools(List.of("stock_price_query", "market_analysis"));
        config.setSkills(List.of());
        return config;
    }

    /**
     * 创建示例工具
     * <p>
     * 创建用于演示的工具，ReActAgent 可以在推理过程中调用这些工具
     * </p>
     */
    private List<Tool> createSampleTools() {
        return List.of(
            new Tool() {
                @Override
                public String getName() {
                    return "stock_price_query";
                }

                @Override
                public String getDescription() {
                    return "查询股票实时价格和基本信息";
                }

                @Override
                public Map<String, Object> getParameterSchema() {
                    return Map.of(
                        "type", "object",
                        "properties", Map.of(
                            "stock_code", Map.of("type", "string", "description", "股票代码，如 600519"),
                            "stock_name", Map.of("type", "string", "description", "股票名称，如 贵州茅台")
                        ),
                        "required", List.of("stock_code")
                    );
                }

                @Override
                public String execute(Map<String, Object> parameters) {
                    String stockCode = (String) parameters.get("stock_code");
                    // 模拟返回股票价格
                    return String.format("{\"stock_code\":\"%s\",\"price\":1888.88,\"change\":2.5,\"volume\":1000000}", stockCode);
                }
            },
            new Tool() {
                @Override
                public String getName() {
                    return "market_analysis";
                }

                @Override
                public String getDescription() {
                    return "分析市场整体情况和趋势";
                }

                @Override
                public Map<String, Object> getParameterSchema() {
                    return Map.of(
                        "type", "object",
                        "properties", Map.of(
                            "market", Map.of("type", "string", "description", "市场名称，如 A股、港股、美股")
                        ),
                        "required", List.of("market")
                    );
                }

                @Override
                public String execute(Map<String, Object> parameters) {
                    String market = (String) parameters.get("market");
                    // 模拟返回市场分析
                    return String.format("{\"market\":\"%s\",\"trend\":\"上涨\",\"sentiment\":\"乐观\",\"recommendation\":\"建议关注科技和消费板块\"}", market);
                }
            }
        );
    }

    /**
     * 模拟工具执行
     */
    private String simulateToolExecution(String toolName, Map<String, Object> parameters) {
        // 根据工具名称返回模拟结果
        if ("stock_price_query".equals(toolName)) {
            String stockCode = (String) parameters.get("stock_code");
            return String.format("{\"stock_code\":\"%s\",\"price\":1888.88,\"change\":2.5,\"volume\":1000000}", stockCode);
        }
        return "{\"result\":\"success\"}";
    }

    /**
     * 估算 token 数量
     * <p>
     * 简单的 token 估算方法，实际使用时应使用模型的 tokenizer
     * </p>
     */
    private int estimateTokens(String text) {
        // 粗略估算：中文约 1.5 字符/token，英文约 4 字符/token
        if (text == null) return 0;
        return (int) (text.length() / 1.5);
    }

    /**
     * 截断字符串
     */
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }
}
