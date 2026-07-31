package org.wall.im.ai.core.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent执行结果
 */
public class AgentResult {

	/** 是否成功 */
	private boolean success;

	/** 输出内容 */
	private String output;

	/** 执行耗时(毫秒) */
	private long costTimeMs;

	/** 使用的token数 */
	private int tokenUsage;

	/** 消息链路 */
	private List<Message> messageChain = new ArrayList<>();

	/** 错误信息 */
	private String errorMessage;

	/** 关联traceId */
	private String traceId;

	public static AgentResult success(String output) {
		AgentResult result = new AgentResult();
		result.setSuccess(true);
		result.setOutput(output);
		return result;
	}

	public static AgentResult failure(String errorMessage) {
		AgentResult result = new AgentResult();
		result.setSuccess(false);
		result.setErrorMessage(errorMessage);
		return result;
	}

	// --- Getters and Setters ---

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getOutput() {
		return output;
	}

	public void setOutput(String output) {
		this.output = output;
	}

	public long getCostTimeMs() {
		return costTimeMs;
	}

	public void setCostTimeMs(long costTimeMs) {
		this.costTimeMs = costTimeMs;
	}

	public int getTokenUsage() {
		return tokenUsage;
	}

	public void setTokenUsage(int tokenUsage) {
		this.tokenUsage = tokenUsage;
	}

	public List<Message> getMessageChain() {
		return messageChain;
	}

	public void setMessageChain(List<Message> messageChain) {
		this.messageChain = messageChain;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public String getTraceId() {
		return traceId;
	}

	public void setTraceId(String traceId) {
		this.traceId = traceId;
	}

}
