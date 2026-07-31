package org.wall.im.ai.core.config;

import org.wall.im.ai.core.model.AgentConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * 全局Agent配置集合
 * <p>
 * 对应agents.yml配置文件的根节点
 * </p>
 */
public class AgentsDefinition {

	/** 全局默认配置 */
	private AgentConfig defaults;

	/** Agent列表 */
	private List<AgentConfig> agents = new ArrayList<>();

	// --- Getters and Setters ---

	public AgentConfig getDefaults() {
		return defaults;
	}

	public void setDefaults(AgentConfig defaults) {
		this.defaults = defaults;
	}

	public List<AgentConfig> getAgents() {
		return agents;
	}

	public void setAgents(List<AgentConfig> agents) {
		this.agents = agents;
	}

}
