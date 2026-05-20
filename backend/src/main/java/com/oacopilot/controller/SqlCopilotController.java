package com.oacopilot.controller;

import com.oacopilot.mapper.SqlCacheMapper;
import com.oacopilot.mapper.SqlHistoryMapper;
import com.oacopilot.mapper.SqlTaskMapper;
import com.oacopilot.model.SqlCache;
import com.oacopilot.model.SqlHistory;
import com.oacopilot.model.SqlTask;
import com.oacopilot.service.AiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/sql")
public class SqlCopilotController {

    private static final Logger log = LoggerFactory.getLogger(SqlCopilotController.class);

    private final SqlHistoryMapper sqlHistoryMapper;
    private final SqlCacheMapper sqlCacheMapper;
    private final SqlTaskMapper sqlTaskMapper;
    private final AiService aiService;

    public SqlCopilotController(SqlHistoryMapper sqlHistoryMapper, SqlCacheMapper sqlCacheMapper,
                                 SqlTaskMapper sqlTaskMapper, AiService aiService) {
        this.sqlHistoryMapper = sqlHistoryMapper;
        this.sqlCacheMapper = sqlCacheMapper;
        this.sqlTaskMapper = sqlTaskMapper;
        this.aiService = aiService;
    }

    @GetMapping("/history")
    public List<SqlHistory> history(@RequestParam(defaultValue = "20") int limit) {
        return sqlHistoryMapper.findRecent(limit);
    }

    @PutMapping("/history/{id}/pin")
    public Map<String, Object> togglePin(@PathVariable Long id) {
        // 先查询当前状态
        SqlHistory record = sqlHistoryMapper.findRecent(100).stream()
                .filter(h -> h.getId().equals(id))
                .findFirst()
                .orElse(null);
        if (record == null) {
            return Map.of("error", "记录不存在");
        }
        boolean newPinned = !record.isPinned();
        sqlHistoryMapper.updatePinned(id, newPinned);
        return Map.of("message", newPinned ? "已置顶" : "已取消置顶");
    }

    @DeleteMapping("/history/batch")
    public Map<String, Object> deleteHistoryBatch(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.getOrDefault("ids", List.of());
        if (ids.isEmpty()) {
            return Map.of("deleted", 0);
        }
        int deleted = sqlHistoryMapper.deleteByIds(ids);
        return Map.of("deleted", deleted);
    }

    @DeleteMapping("/history/{id}")
    public Map<String, Object> deleteHistory(@PathVariable Long id) {
        sqlHistoryMapper.deleteById(id);
        return Map.of("message", "已删除");
    }

    @PostMapping("/generate")
    public Map<String, Object> generate(@RequestBody Map<String, String> body) {
        String prompt = body.getOrDefault("prompt", "");
        String formCode = body.getOrDefault("formCode", null);
        // 空字符串视为 null
        if (formCode != null && formCode.trim().isEmpty()) {
            formCode = null;
        }
        log.info("收到 SQL 生成请求: [{}], formCode: [{}]", prompt, formCode);

        // 生成缓存 key
        String cacheKey = generateCacheKey(prompt, formCode);

        // 查询缓存
        SqlCache cached = sqlCacheMapper.findByKey(cacheKey);
        if (cached != null) {
            log.info("命中缓存，cacheKey: {}", cacheKey);
            return Map.of("sql", cached.getSqlResult(), "id", cached.getId(), "cached", true);
        }

        // 缓存未命中，调用 AI
        String result;
        try {
            result = aiService.generateSql(prompt, formCode);
        } catch (RuntimeException e) {
            log.error("SQL 生成失败: {}", e.getMessage());
            return Map.of("sql", "", "error", e.getMessage());
        }
        log.info("AI 生成成功，结果长度: {}", result.length());

        // 保存缓存
        SqlCache cacheRecord = new SqlCache();
        cacheRecord.setCacheKey(cacheKey);
        cacheRecord.setPrompt(prompt);
        cacheRecord.setFormCode(formCode);
        cacheRecord.setSqlResult(result);
        cacheRecord.setCreateTime(LocalDateTime.now());
        sqlCacheMapper.insert(cacheRecord);

        // 保存历史
        SqlHistory record = new SqlHistory();
        record.setPrompt(prompt);
        record.setSqlResult(result);
        record.setCreateTime(LocalDateTime.now());
        sqlHistoryMapper.insert(record);

        return Map.of("sql", result, "id", record.getId(), "cached", false);
    }

