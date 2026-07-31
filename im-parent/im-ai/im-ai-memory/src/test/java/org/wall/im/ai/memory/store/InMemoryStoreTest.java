package org.wall.im.ai.memory.store;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.wall.im.ai.core.memory.MemoryEntry;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * InMemoryStore单元测试
 */
@DisplayName("InMemoryStore测试")
class InMemoryStoreTest {

	private InMemoryStore store;

	@BeforeEach
	void setUp() {
		store = new InMemoryStore(100);
	}

	@Nested
	@DisplayName("存储与检索测试")
	class StoreAndRetrieveTest {

		@Test
		@DisplayName("store后retrieve应返回存储的条目")
		void storeAndRetrieve_shouldReturnStoredEntries() {
			MemoryEntry entry = new MemoryEntry("1", "Hello", "user");
			store.store("key1", entry);

			List<MemoryEntry> result = store.retrieve("key1");
			assertEquals(1, result.size());
			assertEquals("Hello", result.get(0).getContent());
		}

		@Test
		@DisplayName("retrieve不存在的key应返回空列表")
		void retrieve_nonExistentKey_shouldReturnEmpty() {
			List<MemoryEntry> result = store.retrieve("missing");
			assertNotNull(result);
			assertTrue(result.isEmpty());
		}

		@Test
		@DisplayName("多次store应累积条目")
		void multipleStores_shouldAccumulate() {
			store.store("key", new MemoryEntry("1", "msg1", "user"));
			store.store("key", new MemoryEntry("2", "msg2", "assistant"));
			store.store("key", new MemoryEntry("3", "msg3", "user"));

			List<MemoryEntry> result = store.retrieve("key");
			assertEquals(3, result.size());
		}

		@Test
		@DisplayName("不同key的数据应互相隔离")
		void differentKeys_shouldBeIsolated() {
			store.store("keyA", new MemoryEntry("1", "A-msg", "user"));
			store.store("keyB", new MemoryEntry("2", "B-msg", "user"));

			assertEquals(1, store.retrieve("keyA").size());
			assertEquals(1, store.retrieve("keyB").size());
			assertEquals("A-msg", store.retrieve("keyA").get(0).getContent());
		}

	}

	@Nested
	@DisplayName("批量存储测试")
	class StoreAllTest {

		@Test
		@DisplayName("storeAll应批量存储所有条目")
		void storeAll_shouldStoreAllEntries() {
			List<MemoryEntry> entries = List.of(new MemoryEntry("1", "msg1", "user"),
					new MemoryEntry("2", "msg2", "assistant"), new MemoryEntry("3", "msg3", "user"));

			store.storeAll("batch-key", entries);

			List<MemoryEntry> result = store.retrieve("batch-key");
			assertEquals(3, result.size());
		}

	}

	@Nested
	@DisplayName("最大条目限制测试")
	class MaxEntriesTest {

		@Test
		@DisplayName("超出maxEntries应移除最旧条目")
		void exceedMaxEntries_shouldRemoveOldest() {
			InMemoryStore smallStore = new InMemoryStore(3);

			smallStore.store("key", new MemoryEntry("1", "msg1", "user"));
			smallStore.store("key", new MemoryEntry("2", "msg2", "user"));
			smallStore.store("key", new MemoryEntry("3", "msg3", "user"));
			smallStore.store("key", new MemoryEntry("4", "msg4", "user"));

			List<MemoryEntry> result = smallStore.retrieve("key");
			assertEquals(3, result.size());
			// 最旧的msg1应被移除
			assertEquals("msg2", result.get(0).getContent());
			assertEquals("msg4", result.get(2).getContent());
		}

	}

	@Nested
	@DisplayName("最近N条检索测试")
	class RetrieveRecentTest {

		@Test
		@DisplayName("retrieveRecent应返回最近N条")
		void retrieveRecent_shouldReturnLastN() {
			for (int i = 1; i <= 10; i++) {
				store.store("key", new MemoryEntry(String.valueOf(i), "msg" + i, "user"));
			}

			List<MemoryEntry> recent = store.retrieveRecent("key", 3);
			assertEquals(3, recent.size());
			assertEquals("msg8", recent.get(0).getContent());
			assertEquals("msg10", recent.get(2).getContent());
		}

		@Test
		@DisplayName("retrieveRecent的count大于总数时应返回全部")
		void retrieveRecent_countExceedsTotal_shouldReturnAll() {
			store.store("key", new MemoryEntry("1", "msg1", "user"));

			List<MemoryEntry> recent = store.retrieveRecent("key", 100);
			assertEquals(1, recent.size());
		}

	}

	@Nested
	@DisplayName("搜索测试")
	class SearchTest {

		@Test
		@DisplayName("search应按关键词过滤")
		void search_shouldFilterByKeyword() {
			store.store("key", new MemoryEntry("1", "I love Java", "user"));
			store.store("key", new MemoryEntry("2", "Python is great", "user"));
			store.store("key", new MemoryEntry("3", "Java and Spring", "user"));

			List<MemoryEntry> result = store.search("key", "Java");
			assertEquals(2, result.size());
		}

		@Test
		@DisplayName("search无匹配结果应返回空列表")
		void search_noMatch_shouldReturnEmpty() {
			store.store("key", new MemoryEntry("1", "Hello", "user"));
			List<MemoryEntry> result = store.search("key", "xyz");
			assertTrue(result.isEmpty());
		}

	}

	@Nested
	@DisplayName("清除测试")
	class ClearTest {

		@Test
		@DisplayName("clear应删除指定key的所有数据")
		void clear_shouldRemoveAllDataForKey() {
			store.store("key", new MemoryEntry("1", "msg", "user"));
			assertFalse(store.retrieve("key").isEmpty());

			store.clear("key");
			assertTrue(store.retrieve("key").isEmpty());
		}

	}

	@Test
	@DisplayName("getStoreType应返回memory")
	void getStoreType_shouldReturnMemory() {
		assertEquals("memory", store.getStoreType());
	}

}
