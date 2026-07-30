← 返回索引

# 配置与启动

资源目录下仅有 [application.properties](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-observation/src/main/resources/application.properties)，内容为单行：

```properties
spring.application.name=im-observation
```

- 未显式配置 `server.port`，默认使用 `8080`。
- 未配置其他业务属性，仅声明应用名称。

## 启动方式

```bash
# 在 im-observation 目录下
mvn clean package
mvn spring-boot:run
```

或在 `im-parent` 根目录统一构建（注意：`im-observation` 当前并未列入 `im-parent` 的 `<modules>` 列表，需单独构建或手动纳入聚合）。