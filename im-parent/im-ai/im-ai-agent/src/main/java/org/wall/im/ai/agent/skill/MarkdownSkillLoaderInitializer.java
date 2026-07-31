package org.wall.im.ai.agent.skill;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

/**
 * Markdown技能加载器初始化器
 * <p>
 * 在Spring应用上下文完全启动后触发，扫描并加载配置中的.md技能文件。
 * </p>
 */
public class MarkdownSkillLoaderInitializer {

	private static final Logger log = LoggerFactory.getLogger(MarkdownSkillLoaderInitializer.class);

	private final MarkdownSkillLoader loader;

	private final MarkdownSkillProperties properties;

	public MarkdownSkillLoaderInitializer(MarkdownSkillLoader loader, MarkdownSkillProperties properties) {
		this.loader = loader;
		this.properties = properties;
	}

	/**
	 * 应用就绪后加载所有Markdown技能
	 */
	@EventListener(ApplicationReadyEvent.class)
	public void loadSkills() {
		log.info("开始加载Markdown技能文件...");

		int totalLoaded = 0;

		// 从classpath目录加载
		for (String dir : properties.getClasspathDirs()) {
			int count = loader.loadFromClasspath(dir);
			totalLoaded += count;
			if (count > 0) {
				log.info("从classpath目录 '{}' 加载了 {} 个Markdown技能", dir, count);
			}
		}

		// 从文件系统目录加载
		for (String dir : properties.getFileSystemDirs()) {
			int count = loader.loadFromFileSystem(dir);
			totalLoaded += count;
			if (count > 0) {
				log.info("从文件系统目录 '{}' 加载了 {} 个Markdown技能", dir, count);
			}
		}

		log.info("Markdown技能加载完成，共加载 {} 个技能", totalLoaded);
	}

}
