package org.wall.im.ai.memory.store;

import org.wall.im.ai.core.memory.MemoryEntry;
import org.wall.im.ai.core.memory.MemoryStore;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 基于内存的记忆存储实现
 * <p>
 * 适用于短期记忆和开发测试场景，数据存储在JVM内存中
 * </p>
 */
public class InMemoryStore implements MemoryStore {

	private final Map<String, List<MemoryEntry>> store = new ConcurrentHashMap<>();

	private final int maxEntries;

	public InMemoryStore() {
		this(1000);
	}

	public InMemoryStore(int maxEntries) {
		this.maxEntries = maxEntries;
	}

	@Override
	public void store(String key, MemoryEntry entry) {
		store.computeIfAbsent(key, k -> Collections.synchronizedList(new ArrayList<>()));
		List<MemoryEntry> entries = store.get(key);
		entries.add(entry);
		// 超出最大条目数时移除最旧的
		while (entries.size() > maxEntries) {
			entries.remove(0);
		}
	}

	@Override
	public void storeAll(String key, List<MemoryEntry> entries) {
		entries.forEach(entry -> store(key, entry));
	}

	@Override
	public List<MemoryEntry> retrieve(String key) {
		return new ArrayList<>(store.getOrDefault(key, Collections.emptyList()));
	}

	@Override
	public List<MemoryEntry> retrieveRecent(String key, int count) {
		List<MemoryEntry> entries = retrieve(key);
		int fromIndex = Math.max(0, entries.size() - count);
		return entries.subList(fromIndex, entries.size());
	}

	@Override
	public List<MemoryEntry> search(String key, String query) {
		return retrieve(key).stream()
			.filter(entry -> entry.getContent() != null && entry.getContent().contains(query))
			.collect(Collectors.toList());
	}

	@Override
	public void clear(String key) {
		store.remove(key);
	}

	@Override
	public String getStoreType() {
		return "memory";
	}

}
