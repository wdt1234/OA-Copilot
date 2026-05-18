package com.oacopilot.controller;

import com.oacopilot.mapper.DeeHistoryMapper;
import com.oacopilot.model.DeeHistory;
import com.oacopilot.service.AiService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dee")
public class DeeController {

    private final DeeHistoryMapper deeHistoryMapper;
    private final AiService aiService;

    private static final Map<String, String> MOCK_TEMPLATES = new LinkedHashMap<>() {{
        put("workflow", """
                {
                  "workflowId": "wf_001",
                  "workflowName": "请假审批流程",
                  "steps": [
                    { "stepId": 1, "name": "发起申请", "action": "submit", "nextStep": 2 },
                    { "stepId": 2, "name": "部门审批", "action": "approve", "role": "dept_manager", "nextStep": 3 },
                    { "stepId": 3, "name": "人事备案", "action": "archive", "role": "hr", "nextStep": null }
                  ],
                  "formId": "formmain_1001"
                }""");
        put("token", """
                {
                  "url": "http://oa-server/api/auth/token",
                  "method": "POST",
                  "headers": { "Content-Type": "application/json" },
                  "body": { "appId": "oa_app_001", "appSecret": "your_app_secret", "grantType": "client_credentials" },
                  "responseMapping": { "token": "$.data.accessToken", "expiresIn": "$.data.expiresIn" }
                }""");
        put("json", """
                {
                  "formId": "formmain_2003",
                  "mappings": [
                    { "source": "field0001", "target": "billNo", "type": "string" },
                    { "source": "field0002", "target": "applicant", "type": "string" },
                    { "source": "field0003", "target": "applyDate", "type": "date" },
                    { "source": "field0004", "target": "amount", "type": "number" },
                    { "source": "field0005", "target": "status", "type": "string" }
                  ]
                }""");
        put("java", """
                public class DeeWorkflowHandler {
                    public Map<String, Object> handle(DeeContext context) {
                        Map<String, Object> result = new HashMap<>();
                        String formData = context.getFormData();
                        Map<String, Object> data = JsonUtil.parse(formData);
                        String billNo = (String) data.get("field0001");
                        Double amount = ((Number) data.get("field0004")).doubleValue();
                        ErpResponse erpResult = ErpClient.sendOrder(billNo, amount);
                        result.put("success", erpResult.isSuccess());
                        result.put("message", erpResult.getMessage());
                        return result;
                    }
                }""");
    }};

    public DeeController(DeeHistoryMapper deeHistoryMapper, AiService aiService) {
        this.deeHistoryMapper = deeHistoryMapper;
        this.aiService = aiService;
    }

    @GetMapping("/history")
    public List<DeeHistory> history(@RequestParam(defaultValue = "20") int limit) {
        return deeHistoryMapper.findRecent(limit);
    }

    @PostMapping("/generate")
    public Map<String, Object> generate(@RequestBody Map<String, String> body) {
        String templateType = body.getOrDefault("templateType", "workflow");
        String description = body.getOrDefault("description", "");

        // 优先调用 AI，失败则 fallback 到 mock
        String result = aiService.generateDeeTemplate(templateType, description);
        if (result == null) {
            result = MOCK_TEMPLATES.getOrDefault(templateType, MOCK_TEMPLATES.get("workflow"));
        }

        // 保存历史
        DeeHistory record = new DeeHistory();
        record.setTemplateType(templateType);
        record.setDescription(description);
        record.setResultJson(result);
        record.setCreateTime(LocalDateTime.now());
        deeHistoryMapper.insert(record);

        return Map.of("templateType", templateType, "result", result, "id", record.getId());
    }
}
