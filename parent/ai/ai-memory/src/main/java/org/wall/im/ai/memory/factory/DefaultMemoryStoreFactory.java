package org.wall.im.ai.memory.factory;

import org.wall.im.ai.agent.lifecycle.MemoryStoreFactory;
import org.wall.im.ai.core.memory.MemoryStore;
import org.wall.im.ai.memory.store.InMemoryStore;
import org.wall.im.ai.memory.store.JdbcMemoryStore;
import org.wall.im.ai.memory.store.RedisMemoryStore;
import org.wall.im.ai.memory.store.RedisOperationsAdapter;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 默认MemoryStore工厂实现
 * <p>根据存储类型创建对应的MemoryStore实例</p>
 */
public class DefaultMemoryStoreFactory implements MemoryStoreFactory {

    private final RedisOperationsAdapter redisAdapter;
    private final JdbcTemplate jdbcTemplate;
    private final int defaultMaxEntries;
    private final long defaultTtlSeconds;

    public DefaultMemoryStoreFactory(RedisOperationsAdapter redisAdapter, JdbcTemplate jdbcTemplate,
                                     int defaultMaxEntries, long defaultTtlSeconds) {
        this.redisAdapter = redisAdapter;
        this.jdbcTemplate = jdbcTemplate;
        this.defaultMaxEntries = defaultMaxEntries;
        this.defaultTtlSeconds = defaultTtlSeconds;
    }

    @Override
    public MemoryStore create(String storeType) {
        return switch (storeType.toLowerCase()) {
            case "memory" -> new InMemoryStore(defaultMaxEntries);
            case "redis" -> {
                if (redisAdapter == null) {
                    throw new IllegalStateException("Redis adapter not configured");
                }
                yield new RedisMemoryStore(redisAdapter, defaultMaxEntries, defaultTtlSeconds);
            }
            case "db" -> {
                if (jdbcTemplate == null) {
                    throw new IllegalStateException("JdbcTemplate not configured");
                }
                yield new JdbcMemoryStore(jdbcTemplate, defaultMaxEntries);
            }
            default -> throw new IllegalArgumentException("Unknown store type: " + storeType);
        };
    }
}
