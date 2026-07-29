package org.wall.im.ai.memory.store;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.wall.im.ai.core.memory.MemoryEntry;
import org.wall.im.ai.core.memory.MemoryStore;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 基于数据库的记忆存储实现
 * <p>使用JDBC存储记忆条目到关系型数据库，支持持久化和查询</p>
 * <p>需要引入spring-boot-starter-jdbc依赖</p>
 */
public class JdbcMemoryStore implements MemoryStore {

    private final JdbcTemplate jdbcTemplate;
    private final int maxEntries;

    private static final String TABLE_NAME = "ai_memory";

    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS %s (
                id VARCHAR(64) PRIMARY KEY,
                memory_key VARCHAR(256) NOT NULL,
                content TEXT,
                role VARCHAR(32),
                importance DOUBLE DEFAULT 0.5,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                metadata TEXT
            )
            """.formatted(TABLE_NAME);

    private static final String INSERT_SQL = """
            INSERT INTO %s (id, memory_key, content, role, importance, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """.formatted(TABLE_NAME);

    private static final String SELECT_SQL = """
            SELECT id, memory_key, content, role, importance, created_at
            FROM %s WHERE memory_key = ? ORDER BY created_at ASC
            """.formatted(TABLE_NAME);

    private static final String SELECT_RECENT_SQL = """
            SELECT * FROM (
                SELECT id, memory_key, content, role, importance, created_at
                FROM %s WHERE memory_key = ? ORDER BY created_at DESC LIMIT ?
            ) sub ORDER BY created_at ASC
            """.formatted(TABLE_NAME);

    private static final String DELETE_SQL = """
            DELETE FROM %s WHERE memory_key = ?
            """.formatted(TABLE_NAME);

    private static final String TRIM_SQL = """
            DELETE FROM %s WHERE memory_key = ? AND id NOT IN (
                SELECT id FROM (
                    SELECT id FROM %s WHERE memory_key = ? ORDER BY created_at DESC LIMIT ?
                ) keep
            )
            """.formatted(TABLE_NAME, TABLE_NAME);

    private final RowMapper<MemoryEntry> rowMapper = (rs, rowNum) -> {
        MemoryEntry entry = new MemoryEntry();
        entry.setId(rs.getString("id"));
        entry.setContent(rs.getString("content"));
        entry.setRole(rs.getString("role"));
        entry.setImportance(rs.getDouble("importance"));
        entry.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        return entry;
    };

    public JdbcMemoryStore(JdbcTemplate jdbcTemplate, int maxEntries) {
        this.jdbcTemplate = jdbcTemplate;
        this.maxEntries = maxEntries;
        initTable();
    }

    private void initTable() {
        jdbcTemplate.execute(CREATE_TABLE_SQL);
    }

    @Override
    public void store(String key, MemoryEntry entry) {
        if (entry.getId() == null) {
            entry.setId(UUID.randomUUID().toString());
        }
        jdbcTemplate.update(INSERT_SQL,
                entry.getId(), key, entry.getContent(), entry.getRole(),
                entry.getImportance(), java.sql.Timestamp.from(entry.getCreatedAt()));
        // 裁剪超出部分
        jdbcTemplate.update(TRIM_SQL, key, key, maxEntries);
    }

    @Override
    public void storeAll(String key, List<MemoryEntry> entries) {
        entries.forEach(entry -> store(key, entry));
    }

    @Override
    public List<MemoryEntry> retrieve(String key) {
        return jdbcTemplate.query(SELECT_SQL, rowMapper, key);
    }

    @Override
    public List<MemoryEntry> retrieveRecent(String key, int count) {
        return jdbcTemplate.query(SELECT_RECENT_SQL, rowMapper, key, count);
    }

    @Override
    public List<MemoryEntry> search(String key, String query) {
        String sql = """
                SELECT id, memory_key, content, role, importance, created_at
                FROM %s WHERE memory_key = ? AND content LIKE ? ORDER BY created_at ASC
                """.formatted(TABLE_NAME);
        return jdbcTemplate.query(sql, rowMapper, key, "%" + query + "%");
    }

    @Override
    public void clear(String key) {
        jdbcTemplate.update(DELETE_SQL, key);
    }

    @Override
    public String getStoreType() {
        return "db";
    }
}
