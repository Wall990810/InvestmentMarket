# ← 返回索引

# application.yml 关键配置

[application.yml](file:///d:/IdeaProject/InvestmentMarket/im-admin/src/main/resources/application.yml)：

```yaml
server:
  port: 8080

spring:
  ai:
    dashscope:
      # API Key 通过环境变量 AI_DASHSCOPE_API_KEY 或直接在此处配置
      # 获取地址: https://bailian.console.aliyun.com/
      api-key: ${AI_DASHSCOPE_API_KEY:}
      chat:
        model: qwen-plus
        temperature: 0.7
        max-tokens: 4096
```

- 服务端口：`8080`
- DashScope API Key：从环境变量 `AI_DASHSCOPE_API_KEY` 注入，未设置则为空字符串
- 通义千问模型：`qwen-plus`，温度 `0.7`，最大 token `4096`