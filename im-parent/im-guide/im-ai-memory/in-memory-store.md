# InMemoryStore 详解

← [返回索引](../README.md)

源码：[InMemoryStore.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-memory/src/main/java/org/wall/im/ai/memory/store/InMemoryStore.java)

**用途**：将记忆条目保存在 JVM 内存中，适用于短期记忆、单元测试与本地开发联调，进程结束即丢失。

**核心特性**：

- **容量限制**：构造时传入 `maxEntries`，默认 `1000`。当单个 `key` 下的条目数超过上限时，会从列表头部（最旧条目）开始移除，实现"先进先出"裁剪。
- **线程安全**：外层使用 `ConcurrentHashMap<String, List<MemoryEntry>>` 管理各 `key` 的列表；每个 `key` 对应的 `List` 通过 `Collections.synchronizedList(new ArrayList<>())` 创建，保证多线程写入安全。
- **最近 N 条**：`retrieveRecent(key, count)` 基于内存列表尾部切片返回。
- **搜索**：`search(key, query)` 对 `content` 做包含匹配（`String.contains`），属线性扫描。
- `getStoreType()` 返回 `"memory"`。

**关键行为说明**：

- `retrieve(key)` 返回列表的**副本**（`new ArrayList<>(...)`），调用方修改不会影响内部存储；
- `retrieveRecent` 返回的是 `subList` 视图，使用时请注意勿在并发修改场景下依赖其一致性。