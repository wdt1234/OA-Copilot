package com.oacopilot.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ErrorCaseService {

    private static final Logger log = LoggerFactory.getLogger(ErrorCaseService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, List<Map<String, Object>>> cache = new ConcurrentHashMap<>();

    /**
     * 获取所有错误案例
     */
    public List<Map<String, Object>> getAllErrorCases() {
        List<Map<String, Object>> allCases = new ArrayList<>();
        allCases.addAll(loadErrorCases("sql_errors"));
        allCases.addAll(loadErrorCases("dee_errors"));
        allCases.addAll(loadErrorCases("token_errors"));
        return allCases;
    }

    /**
     * 按类别获取错误案例
     */
    public List<Map<String, Object>> getErrorCasesByCategory(String category) {
        return switch (category.toLowerCase()) {
            case "sql" -> loadErrorCases("sql_errors");
            case "dee" -> loadErrorCases("dee_errors");
            case "token" -> loadErrorCases("token_errors");
            default -> getAllErrorCases();
        };
    }

    /**
     * 搜索错误案例
     */
    public List<Map<String, Object>> searchErrorCases(String keyword) {
        String lowerKeyword = keyword.toLowerCase();
        return getAllErrorCases().stream()
                .filter(c -> matchesKeyword(c, lowerKeyword))
                .collect(Collectors.toList());
    }

    private boolean matchesKeyword(Map<String, Object> errorCase, String keyword) {
        String title = getStringValue(errorCase, "title");
        String description = getStringValue(errorCase, "description");
        String symptom = getStringValue(errorCase, "symptom");
        String solution = getStringValue(errorCase, "solution");

        return title.toLowerCase().contains(keyword)
                || description.toLowerCase().contains(keyword)
                || symptom.toLowerCase().contains(keyword)
                || solution.toLowerCase().contains(keyword);
    }

    /**
     * 加载错误案例文件
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> loadErrorCases(String fileName) {
        if (cache.containsKey(fileName)) {
            return cache.get(fileName);
        }

        try {
            ClassPathResource resource = new ClassPathResource("knowledge/error_cases/" + fileName + ".json");
            if (!resource.exists()) {
                log.warn("错误案例文件不存在: {}", fileName);
                return Collections.emptyList();
            }

            try (InputStream is = resource.getInputStream()) {
                List<Map<String, Object>> cases = objectMapper.readValue(is, new TypeReference<>() {});
                cache.put(fileName, cases);
                log.info("加载错误案例: {} - {} 条", fileName, cases.size());
                return cases;
            }
        } catch (Exception e) {
            log.error("加载错误案例失败: {}", fileName, e);
            return Collections.emptyList();
        }
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : "";
    }
}
