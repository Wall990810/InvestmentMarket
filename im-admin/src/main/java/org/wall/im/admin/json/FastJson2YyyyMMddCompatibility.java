package org.wall.im.admin.json;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.reader.YyyyMMddCompatibleLocalDateReader;

import java.time.LocalDate;
import java.util.Date;

/**
 * fastjson2 日期兼容注册器：将 yyyyMMdd 数值兼容的 Reader 注册到全局 Provider
 * <p>
 * 注册后全局生效（覆盖 fastjson2 默认实现），影响所有通过 fastjson2 API 进行的
 * {@link Date}、{@link LocalDate} 由 JSON 到日期的解析：
 * <ul>
 *     <li>反序列化：{@code {"date":20240831}} → 2024年8月31日（Date/LocalDate）</li>
 * </ul>
 * </p>
 * <p>
 * 覆盖范围说明：
 * <ul>
 *     <li>{@link Date}：全局生效，含 bean 字段（fastjson2 反射 creator 的 getInitReader
 *         会检测用户自定义 Date reader 并优先使用）</li>
 *     <li>{@link LocalDate} 直接类型解析（{@code JSON.parseObject("20240831", LocalDate.class)} 等）：全局生效</li>
 *     <li>bean 字段为 {@link LocalDate}：fastjson2 的 {@code FieldReaderLocalDate} 硬编码内置实现、
 *         不读取全局注册表（官方扩展缺口），需在字段上配合
 *         {@code @JSONField(deserializeUsing = YyyyMMddCompatibleLocalDateReader.class)} 生效</li>
 * </ul>
 * </p>
 * <p>序列化沿用 fastjson2 原生行为（LocalDate 默认输出 "yyyy-MM-dd" 字符串），不做覆盖。</p>
 * <p>注册幂等（重复注册仅覆盖，无副作用）。应用通过
 * {@code org.wall.im.admin.config.FastJson2CompatibilityConfig} 在启动时完成注册。</p>
 */
public final class FastJson2YyyyMMddCompatibility {

    private FastJson2YyyyMMddCompatibility() {
    }

    /**
     * 注册全局兼容实现（幂等）
     */
    public static void register() {
        JSON.register(Date.class, YyyyMMddCompatibleDateReader.INSTANCE);
        JSON.register(LocalDate.class, YyyyMMddCompatibleLocalDateReader.INSTANCE);
    }
}
