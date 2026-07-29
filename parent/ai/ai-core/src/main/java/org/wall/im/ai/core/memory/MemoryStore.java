package org.wall.im.ai.core.memory;

import java.util.List;

/**
 * 记忆存储抽象接口
 * <p>支持短期记忆和长期记忆的统一存储抽象</p>
 */
public interface MemoryStore {

    /**
     * 存储记忆条目
     *
     * @param key   记忆键
     * @param entry 记忆条目
     */
    void store(String key, MemoryEntry entry);

    /**
     * 批量存储
     *
     * @param key     记忆键
     * @param entries 记忆条目列表
     */
    void storeAll(String key, List<MemoryEntry> entries);

    /**
     * 获取记忆条目列表
     *
     * @param key 记忆键
     * @return 记忆条目列表
     */
    List<MemoryEntry> retrieve(String key);

    /**
     * 获取最近的N条记忆
     *
     * @param key   记忆键
     * @param count 条数
     * @return 记忆条目列表
     */
    List<MemoryEntry> retrieveRecent(String key, int count);

    /**
     * 搜索记忆
     *
     * @param key   记忆键
     * @param query 搜索关键词
     * @return 匹配的记忆条目
     */
    List<MemoryEntry> search(String key, String query);

    /**
     * 删除指定记忆
     *
     * @param key 记忆键
     */
    void clear(String key);

    /**
     * 获取存储类型标识
     */
    String getStoreType();
}
