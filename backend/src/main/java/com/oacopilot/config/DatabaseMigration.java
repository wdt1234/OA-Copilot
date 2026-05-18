package com.oacopilot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseMigration implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigration.class);
    private final JdbcTemplate jdbcTemplate;

    public DatabaseMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        addColumnIfNotExists("data_dictionary", "is_pinned", "INTEGER NOT NULL DEFAULT 0");
    }

    private void addColumnIfNotExists(String table, String column, String type) {
        try {
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
            log.info("添加列: {}.{}", table, column);
        } catch (Exception e) {
            // 列已存在，忽略
            if (!e.getMessage().contains("duplicate column")) {
                log.warn("添加列失败: {}.{} - {}", table, column, e.getMessage());
            }
        }
    }
}
