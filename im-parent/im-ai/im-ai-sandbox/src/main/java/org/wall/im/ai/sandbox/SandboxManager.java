package org.wall.im.ai.sandbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wall.im.ai.core.model.SandboxConfig;
import org.wall.im.ai.core.sandbox.Sandbox;
import org.wall.im.ai.core.sandbox.SandboxContext;
import org.wall.im.ai.core.sandbox.SandboxLifecycleListener;
import org.wall.im.ai.core.sandbox.SandboxResult;
import org.wall.im.ai.core.sandbox.policy.CommandPolicy;
import org.wall.im.ai.sandbox.policy.DefaultCommandPolicy;

import java.util.List;

/**
 * 安全沙盒管理器
 * <p>
 * 封装沙盒的安全检查逻辑，提供统一的沙盒访问入口。 通过注入 {@link CommandPolicy} 替代旧硬编码黑名单， 支持策略链扩展；通过
 * {@link SandboxLifecycleListener} 提供生命周期钩子。
 * </p>
 *
 * <h3>向后兼容</h3>
 * <ul>
 * <li>旧构造器 {@link #SandboxManager(Sandbox, SandboxConfig)} 签名与行为保持等价 （内部退回
 * {@link DefaultCommandPolicy}，黑名单与旧硬编码 1:1 迁移）。</li>
 * <li>策略拒绝时的失败文案保留 "Dangerous operations detected and blocked" / "Dangerous command
 * blocked"，以兼容旧测试断言 {@code result.getErrorOutput().contains("Dangerous")}。</li>
 * <li>策略拒绝时不调用 sandbox 的任何方法（与旧 {@code verifyNoInteractions(sandbox)} 行为一致）。</li>
 * </ul>
 */
public class SandboxManager {

	private static final Logger log = LoggerFactory.getLogger(SandboxManager.class);

	/** 策略拒绝代码时的失败文案（保持向后兼容） */
	public static final String DANGEROUS_CODE_MSG = "Dangerous operations detected and blocked";

	/** 策略拒绝命令时的失败文案（保持向后兼容） */
	public static final String DANGEROUS_COMMAND_MSG = "Dangerous command blocked";

	private final Sandbox sandbox;

	private final SandboxConfig config;

	private final CommandPolicy commandPolicy;

	private final List<SandboxLifecycleListener> listeners;

	/** sandbox 是否由本管理器创建（Registry 路由路径下 destroy 时需销毁，外部传入时不销毁） */
	private final boolean ownedByManager;

	/**
	 * 旧构造器（保留，行为等价）。
	 * <p>
	 * 内部等价于
	 * {@code this(sandbox, config, config.getCommandPolicy() != null ? config.getCommandPolicy() : new DefaultCommandPolicy())}。
	 * 因 {@link SandboxConfig#getCommandPolicy()} 默认为 null，自动落到
	 * {@link DefaultCommandPolicy}（黑名单与旧硬编码等价）。
	 * </p>
	 */
	public SandboxManager(Sandbox sandbox, SandboxConfig config) {
		this(sandbox, config, config != null && config.getCommandPolicy() != null ? config.getCommandPolicy()
				: new DefaultCommandPolicy());
	}

	/**
	 * 注入命令策略
	 */
	public SandboxManager(Sandbox sandbox, SandboxConfig config, CommandPolicy commandPolicy) {
		this(sandbox, config, commandPolicy, List.of());
	}

	/**
	 * 注入命令策略 + 生命周期监听
	 */
	public SandboxManager(Sandbox sandbox, SandboxConfig config, CommandPolicy commandPolicy,
			List<SandboxLifecycleListener> listeners) {
		this.sandbox = sandbox;
		this.config = config;
		this.commandPolicy = commandPolicy != null ? commandPolicy : new DefaultCommandPolicy();
		this.listeners = listeners == null ? List.of() : listeners;
		this.ownedByManager = false;
	}

	/**
	 * 基于 Registry 路由创建 sandbox（Agent 框架推荐路径）。
	 */
	public SandboxManager(SandboxConfig config, SandboxRegistry registry) {
		this(config, registry, null);
	}

