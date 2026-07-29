package org.wall.im.ai.core.skill;

/**
 * 技能接口
 * <p>技能是Agent可被调用的能力单元，一个Agent可以拥有多个技能</p>
 */
public interface Skill {

    /**
     * 获取技能名称
     */
    String getName();

    /**
     * 获取技能描述
     */
    String getDescription();

    /**
     * 执行技能
     *
     * @param input 输入参数
     * @return 执行结果
     */
    String execute(String input);

    /**
     * 判断技能是否可执行
     *
     * @param input 输入参数
     * @return 是否可执行
     */
    default boolean canExecute(String input) {
        return true;
    }
}
