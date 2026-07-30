← 返回索引

# 典型运行方式

1. **本地开发调试**：在 IDE 中直接运行 `ImObservationApplication.main(...)`，或使用 `mvn spring-boot:run` 启动。
2. **打包部署**：执行 `mvn clean package` 生成可执行 jar，通过 `java -jar im-observation-0.0.1-SNAPSHOT.jar` 运行。
3. **测试验证**：执行 `mvn test` 运行 `ImObservationApplicationTests.contextLoads()`，确认 Spring 上下文可正常加载。

后续接入 `im-ai-observation` 后，典型用法是在该应用中配置 Zipkin endpoint、Micrometer registry、Langfuse 凭据等，以集中观测智能体运行状态。