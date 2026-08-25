package org.wall.im.ai.core.sandbox.policy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 组合命令策略
 * <p>
 * 链式叠加多个 {@link CommandPolicy}，采用"任一拒绝即拒绝"的短路语义： 只要有任一子策略返回 {@code false}，整体即返回
 * {@code false}。子策略全部放行才放行。 适合做"关键字黑名单 + 正则白名单 + 路径/网络检查"的组合。
 * </p>
 *
 * <p>
 * 用法：
 * 
 * <pre>{@code
 * CommandPolicy policy = new CompositeCommandPolicy(new KeywordBlacklistPolicy("rm -rf", "mkfs"))
 * 		.add(new RegexWhitelistPolicy("^(echo|ls|cat)\\b.*"));
 * }</pre>
 * </p>
 */
public class CompositeCommandPolicy implements CommandPolicy {

	private final List<CommandPolicy> policies;

	public CompositeCommandPolicy() {
		this.policies = new ArrayList<>();
	}

	public CompositeCommandPolicy(List<CommandPolicy> policies) {
		this.policies = policies == null ? new ArrayList<>() : new ArrayList<>(policies);
	}

	public CompositeCommandPolicy(CommandPolicy... policies) {
		this.policies = policies == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(policies));
	}

	/**
	 * 链式追加子策略
	 */
	public CompositeCommandPolicy add(CommandPolicy policy) {
		if (policy != null) {
			this.policies.add(policy);
		}
		return this;
	}

	/**
	 * 返回不可变子策略视图
	 */
	public List<CommandPolicy> getPolicies() {
		return Collections.unmodifiableList(policies);
	}

	@Override
	public boolean isAllowed(String command) {
		if (command == null) {
			return true;
		}
		for (CommandPolicy p : policies) {
			if (!p.isAllowed(command)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String getPolicyName() {
		return "composite";
	}

}
