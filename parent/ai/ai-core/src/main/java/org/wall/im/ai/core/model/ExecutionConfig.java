package org.wall.im.ai.core.model;

import java.util.HashMap;
import java.util.Map;

/**
 * 执行环境配置
 */
public class ExecutionConfig {

    /** 最大并发数 */
    private int maxConcurrency = 10;

    /** 超时时间(秒) */
    private int timeoutSeconds = 60;

    /** 重试次数 */
    private int retryCount = 3;

    /** 环境变量 */
    private Map<String, String> envVars = new HashMap<>();

    // --- Getters and Setters ---

    public int getMaxConcurrency() { return maxConcurrency; }
    public void setMaxConcurrency(int maxConcurrency) { this.maxConcurrency = maxConcurrency; }

    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public Map<String, String> getEnvVars() { return envVars; }
    public void setEnvVars(Map<String, String> envVars) { this.envVars = envVars; }
}
