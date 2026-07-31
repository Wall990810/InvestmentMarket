package org.wall.im.ai.agent.lifecycle;

import org.wall.im.ai.core.skill.Skill;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 技能注册表
 */
public class SkillRegistry {

	private final Map<String, Skill> skills = new ConcurrentHashMap<>();

	public void register(Skill skill) {
		skills.put(skill.getName(), skill);
	}

	public Skill get(String name) {
		return skills.get(name);
	}

	public Collection<Skill> getAll() {
		return skills.values();
	}

	public void unregister(String name) {
		skills.remove(name);
	}

}
