package com.oacopilot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DataDictionaryParserService {

    private static final Logger log = LoggerFactory.getLogger(DataDictionaryParserService.class);
    private final ObjectMapper objectMapper;

    // 匹配表单名称行
    private static final Pattern FORM_NAME_PATTERN = Pattern.compile("表单名称[：:]\\s*(.+)");
    // 匹配数据库表名称行
    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("数据库表名称[：:]\\s*(\\S+)");
    // 匹配表名称行（从表的显示名）
    private static final Pattern TABLE_DISPLAY_PATTERN = Pattern.compile("表名称[：:]\\s*(.+)");
    // 匹配字段行起始：field0001
    private static final Pattern FIELD_START_PATTERN = Pattern.compile("^field\\d+", Pattern.CASE_INSENSITIVE);
    // 匹配全角空格
    private static final Pattern FULL_WIDTH_SPACE = Pattern.compile("　");
    // 匹配空格分隔的字段行（无 Tab 时使用）
    // group1=field名 group2=字段类型 group3=长度 group4=显示名 group5=输入类型 group6=最终类型
    private static final Pattern FIELD_LINE_PATTERN = Pattern.compile(
            "^(field\\d+)\\s+(\\S+)\\s+(\\d+)\\s+(.+?)\\s+(选人|选多人|选部门|下拉|单选|上传附件|日期时间|日期|文本域|序号|关联文档|流程处理意见|表单自定义控件|文本)\\s+(.+)$",
            Pattern.CASE_INSENSITIVE);

    public DataDictionaryParserService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 解析 OA 表单原始文本为标准数据字典 JSON
     * @param rawText 原始文本（从 OA 表单设计器复制）
     * @return 解析结果 Map：formName, mainTable, sonTables
     */
    public Map<String, Object> parse(String rawText) {
        Map<String, Object> result = new LinkedHashMap<>();

        String[] lines = rawText.split("\\n");

        // 提取表单名称
        String formName = "";
        for (String line : lines) {
            Matcher m = FORM_NAME_PATTERN.matcher(line.trim());
            if (m.find()) {
                formName = m.group(1).trim();
                break;
            }
        }
        result.put("formName", formName);

        // 分段：主表 + 从表
        List<TableSection> sections = splitSections(lines);

        if (sections.isEmpty()) {
            throw new RuntimeException("未找到表结构信息，请检查格式");
        }

        // 主表
        TableSection mainSection = sections.get(0);
        ObjectNode mainDict = buildTableJson(mainSection, "main");
        result.put("mainTable", mainDict);

        // 从表
        List<ObjectNode> sonTables = new ArrayList<>();
        for (int i = 1; i < sections.size(); i++) {
            ObjectNode sonDict = buildTableJson(sections.get(i), "son");
            sonTables.add(sonDict);
        }
        result.put("sonTables", sonTables);

        return result;
    }

    /**
     * 将解析结果转为标准数据字典 JSON 字符串
     */
    public String toJsonString(Map<String, Object> parsed) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(parsed);
        } catch (Exception e) {
            throw new RuntimeException("JSON 序列化失败", e);
        }
    }

    /**
     * 生成 formCode 建议（直接用表名）
     */
    public String suggestFormCode(String mainTableName) {
        if (mainTableName == null) return "";
        return mainTableName.toUpperCase();
    }

    // ── 内部方法 ──

    private List<TableSection> splitSections(String[] lines) {
        List<TableSection> sections = new ArrayList<>();
        TableSection current = null;
        boolean inMainSection = false;
        boolean inSonSection = false;

        for (int i = 0; i < lines.length; i++) {
            // 全角空格→半角，然后 trim
            String line = FULL_WIDTH_SPACE.matcher(lines[i]).replaceAll(" ").trim();

            // 检测主表信息段
            if (line.contains("主表信息")) {
                inMainSection = true;
                inSonSection = false;
                current = new TableSection();
                current.displayName = "主表";
                sections.add(current);
                continue;
            }

            // 检测从表信息段
            if (line.contains("从表信息")) {
                inMainSection = false;
                inSonSection = true;
                continue;
            }

            // 从表中的"表名称"行 → 新的从表段
            if (inSonSection) {
                Matcher tableDisplayMatcher = TABLE_DISPLAY_PATTERN.matcher(line);
                if (tableDisplayMatcher.find() && !line.contains("数据库表名称")) {
                    current = new TableSection();
                    current.displayName = tableDisplayMatcher.group(1).trim();
                    sections.add(current);
                    continue;
                }
            }

            if (current == null) continue;

            // 提取数据库表名称
            Matcher tableNameMatcher = TABLE_NAME_PATTERN.matcher(line);
            if (tableNameMatcher.find()) {
                current.tableName = tableNameMatcher.group(1).trim().toLowerCase();
                continue;
            }

            // 跳过表头行
            if (line.startsWith("字段名称")) {
                continue;
            }

            // 解析字段行
            if (FIELD_START_PATTERN.matcher(line).find()) {
                FieldInfo field = parseFieldLine(line);
                if (field != null) {
                    current.fields.add(field);
                }
            }
        }

        return sections;
    }

    /**
     * 解析单行字段数据，兼容各种分隔格式
     */
    private FieldInfo parseFieldLine(String line) {
        // 策略1: 有 Tab → 先归一化 \t \t (tab+空格+tab) 为 \t，再按 Tab 分
        if (line.contains("\t")) {
            String normalized = line.replaceAll("\\t\\s*\\t", "\t");
            String[] parts = normalized.split("\t", -1);
            // 过滤空串和纯空格
            java.util.List<String> cleaned = new java.util.ArrayList<>();
            for (String p : parts) {
                String trimmed = p.trim();
                if (!trimmed.isEmpty()) {
                    cleaned.add(trimmed);
                }
            }
            // 标准 6 列：field, 类型, 长度, 显示名, 输入类型, 最终类型
            // LONGTEXT 无长度时 5 列：field, 类型, 显示名, 输入类型, 最终类型
            if (cleaned.size() >= 6) {
                return buildFieldInfo(cleaned.get(0), cleaned.get(3), cleaned.get(4), cleaned.get(5));
            } else if (cleaned.size() == 5) {
                return buildFieldInfo(cleaned.get(0), cleaned.get(2), cleaned.get(3), cleaned.get(4));
            }
        }

        // 策略2: 无 Tab → 用正则匹配字段结构
        java.util.regex.Matcher m = FIELD_LINE_PATTERN.matcher(line);
        if (m.find()) {
            return buildFieldInfo(m.group(1), m.group(4), m.group(5), m.group(6));
        }

        log.warn("无法解析字段行: {}", line);
        return null;
    }

    private FieldInfo buildFieldInfo(String fieldName, String displayName, String inputType, String dbFinalType) {
        FieldInfo field = new FieldInfo();
        field.fieldName = fieldName.trim().toLowerCase();
        field.displayName = displayName.trim();
        field.inputType = inputType.trim();
        field.dbFinalType = normalizeDbFinalType(inputType.trim(), dbFinalType.trim());
        if (field.fieldName.isEmpty() || field.displayName.isEmpty()) {
            log.warn("字段名或显示名为空: fieldName={}, displayName={}", field.fieldName, field.displayName);
            return null;
        }
        return field;
    }

    /**
     * 归一化数据库最终类型
     * - CLOB/LONGTEXT → VARCHAR2(2000 CHAR)
     * - 未知输入类型 → VARCHAR2(2000 CHAR)
     * - dbFinalType 为空 → VARCHAR2(2000 CHAR)
     */
    private String normalizeDbFinalType(String inputType, String dbFinalType) {
        // CLOB/LONGTEXT 统一处理
        if (dbFinalType != null && (dbFinalType.equalsIgnoreCase("CLOB") || dbFinalType.equalsIgnoreCase("LONGTEXT"))) {
            return "VARCHAR2(2000 CHAR)";
        }
        // dbFinalType 为空
        if (dbFinalType == null || dbFinalType.isEmpty()) {
            return "VARCHAR2(2000 CHAR)";
        }
        // 未知输入类型，dbFinalType 也按 VARCHAR2(2000 CHAR) 处理
        if (!isKnownInputType(inputType)) {
            return "VARCHAR2(2000 CHAR)";
        }
        return dbFinalType;
    }

    private boolean isKnownInputType(String inputType) {
        return "选人".equals(inputType) || "选多人".equals(inputType) || "选部门".equals(inputType)
                || "下拉".equals(inputType) || "单选".equals(inputType) || "上传附件".equals(inputType)
                || "日期".equals(inputType) || "日期时间".equals(inputType) || "文本域".equals(inputType)
                || "序号".equals(inputType) || "关联文档".equals(inputType) || "流程处理意见".equals(inputType)
                || "文本".equals(inputType);
    }

    private ObjectNode buildTableJson(TableSection section, String tableType) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("tableName", section.tableName.toUpperCase());
        node.put("tableType", tableType);
        if ("son".equals(tableType)) {
            node.put("sonName", section.displayName);
        }

        ArrayNode fieldsArray = objectMapper.createArrayNode();
        for (FieldInfo field : section.fields) {
            ObjectNode fieldNode = objectMapper.createObjectNode();
            fieldNode.put("fieldName", field.fieldName.toUpperCase());
            fieldNode.put("displayName", field.displayName);
            fieldNode.put("dbFinalType", field.dbFinalType);
            fieldNode.put("inputType", mapInputType(field.inputType));

            boolean isSpecial = isSpecialField(field.inputType);
            fieldNode.put("isSpecial", isSpecial);
            fieldNode.put("isMultiSelect", false);

            // 特殊字段自动填充 refTable
            String refTable = getRefTable(field.inputType);
            if (refTable != null) {
                fieldNode.put("refTable", refTable);
                fieldNode.put("refKey", "ID");
                fieldNode.put("refDisplay", getRefDisplay(refTable));
            } else {
                fieldNode.putNull("refTable");
                fieldNode.putNull("refKey");
                fieldNode.putNull("refDisplay");
            }

            fieldsArray.add(fieldNode);
        }
        node.set("fields", fieldsArray);

        return node;
    }

    private String mapInputType(String raw) {
        switch (raw) {
            case "选人": return "选人";
            case "选多人": return "选多人";
            case "选部门": return "选部门";
            case "下拉": return "下拉";
            case "单选": return "单选";
            case "上传附件": return "上传附件";
            case "日期": return "日期";
            case "日期时间": return "日期时间";
            case "文本域": return "文本域";
            case "序号": return "序号";
            case "关联文档": return "关联文档";
            case "流程处理意见": return "流程处理意见";
            case "表单自定义控件": return "表单自定义控件";
            default: return "文本";
        }
    }

    private boolean isSpecialField(String inputType) {
        return "选人".equals(inputType) || "选多人".equals(inputType)
                || "选部门".equals(inputType)
                || "下拉".equals(inputType) || "单选".equals(inputType)
                || "上传附件".equals(inputType);
    }

    private String getRefTable(String inputType) {
        switch (inputType) {
            case "选人":
            case "选多人": return "ORG_MEMBER";
            case "选部门": return "ORG_UNIT";
            case "下拉":
            case "单选": return "CTP_ENUM_ITEM";
            case "上传附件": return "CTP_ATTACHMENT";
            default: return null;
        }
    }

    private String getRefDisplay(String refTable) {
        switch (refTable) {
            case "ORG_MEMBER": return "NAME";
            case "ORG_UNIT": return "NAME";
            case "CTP_ENUM_ITEM": return "SHOWVALUE";
            case "CTP_ATTACHMENT": return "FILENAME";
            default: return null;
        }
    }

    // ── 内部数据结构 ──

    private static class TableSection {
        String displayName = "";
        String tableName = "";
        List<FieldInfo> fields = new ArrayList<>();
    }

    private static class FieldInfo {
        String fieldName = "";
        String displayName = "";
        String inputType = "";
        String dbFinalType = "";
    }
}
