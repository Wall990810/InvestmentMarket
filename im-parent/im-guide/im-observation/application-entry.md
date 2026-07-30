← 返回索引

# 应用入口 ImObservationApplication

[ImObservationApplication.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-observation/src/main/java/org/wall/im/imobservation/ImObservationApplication.java) 是标准的 Spring Boot 启动类：

```java
@SpringBootApplication
public class ImObservationApplication {
    public static void main(String[] args) {
        SpringApplication.run(ImObservationApplication.class, args);
    }
}
```

包路径为 `org.wall.im.imobservation`。

对应的测试类 [ImObservationApplicationTests.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-observation/src/test/java/org/wall/im/imobservation/ImObservationApplicationTests.java) 提供基础的上下文加载校验：

```java
@SpringBootTest
class ImObservationApplicationTests {
    @Test
    void contextLoads() {
    }
}
```