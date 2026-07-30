# 配置与切换后端

← [返回索引](../README.md)

## YAML 配置示例

下面是一个典型的 `application.yml` 片段，体现 [MemoryConfig](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-core/src/main/java/org/wall/im/ai/core/model/MemoryConfig.java) 的字段含义（字段名需与配置绑定方式对应）：

```yaml
im:
  ai:
    memory:
      short-term-store: memory      # 短期记忆后端: memory / redis / db
      long-term-store: db           # 长期记忆后端: memory / redis / db
      short-term-max-entries: 100
      long-term-max-entries: 10000
      ttl-seconds: 86400            # 仅 Redis 后端生效，0 表示不过期

spring:
  datasource:                       # 使用 db 后端时需要
    url: jdbc:mysql://localhost:3306/im_ai
    username: root
    password: secret
    driver-class-name: com.mysql.cj.jdbc.Driver
  redis:                            # 使用 redis 后端时需要
    host: localhost
    port: 6379
```

> `MemoryConfig` 默认值：`shortTermStore=memory`、`longTermStore=memory`、`shortTermMaxEntries=100`、`longTermMaxEntries=10000`、`ttlSeconds=0`。

## Java 配置示例

将 `DefaultMemoryStoreFactory` 注册为 Spring Bean，并按需注入 `JdbcTemplate` 与 `RedisOperationsAdapter`：

```java
@Configuration
public class MemoryStoreConfig {

    @Bean
    public RedisOperationsAdapter redisOperationsAdapter(StringRedisTemplate redisTemplate) {
        return new RedisOperationsAdapter() {
            @Override
            public void listRightPush(String key, String value) {
                redisTemplate.opsForList().rightPush(key, value);
            }
            @Override
            public void listRightPushAll(String key, List<String> values) {
                redisTemplate.opsForList().rightPushAll(key, values);
            }
            @Override
            public List<String> listRange(String key, long start, long end) {
                return redisTemplate.opsForList().range(key, start, end);
            }
            @Override
            public void listTrim(String key, long start, long end) {
                redisTemplate.opsForList().trim(key, start, end);
            }
            @Override
            public void expire(String key, long seconds) {
                redisTemplate.expire(key, Duration.ofSeconds(seconds));
            }
            @Override
            public void delete(String key) {
                redisTemplate.delete(key);
            }
        };
    }

    @Bean
    public MemoryStoreFactory memoryStoreFactory(
            @Autowired(required = false) RedisOperationsAdapter redisAdapter,
            @Autowired(required = false) JdbcTemplate jdbcTemplate) {
        return new DefaultMemoryStoreFactory(redisAdapter, jdbcTemplate, 1000, 0L);
    }
}
```

随后即可根据 `MemoryConfig` 选择实现：

```java
MemoryConfig memoryConfig = ...; // 来自配置绑定
MemoryStore shortTerm = memoryStoreFactory.create(memoryConfig.getShortTermStore());
MemoryStore longTerm  = memoryStoreFactory.create(memoryConfig.getLongTermStore());
```