package org.wall.im.admin.agent.skill;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wall.im.ai.core.skill.Skill;

/**
 * 投资分析技能
 * 负责分析市场行情、技术面/基本面数据，输出投资分析报告
 */
public class InvestmentAnalysisSkill implements Skill {

    private static final Logger log = LoggerFactory.getLogger(InvestmentAnalysisSkill.class);

    @Override
    public String getName() {
        return "investment-analysis-skill";
    }

    @Override
    public String getDescription() {
        return "投资分析技能：对指定标的进行技术面和基本面分析，输出投资建议报告";
    }

    @Override
    public String execute(String input) {
        log.info("执行投资分析，输入: {}", input);
        // 实际实现中应调用行情数据接口、技术指标计算等
        return String.format(
                "【投资分析报告】\n" +
                "分析标的: %s\n" +
                "技术面: 短期均线多头排列，MACD金叉，RSI处于中性区间\n" +
                "基本面: 市盈率合理，营收增速稳定，行业景气度上行\n" +
                "综合评级: 推荐关注\n" +
                "建议操作: 逢低分批建仓，止损位设在近期低点下方3%%",
                input
        );
    }

    @Override
    public boolean canExecute(String input) {
        return input != null && !input.isBlank();
    }
}
