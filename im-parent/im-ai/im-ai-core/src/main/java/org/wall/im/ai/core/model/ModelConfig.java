package org.wall.im.ai.core.model;

/**
 * 模型配置
 */
public class ModelConfig {

    /** 模型提供者，如: openai, azure, local */
    private String provider;

    /** 模型名称，如: gpt-4, qwen-72b */
    private String name;

    /** API密钥 */
    private String apiKey;

    /** API端点 */
    private String endpoint;

    /** 温度参数 */
    private Double temperature = 0.7;

    /** 最大Token数 */
    private Integer maxTokens = 4096;

    // --- Getters and Setters ---

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }

    public Integer getMaxTokens() { return maxTokens; }
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
}
