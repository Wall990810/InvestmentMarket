package org.wall.im.ai.core.agent;

import org.wall.im.ai.core.memory.MemoryStore;
import org.wall.im.ai.core.skill.Skill;
import org.wall.im.ai.core.tool.Tool;

import java.util.HashMap;
import java.util.Map;

/**
 * Agent执行上下文
 * <p>
 * 封装Agent运行时所需的所有依赖和状态
 * </p>
 */
public class AgentContext {

	/** 关联的Agent */
	private final Agent agent;

	/** 已注册的技能 */
	private final Map<String, Skill> skills = new HashMap<>();

	/** 已注册的工具 */
	private final Map<String, Tool> tools = new HashMap<>();

	/** 短期记忆 */
	private MemoryStore shortTermMemory;

	/** 长期记忆 */
	private MemoryStore longTermMemory;

	/** 上下文变量 */
	private final Map<String, Object> variables = new HashMap<>();

	public AgentContext(Agent agent) {
		this.agent = agent;
	}

	public void registerSkill(Skill skill) {
		skills.put(skill.getName(), skill);
	}

	public void registerTool(Tool tool) {
		tools.put(tool.getName(), tool);
	}

	public Skill getSkill(String name) {
		return skills.get(name);
	}

	public Tool getTool(String name) {
		return tools.get(name);
	}

	public void setVariable(String key, Object value) {
		variables.put(key, value);
	}

	@SuppressWarnings("unchecked")
	public <T> T getVariable(String key) {
		return (T) variables.get(key);
	}

	// --- Getters and Setters ---

	public Agent getAgent() {
		return agent;
	}

	public Map<String, Skill> getSkills() {
		return skills;
	}

	public Map<String, Tool> getTools() {
		return tools;
	}

	public MemoryStore getShortTermMemory() {
		return shortTermMemory;
	}

	public void setShortTermMemory(MemoryStore shortTermMemory) {
		this.shortTermMemory = shortTermMemory;
	}

	public MemoryStore getLongTermMemory() {
		return longTermMemory;
	}

	public void setLongTermMemory(MemoryStore longTermMemory) {
		this.longTermMemory = longTermMemory;
	}

	public Map<String, Object> getVariables() {
		return variables;
	}

}
