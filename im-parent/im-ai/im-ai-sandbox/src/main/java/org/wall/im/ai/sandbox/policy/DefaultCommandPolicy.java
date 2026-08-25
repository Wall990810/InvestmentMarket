package org.wall.im.ai.sandbox.policy;

import org.wall.im.ai.core.sandbox.policy.CompositeCommandPolicy;
import org.wall.im.ai.core.sandbox.policy.KeywordBlacklistPolicy;

/**
 * 默认命令策略
 * <p>
 * 迁移自旧 {@code SandboxManager.containsDangerousOperations} 的硬编码黑名单，保持向后兼容。 无参构造等价于旧行为（11
 * 条关键字，大小写不敏感）；传入 {@code networkRestricted=true} 时额外补充网络命令关键字。
 * </p>
 *
 * <p>
 * 第三方扩展可继承本类覆盖 {@link #buildDefault()} 或直接使用 {@link CompositeCommandPolicy} 链式叠加。
 * </p>
 */
public class DefaultCommandPolicy extends CompositeCommandPolicy {

	/** 旧 SandboxManager 硬编码黑名单，原样迁移以保持兼容 */
	public static final String[] DEFAULT_DANGEROUS_KEYWORDS = { "rm -rf /", "rm -rf ~", "mkfs", "dd if=", ":(){:|:&};:",
			"chmod -R 777 /", "wget", "curl -o", "nc -l", "> /dev/sda", "format c:" };

	/** 网络受限时额外补充的关键字（与默认黑名单不冲突，按词边界匹配） */
	public static final String[] NETWORK_EXTRA_KEYWORDS = { "scp ", "rsync ", "ssh ", "telnet ", "ftp ", "ncat",
			"netcat " };

	public DefaultCommandPolicy() {
		this(false);
	}

	public DefaultCommandPolicy(boolean networkRestricted) {
		super();
		buildDefault();
		if (networkRestricted) {
			add(new KeywordBlacklistPolicy(NETWORK_EXTRA_KEYWORDS));
		}
	}

	/**
	 * 子类可覆盖以自定义默认策略链
	 */
	protected void buildDefault() {
		add(new KeywordBlacklistPolicy(DEFAULT_DANGEROUS_KEYWORDS));
	}

	@Override
	public String getPolicyName() {
		return "default";
	}

}
