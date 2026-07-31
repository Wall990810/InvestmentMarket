package org.wall.im.ai.agent.lifecycle;

import org.wall.im.ai.core.agent.Agent;

/**
 * Agent生命周期监听器
 */
public interface AgentLifecycleListener {

	/** Agent创建完成 */
	default void onCreated(Agent agent) {
	}

	/** Agent启动 */
	default void onStarted(Agent agent) {
	}

	/** Agent停止 */
	default void onStopped(Agent agent) {
	}

	/** Agent销毁 */
	default void onDestroyed(Agent agent) {
	}

	/** Agent创建/初始化出错 */
	default void onError(String agentName, Exception e) {
	}

}
