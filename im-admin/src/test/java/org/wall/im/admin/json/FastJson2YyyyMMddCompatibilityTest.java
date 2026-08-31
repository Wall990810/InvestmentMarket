package org.wall.im.admin.json;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.annotation.JSONField;
import com.alibaba.fastjson2.reader.YyyyMMddCompatibleLocalDateReader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * yyyyMMdd 数值兼容解析验证
 * <p>
 * 说明：Date 字段与"直接类型解析"场景由全局注册覆盖；
 * bean 中的 LocalDate 字段因 fastjson2 的 {@code FieldReaderLocalDate} 硬编码
 * 不读取全局注册表，需配合字段级 {@code @JSONField(deserializeUsing)} 才能生效（见下方用例）。
 * 序列化沿用 fastjson2 原生行为，不做覆盖。
 * </p>
 */
class FastJson2YyyyMMddCompatibilityTest {

    public static class DateBean {
        public Date date;
    }

    public static class LocalDateBean {
        public LocalDate date;
    }

    public static class LocalDateFieldBean {
        @JSONField(deserializeUsing = YyyyMMddCompatibleLocalDateReader.class)
        public LocalDate date;
    }

    @BeforeAll
    static void setup() {
        FastJson2YyyyMMddCompatibility.register();
    }

    @Test
    @DisplayName("Date：数值 20240831 解析为 2024-08-31 00:00")
    void dateNumberParsedAsYyyyMMdd() {
        DateBean bean = JSON.parseObject("{\"date\":20240831}", DateBean.class);
        Date expected = Date.from(
                LocalDate.of(2024, 8, 31).atStartOfDay(ZoneId.systemDefault()).toInstant());
        assertEquals(expected, bean.date);
    }

    @Test
    @DisplayName("LocalDate：数值 20240831 解析为 2024-08-31（直接类型解析，全局注册覆盖）")
    void localDateNumberParsedAsYyyyMMdd() {
        assertEquals(LocalDate.of(2024, 8, 31), JSON.parseObject("20240831", LocalDate.class));
    }

    @Test
    @DisplayName("LocalDate bean 字段：@JSONField(deserializeUsing) 兼容数值 yyyyMMdd")
    void localDateBeanFieldWithDeserializeUsing() {
        LocalDateFieldBean bean = JSON.parseObject("{\"date\":20240831}", LocalDateFieldBean.class);
        assertEquals(LocalDate.of(2024, 8, 31), bean.date);
    }

    @Test
    @DisplayName("Date：13位毫秒时间戳保持默认 epoch-millis 语义")
    void dateMillisNumberStillWorks() {
        long millis = 1725062400000L;
        DateBean bean = JSON.parseObject("{\"date\":" + millis + "}", DateBean.class);
        assertEquals(new Date(millis), bean.date);
    }

    @Test
    @DisplayName("LocalDate：13位毫秒时间戳保持默认 epoch-millis 语义")
    void localDateMillisNumberStillWorks() {
        long millis = 1725062400000L;
        LocalDateBean bean = JSON.parseObject("{\"date\":" + millis + "}", LocalDateBean.class);
        LocalDate expected = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate();
        assertEquals(expected, bean.date);
    }

    @Test
    @DisplayName("字符串 \"2024-08-31\" 保持默认解析")
    void stringDateStillWorks() {
        DateBean dateBean = JSON.parseObject("{\"date\":\"2024-08-31\"}", DateBean.class);
        assertEquals(Date.from(LocalDate.of(2024, 8, 31)
                .atStartOfDay(ZoneId.systemDefault()).toInstant()), dateBean.date);

        LocalDateBean localDateBean = JSON.parseObject("{\"date\":\"2024-08-31\"}", LocalDateBean.class);
        assertEquals(LocalDate.of(2024, 8, 31), localDateBean.date);
    }

    @Test
    @DisplayName("字符串 \"20240831\" 保持默认解析")
    void stringYyyyMMddStillWorks() {
        LocalDateBean bean = JSON.parseObject("{\"date\":\"20240831\"}", LocalDateBean.class);
        assertEquals(LocalDate.of(2024, 8, 31), bean.date);
    }

    @Test
    @DisplayName("null 解析为 null")
    void nullValueParsedAsNull() {
        assertNull(JSON.parseObject("{\"date\":null}", DateBean.class).date);
        assertNull(JSON.parseObject("{\"date\":null}", LocalDateBean.class).date);
    }

    @Test
    @DisplayName("非法日期数值（如 20241332）回退默认 epoch-millis 语义")
    void invalidYyyyMMddFallsBackToMillis() {
        DateBean dateBean = JSON.parseObject("{\"date\":20241332}", DateBean.class);
        assertEquals(new Date(20241332L), dateBean.date);

        LocalDateBean localDateBean = JSON.parseObject("{\"date\":20241332}", LocalDateBean.class);
        assertEquals(Instant.ofEpochMilli(20241332L)
                .atZone(ZoneId.systemDefault()).toLocalDate(), localDateBean.date);
    }
}
