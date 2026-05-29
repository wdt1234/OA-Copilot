package com.oacopilot.controller;

import com.oacopilot.service.LogService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/logs")
public class LogController {

    private final LogService logService;

    public LogController(LogService logService) {
        this.logService = logService;
    }

    /**
     * 获取日志文件列表
     */
    @GetMapping("/files")
    public List<Map<String, Object>> listLogFiles() {
        return logService.listLogFiles();
    }

    /**
     * 读取日志内容（支持分页和筛选）
     */
    @GetMapping("/read")
    public Map<String, Object> readLogs(
            @RequestParam(defaultValue = "app.log") String fileName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int pageSize,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String keyword) {
        return logService.readLogs(fileName, page, pageSize, level, keyword);
    }

    /**
     * 获取最近N行日志
     */
    @GetMapping("/recent")
    public List<String> getRecentLogs(@RequestParam(defaultValue = "100") int lines) {
        return logService.getRecentLogs(lines);
    }

    /**
     * 清除日志文件
     */
    @DeleteMapping("/clear/{fileName}")
    public Map<String, Object> clearLogFile(@PathVariable String fileName) {
        boolean success = logService.clearLogFile(fileName);
        return Map.of("success", success, "message", success ? "已清除" : "清除失败");
    }

    /**
     * 删除日志文件
     */
    @DeleteMapping("/delete/{fileName}")
    public Map<String, Object> deleteLogFile(@PathVariable String fileName) {
        boolean success = logService.deleteLogFile(fileName);
        return Map.of("success", success, "message", success ? "已删除" : "删除失败");
    }
}
