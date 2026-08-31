package org.wall.im.admin.json;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.reader.ObjectReaderImplDate;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/**
 * {@link ObjectReaderImplDate} 的重写实现：兼容 yyyyMMdd 数值类型的日期解析
 * <p>
 * fastjson2 默认将 JSON 中的数值一律按 epoch-millis 处理（如 {@code 20240831} 会被解析为 1970-08-23），
 * 本实现在默认行为之上增加兼容：当数值形如合法的 {@code yyyyMMdd}（8位，如 {@code {"date":20240831}}）时，
 * 按日期语义解析为 2024年8月31日 00:00:00（系统默认时区）。
 * </p>
 * <p>
 * 兼容规则：
 * <ul>
 *     <li>8 位数值且构成合法日期（如 20240831）→ 按 yyyyMMdd 解析</li>
 *     <li>其余数值（含 13 位毫秒时间戳、非法日期如 20241332）→ 保持默认 epoch-millis 语义</li>
 *     <li>字符串（"2024-08-31"、"20240831"）与 null → 交由默认逻辑处理</li>
 * </ul>
 * 注意：1970 年代初期的毫秒时间戳恰好为 8 位时会优先按日期语义解析，这是兼容模式的固有取舍。
 * </p>
 */
public class YyyyMMddCompatibleDateReader extends ObjectReaderImplDate {

    public static final YyyyMMddCompatibleDateReader INSTANCE = new YyyyMMddCompatibleDateReader();

    public YyyyMMddCompatibleDateReader() {
        super(null, null);
    }

    @Override
    public Object readObject(JSONReader jsonReader, java.lang.reflect.Type fieldType, Object fieldName, long features) {
        if (jsonReader.isInt()) {
            long value = jsonReader.readInt64Value();
            LocalDate date = YyyyMMddDates.parse(value);
            if (date != null) {
                return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
            }
            // 非 yyyyMMdd 语义，回退默认 epoch-millis 行为（与父类 isInt 分支一致）
            if (formatUnixTime) {
                value *= 1000;
            }
            return new Date(value);
        }
        return super.readObject(jsonReader, fieldType, fieldName, features);
    }
}
