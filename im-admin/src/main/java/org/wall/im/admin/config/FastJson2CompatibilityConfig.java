package org.wall.im.admin.config;

import org.wall.im.admin.json.FastJson2YyyyMMddCompatibility;
import org.springframework.context.annotation.Configuration;

/**
 * fastjson2 日期兼容配置
 * <p>Spring 启动加载本配置类时，将 yyyyMMdd 数值兼容的 Reader/Writer
 * 注册到 fastjson2 全局 Provider，详见 {@link FastJson2YyyyMMddCompatibility}。</p>
 */
@Configuration(proxyBeanMethods = false)
public class FastJson2CompatibilityConfig {

    static {
        FastJson2YyyyMMddCompatibility.register();
    }
}
