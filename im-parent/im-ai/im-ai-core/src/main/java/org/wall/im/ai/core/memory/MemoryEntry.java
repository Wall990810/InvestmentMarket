package org.wall.im.ai.core.memory;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 记忆条目
 */
public class MemoryEntry {

	/** 唯一ID */
	private String id;

	/** 内容 */
	private String content;

	/** 角色 */
	private String role;

	/** 创建时间 */
	private Instant createdAt;

	/** 重要性评分 (0.0 ~ 1.0) */
	private double importance;

	/** 元数据 */
	private Map<String, Object> metadata = new HashMap<>();

	public MemoryEntry() {
		this.createdAt = Instant.now();
	}

	public MemoryEntry(String id, String content, String role) {
		this.id = id;
		this.content = content;
		this.role = role;
		this.createdAt = Instant.now();
		this.importance = 0.5;
	}

	// --- Getters and Setters ---

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public double getImportance() {
		return importance;
	}

	public void setImportance(double importance) {
		this.importance = importance;
	}

	public Map<String, Object> getMetadata() {
		return metadata;
	}

	public void setMetadata(Map<String, Object> metadata) {
		this.metadata = metadata;
	}

}
