package org.wall.im.ai.agent.lifecycle;

import org.wall.im.ai.core.memory.MemoryStore;

/**
 * MemoryStore工厂接口
 */
public interface MemoryStoreFactory {

    /**
     * 根据存储类型创建MemoryStore
     *
     * @param storeType 存储类型: memory, redis, db
     * @return MemoryStore实例
     */
    MemoryStore create(String storeType);
}
