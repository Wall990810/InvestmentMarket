package org.wall.im.ai.core.sandbox.policy;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 关键字黑名单策略
 * <p>
 * 命令中包含任一关键字（大小写不敏感）即拒绝。 兼容旧 SandboxManager.containsDangerousOperations 的行为（lowerCase +
 * contains）。
 * </p>
 */
public class KeywordBlacklistPolicy implements CommandPolicy {

	private final Set<String> keywords;

	public KeywordBlacklistPolicy(Collection<String> keywords) {
		Set<String> lower = new LinkedHashSet<>();
		if (keywords != null) {
			for (String k : keywords) {
				if (k != null && !k.isBlank()) {
					lower.add(k.toLowerCase());
				}
			}
		}
		this.keywords = lower;
	}

	public KeywordBlacklistPolicy(String... keywords) {
		this(keywords == null ? Collections.emptyList() : Arrays.asList(keywords));
	}

	@Override
	public boolean isAllowed(String command) {
		if (command == null || command.isEmpty()) {
			return true;
		}
		String lower = command.toLowerCase();
		for (String k : keywords) {
			if (lower.contains(k)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String getPolicyName() {
		return "keyword-blacklist";
	}

	/**
	 * 返回不可变关键字视图
	 */
	public Set<String> getKeywords() {
		return Collections.unmodifiableSet(keywords);
	}

}
