package com.oacopilot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
                .sorted((a, b) -> b.compareTo(a))
                .forEach(p -> {
                    Map<String, Object> fileInfo = new LinkedHashMap<>();
                    fileInfo.put("name", p.getFileName().toString());
                    fileInfo.put("size", getSize(p.toFile()));
                    fileInfo.put("sizeBytes", p.toFile().length());
                    fileInfo.put("lastModified", formatDate(p.toFile().lastModified()));
                    files.add(fileInfo);
                });
        } catch (IOException e) {
            log.error("列出日志文件失败: {}", e.getMessage());
        }

        return files;
    }

    /**
     * 读取日志内容（高性能：倒序读取 + 提前终止）
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

        // 使用倒序读取，先读文件尾部（最新日志在前面）
        try (RandomAccessFile raf = new RandomAccessFile(logPath.toFile(), "r")) {
            long fileLength = raf.length();
            if (fileLength == 0) {
                result.put("lines", Collections.emptyList());
                result.put("total", 0);
                result.put("page", page);
                result.put("pageSize", pageSize);
                return result;
            }

            // 计算需要读取的位置（从文件尾部开始）
            int linesToSkip = (page - 1) * pageSize;
            int linesToRead = pageSize;

            // 倒序读取文件
            List<String> filteredLines = readLinesReverse(raf, level, keyword, linesToSkip, linesToRead);
            allLines = filteredLines;
        } catch (IOException e) {
            log.error("读取日志文件失败: {}", e.getMessage());
            // fallback 到普通读取
            allLines = readLogsFallback(logPath, level, keyword);
        }

        result.put("lines", allLines);
        result.put("total", countTotalLines(logPath, level, keyword));
        result.put("page", page);
        result.put("pageSize", pageSize);

        return result;
    }

    /**
     * 倒序读取日志行（高性能）
     */
    private List<String> readLinesReverse(RandomAccessFile raf, String level, String keyword,
                                          int skipLines, int maxLines) throws IOException {
        List<String> result = new ArrayList<>();
        long fileLength = raf.length();
        long position = fileLength;
        int skipped = 0;
        int read = 0;

        // 从文件尾部开始，逐块读取
        byte[] buffer = new byte[8192];
        StringBuilder remainder = new StringBuilder();

        while (position > 0 && read < maxLines) {
            // 计算本次读取的大小
            int readSize = (int) Math.min(buffer.length, position);
            position -= readSize;
            raf.seek(position);
            raf.read(buffer, 0, readSize);

            // 将读取的内容转为字符串并拼接前面的剩余部分
            String chunk = new String(buffer, 0, readSize, StandardCharsets.UTF_8);
            String content = remainder.toString() + chunk;
            String[] lines = content.split("\n", -1);

            // 最后一个元素可能是不完整的行，保留下次拼接
            remainder = new StringBuilder(lines[lines.length - 1]);

            // 处理完整的行（除了最后一个不完整的行）
            for (int i = lines.length - 2; i >= 0; i--) {
                String line = lines[i];
                if (line.isEmpty()) continue;

                // 筛选
                if (!matchFilter(line, level, keyword)) continue;

                // 跳过不需要的行
                if (skipped < skipLines) {
                    skipped++;
                    continue;
                }

                result.add(line);
                read++;
                if (read >= maxLines) break;
            }
        }

        // 处理剩余的第一行
        if (remainder.length() > 0 && read < maxLines) {
            String firstLine = remainder.toString();
            if (!firstLine.isEmpty() && matchFilter(firstLine, level, keyword)) {
                if (skipped >= skipLines) {
                    result.add(firstLine);
                }
            }
        }

        // 因为是倒序读取，需要反转
        Collections.reverse(result);
        return result;
    }

    /**
     * 检查行是否匹配筛选条件
     */
    private boolean matchFilter(String line, String level, String keyword) {
        if (level != null && !level.isEmpty()) {
            if (!line.contains(level)) return false;
        }
        if (keyword != null && !keyword.isEmpty()) {
            if (!line.toLowerCase().contains(keyword.toLowerCase())) return false;
        }
        return true;
    }

    /**
     * 统计总行数（支持筛选）
     */
    private int countTotalLines(Path logPath, String level, String keyword) {
        if (level == null || level.isEmpty()) {
            if (keyword == null || keyword.isEmpty()) {
                // 无筛选，直接统计行数
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(Files.newInputStream(logPath), StandardCharsets.UTF_8))) {
                    int count = 0;
                    while (reader.readLine() != null) count++;
                    return count;
                } catch (IOException e) {
                    return 0;
                }
            }
        }

        // 有筛选条件，需要逐行统计
        int count = 0;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Files.newInputStream(logPath), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (matchFilter(line, level, keyword)) count++;
            }
        } catch (IOException e) {
            return 0;
        }
        return count;
    }

    /**
     * 普通读取（fallback）
     */
    private List<String> readLogsFallback(Path logPath, String level, String keyword) {
        List<String> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Files.newInputStream(logPath), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (matchFilter(line, level, keyword)) {
                    result.add(line);
                }
            }
        } catch (IOException e) {
            log.error("读取日志文件失败: {}", e.getMessage());
        }
        return result;
    }

    /**
     * 获取最近N行日志
     */
    public List<String> getRecentLogs(int lines) {
        List<String> result = new ArrayList<>();
        Path logPath = Path.of(LOG_DIR, LOG_FILE);

        if (!Files.exists(logPath)) {
            return result;
        }

        try (RandomAccessFile raf = new RandomAccessFile(logPath.toFile(), "r")) {
            result = readLinesReverse(raf, null, null, 0, lines);
        } catch (IOException e) {
            log.error("读取日志文件失败: {}", e.getMessage());
        }

        return result;
    }

    /**
     * 获取日志统计信息
     */
    public Map<String, Object> getLogStats(String fileName) {
        Map<String, Object> stats = new LinkedHashMap<>();
        Path logPath = Path.of(LOG_DIR, fileName);

        if (!Files.exists(logPath)) {
            stats.put("total", 0);
            stats.put("errors", 0);
            stats.put("warns", 0);
            stats.put("infos", 0);
            return stats;
        }

        int total = 0, errors = 0, warns = 0, infos = 0;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Files.newInputStream(logPath), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                total++;
                if (line.contains("ERROR")) errors++;
                else if (line.contains("WARN")) warns++;
                else if (line.contains("INFO")) infos++;
            }
        } catch (IOException e) {
            log.error("统计日志失败: {}", e.getMessage());
        }

        stats.put("total", total);
        stats.put("errors", errors);
        stats.put("warns", warns);
        stats.put("infos", infos);
        stats.put("size", getSize(logPath.toFile()));

        return stats;
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

    /**
     * 导出日志文件
     */
    public byte[] exportLogFile(String fileName) {
        try {
            Path logPath = Path.of(LOG_DIR, fileName);
            if (Files.exists(logPath)) {
                return Files.readAllBytes(logPath);
            }
            return null;
        } catch (IOException e) {
            log.error("导出日志文件失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 导出筛选后的日志
     */
    public String exportFilteredLogs(String fileName, String level, String keyword) {
        StringBuilder sb = new StringBuilder();
        Path logPath = Path.of(LOG_DIR, fileName);

        if (!Files.exists(logPath)) {
            return "";
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Files.newInputStream(logPath), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (matchFilter(line, level, keyword)) {
                    sb.append(line).append("\n");
                }
            }
        } catch (IOException e) {
            log.error("导出日志失败: {}", e.getMessage());
        }

        return sb.toString();
    }

    private String getSize(File file) {
        long bytes = file.length();
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    private String formatDate(long timestamp) {
        return Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
