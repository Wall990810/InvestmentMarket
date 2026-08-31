package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONReader;
import org.wall.im.admin.json.YyyyMMddDates;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;

/**
 * LocalDate 反序列化实现：兼容 yyyyMMdd 数值类型的日期解析
 * <p>
 * 本类置于 fastjson2 原生 reader 包路径下，直接继承 {@code ObjectReaderImplLocalDate}
 * （该类为包私有、非 final，同包可继承、public 构造器可调用）。
 * 非整数输入（字符串、null、JSONB 等）全部委托父类原生逻辑，仅在整数分支增加
 * {@code yyyyMMdd} 兼容：8 位合法数值（如 {@code 20240831}）解析为 2024年8月31日，
 * 其余数值回退 epoch-millis 语义（对齐 {@link JSONReader#readLocalDate()} 的数字分支）。
 * </p>
 * <p>
 * 兼容规则：
 * <ul>
 *     <li>8 位数值且构成合法日期（如 20240831）→ 按 yyyyMMdd 解析</li>
 *     <li>其余数值（含 13 位毫秒时间戳、非法日期如 20241332）→ 保持默认 epoch-millis 语义</li>
 *     <li>字符串（"yyyy-MM-dd"、"yyyyMMdd"）、null、JSONB → 父类原生逻辑</li>
 * </ul>
 * </p>
 */
public class YyyyMMddCompatibleLocalDateReader extends ObjectReaderImplLocalDate {

    public static final YyyyMMddCompatibleLocalDateReader INSTANCE = new YyyyMMddCompatibleLocalDateReader();

    public YyyyMMddCompatibleLocalDateReader() {
        super(null, null);
    }

    @Override
    public Object readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
        // JSONB 或非整数（字符串、null 等）委托父类原生逻辑
        if (jsonReader.jsonb || !jsonReader.isInt()) {
            return super.readObject(jsonReader, fieldType, fieldName, features);
        }
        // 整数：优先 yyyyMMdd，否则回退 epoch-millis
        long value = jsonReader.readInt64Value();
        LocalDate date = YyyyMMddDates.parse(value);
        if (date != null) {
            return date;
        }
        JSONReader.Context context = jsonReader.getContext();
        long millis = "unixtime".equals(context.getDateFormat()) ? value * 1000L : value;
        return Instant.ofEpochMilli(millis).atZone(context.getZoneId()).toLocalDate();
    }
}