	/**
	 * 基于 Registry 路由创建 sandbox + 显式注入策略。
	 * <p>
	 * 策略优先级：显式传入 &gt; {@link SandboxConfig#getCommandPolicy()} &gt;
	 * {@link DefaultCommandPolicy}。
	 * </p>
	 */
	public SandboxManager(SandboxConfig config, SandboxRegistry registry, CommandPolicy policy) {
		this.sandbox = registry.create(config);
		this.config = config;
		this.commandPolicy = policy != null ? policy : (config != null && config.getCommandPolicy() != null
				? config.getCommandPolicy() : new DefaultCommandPolicy());
		this.listeners = List.of();
		this.ownedByManager = true;
	}

	/**
	 * 在沙盒中安全执行代码
	 * @param code 要执行的代码
	 * @return 执行结果
	 */
	public SandboxResult safeExecute(String code) {
		firePreExecute(code);
		if (!config.isEnabled()) {
			log.warn("Sandbox is disabled, executing without restrictions");
			SandboxResult result = sandbox.execute(code, null);
			firePostExecute(result);
			return result;
		}

		// 策略检查：任一拒绝即拦截
		if (!commandPolicy.isAllowed(code)) {
			log.warn("Dangerous operations detected in code, blocking execution");
			SandboxResult failure = SandboxResult.failure(DANGEROUS_CODE_MSG, -1, 0);
			firePostExecute(failure);
			return failure;
		}

		SandboxResult result = sandbox.execute(code, config.getWorkDir());
		firePostExecute(result);
		return result;
	}

	/**
	 * 在沙盒中安全执行命令
	 * @param command 命令
	 * @return 执行结果
	 */
	public SandboxResult safeExecuteCommand(String command) {
		firePreExecute(command);
		if (!config.isEnabled()) {
			SandboxResult result = sandbox.executeCommand(command);
			firePostExecute(result);
			return result;
		}

		if (!commandPolicy.isAllowed(command)) {
			log.warn("Dangerous command detected, blocking execution");
			SandboxResult failure = SandboxResult.failure(DANGEROUS_COMMAND_MSG, -1, 0);
			firePostExecute(failure);
			return failure;
		}

		SandboxResult result = sandbox.executeCommand(command);
		firePostExecute(result);
		return result;
	}

	/**
	 * 检查路径是否可访问
	 */
	public boolean canAccess(String path) {
		return sandbox.isPathAllowed(path);
	}

	/**
	 * 销毁沙盒环境。
	 * <p>
	 * 仅当 sandbox 由本管理器创建（Registry 路由）时才调用 sandbox.destroy()； 外部传入的 sandbox 由外部负责生命周期。
	 * 始终触发 onDestroy 回调。
	 * </p>
	 */
	public void destroy() {
		SandboxContext ctx = config != null ? config.getContext() : null;
		try {
			listeners.forEach(l -> safeListener(() -> l.onDestroy(sandbox, ctx)));
		}
		finally {
			if (ownedByManager) {
				try {
					sandbox.destroy();
				}
				catch (Exception e) {
					log.warn("Error destroying sandbox", e);
				}
			}
		}
	}

	/**
	 * 初始化沙盒（触发 onInitialize 回调，委托 sandbox.initialize）
	 */
	public SandboxManager initialize() {
		SandboxContext ctx = config != null ? config.getContext() : null;
		listeners.forEach(l -> safeListener(() -> l.onInitialize(sandbox, ctx)));
		sandbox.initialize();
		return this;
	}

	// --- 内部辅助 ---

	private void firePreExecute(String code) {
		if (listeners.isEmpty()) {
			return;
		}
		SandboxContext ctx = config != null ? config.getContext() : null;
		listeners.forEach(l -> safeListener(() -> l.onPreExecute(sandbox, code, ctx)));
	}

	private void firePostExecute(SandboxResult result) {
		if (listeners.isEmpty()) {
			return;
		}
		SandboxContext ctx = config != null ? config.getContext() : null;
		listeners.forEach(l -> safeListener(() -> l.onPostExecute(sandbox, result, ctx)));
	}

	/**
	 * 安全回调：listener 抛异常不应影响主流程
	 */
	private void safeListener(Runnable action) {
		try {
			action.run();
		}
		catch (Exception e) {
			log.warn("Sandbox lifecycle listener error", e);
		}
	}

}
