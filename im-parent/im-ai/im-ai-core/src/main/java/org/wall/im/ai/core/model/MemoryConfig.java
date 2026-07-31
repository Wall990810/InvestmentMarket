package org.wall.im.ai.core.model;

/**
 * 记忆配置
 */
public class MemoryConfig {

	/** 短期记忆存储类型: memory, redis, db */
	private String shortTermStore = "memory";

	/** 长期记忆存储类型: memory, redis, db */
	private String longTermStore = "memory";

	/** 短期记忆最大条目数 */
	private int shortTermMaxEntries = 100;

	/** 长期记忆最大条目数 */
	private int longTermMaxEntries = 10000;

	/** 记忆过期时间(秒)，0表示不过期 */
	private long ttlSeconds = 0;

	// --- Getters and Setters ---

	public String getShortTermStore() {
		return shortTermStore;
	}

	public void setShortTermStore(String shortTermStore) {
		this.shortTermStore = shortTermStore;
	}

	public String getLongTermStore() {
		return longTermStore;
	}

	public void setLongTermStore(String longTermStore) {
		this.longTermStore = longTermStore;
	}

	public int getShortTermMaxEntries() {
		return shortTermMaxEntries;
	}

	public void setShortTermMaxEntries(int shortTermMaxEntries) {
		this.shortTermMaxEntries = shortTermMaxEntries;
	}

	public int getLongTermMaxEntries() {
		return longTermMaxEntries;
	}

	public void setLongTermMaxEntries(int longTermMaxEntries) {
		this.longTermMaxEntries = longTermMaxEntries;
	}

	public long getTtlSeconds() {
		return ttlSeconds;
	}

	public void setTtlSeconds(long ttlSeconds) {
		this.ttlSeconds = ttlSeconds;
	}

}
