package org.wall.im.ai.agent.skill;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.wall.im.ai.agent.lifecycle.SkillRegistry;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MarkdownSkill解析与加载测试
 */
class MarkdownSkillTest {

	private SkillRegistry skillRegistry;

	private MarkdownSkillLoader loader;

	@BeforeEach
	void setUp() {
		skillRegistry = new SkillRegistry();
		loader = new MarkdownSkillLoader(skillRegistry);
	}

	@Test
	void testParseSkillBasic() {
		String md = """
				---
				name: test-skill
				description: 测试技能
				---

				# 测试指令
				请处理以下输入：{{input}}
				""";

		MarkdownSkill skill = loader.parseSkill(md, "test.md");
		assertNotNull(skill);
		assertEquals("test-skill", skill.getName());
		assertEquals("测试技能", skill.getDescription());
		assertTrue(skill.getTools().isEmpty());
	}

	@Test
	void testParseSkillWithTools() {
		String md = """
				---
				name: analysis-skill
				description: 分析技能
				tools:
				  - tool-a
				  - tool-b
				---

				# 分析
				分析 {{input}}
				""";

		MarkdownSkill skill = loader.parseSkill(md, "analysis.md");
		assertNotNull(skill);
		assertEquals("analysis-skill", skill.getName());
		assertEquals(2, skill.getTools().size());
		assertTrue(skill.getTools().contains("tool-a"));
		assertTrue(skill.getTools().contains("tool-b"));
	}

	@Test
	void testExecuteWithVariableSubstitution() {
		String md = """
				---
				name: echo-skill
				description: 回显技能
				---

				用户输入: {{input}}
				技能名: {{skillName}}
				""";

		MarkdownSkill skill = loader.parseSkill(md, "echo.md");
		assertNotNull(skill);

		String result = skill.execute("hello world");
		assertTrue(result.contains("hello world"));
		assertTrue(result.contains("echo-skill"));
		assertFalse(result.contains("{{input}}"));
		assertFalse(result.contains("{{skillName}}"));
	}

	@Test
	void testCanExecute() {
		String md = """
				---
				name: test-skill
				description: 测试
				---

				Body
				""";

		MarkdownSkill skill = loader.parseSkill(md, "test.md");
		assertNotNull(skill);
		assertTrue(skill.canExecute("input"));
		assertFalse(skill.canExecute(null));
		assertFalse(skill.canExecute(""));
		assertFalse(skill.canExecute("   "));
	}

	@Test
	void testParseSkillNoFrontmatter() {
		String md = "# Just a markdown body\n\nNo frontmatter here.";
		MarkdownSkill skill = loader.parseSkill(md, "no-fm.md");
		assertNull(skill);
	}

	@Test
	void testParseSkillMissingName() {
		String md = """
				---
				description: 无名称技能
				---

				Body
				""";

		MarkdownSkill skill = loader.parseSkill(md, "noname.md");
		assertNull(skill);
	}

	@Test
	void testRegisterAndLoad() {
		String md = """
				---
				name: registered-skill
				description: 注册测试
				---

				Body: {{input}}
				""";

		MarkdownSkill skill = loader.parseSkill(md, "registered.md");
		assertNotNull(skill);
		skillRegistry.register(skill);

		var retrieved = skillRegistry.get("registered-skill");
		assertNotNull(retrieved);
		assertEquals("注册测试", retrieved.getDescription());
	}

	@Test
	void testExtraMetadata() {
		String md = """
				---
				name: meta-skill
				description: 元数据测试
				version: 2.0
				author: wall
				---

				Body
				""";

		MarkdownSkill skill = loader.parseSkill(md, "meta.md");
		assertNotNull(skill);
		assertEquals("2.0", skill.getMetadata().get("version"));
		assertEquals("wall", skill.getMetadata().get("author"));
	}

	@Test
	void testLoadFromClasspath() {
		int count = loader.loadFromClasspath("skills");
		assertTrue(count >= 2, "Should load at least 2 skills from classpath skills directory, got: " + count);
		assertNotNull(skillRegistry.get("investment-analysis-skill"));
		assertNotNull(skillRegistry.get("portfolio-recommend-skill"));
	}

}
