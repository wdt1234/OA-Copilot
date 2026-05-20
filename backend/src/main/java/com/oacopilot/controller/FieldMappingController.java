package com.oacopilot.controller;

import com.oacopilot.mapper.FieldMappingHistoryMapper;
import com.oacopilot.model.FieldMappingHistory;
import com.oacopilot.service.AiService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/field-mapping")
public class FieldMappingController {

    private final FieldMappingHistoryMapper fieldMappingHistoryMapper;
    private final AiService aiService;

    private static final Map<String, String> MOCK_MAPPINGS = Map.of(
            "formmain_1001", """
                    {
                      "formId": "formmain_1001",
                      "formName": "请假申请单",
                      "fields": [
                        { "source": "field0001", "target": "applicant", "label": "申请人", "type": "string" },
                        { "source": "field0002", "target": "department", "label": "部门", "type": "string" },
                        { "source": "field0003", "target": "startDate", "label": "开始日期", "type": "date" },
                        { "source": "field0004", "target": "endDate", "label": "结束日期", "type": "date" },
                        { "source": "field0005", "target": "days", "label": "请假天数", "type": "number" },
                        { "source": "field0006", "target": "reason", "label": "请假事由", "type": "string" }
                      ]
                    }""",
            "formmain_2002", """
                    {
                      "formId": "formmain_2002",
                      "formName": "报销申请单",
                      "fields": [
                        { "source": "field0001", "target": "billNo", "label": "单据编号", "type": "string" },
                        { "source": "field0002", "target": "applicant", "label": "申请人", "type": "string" },
                        { "source": "field0003", "target": "expenseType", "label": "费用类型", "type": "string" },
                        { "source": "field0004", "target": "amount", "label": "报销金额", "type": "number" },
                        { "source": "field0005", "target": "invoiceNo", "label": "发票号", "type": "string" },
                        { "source": "field0006", "target": "applyDate", "label": "申请日期", "type": "date" }
                      ]
                    }""",
            "formmain_2003", """
                    {
                      "formId": "formmain_2003",
                      "formName": "采购申请单",
                      "fields": [
                        { "source": "field0001", "target": "billNo", "label": "单据编号", "type": "string" },
                        { "source": "field0002", "target": "applicant", "label": "申请人", "type": "string" },
                        { "source": "field0003", "target": "applyDate", "label": "申请日期", "type": "date" },
                        { "source": "field0004", "target": "amount", "label": "申请金额", "type": "number" },
                        { "source": "field0005", "target": "status", "label": "审批状态", "type": "string" },
                        { "source": "field0006", "target": "supplier", "label": "供应商", "type": "string" }
                      ]
                    }"""
    );

    private static final String DEFAULT_MAPPING = """
            {
              "formId": "unknown",
              "formName": "未识别表单",
              "fields": [
                { "source": "field0001", "target": "field01", "label": "字段1", "type": "string" },
                { "source": "field0002", "target": "field02", "label": "字段2", "type": "string" },
                { "source": "field0003", "target": "field03", "label": "字段3", "type": "string" }
              ],
              "note": "未识别的表单 ID，请手动确认字段映射"
            }""";

    public FieldMappingController(FieldMappingHistoryMapper fieldMappingHistoryMapper, AiService aiService) {
        this.fieldMappingHistoryMapper = fieldMappingHistoryMapper;
        this.aiService = aiService;
    }

    @GetMapping("/history")
    public List<FieldMappingHistory> history(@RequestParam(defaultValue = "20") int limit) {
        return fieldMappingHistoryMapper.findRecent(limit);
    }

    @PutMapping("/history/{id}/pin")
    public Map<String, Object> togglePin(@PathVariable Long id) {
        FieldMappingHistory record = fieldMappingHistoryMapper.findRecent(100).stream()
                .filter(h -> h.getId().equals(id))
                .findFirst()
                .orElse(null);
        if (record == null) {
            return Map.of("error", "记录不存在");
        }
        boolean newPinned = !record.isPinned();
        fieldMappingHistoryMapper.updatePinned(id, newPinned);
        return Map.of("message", newPinned ? "已置顶" : "已取消置顶");
    }

    @DeleteMapping("/history/batch")
    public Map<String, Object> deleteHistoryBatch(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.getOrDefault("ids", List.of());
        if (ids.isEmpty()) {
            return Map.of("deleted", 0);
        }
        int deleted = fieldMappingHistoryMapper.deleteByIds(ids);
        return Map.of("deleted", deleted);
    }

    @DeleteMapping("/history/{id}")
    public Map<String, Object> deleteHistory(@PathVariable Long id) {
        fieldMappingHistoryMapper.deleteById(id);
        return Map.of("message", "已删除");
    }

    @PostMapping("/generate")
    public Map<String, Object> generate(@RequestBody Map<String, String> body) {
        String formId = body.getOrDefault("formId", "").trim();
        String inputFields = body.getOrDefault("inputFields", "");

        // 优先调用 AI，失败则 fallback 到 mock
        String result = aiService.generateFieldMapping(formId, inputFields);
        if (result == null) {
            result = DEFAULT_MAPPING;
            for (Map.Entry<String, String> entry : MOCK_MAPPINGS.entrySet()) {
                if (formId.contains(entry.getKey()) || inputFields.contains(entry.getKey())) {
                    result = entry.getValue();
                    break;
                }
            }
        }

        // 保存历史
        FieldMappingHistory record = new FieldMappingHistory();
        record.setFormId(formId);
        record.setInputFields(inputFields);
        record.setResultJson(result);
        record.setCreateTime(LocalDateTime.now());
        fieldMappingHistoryMapper.insert(record);

        return Map.of("formId", formId, "result", result, "id", record.getId());
    }
}
