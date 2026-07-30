# JdbcMemoryStore 详解

← [返回索引](../README.md)

源码：[JdbcMemoryStore.java](file:///d:/IdeaProject/InvestmentMarket/im-parent/im-ai/im-ai-memory/src/main/java/org/wall/im/ai/memory/store/JdbcMemoryStore.java)

**用途**：将记忆持久化到关系型数据库，适用于长期记忆与需要跨进程留存的场景。依赖 Spring 的 `JdbcTemplate`。

**表结构**：表名固定为 `ai_memory`，构造时会自动执行 `CREATE TABLE IF NOT EXISTS`（`initTable()`）。建表 SQL 如下：

```sql
CREATE TABLE IF NOT EXISTS ai_memory (
    id VARCHAR(64) PRIMARY KEY,
    memory_key VARCHAR(256) NOT NULL,
    content TEXT,
    role VARCHAR(32),
    importance DOUBLE DEFAULT 0.5,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    metadata TEXT
)
```

**主要 SQL**：

- 插入（`INSERT_SQL`）：写入 `id, memory_key, content, role, importance, created_at`；若 `entry.getId()` 为空，则自动生成 `UUID`。
- 全量查询（`SELECT_SQL`）：`WHERE memory_key = ? ORDER BY created_at ASC`，按时间正序返回。
- 最近 N 条（`SELECT_RECENT_SQL`）：子查询按 `created_at DESC LIMIT ?` 取最近，再外层 `ORDER BY created_at ASC` 复原正序。
- 搜索（`search`）：动态 SQL `content LIKE '%query%'`，按 `created_at ASC` 排序。
- 删除（`DELETE_SQL`）：`DELETE FROM ai_memory WHERE memory_key = ?`。
- 容量裁剪（`TRIM_SQL`）：每次 `store` 之后执行，删除该 `memory_key` 下不在"最近 `maxEntries` 条"之内的旧记录。

**配置数据源**：

- 构造方法为 `JdbcMemoryStore(JdbcTemplate jdbcTemplate, int maxEntries)`；
- 应用需自行配置 `DataSource` 与 `JdbcTemplate` Bean（通常由 `spring-boot-starter-jdbc` + `application.yml` 中 `spring.datasource.*` 提供）；
- `getStoreType()` 返回 `"db"`。

> 说明：建表语句中定义了 `metadata` 列，但当前 `INSERT_SQL` / `SELECT_SQL` / `rowMapper` 并未读写该字段，`MemoryEntry.metadata` 暂不落库。如需保存元数据，需扩展 SQL 与映射逻辑。