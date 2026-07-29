package org.wall.im.ai.core.model;

import java.util.HashMap;
import java.util.Map;

/**
 * 监控配置
 */
public class MonitorConfig {

    /** 是否启用监控 */
    private boolean enabled = true;

    /** Zipkin端点 */
    private String zipkinEndpoint = "http://localhost:9411/api/v2/spans";

    /** Langfuse配置 */
    private LangfuseConfig langfuse;

    /** 自定义指标配置 */
    private Map<String, String> customMetrics = new HashMap<>();

    // --- Getters and Setters ---

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getZipkinEndpoint() { return zipkinEndpoint; }
    public void setZipkinEndpoint(String zipkinEndpoint) { this.zipkinEndpoint = zipkinEndpoint; }

    public LangfuseConfig getLangfuse() { return langfuse; }
    public void setLangfuse(LangfuseConfig langfuse) { this.langfuse = langfuse; }

    public Map<String, String> getCustomMetrics() { return customMetrics; }
    public void setCustomMetrics(Map<String, String> customMetrics) { this.customMetrics = customMetrics; }

    /**
     * Langfuse配置
     */
    public static class LangfuseConfig {
        private boolean enabled = false;
        private String host = "http://localhost:3000";
        private String publicKey;
        private String secretKey;
        /** 是否启用调试日志 */
        private boolean debug = false;
        /** 刷新间隔(毫秒)，0表示不自动刷新 */
        private long flushIntervalMs = 5000;
        /** 批量上报最大条数 */
        private int maxBatchSize = 50;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }

        public String getPublicKey() { return publicKey; }
        public void setPublicKey(String publicKey) { this.publicKey = publicKey; }

        public String getSecretKey() { return secretKey; }
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

        public boolean isDebug() { return debug; }
        public void setDebug(boolean debug) { this.debug = debug; }

        public long getFlushIntervalMs() { return flushIntervalMs; }
        public void setFlushIntervalMs(long flushIntervalMs) { this.flushIntervalMs = flushIntervalMs; }

        public int getMaxBatchSize() { return maxBatchSize; }
        public void setMaxBatchSize(int maxBatchSize) { this.maxBatchSize = maxBatchSize; }

        /**
         * 检查配置是否完整（包含必要的连接信息）
         */
        public boolean isConfigured() {
            return publicKey != null && !publicKey.isEmpty()
                    && secretKey != null && !secretKey.isEmpty();
        }
    }
}
