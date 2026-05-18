package com.oacopilot.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.oacopilot.mapper.DataDictionaryMapper;
import com.oacopilot.model.DataDictionary;
import com.oacopilot.service.DataDictionaryParserService;
import com.oacopilot.service.KnowledgeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/dictionary")
public class DataDictionaryController {

    private static final Logger log = LoggerFactory.getLogger(DataDictionaryController.class);

    private final DataDictionaryParserService parserService;
    private final DataDictionaryMapper dictionaryMapper;
    private final KnowledgeService knowledgeService;
    private final ObjectMapper objectMapper;

    public DataDictionaryController(DataDictionaryParserService parserService,
                                     DataDictionaryMapper dictionaryMapper,
                                     KnowledgeService knowledgeService,
                                     ObjectMapper objectMapper) {
        this.parserService = parserService;
        this.dictionaryMapper = dictionaryMapper;
        this.knowledgeService = knowledgeService;
        this.objectMapper = objectMapper;
    }

    /**
     * 解析原始文本（预览，不保存）
     */
    @PostMapping("/parse")
    public Map<String, Object> parse(@RequestBody Map<String, String> body) {
        String rawText = body.get("rawText");
        if (rawText == null || rawText.trim().isEmpty()) {
            throw new RuntimeException("请输入表单数据字典文本");
        }
        Map<String, Object> parsed = parserService.parse(rawText);

        // 生成建议的 formCode
        String mainTableName = "";
        if (parsed.get("mainTable") instanceof JsonNode) {
            mainTableName = ((JsonNode) parsed.get("mainTable")).path("tableName").asText("");
        }
        parsed.put("suggestedFormCode", parserService.suggestFormCode(mainTableName));

        return parsed;
    }

    /**
     * 保存数据字典
     */
    @PostMapping("/save")
    public Map<String, Object> save(@RequestBody Map<String, String> body) {
        String rawText = body.get("rawText");
        String formCode = body.get("formCode");

        if (rawText == null || rawText.trim().isEmpty()) {
            throw new RuntimeException("请输入表单数据字典文本");
        }

        // 解析
        Map<String, Object> parsed = parserService.parse(rawText);
        String formName = (String) parsed.get("formName");

        // 获取主表名
        String mainTableName = "";
        JsonNode mainTable = (JsonNode) parsed.get("mainTable");
        if (mainTable != null) {
            mainTableName = mainTable.path("tableName").asText("");
        }

        // 如果没提供 formCode，自动生成
        if (formCode == null || formCode.trim().isEmpty()) {
            formCode = parserService.suggestFormCode(mainTableName);
        }

        // 构建标准数据字典 JSON
        String dictJson = buildStandardDictionary(parsed);

        // 检查是否已存在
        DataDictionary existing = dictionaryMapper.findByFormCode(formCode);
        LocalDateTime now = LocalDateTime.now();

        if (existing != null) {
            existing.setFormName(formName);
            existing.setTableName(mainTableName);
            existing.setDictionaryJson(dictJson);
            existing.setRawText(rawText);
            existing.setUpdateTime(now);
            dictionaryMapper.update(existing);
            log.info("更新数据字典: {}", formCode);
        } else {
            DataDictionary record = new DataDictionary();
            record.setFormCode(formCode);
            record.setFormName(formName);
            record.setTableName(mainTableName);
            record.setDictionaryJson(dictJson);
            record.setRawText(rawText);
            record.setCreateTime(now);
            record.setUpdateTime(now);
            dictionaryMapper.insert(record);
            log.info("新增数据字典: {}", formCode);
        }

        // 保存从表为独立记录
        saveSonTables(parsed, formCode, now);

        // 清除缓存，让 KnowledgeService 重新加载
        knowledgeService.clearDictionaryCache();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("formCode", formCode);
        result.put("formName", formName);
        result.put("tableName", mainTableName);
        result.put("message", existing != null ? "更新成功" : "保存成功");
        return result;
    }

