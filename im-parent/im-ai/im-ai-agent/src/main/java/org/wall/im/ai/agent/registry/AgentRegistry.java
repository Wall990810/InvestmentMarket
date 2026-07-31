package org.wall.im.ai.agent.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wall.im.ai.core.agent.Agent;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent注册表
 * <p>
 * 管理所有已创建的Agent实例，提供注册、查找、销毁等操作
 * </p>
 */
public class AgentRegistry {

	private static final Logger log = LoggerFactory.getLogger(AgentRegistry.class);

	private final Map<String, Agent> agents = new ConcurrentHashMap<>();

	/**
	 * 注册Agent
	 * @param agent Agent实例
	 */
	public void register(Agent agent) {
		String name = agent.getName();
		if (agents.containsKey(name)) {
			log.warn("Agent with name '{}' already exists, replacing...", name);
			agents.get(name).destroy();
		}
		agents.put(name, agent);
		log.info("Registered agent: {}", name);
	}

	/**
	 * 根据名称获取Agent
	 * @param name Agent名称
	 * @return Agent实例
	 */
	public Optional<Agent> get(String name) {
		return Optional.ofNullable(agents.get(name));
	}

	/**
	 * 根据名称获取Agent，不存在则抛出异常
	 */
	public Agent getRequired(String name) {
		return get(name).orElseThrow(() -> new IllegalArgumentException("Agent not found: " + name));
	}

	/**
	 * 获取所有已注册的Agent
	 */
	public Collection<Agent> getAll() {
		return Collections.unmodifiableCollection(agents.values());
	}

	/**
	 * 判断Agent是否已注册
	 */
	public boolean contains(String name) {
		return agents.containsKey(name);
	}

	/**
	 * 注销Agent
	 */
	public void unregister(String name) {
		Agent agent = agents.remove(name);
		if (agent != null) {
			agent.destroy();
			log.info("Unregistered agent: {}", name);
		}
	}

	/**
	 * 销毁所有Agent
	 */
	public void destroyAll() {
		agents.values().forEach(agent -> {
			try {
				agent.destroy();
				log.info("Destroyed agent: {}", agent.getName());
			}
			catch (Exception e) {
				log.error("Error destroying agent: {}", agent.getName(), e);
			}
		});
		agents.clear();
		log.info("All agents destroyed");
	}

	/**
	 * 获取已注册Agent数量
	 */
	public int size() {
		return agents.size();
	}

}
