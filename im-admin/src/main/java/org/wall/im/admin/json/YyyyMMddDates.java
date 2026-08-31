package org.wall.im.admin.json;

import java.time.DateTimeException;
import java.time.LocalDate;

/**
 * yyyyMMdd 数值解析工具
 * <p>用于将形如 {@code 20240831} 的 8 位数值解析为 {@link LocalDate}（2024年8月31日），
 * 解析失败时返回 {@code null}，由调用方回退到默认的 epoch-millis 语义。</p>
 */
public final class YyyyMMddDates {

    /** yyyyMMdd 数值下限：1000-01-01（8 位数值表示的最小合法日期） */
    private static final long MIN_YYYYMMDD_VALUE = 10000101L;
    /** yyyyMMdd 数值上限：9999-12-31（8 位数值表示的最大合法日期） */
    private static final long MAX_YYYYMMDD_VALUE = 99991231L;

    private YyyyMMddDates() {
    }

    /**
     * 尝试将数值按 yyyyMMdd 语义解析为日期
     *
     * @param value 待解析数值，如 20240831
     * @return 对应的 LocalDate；数值不在 yyyyMMdd 合法范围内时返回 null
     */
    public static LocalDate parse(long value) {
        if (value < MIN_YYYYMMDD_VALUE || value > MAX_YYYYMMDD_VALUE) {
            return null;
        }
        int year = (int) (value / 10000);
        int month = (int) (value / 100) % 100;
        int day = (int) (value % 100);
        if (month < 1 || month > 12 || day < 1 || day > 31) {
            return null;
        }
        try {
            return LocalDate.of(year, month, day);
        } catch (DateTimeException e) {
            // 非法日期（如 20240230），回退默认语义
            return null;
        }
    }
}