    @PostMapping("/async")
    public Map<String, Object> submitAsync(@RequestBody Map<String, String> body) {
        String prompt = body.getOrDefault("prompt", "");
        String formCode = body.getOrDefault("formCode", null);
        if (formCode != null && formCode.trim().isEmpty()) {
            formCode = null;
        }

        // 检查缓存
        String cacheKey = generateCacheKey(prompt, formCode);
        SqlCache cached = sqlCacheMapper.findByKey(cacheKey);
        if (cached != null) {
            log.info("异步请求命中缓存，直接返回");
            return Map.of("taskId", "cached", "status", "COMPLETED", "sql", cached.getSqlResult());
        }

        // 创建任务
        String taskId = UUID.randomUUID().toString().replace("-", "");
        SqlTask task = new SqlTask();
        task.setTaskId(taskId);
        task.setPrompt(prompt);
        task.setFormCode(formCode);
        task.setStatus("PENDING");
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        sqlTaskMapper.insert(task);

        log.info("创建异步任务: taskId={}, prompt=[{}], formCode=[{}]", taskId, prompt, formCode);

        // 异步执行
        executeSqlTask(taskId, prompt, formCode);

        return Map.of("taskId", taskId, "status", "PENDING");
    }

    @GetMapping("/task/{taskId}")
    public Map<String, Object> getTaskStatus(@PathVariable String taskId) {
        SqlTask task = sqlTaskMapper.findByTaskId(taskId);
        if (task == null) {
            return Map.of("error", "任务不存在");
        }

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("taskId", task.getTaskId());
        result.put("status", task.getStatus());
        result.put("createTime", task.getCreateTime());
        result.put("updateTime", task.getUpdateTime());

        if ("COMPLETED".equals(task.getStatus())) {
            result.put("sql", task.getSqlResult());
        } else if ("FAILED".equals(task.getStatus())) {
            result.put("error", task.getErrorMessage());
        }

        return result;
    }

    @DeleteMapping("/cache")
    public Map<String, Object> clearCache(@RequestParam(defaultValue = "0") int days) {
        int deleted;
        if (days <= 0) {
            deleted = sqlCacheMapper.deleteAll();
            log.info("清除所有缓存，删除 {} 条", deleted);
        } else {
            deleted = sqlCacheMapper.deleteExpired(days);
            log.info("清除 {} 天前缓存，删除 {} 条", days, deleted);
        }
        return Map.of("deleted", deleted);
    }

    @DeleteMapping("/task")
    public Map<String, Object> clearTasks(@RequestParam(defaultValue = "7") int days) {
        int deleted = sqlTaskMapper.deleteExpired(days);
        log.info("清除 {} 天前任务，删除 {} 条", days, deleted);
        return Map.of("deleted", deleted);
    }

    @Async
    public void executeSqlTask(String taskId, String prompt, String formCode) {
        log.info("开始执行异步任务: taskId={}", taskId);
        sqlTaskMapper.updateStatus(taskId, "PROCESSING", null, null);

        try {
            // 生成缓存 key
            String cacheKey = generateCacheKey(prompt, formCode);

            // 调用 AI
            String result = aiService.generateSql(prompt, formCode);
            log.info("异步任务完成: taskId={}, 结果长度: {}", taskId, result.length());

            // 保存缓存
            SqlCache cacheRecord = new SqlCache();
            cacheRecord.setCacheKey(cacheKey);
            cacheRecord.setPrompt(prompt);
            cacheRecord.setFormCode(formCode);
            cacheRecord.setSqlResult(result);
            cacheRecord.setCreateTime(LocalDateTime.now());
            sqlCacheMapper.insert(cacheRecord);

            // 保存历史
            SqlHistory record = new SqlHistory();
            record.setPrompt(prompt);
            record.setSqlResult(result);
            record.setCreateTime(LocalDateTime.now());
            sqlHistoryMapper.insert(record);

            // 更新任务状态
            sqlTaskMapper.updateStatus(taskId, "COMPLETED", result, null);

        } catch (Exception e) {
            log.error("异步任务失败: taskId={}, error={}", taskId, e.getMessage());
            sqlTaskMapper.updateStatus(taskId, "FAILED", null, e.getMessage());
        }
    }

    private String generateCacheKey(String prompt, String formCode) {
        try {
            String raw = prompt + "|" + (formCode != null ? formCode : "");
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return prompt.hashCode() + "_" + (formCode != null ? formCode.hashCode() : 0);
        }
    }
}
