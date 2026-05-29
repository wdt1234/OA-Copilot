package com.oacopilot.controller;

import com.oacopilot.service.ErrorCaseService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/error-cases")
public class ErrorCaseController {

    private final ErrorCaseService errorCaseService;

    public ErrorCaseController(ErrorCaseService errorCaseService) {
        this.errorCaseService = errorCaseService;
    }

    /**
     * 获取所有错误案例
     */
    @GetMapping
    public List<Map<String, Object>> getAllErrorCases() {
        return errorCaseService.getAllErrorCases();
    }

    /**
     * 按类别获取错误案例
     */
    @GetMapping("/category/{category}")
    public List<Map<String, Object>> getErrorCasesByCategory(@PathVariable String category) {
        return errorCaseService.getErrorCasesByCategory(category);
    }

    /**
     * 搜索错误案例
     */
    @GetMapping("/search")
    public List<Map<String, Object>> searchErrorCases(@RequestParam String keyword) {
        return errorCaseService.searchErrorCases(keyword);
    }
}
