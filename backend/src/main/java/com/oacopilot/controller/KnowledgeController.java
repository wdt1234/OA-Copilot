package com.oacopilot.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.oacopilot.service.KnowledgeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    /**
     * 列出所有可用的表单数据字典
     */
    @GetMapping("/forms")
    public List<String> listForms() {
        return knowledgeService.listFormCodes();
    }

    /**
     * 模糊搜索表单数据字典
     * @param keyword 关键词（如"采购"、"付款"）
     * @return 匹配的表单列表（formCode + formName + tableName）
     */
    @GetMapping("/forms/search")
    public List<Map<String, String>> searchForms(@RequestParam String keyword) {
        return knowledgeService.searchForms(keyword);
    }

    /**
     * 获取指定表单的数据字典
     */
    @GetMapping("/forms/{formCode}")
    public JsonNode getFormDictionary(@PathVariable String formCode) {
        return knowledgeService.getFormDictionary(formCode);
    }

    /**
     * 列出所有系统表数据字典
     */
    @GetMapping("/system-tables")
    public List<Map<String, Object>> listSystemTables() {
        return knowledgeService.listSystemTables();
    }

    /**
     * 列出所有 Prompt 模板
     */
    @GetMapping("/prompts")
    public List<Map<String, Object>> listPrompts() {
        return knowledgeService.listPrompts();
    }

    /**
     * 更新 Prompt 内容
     */
    @PutMapping("/prompts/{path}")
    public Map<String, Object> updatePrompt(@PathVariable String path, @RequestBody Map<String, String> body) {
        String content = body.getOrDefault("content", "");
        boolean success = knowledgeService.updatePrompt(path, content);
        if (success) {
            return Map.of("message", "更新成功");
        } else {
            return Map.of("error", "更新失败，文件不存在");
        }
    }
}
