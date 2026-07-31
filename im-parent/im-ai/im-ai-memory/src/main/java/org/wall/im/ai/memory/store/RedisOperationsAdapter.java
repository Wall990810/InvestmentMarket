package org.wall.im.ai.memory.store;

import java.util.List;

/**
 * Redis操作适配器
 * <p>
 * 隔离对Spring Data Redis的直接依赖，便于测试和替换
 * </p>
 */
public interface RedisOperationsAdapter {

	void listRightPush(String key, String value);

	void listRightPushAll(String key, List<String> values);

	List<String> listRange(String key, long start, long end);

	void listTrim(String key, long start, long end);

	void expire(String key, long seconds);

	void delete(String key);

}
