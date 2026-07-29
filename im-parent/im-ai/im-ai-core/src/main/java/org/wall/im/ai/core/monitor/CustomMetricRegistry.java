package org.wall.im.ai.core.monitor;

/**
 * 自定义指标注册器
 * <p>允许开发者注册和更新自定义监控指标</p>
 */
public interface CustomMetricRegistry {

    /**
     * 注册计数器
     *
     * @param name        指标名称
     * @param description 描述
     */
    void registerCounter(String name, String description);

    /**
     * 递增计数器
     *
     * @param name 指标名称
     */
    void incrementCounter(String name);

    /**
     * 注册仪表盘
     *
     * @param name        指标名称
     * @param description 描述
     */
    void registerGauge(String name, String description);

    /**
     * 设置仪表盘值
     *
     * @param name  指标名称
     * @param value 值
     */
    void setGaugeValue(String name, double value);

    /**
     * 注册定时器
     *
     * @param name        指标名称
     * @param description 描述
     */
    void registerTimer(String name, String description);

    /**
     * 记录定时
     *
     * @param name       指标名称
     * @param durationMs 耗时(毫秒)
     */
    void recordTimer(String name, long durationMs);
}
