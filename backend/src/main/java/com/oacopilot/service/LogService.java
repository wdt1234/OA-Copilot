package com.oacopilot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LogService {

    private static final Logger log = LoggerFactory.getLogger(LogService.class);
    private static final String LOG_DIR = "logs";
    private static final String LOG_FILE = "app.log";

    /**
     * 获取日志文件列表
     */
    public List<Map<String, Object>> listLogFiles() {
        List<Map<String, Object>> files = new ArrayList<>();
        Path logPath = Path.of(LOG_DIR);

        if (!Files.exists(logPath)) {
            return files;
        }

        try {
            Files.list(logPath)
                .filter(p -> p.toString().endsWith(".log"))
                .sorted(Comparator.reverseSorted())
                .forEach(p -> {
                    Map<String, Object> fileInfo = new LinkedHashMap<>();
                    fileInfo.put("name", p.getFileName().toString());
                    fileInfo.put("size", getSize(p.toFile()));
                    fileInfo.put("lastModified", formatDate(p.toFile().lastModified()));
                    files.add(fileInfo);
                });
        } catch (IOException e) {
            log.error("列出日志文件失败: {}", e.getMessage());
        }

        return files;
    }

    /**
     * 读取日志内容（支持分页和筛选）
     */
    public Map<String, Object> readLogs(String fileName, int page, int pageSize, String level, String keyword) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> allLines = new ArrayList<>();

        Path logPath = Path.of(LOG_DIR, fileName);
        if (!Files.exists(logPath)) {
            result.put("lines", Collections.emptyList());
            result.put("total", 0);
            result.put("page", page);
            result.put("pageSize", pageSize);
            return result;
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Files.newInputStream(logPath), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // 筛选日志级别
                if (level != null && !level.isEmpty()) {
                    if (!line.contains(level)) {
                        continue;
                    }
                }
                // 筛选关键词
                if (keyword != null && !keyword.isEmpty()) {
                    if (!line.toLowerCase().contains(keyword.toLowerCase())) {
                        continue;
                    }
                }
                allLines.add(line);
            }
        } catch (IOException e) {
            log.error("读取日志文件失败: {}", e.getMessage());
        }

        // 分页
        int total = allLines.size();
        int fromIndex = Math.min((page - 1) * pageSize, total);
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<String> pageLines = allLines.subList(fromIndex, toIndex);

        // 反序显示（最新的在前面）
        Collections.reverse(pageLines);

        result.put("lines", pageLines);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);

        return result;
    }

    /**
     * 获取当前日志文件内容（最后N行）
     */
    public List<String> getRecentLogs(int lines) {
        List<String> result = new ArrayList<>();
        Path logPath = Path.of(LOG_DIR, LOG_FILE);

        if (!Files.exists(logPath)) {
            return result;
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Files.newInputStream(logPath), StandardCharsets.UTF_8))) {
            List<String> allLines = reader.lines().collect(Collectors.toList());
            int start = Math.max(0, allLines.size() - lines);
            result = allLines.subList(start, allLines.size());
            Collections.reverse(result);
        } catch (IOException e) {
            log.error("读取日志文件失败: {}", e.getMessage());
        }

        return result;
    }

    /**
     * 清除指定日志文件
     */
    public boolean clearLogFile(String fileName) {
        try {
            Path logPath = Path.of(LOG_DIR, fileName);
            if (Files.exists(logPath)) {
                Files.writeString(logPath, "", StandardCharsets.UTF_8);
                log.info("已清除日志文件: {}", fileName);
                return true;
            }
            return false;
        } catch (IOException e) {
            log.error("清除日志文件失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 删除指定日志文件
     */
    public boolean deleteLogFile(String fileName) {
        try {
            Path logPath = Path.of(LOG_DIR, fileName);
            if (Files.exists(logPath)) {
                Files.delete(logPath);
                log.info("已删除日志文件: {}", fileName);
                return true;
            }
            return false;
        } catch (IOException e) {
            log.error("删除日志文件失败: {}", e.getMessage());
            return false;
        }
    }

    private String getSize(File file) {
        long bytes = file.length();
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    private String formatDate(long timestamp) {
        return java.time.Instant.ofEpochMilli(timestamp)
                .atZone(java.time.ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
