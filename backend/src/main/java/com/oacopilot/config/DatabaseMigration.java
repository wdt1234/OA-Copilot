package com.oacopilot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class DatabaseMigration implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigration.class);
    private final JdbcTemplate jdbcTemplate;

    @Value("${ai.enabled:true}")
    private boolean aiEnabled;

    @Value("${ai.endpoint:}")
    private String aiEndpoint;

    @Value("${ai.api-key:}")
    private String aiApiKey;

    @Value("${ai.model:}")
    private String aiModel;

    @Value("${ai.timeout:300}")
    private int aiTimeout;

    public DatabaseMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        addColumnIfNotExists("data_dictionary", "is_pinned", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfNotExists("sql_history", "is_pinned", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfNotExists("dee_history", "is_pinned", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfNotExists("field_mapping_history", "is_pinned", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfNotExists("sql_history", "form_code", "TEXT");

        // 初始化 AI 配置（如果表为空）
        initAiConfig();
    }

    /**
     * 初始化 AI 配置：将 application.yml 中的配置作为第一套配置
     */
    private void initAiConfig() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM ai_config", Integer.class);
            if (count != null && count == 0 && aiApiKey != null && !aiApiKey.isEmpty()) {
                String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                jdbcTemplate.update(
                        "INSERT INTO ai_config (name, endpoint, api_key, model, timeout, is_active, create_time, update_time) " +
                                "VALUES (?, ?, ?, ?, ?, 1, ?, ?)",
                        "默认配置", aiEndpoint, aiApiKey, aiModel, aiTimeout, now, now
                );
                log.info("已初始化 AI 配置：{}", aiModel);
            }
        } catch (Exception e) {
            log.warn("初始化 AI 配置失败: {}", e.getMessage());
        }
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
