package com.oacopilot.service;

import com.oacopilot.config.AiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

@Service
public class SettingsService {

    private static final Logger log = LoggerFactory.getLogger(SettingsService.class);
    private static final String CONFIG_FILE = "src/main/resources/application.yml";

    private final AiProperties aiProperties;

    @Value("${server.port:8080}")
    private int serverPort;

    public SettingsService(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
    }

    /**
     * 获取系统设置
     */
    public Map<String, Object> getSettings() {
        Map<String, Object> settings = new LinkedHashMap<>();

        // AI 配置
        Map<String, Object> aiConfig = new LinkedHashMap<>();
        aiConfig.put("enabled", aiProperties.isEnabled());
        aiConfig.put("endpoint", aiProperties.getEndpoint());
        aiConfig.put("apiKey", maskApiKey(aiProperties.getApiKey()));
        aiConfig.put("model", aiProperties.getModel());
        aiConfig.put("timeout", aiProperties.getTimeout());
        settings.put("ai", aiConfig);

        // 服务器配置
        Map<String, Object> serverConfig = new LinkedHashMap<>();
        serverConfig.put("port", serverPort);
        settings.put("server", serverConfig);

        return settings;
    }

    /**
     * 更新 AI 设置
     */
    public Map<String, Object> updateAiSettings(Map<String, Object> newSettings) {
        try {
            Path configPath = Path.of(CONFIG_FILE);
            if (!Files.exists(configPath)) {
                // 尝试从 backend 目录查找
                configPath = Path.of("backend", CONFIG_FILE);
                if (!Files.exists(configPath)) {
                    return Map.of("success", false, "message", "配置文件不存在");
                }
            }

            String content = Files.readString(configPath, StandardCharsets.UTF_8);

            // 更新 AI 配置
            if (newSettings.containsKey("enabled")) {
                content = updateYamlValue(content, "ai.enabled", newSettings.get("enabled").toString());
            }
            if (newSettings.containsKey("endpoint")) {
                content = updateYamlValue(content, "ai.endpoint", newSettings.get("endpoint").toString());
            }
            if (newSettings.containsKey("apiKey")) {
                String apiKey = newSettings.get("apiKey").toString();
                // 不更新如果还是掩码
                if (!apiKey.contains("****")) {
                    content = updateYamlValue(content, "ai.api-key", apiKey);
                }
            }
            if (newSettings.containsKey("model")) {
                content = updateYamlValue(content, "ai.model", newSettings.get("model").toString());
            }
            if (newSettings.containsKey("timeout")) {
                content = updateYamlValue(content, "ai.timeout", newSettings.get("timeout").toString());
            }

            Files.writeString(configPath, content, StandardCharsets.UTF_8);

            log.info("系统设置已更新");
            return Map.of("success", true, "message", "设置已保存，重启后生效");

        } catch (Exception e) {
            log.error("更新设置失败: {}", e.getMessage());
            return Map.of("success", false, "message", "保存失败: " + e.getMessage());
        }
    }

    /**
     * 更新 YAML 配置值
     */
    private String updateYamlValue(String content, String key, String value) {
        String[] lines = content.split("\n");
        StringBuilder sb = new StringBuilder();
        String targetKey = key.substring(key.lastIndexOf('.') + 1);

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith(targetKey + ":")) {
                // 找到目标键，替换值
                int indent = 0;
                while (indent < line.length() && line.charAt(indent) == ' ') indent++;
                String indentStr = line.substring(0, Math.min(indent, line.length()));
                sb.append(indentStr).append(targetKey).append(": ").append(value).append("\n");
            } else {
                sb.append(line).append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * 掩码 API Key
     */
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 10) {
            return "****";
        }
        return apiKey.substring(0, 6) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}
