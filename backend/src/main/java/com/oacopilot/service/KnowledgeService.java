package com.oacopilot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.oacopilot.mapper.DataDictionaryMapper;
import com.oacopilot.model.DataDictionary;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class KnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeService.class);

    private final ObjectMapper objectMapper;
    private final DataDictionaryMapper dictionaryMapper;
    private final Map<String, JsonNode> dictionaryCache = new ConcurrentHashMap<>();
    private final Map<String, String> promptCache = new ConcurrentHashMap<>();
    private final Map<String, JsonNode> systemTableCache = new ConcurrentHashMap<>();

    public KnowledgeService(ObjectMapper objectMapper, DataDictionaryMapper dictionaryMapper) {
        this.objectMapper = objectMapper;
        this.dictionaryMapper = dictionaryMapper;
    }

    /**
     * 清除数据字典缓存（保存新字典后调用）
     */
    public void clearDictionaryCache() {
        dictionaryCache.clear();
        log.info("数据字典缓存已清除");
    }

    /**
     * 获取表单数据字典 JSON
     * @param formCode 表单代码，如 guazhang_apply
     * @return JSON 树，找不到返回 null
     */
    public JsonNode getFormDictionary(String formCode) {
        // 先查内存缓存
        if (dictionaryCache.containsKey(formCode)) {
            JsonNode cached = dictionaryCache.get(formCode);
            if (cached != null) return cached;
        }
        // 再查数据库
        try {
            DataDictionary dbRecord = dictionaryMapper.findByFormCode(formCode);
            if (dbRecord != null) {
                JsonNode node = objectMapper.readTree(dbRecord.getDictionaryJson());
                dictionaryCache.put(formCode, node);
                log.info("从数据库加载数据字典: {}", formCode);
                return node;
            }
        } catch (Exception e) {
            log.error("从数据库加载数据字典失败: {}", e.getMessage());
        }
        // 最后 fallback 到 classpath 文件
        return dictionaryCache.computeIfAbsent(formCode, code -> {
            try {
                // 扫描目录找 formmain_*.json
                ClassPathResource dirResource = new ClassPathResource("knowledge/data_dictionary/forms/" + code + "/");
                if (!dirResource.exists()) {
                    log.warn("数据字典目录不存在: {}", code);
                    return null;
                }
                java.io.File dir = dirResource.getFile();
                if (!dir.isDirectory()) {
                    log.warn("数据字典路径不是目录: {}", code);
                    return null;
                }
                // 找 formmain_*.json 文件
                for (java.io.File file : dir.listFiles()) {
                    if (file.getName().startsWith("formmain_") && file.getName().endsWith(".json")) {
                        try (InputStream is = new java.io.FileInputStream(file)) {
                            JsonNode node = objectMapper.readTree(is);
                            log.info("加载数据字典: {}/{}", code, file.getName());
                            return node;
                        }
                    }
                }
                log.warn("数据字典未找到: {}", code);
                return null;
            } catch (Exception e) {
                log.error("加载数据字典失败: {}", e.getMessage());
                return null;
            }
        });
    }

    /**
     * 获取 Prompt 模板
     * @param name prompt 名称（含子目录），如 sql/form_query 或 dee/workflow
     * @return prompt 文本，找不到返回 null
     */
    public String getPrompt(String name) {
        return promptCache.computeIfAbsent(name, n -> {
            String path = "knowledge/prompts/" + n + ".md";
            try {
                ClassPathResource resource = new ClassPathResource(path);
                if (resource.exists()) {
                    try (InputStream is = resource.getInputStream()) {
                        String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                        log.info("加载 Prompt: {}", path);
                        return content;
                    }
                }
                log.warn("Prompt 未找到: {}", n);
                return null;
            } catch (Exception e) {
                log.error("加载 Prompt 失败: {}", e.getMessage());
                return null;
            }
        });
    }

    /**
     * 生成带数据字典上下文的 SQL Prompt
     * @param formCode 表单代码（可选，为 null 时不加字典）
     * @return 完整的 system prompt
     */
    public String buildSqlSystemPrompt(String formCode) {
        String basePrompt = getPrompt("sql/form_query");
        if (basePrompt == null) {
            basePrompt = "你是 Oracle SQL 专家。企业环境使用致远 OA V8.1SP2。数据库为 Oracle。只返回 SQL 语句，不要解释。";
        }

        // 始终追加系统表摘要
        String systemTableSummary = buildSystemTableSummary();
        if (!systemTableSummary.isEmpty()) {
            basePrompt = basePrompt + systemTableSummary;
        }

        // 注入当前系统日期
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        String[] weekDays = {"一", "二", "三", "四", "五", "六", "日"};
        String weekDay = weekDays[now.getDayOfWeek().getValue() - 1];
        basePrompt = basePrompt + "\n\n## 当前系统日期\n\n"
                + "今天是 " + now.getYear() + "年" + now.getMonthValue() + "月" + now.getDayOfMonth() + "日"
                + "（星期" + weekDay + "）。\n"
                + "用户提到的相对日期请基于此推算。";

        if (formCode == null) {
            return basePrompt;
        }

        JsonNode dict = getFormDictionary(formCode);
        if (dict == null) {
            return basePrompt;
        }

        StringBuilder sb = new StringBuilder(basePrompt);
        sb.append("\n\n## 当前表单数据字典\n\n");
        sb.append("表单名称：").append(dict.path("formName").asText()).append("\n");
        sb.append("主表：").append(dict.path("tableName").asText()).append("\n");

        // 主表字段
        sb.append("\n### 主表字段（").append(dict.path("tableName").asText()).append("）\n\n");
        appendFieldTable(sb, dict.path("fields"));

        // 查找并注入从表
        String mainTableName = dict.path("tableName").asText("");
        java.util.List<JsonNode> sonTables = findSonTables(mainTableName);
        for (JsonNode sonDict : sonTables) {
            String sonTableName = sonDict.path("tableName").asText("");
            String sonName = sonDict.path("sonName").asText(sonTableName);
            sb.append("\n### 明细表（").append(sonTableName).append(" - ").append(sonName).append("）\n\n");
            sb.append("关联方式：`").append(mainTableName).append(".ID = ").append(sonTableName).append(".FORMMAIN_ID`\n\n");
            appendFieldTable(sb, sonDict.path("fields"));
        }

        if (!sonTables.isEmpty()) {
            sb.append("\n### 主从表关联规则\n\n");
            sb.append("- 主表与明细表通过 `FORMMAIN_ID = ID` 关联\n");
            sb.append("- 即：`FORMMAIN_XXXX.ID = FORMSON_XXXX.FORMMAIN_ID`\n");
            sb.append("- 一条主表记录对应多条明细表记录\n");
            sb.append("- 查询明细数据时必须 JOIN 明细表\n");
        }

        return sb.toString();
    }

    /**
     * 追加字段表格到 StringBuilder
     */
    private void appendFieldTable(StringBuilder sb, JsonNode fields) {
        sb.append("| 字段名 | 显示名 | 类型 | 特殊字段 | 多选 | 关联表 |\n");
        sb.append("|--------|--------|------|----------|------|--------|\n");

        for (JsonNode field : fields) {
            String fieldName = field.path("fieldName").asText();
            if ("ID".equals(fieldName) || "MAINID".equals(fieldName)) continue;

            String displayName = field.path("displayName").asText();
            String inputType = field.path("inputType").asText();
            boolean isSpecial = field.path("isSpecial").asBoolean();
            boolean isMultiSelect = field.path("isMultiSelect").asBoolean();
            String refTable = field.path("refTable").asText("—");

            sb.append("| ").append(fieldName)
              .append(" | ").append(displayName)
              .append(" | ").append(inputType)
              .append(" | ").append(isSpecial ? "是" : "否")
              .append(" | ").append(isMultiSelect ? "是" : "否")
              .append(" | ").append(refTable)
              .append(" |\n");
        }
    }

    /**
     * 查找主表对应的所有从表
     */
    private java.util.List<JsonNode> findSonTables(String mainTableName) {
        java.util.List<JsonNode> result = new java.util.ArrayList<>();
        if (mainTableName == null || mainTableName.isEmpty()) return result;

        String mainUpper = mainTableName.toUpperCase();
        java.util.List<com.oacopilot.model.DataDictionary> allRecords = dictionaryMapper.findAll();
        for (com.oacopilot.model.DataDictionary record : allRecords) {
            try {
                JsonNode node = objectMapper.readTree(record.getDictionaryJson());
                String tableType = node.path("tableType").asText("");
                String parentTable = node.path("parentTable").asText("");

                // 优先用 parentTable 字段匹配（新保存的从表都有此字段）
                if ("son".equals(tableType) && mainUpper.equals(parentTable.toUpperCase())) {
                    result.add(node);
                    continue;
                }

                // fallback: 通过表名数字部分匹配（兼容旧数据）
                String tableName = node.path("tableName").asText("");
                if (tableName.toLowerCase().startsWith("formson_")) {
                    String mainNumber = mainUpper.replaceAll("[^0-9]", "");
                    String sonNumber = tableName.toUpperCase().replaceAll("[^0-9]", "");
                    if (!mainNumber.isEmpty() && mainNumber.equals(sonNumber)) {
                        result.add(node);
                    }
                }
            } catch (Exception e) {
                // skip
            }
        }
        return result;
    }

    /**
     * 获取 DEE Prompt 模板
     * @param templateType 模板类型：workflow / token / field_mapping / handler
     * @return prompt 文本，找不到返回 null
     */
    public String getDeePrompt(String templateType) {
        return getPrompt("dee/" + templateType);
    }

    /**
     * 列出所有可用的表单代码
     */
    public java.util.List<String> listFormCodes() {
        java.util.List<String> codes = new java.util.ArrayList<>();
        // 从数据库加载
        try {
            java.util.List<DataDictionary> dbRecords = dictionaryMapper.findAll();
            for (DataDictionary d : dbRecords) {
                codes.add(d.getFormCode());
            }
        } catch (Exception e) {
            log.warn("从数据库列出表单代码失败: {}", e.getMessage());
        }
        // 从 classpath 加载
        try {
            ClassPathResource resource = new ClassPathResource("knowledge/data_dictionary/forms/");
            if (resource.exists()) {
                java.io.File dir = resource.getFile();
                if (dir.isDirectory()) {
                    for (java.io.File child : dir.listFiles()) {
                        if (child.isDirectory() && !codes.contains(child.getName())) {
                            codes.add(child.getName());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("列出表单代码失败: {}", e.getMessage());
        }
        return codes;
    }

    /**
     * 模糊搜索表单
     * @param keyword 关键词
     * @return 匹配的表单信息列表
     */
    public java.util.List<java.util.Map<String, String>> searchForms(String keyword) {
        java.util.List<java.util.Map<String, String>> results = new java.util.ArrayList<>();
        java.util.List<String> codes = listFormCodes();
        String lowerKeyword = keyword.toLowerCase();

        for (String code : codes) {
            JsonNode dict = getFormDictionary(code);
            if (dict == null) continue;

            String formName = dict.path("formName").asText("");
            String tableName = dict.path("tableName").asText("");

            // 匹配 formCode、formName、tableName
            if (code.toLowerCase().contains(lowerKeyword)
                    || formName.toLowerCase().contains(lowerKeyword)
                    || tableName.toLowerCase().contains(lowerKeyword)) {
                java.util.Map<String, String> item = new java.util.LinkedHashMap<>();
                item.put("formCode", code);
                item.put("formName", formName);
                item.put("tableName", tableName);
                results.add(item);
            }
        }
        return results;
    }

    /**
     * 获取系统表数据字典
     * @param tableName 表名（如 org_member）
     * @return JSON 树，找不到返回 null
     */
    public JsonNode getSystemTable(String tableName) {
        return systemTableCache.computeIfAbsent(tableName, name -> {
            String path = "knowledge/system_tables/" + name.toLowerCase() + ".json";
            try {
                ClassPathResource resource = new ClassPathResource(path);
                if (resource.exists()) {
                    try (InputStream is = resource.getInputStream()) {
                        JsonNode node = objectMapper.readTree(is);
                        log.info("加载系统表: {}", path);
                        return node;
                    }
                }
                log.warn("系统表未找到: {}", name);
                return null;
            } catch (Exception e) {
                log.error("加载系统表失败: {}", e.getMessage());
                return null;
            }
        });
    }

    /**
     * 加载所有系统表
     */
    public java.util.Map<String, JsonNode> getAllSystemTables() {
        String[] tableNames = {"org_member", "org_unit", "org_post", "ctp_enum_item",
                               "ctp_affair", "col_summary", "ctp_comment_all"};
        java.util.Map<String, JsonNode> tables = new java.util.LinkedHashMap<>();
        for (String name : tableNames) {
            JsonNode node = getSystemTable(name);
            if (node != null) {
                tables.put(name, node);
            }
        }
        return tables;
    }

    /**
     * 构建系统表摘要（注入 SQL Prompt）
     */
    private String buildSystemTableSummary() {
        java.util.Map<String, JsonNode> tables = getAllSystemTables();
        if (tables.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n\n## 系统特殊表参考\n\n");

        for (java.util.Map.Entry<String, JsonNode> entry : tables.entrySet()) {
            JsonNode node = entry.getValue();
            String tableName = node.path("tableName").asText();
            String description = node.path("description").asText();
            String joinRule = node.path("joinRule").asText("");
            String defaultFilter = node.path("defaultFilter").asText("");

            sb.append("### ").append(tableName).append("\n");
            sb.append("- 用途：").append(description).append("\n");
            if (!joinRule.isEmpty()) {
                sb.append("- JOIN：`").append(joinRule).append("`\n");
            }
            if (!defaultFilter.isEmpty()) {
                sb.append("- 默认过滤：`").append(defaultFilter).append("`\n");
            }

            // 核心字段
            JsonNode fields = node.path("fields");
            if (fields.isArray() && fields.size() > 0) {
                sb.append("- 核心字段：");
                int count = 0;
                for (JsonNode field : fields) {
                    if (count >= 8) {
                        sb.append("...");
                        break;
                    }
                    if (count > 0) sb.append(", ");
                    sb.append(field.path("fieldName").asText());
                    count++;
                }
                sb.append("\n");
            }

            // 关键规则
            JsonNode rules = node.path("criticalRules");
            if (rules.isArray()) {
                for (JsonNode rule : rules) {
                    sb.append("- ").append(rule.asText()).append("\n");
                }
            }

            sb.append("\n");
        }

        return sb.toString();
    }
}
