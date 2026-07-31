package org.wall.im.ai.core.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 消息模型
 */
public class Message {

	/** 消息角色: system, user, assistant, tool */
	private String role;

	/** 消息内容 */
	private String content;

	/** 消息名称（用于tool调用） */
	private String name;

	/** 时间戳 */
	private Instant timestamp;

	/** 扩展元数据 */
	private Map<String, Object> metadata = new HashMap<>();

	/** 关联的traceId */
	private String traceId;

	public Message() {
		this.timestamp = Instant.now();
	}

	public Message(String role, String content) {
		this.role = role;
		this.content = content;
		this.timestamp = Instant.now();
	}

	// --- 工厂方法 ---

	public static Message system(String content) {
		return new Message("system", content);
	}

	public static Message user(String content) {
		return new Message("user", content);
	}

	public static Message assistant(String content) {
		return new Message("assistant", content);
	}

	public static Message tool(String name, String content) {
		Message msg = new Message("tool", content);
		msg.setName(name);
		return msg;
	}

	// --- Getters and Setters ---

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Instant getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Instant timestamp) {
		this.timestamp = timestamp;
	}

	public Map<String, Object> getMetadata() {
		return metadata;
	}

	public void setMetadata(Map<String, Object> metadata) {
		this.metadata = metadata;
	}

	public String getTraceId() {
		return traceId;
	}

	public void setTraceId(String traceId) {
		this.traceId = traceId;
	}

}
