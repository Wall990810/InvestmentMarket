package org.wall.im.ai.agent.skill;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.wall.im.ai.agent.lifecycle.SkillRegistry;

/**
 * Markdown技能自动配置
 * <p>
 * 当Spring Boot应用启动时，若存在{@link SkillRegistry} Bean且开启了
 * {@code im.ai.markdown-skills.enabled=true}（默认开启）， 则自动扫描并加载classpath和文件系统中的.md技能文件。
 * </p>
 *
 * <h3>使用方式</h3>
 * <ol>
 * <li>在classpath的{@code skills/}目录下创建.md技能文件</li>
 * <li>应用启动时自动加载并注册到SkillRegistry</li>
 * <li>在Agent配置(YAML)中通过skills列表引用技能名称即可使用</li>
 * </ol>
 *
 * <h3>自定义配置</h3> <pre>
 * im.ai.markdown-skills:
 *   enabled: true
 *   classpath-dirs:
 *     - skills
 *     - custom-skills
 *   file-system-dirs:
 *     - /opt/skills
 * </pre>
 */
@AutoConfiguration
@ConditionalOnClass(SkillRegistry.class)
@ConditionalOnProperty(prefix = "im.ai.markdown-skills", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(MarkdownSkillProperties.class)
public class MarkdownSkillAutoConfiguration {

	private static final Logger log = LoggerFactory.getLogger(MarkdownSkillAutoConfiguration.class);

	@Bean
	@ConditionalOnMissingBean
	public MarkdownSkillLoader markdownSkillLoader(SkillRegistry skillRegistry) {
		return new MarkdownSkillLoader(skillRegistry);
	}

	@Bean
	@ConditionalOnBean(MarkdownSkillLoader.class)
	public MarkdownSkillLoaderInitializer markdownSkillLoaderInitializer(MarkdownSkillLoader loader,
			MarkdownSkillProperties properties) {
		return new MarkdownSkillLoaderInitializer(loader, properties);
	}

}
