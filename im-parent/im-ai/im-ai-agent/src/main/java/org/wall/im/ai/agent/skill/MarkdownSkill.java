package org.wall.im.ai.agent.skill;

import org.wall.im.ai.core.skill.Skill;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基于Markdown文件定义的技能实现
 * <p>
 * 通过编写MD文件即可快速新增技能，无需编写Java代码类。
 * MD文件结构：
 * <pre>
 * ---
 * name: skill-name
 * description: 技能描述
 * tools: [tool1, tool2]
 * ---
 *
 * # 技能指令
 * 在此编写技能的prompt模板，支持 {{input}} 变量占位符
 * </pre>
 * </p>
 */
public class MarkdownSkill implements Skill {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{(\\w+)\\}\\}");

    private final String name;
    private final String description;
    private final String template;
    private final List<String> tools;
    private final Map<String, Object> metadata;

    public MarkdownSkill(String name, String description, String template,
                         List<String> tools, Map<String, Object> metadata) {
        this.name = name;
        this.description = description;
        this.template = template;
        this.tools = tools;
        this.metadata = metadata;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    /**
     * 获取该技能关联的工具列表
     */
    public List<String> getTools() {
        return tools;
    }

    /**
     * 获取技能元数据
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public String execute(String input) {
        return renderTemplate(input);
    }

    @Override
    public boolean canExecute(String input) {
        return input != null && !input.isBlank();
    }

    /**
     * 渲染模板，将变量替换为实际值
     *
     * @param input 用户输入
     * @return 渲染后的模板内容
     */
    private String renderTemplate(String input) {
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String varName = matcher.group(1);
            String replacement = switch (varName) {
                case "input" -> input != null ? input : "";
                case "skillName" -> name;
                case "description" -> description;
                default -> {
                    Object value = metadata != null ? metadata.get(varName) : null;
                    yield value != null ? value.toString() : matcher.group(0);
                }
            };
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