    /**
     * 列出所有数据字典（置顶排最前）
     */
    @GetMapping("/list")
    public List<Map<String, Object>> list() {
        List<DataDictionary> all = dictionaryMapper.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (DataDictionary d : all) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("formCode", d.getFormCode());
            item.put("formName", d.getFormName());
            item.put("tableName", d.getTableName());
            item.put("isPinned", d.getIsPinned() != null && d.getIsPinned() == 1);
            result.add(item);
        }
        return result;
    }

    /**
     * 获取指定数据字典详情
     */
    @GetMapping("/{formCode}")
    public Map<String, Object> getDetail(@PathVariable String formCode) {
        DataDictionary record = dictionaryMapper.findByFormCode(formCode);
        if (record == null) {
            throw new RuntimeException("数据字典不存在: " + formCode);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("formCode", record.getFormCode());
        result.put("formName", record.getFormName());
        result.put("tableName", record.getTableName());
        result.put("rawText", record.getRawText());
        try {
            result.put("dictionaryJson", objectMapper.readTree(record.getDictionaryJson()));
        } catch (Exception e) {
            result.put("dictionaryJson", record.getDictionaryJson());
        }
        return result;
    }

    /**
     * 删除数据字典
     */
    @DeleteMapping("/{formCode}")
    public Map<String, String> delete(@PathVariable String formCode) {
        int deleted = dictionaryMapper.deleteByFormCode(formCode);
        knowledgeService.clearDictionaryCache();
        Map<String, String> result = new LinkedHashMap<>();
        if (deleted > 0) {
            result.put("message", "删除成功");
        } else {
            result.put("message", "数据字典不存在");
        }
        return result;
    }

    /**
     * 清空所有数据字典
     */
    @DeleteMapping("/all")
    public Map<String, String> deleteAll() {
        int count = dictionaryMapper.deleteAll();
        knowledgeService.clearDictionaryCache();
        Map<String, String> result = new LinkedHashMap<>();
        result.put("message", "已清空 " + count + " 条记录");
        return result;
    }

    /**
     * 置顶/取消置顶
     */
    @PutMapping("/{formCode}/pin")
    public Map<String, Object> togglePin(@PathVariable String formCode) {
        DataDictionary record = dictionaryMapper.findByFormCode(formCode);
        if (record == null) {
            throw new RuntimeException("数据字典不存在: " + formCode);
        }
        int newPinned = (record.getIsPinned() != null && record.getIsPinned() == 1) ? 0 : 1;
        dictionaryMapper.setPinned(formCode, newPinned);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("formCode", formCode);
        result.put("isPinned", newPinned == 1);
        result.put("message", newPinned == 1 ? "已置顶" : "已取消置顶");
        return result;
    }

    // ── 保存从表 ──

    @SuppressWarnings("unchecked")
    private void saveSonTables(Map<String, Object> parsed, String parentFormCode, LocalDateTime now) {
        List<ObjectNode> sonTables = (List<ObjectNode>) parsed.get("sonTables");
        if (sonTables == null || sonTables.isEmpty()) return;

        String mainTableName = "";
        JsonNode mainTable = (JsonNode) parsed.get("mainTable");
        if (mainTable != null) {
            mainTableName = mainTable.path("tableName").asText("");
        }

        for (ObjectNode sonNode : sonTables) {
            String sonTableName = sonNode.path("tableName").asText("");
            if (sonTableName.isEmpty()) continue;

            String sonFormCode = sonTableName.toUpperCase();
            String sonName = sonNode.path("sonName").asText(sonTableName);

            // 构建从表的完整 JSON（含 parentTable 关联信息）
            ObjectNode fullSon = objectMapper.createObjectNode();
            fullSon.put("formName", (String) parsed.get("formName") + " - " + sonName);
            fullSon.put("tableName", sonTableName.toUpperCase());
            fullSon.put("tableType", "son");
            fullSon.put("sonName", sonName);
            fullSon.put("parentTable", mainTableName.toUpperCase());
            fullSon.set("fields", sonNode.path("fields"));

            try {
                String dictJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(fullSon);

                DataDictionary existing = dictionaryMapper.findByFormCode(sonFormCode);
                if (existing != null) {
                    existing.setFormName(sonName);
                    existing.setTableName(sonTableName.toUpperCase());
                    existing.setDictionaryJson(dictJson);
                    existing.setRawText("");
                    existing.setUpdateTime(now);
                    dictionaryMapper.update(existing);
                    log.info("更新从表数据字典: {}", sonFormCode);
                } else {
                    DataDictionary record = new DataDictionary();
                    record.setFormCode(sonFormCode);
                    record.setFormName(sonName);
                    record.setTableName(sonTableName.toUpperCase());
                    record.setDictionaryJson(dictJson);
                    record.setRawText("");
                    record.setCreateTime(now);
                    record.setUpdateTime(now);
                    dictionaryMapper.insert(record);
                    log.info("新增从表数据字典: {}", sonFormCode);
                }
            } catch (Exception e) {
                log.error("保存从表数据字典失败: {}", sonFormCode, e);
            }
        }
    }

    // ── 构建标准数据字典 JSON ──

    private String buildStandardDictionary(Map<String, Object> parsed) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("formName", (String) parsed.get("formName"));

            JsonNode mainTable = (JsonNode) parsed.get("mainTable");
            root.put("tableName", mainTable.path("tableName").asText());
            root.put("tableType", "main");

            // 如果有从表，添加 sonName
            List<ObjectNode> sonTables = (List<ObjectNode>) parsed.get("sonTables");
            if (sonTables != null && !sonTables.isEmpty()) {
                root.put("sonName", sonTables.get(0).path("sonName").asText(""));
            }

            // 主表字段
            root.set("fields", mainTable.path("fields"));

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("构建数据字典 JSON 失败", e);
        }
    }
}
