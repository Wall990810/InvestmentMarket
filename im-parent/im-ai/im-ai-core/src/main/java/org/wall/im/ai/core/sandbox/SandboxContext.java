package org.wall.im.ai.core.sandbox;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 沙盒执行上下文
 * <p>
 * 携带 Agent 与会话标识，用于远端沙盒的租户/会话隔离与请求头透传（X-Agent-Id / X-Session-Id /
 * X-Meta-*）。本地沙盒可仅用于审计与容器命名。
 * </p>
 */
public class SandboxContext {

	/** Agent 标识 */
	private String agentId;

	/** 会话标识 */
	private String sessionId;

	/** 透传到远端请求头的元数据（key 转为 X-Meta-&lt;Key&gt;） */
	private Map<String, String> metadata;

	public SandboxContext() {
	}

	public SandboxContext(String agentId, String sessionId) {
		this.agentId = agentId;
		this.sessionId = sessionId;
	}

	/**
	 * 静态工厂
	 */
	public static SandboxContext of(String agentId, String sessionId) {
		return new SandboxContext(agentId, sessionId);
	}

	public String getAgentId() {
		return agentId;
	}

	public void setAgentId(String agentId) {
		this.agentId = agentId;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public Map<String, String> getMetadata() {
		return metadata;
	}

	public void setMetadata(Map<String, String> metadata) {
		this.metadata = metadata;
	}

	/**
	 * 返回不可变视图，便于安全透传
	 */
	public Map<String, String> metadataView() {
		return metadata == null ? Collections.emptyMap() : Collections.unmodifiableMap(metadata);
	}

	/**
	 * 添加元数据条目，链式调用
	 */
	public SandboxContext withMetadata(String key, String value) {
		if (this.metadata == null) {
			this.metadata = new HashMap<>();
		}
		this.metadata.put(key, value);
		return this;
	}

}
