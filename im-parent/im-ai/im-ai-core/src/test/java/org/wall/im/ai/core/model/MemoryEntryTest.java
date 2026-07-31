package org.wall.im.ai.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.wall.im.ai.core.memory.MemoryEntry;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MemoryEntry模型单元测试
 */
@DisplayName("MemoryEntry模型测试")
class MemoryEntryTest {

	@Test
	@DisplayName("默认构造 - 应设置当前时间和空元数据")
	void defaultConstructor_shouldSetTimestampAndEmptyMetadata() {
		MemoryEntry entry = new MemoryEntry();
		assertNotNull(entry.getCreatedAt());
		assertNotNull(entry.getMetadata());
		assertTrue(entry.getMetadata().isEmpty());
	}

	@Test
	@DisplayName("参数构造 - 应正确设置id、content、role和默认importance")
	void parameterizedConstructor_shouldSetFieldsWithDefaultImportance() {
		MemoryEntry entry = new MemoryEntry("id-1", "Hello world", "user");

		assertEquals("id-1", entry.getId());
		assertEquals("Hello world", entry.getContent());
		assertEquals("user", entry.getRole());
		assertEquals(0.5, entry.getImportance());
		assertNotNull(entry.getCreatedAt());
	}

	@Test
	@DisplayName("importance可以自定义设置")
	void importance_shouldBeSettable() {
		MemoryEntry entry = new MemoryEntry();
		entry.setImportance(0.9);
		assertEquals(0.9, entry.getImportance());
	}

}
