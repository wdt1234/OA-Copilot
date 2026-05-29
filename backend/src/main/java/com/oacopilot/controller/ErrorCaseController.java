package com.oacopilot.controller;

import com.oacopilot.model.ErrorCase;
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
    public List<ErrorCase> getAllErrorCases() {
        return errorCaseService.getAllErrorCases();
    }

    /**
     * 按类别获取错误案例
     */
    @GetMapping("/category/{category}")
    public List<ErrorCase> getErrorCasesByCategory(@PathVariable String category) {
        return errorCaseService.getErrorCasesByCategory(category);
    }

    /**
     * 搜索错误案例
     */
    @GetMapping("/search")
    public List<ErrorCase> searchErrorCases(
            @RequestParam(required = false) String category,
            @RequestParam String keyword) {
        return errorCaseService.searchErrorCases(category, keyword);
    }

    /**
     * 根据ID获取错误案例
     */
    @GetMapping("/{id}")
    public ErrorCase getErrorCaseById(@PathVariable Long id) {
        return errorCaseService.getErrorCaseById(id);
    }

    /**
     * 新增错误案例
     */
    @PostMapping
    public ErrorCase addErrorCase(@RequestBody ErrorCase errorCase) {
        return errorCaseService.addErrorCase(errorCase);
    }

    /**
     * 更新错误案例
     */
    @PutMapping("/{id}")
    public ErrorCase updateErrorCase(@PathVariable Long id, @RequestBody ErrorCase errorCase) {
        errorCase.setId(id);
        return errorCaseService.updateErrorCase(errorCase);
    }

    /**
     * 更新置顶状态
     */
    @PutMapping("/{id}/pinned")
    public void updatePinned(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        errorCaseService.updatePinned(id, body.get("isPinned"));
    }

    /**
     * 删除错误案例
     */
    @DeleteMapping("/{id}")
    public boolean deleteErrorCase(@PathVariable Long id) {
        return errorCaseService.deleteErrorCase(id);
    }

    /**
     * 批量删除错误案例
     */
    @DeleteMapping("/batch")
    public boolean deleteErrorCases(@RequestBody List<Long> ids) {
        return errorCaseService.deleteErrorCases(ids);
    }
}
