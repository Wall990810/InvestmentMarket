package org.wall.im.ai.agent.lifecycle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wall.im.ai.agent.registry.AgentRegistry;
import org.wall.im.ai.core.agent.Agent;
import org.wall.im.ai.core.config.AgentsDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent生命周期管理器
 * <p>
 * 负责Agent的创建、初始化、启动、停止、销毁等全生命周期管理
 * </p>
 */
public class AgentLifecycleManager {

	private static final Logger log = LoggerFactory.getLogger(AgentLifecycleManager.class);

	private final AgentRegistry registry;

	private final AgentFactory factory;

	private final List<AgentLifecycleListener> listeners = new ArrayList<>();

	public AgentLifecycleManager(AgentRegistry registry, AgentFactory factory) {
		this.registry = registry;
		this.factory = factory;
	}

	/**
	 * 添加生命周期监听器
	 */
	public void addListener(AgentLifecycleListener listener) {
		listeners.add(listener);
	}

	/**
	 * 根据配置定义创建并注册所有Agent
	 * @param definition Agent配置定义
	 */
	public void createAgents(AgentsDefinition definition) {
		log.info("Starting creation of {} agents...", definition.getAgents().size());
		for (var agentConfig : definition.getAgents()) {
			try {
				Agent agent = factory.create(agentConfig);
				agent.initialize();
				registry.register(agent);
				notifyCreated(agent);
				log.info("Agent '{}' created and initialized successfully", agent.getName());
			}
			catch (Exception e) {
				log.error("Failed to create agent: {}", agentConfig.getName(), e);
				notifyError(agentConfig.getName(), e);
			}
		}
		log.info("Agent creation completed. Total registered: {}", registry.size());
	}

	/**
	 * 启动所有Agent
	 */
	public void startAll() {
		log.info("Starting all registered agents...");
		registry.getAll().forEach(agent -> {
			try {
				notifyStarted(agent);
			}
			catch (Exception e) {
				log.error("Failed to start agent: {}", agent.getName(), e);
			}
		});
	}

	/**
	 * 停止所有Agent
	 */
	public void stopAll() {
		log.info("Stopping all registered agents...");
		registry.getAll().forEach(agent -> {
			try {
				agent.reset();
				notifyStopped(agent);
			}
			catch (Exception e) {
				log.error("Failed to stop agent: {}", agent.getName(), e);
			}
		});
	}

	/**
	 * 销毁所有Agent
	 */
	public void destroyAll() {
		registry.getAll().forEach(agent -> {
			try {
				agent.destroy();
				notifyDestroyed(agent);
			}
			catch (Exception e) {
				log.error("Failed to destroy agent: {}", agent.getName(), e);
			}
		});
		registry.destroyAll();
	}

	// --- 通知方法 ---

	private void notifyCreated(Agent agent) {
		listeners.forEach(l -> l.onCreated(agent));
	}

	private void notifyStarted(Agent agent) {
		listeners.forEach(l -> l.onStarted(agent));
	}

	private void notifyStopped(Agent agent) {
		listeners.forEach(l -> l.onStopped(agent));
	}

	private void notifyDestroyed(Agent agent) {
		listeners.forEach(l -> l.onDestroyed(agent));
	}

	private void notifyError(String agentName, Exception e) {
		listeners.forEach(l -> l.onError(agentName, e));
	}

}
