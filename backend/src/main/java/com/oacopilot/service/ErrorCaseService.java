package com.oacopilot.service;

import com.oacopilot.mapper.ErrorCaseMapper;
import com.oacopilot.model.ErrorCase;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ErrorCaseService {

    private final ErrorCaseMapper errorCaseMapper;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ErrorCaseService(ErrorCaseMapper errorCaseMapper) {
        this.errorCaseMapper = errorCaseMapper;
    }

    /**
     * 获取所有错误案例
     */
    public List<ErrorCase> getAllErrorCases() {
        return errorCaseMapper.findAll();
    }

    /**
     * 按类别获取错误案例
     */
    public List<ErrorCase> getErrorCasesByCategory(String category) {
        return errorCaseMapper.findByCategory(category);
    }

    /**
     * 搜索错误案例
     */
    public List<ErrorCase> searchErrorCases(String category, String keyword) {
        String searchKeyword = "%" + keyword + "%";
        return errorCaseMapper.search(category, searchKeyword);
    }

    /**
     * 根据ID获取错误案例
     */
    public ErrorCase getErrorCaseById(Long id) {
        return errorCaseMapper.findById(id);
    }

    /**
     * 新增错误案例
     */
    public ErrorCase addErrorCase(ErrorCase errorCase) {
        String now = LocalDateTime.now().format(FORMATTER);
        errorCase.setCreateTime(now);
        errorCase.setUpdateTime(now);
        if (errorCase.getIsPinned() == null) {
            errorCase.setIsPinned(0);
        }
        errorCaseMapper.insert(errorCase);
        return errorCase;
    }

    /**
     * 更新错误案例
     */
    public ErrorCase updateErrorCase(ErrorCase errorCase) {
        errorCase.setUpdateTime(LocalDateTime.now().format(FORMATTER));
        errorCaseMapper.update(errorCase);
        return errorCase;
    }

    /**
     * 更新置顶状态
     */
    public void updatePinned(Long id, Integer isPinned) {
        errorCaseMapper.updatePinned(id, isPinned, LocalDateTime.now().format(FORMATTER));
    }

    /**
     * 删除错误案例
     */
    public boolean deleteErrorCase(Long id) {
        return errorCaseMapper.deleteById(id) > 0;
    }

    /**
     * 批量删除错误案例
     */
    public boolean deleteErrorCases(List<Long> ids) {
        return errorCaseMapper.deleteByIds(ids) > 0;
    }
}
