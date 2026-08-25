package org.wall.im.ai.core.sandbox.policy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 正则白名单策略
 * <p>
 * 命令必须匹配任一正则（整行匹配）才放行，否则拒绝。 适合"只允许 echo/ls/cat/git status 等安全命令"的精细控制。 默认无正则时视为不限制（返回
 * true），方便作为可选叠加策略。
 * </p>
 */
public class RegexWhitelistPolicy implements CommandPolicy {

	private final List<Pattern> patterns;

	public RegexWhitelistPolicy(Collection<Pattern> patterns) {
		this.patterns = patterns == null ? new ArrayList<>() : new ArrayList<>(patterns);
	}

	public RegexWhitelistPolicy(String... regexes) {
		this.patterns = new ArrayList<>();
		if (regexes != null) {
			for (String r : regexes) {
				if (r != null && !r.isBlank()) {
					this.patterns.add(Pattern.compile(r));
				}
			}
		}
	}

	@Override
	public boolean isAllowed(String command) {
		if (patterns.isEmpty()) {
			return true;
		}
		if (command == null) {
			return false;
		}
		for (Pattern p : patterns) {
			if (p.matcher(command).find()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String getPolicyName() {
		return "regex-whitelist";
	}

	/**
	 * 返回不可变正则视图
	 */
	public List<Pattern> getPatterns() {
		return Collections.unmodifiableList(patterns);
	}

}
