package org.wall.im.ai.memory.store;

import org.wall.im.ai.core.memory.MemoryEntry;
import org.wall.im.ai.core.memory.MemoryStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于Redis的记忆存储实现
 * <p>
 * 利用Redis的List结构存储记忆条目，支持TTL过期、持久化
 * </p>
 * <p>
 * 需要引入spring-boot-starter-data-redis依赖
 * </p>
 */
public class RedisMemoryStore implements MemoryStore {

	private static final String KEY_PREFIX = "ai:memory:";

	private final RedisOperationsAdapter redisAdapter;

	private final int maxEntries;

	private final long ttlSeconds;

	public RedisMemoryStore(RedisOperationsAdapter redisAdapter, int maxEntries, long ttlSeconds) {
		this.redisAdapter = redisAdapter;
		this.maxEntries = maxEntries;
		this.ttlSeconds = ttlSeconds;
	}

	@Override
	public void store(String key, MemoryEntry entry) {
		String redisKey = buildKey(key);
		String serialized = serialize(entry);
		redisAdapter.listRightPush(redisKey, serialized);
		// 裁剪到最大长度
		redisAdapter.listTrim(redisKey, -maxEntries, -1);
		if (ttlSeconds > 0) {
			redisAdapter.expire(redisKey, ttlSeconds);
		}
	}

	@Override
	public void storeAll(String key, List<MemoryEntry> entries) {
		String redisKey = buildKey(key);
		List<String> serialized = entries.stream().map(this::serialize).collect(Collectors.toList());
		redisAdapter.listRightPushAll(redisKey, serialized);
		redisAdapter.listTrim(redisKey, -maxEntries, -1);
		if (ttlSeconds > 0) {
			redisAdapter.expire(redisKey, ttlSeconds);
		}
	}

	@Override
	public List<MemoryEntry> retrieve(String key) {
		String redisKey = buildKey(key);
		List<String> items = redisAdapter.listRange(redisKey, 0, -1);
		return items.stream().map(this::deserialize).collect(Collectors.toList());
	}

	@Override
	public List<MemoryEntry> retrieveRecent(String key, int count) {
		String redisKey = buildKey(key);
		List<String> items = redisAdapter.listRange(redisKey, -count, -1);
		return items.stream().map(this::deserialize).collect(Collectors.toList());
	}

	@Override
	public List<MemoryEntry> search(String key, String query) {
		// Redis不支持列表内全文搜索，取全量后过滤
		return retrieve(key).stream()
			.filter(e -> e.getContent() != null && e.getContent().contains(query))
			.collect(Collectors.toList());
	}

	@Override
	public void clear(String key) {
		redisAdapter.delete(buildKey(key));
	}

	@Override
	public String getStoreType() {
		return "redis";
	}

	private String buildKey(String key) {
		return KEY_PREFIX + key;
	}

	private String serialize(MemoryEntry entry) {
		return entry.getId() + "|" + entry.getRole() + "|" + entry.getImportance() + "|" + entry.getContent();
	}

	private MemoryEntry deserialize(String data) {
		String[] parts = data.split("\\|", 4);
		MemoryEntry entry = new MemoryEntry();
		if (parts.length >= 4) {
			entry.setId(parts[0]);
			entry.setRole(parts[1]);
			entry.setImportance(Double.parseDouble(parts[2]));
			entry.setContent(parts[3]);
		}
		else {
			entry.setContent(data);
		}
		return entry;
	}

}
