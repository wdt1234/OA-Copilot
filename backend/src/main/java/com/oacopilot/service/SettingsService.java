package com.oacopilot.service;

import com.oacopilot.config.AiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

        return settings;
    }

    /**
     * 更新 AI 设置（实时生效，无需重启）
     */
    public Map<String, Object> updateAiSettings(Map<String, Object> newSettings) {
        try {
            // 1. 实时更新内存中的配置（立即生效）
            if (newSettings.containsKey("enabled")) {
                aiProperties.setEnabled(Boolean.parseBoolean(newSettings.get("enabled").toString()));
            }
            if (newSettings.containsKey("endpoint")) {
                aiProperties.setEndpoint(newSettings.get("endpoint").toString());
            }
            if (newSettings.containsKey("apiKey")) {
                String apiKey = newSettings.get("apiKey").toString();
                if (!apiKey.contains("****")) {
                    aiProperties.setApiKey(apiKey);
                }
            }
            if (newSettings.containsKey("model")) {
                aiProperties.setModel(newSettings.get("model").toString());
            }
            if (newSettings.containsKey("timeout")) {
                aiProperties.setTimeout(Integer.parseInt(newSettings.get("timeout").toString()));
            }

            // 2. 同时保存到文件（持久化）
            Path configPath = Path.of(CONFIG_FILE);
            if (!Files.exists(configPath)) {
                configPath = Path.of("backend", CONFIG_FILE);
                if (!Files.exists(configPath)) {
                    return Map.of("success", true, "message", "设置已生效（内存），但配置文件保存失败");
                }
            }

            String content = Files.readString(configPath, StandardCharsets.UTF_8);

            if (newSettings.containsKey("enabled")) {
                content = updateYamlValue(content, "ai.enabled", newSettings.get("enabled").toString());
            }
            if (newSettings.containsKey("endpoint")) {
                content = updateYamlValue(content, "ai.endpoint", newSettings.get("endpoint").toString());
            }
            if (newSettings.containsKey("apiKey")) {
                String apiKey = newSettings.get("apiKey").toString();
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

            log.info("系统设置已更新（实时生效）");
            return Map.of("success", true, "message", "设置已保存并立即生效");

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
