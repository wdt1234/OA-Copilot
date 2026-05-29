package com.oacopilot.controller;

import com.oacopilot.model.AiConfig;
import com.oacopilot.service.AiConfigService;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai-configs")
public class AiConfigController {

    private final AiConfigService aiConfigService;
    private final HttpClient httpClient;

    public AiConfigController(AiConfigService aiConfigService) {
        this.aiConfigService = aiConfigService;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * 获取所有配置
     */
    @GetMapping
    public List<AiConfig> getAllConfigs() {
        List<AiConfig> configs = aiConfigService.getAllConfigs();
        // 掩码 API Key
        configs.forEach(c -> c.setApiKey(aiConfigService.maskApiKey(c.getApiKey())));
        return configs;
    }

    /**
     * 获取当前激活的配置
     */
    @GetMapping("/active")
    public AiConfig getActiveConfig() {
        AiConfig config = aiConfigService.getActiveConfig();
        if (config != null) {
            config.setApiKey(aiConfigService.maskApiKey(config.getApiKey()));
        }
        return config;
    }

    /**
     * 新增配置
     */
    @PostMapping
    public AiConfig addConfig(@RequestBody AiConfig config) {
        return aiConfigService.addConfig(config);
    }

    /**
     * 更新配置
     */
    @PutMapping("/{id}")
    public AiConfig updateConfig(@PathVariable Long id, @RequestBody AiConfig config) {
        config.setId(id);
        return aiConfigService.updateConfig(config);
    }

    /**
     * 切换激活配置
     */
    @PutMapping("/{id}/activate")
    public Map<String, Object> activateConfig(@PathVariable Long id) {
        boolean success = aiConfigService.switchConfig(id);
        if (success) {
            return Map.of("success", true, "message", "配置已切换并立即生效");
        }
        return Map.of("success", false, "message", "切换失败");
    }

    /**
     * 删除配置
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteConfig(@PathVariable Long id) {
        boolean success = aiConfigService.deleteConfig(id);
        if (success) {
            return Map.of("success", true, "message", "删除成功");
        }
        return Map.of("success", false, "message", "无法删除当前激活的配置");
    }

    /**
     * 测试连接（不保存，直接测试指定的配置）
     */
    @PostMapping("/test")
    public Map<String, Object> testConnection(@RequestBody AiConfig config) {
        try {
            String requestBody = "{\"model\":\"" + config.getModel() +
                    "\",\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}],\"max_tokens\":5}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getEndpoint()))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() == 200) {
                return Map.of("success", true, "message", "连接成功！模型: " + config.getModel());
            } else {
                String detail = response.body();
                if (detail.length() > 300) detail = detail.substring(0, 300);
                return Map.of("success", false, "message", "HTTP " + response.statusCode() + ": " + detail,
                        "endpoint", config.getEndpoint(), "model", config.getModel());
            }
        } catch (java.net.ConnectException e) {
            return Map.of("success", false, "message", "无法连接到服务器: " + config.getEndpoint());
        } catch (java.net.http.HttpTimeoutException e) {
            return Map.of("success", false, "message", "连接超时，请检查网络或端点地址");
        } catch (Exception e) {
            return Map.of("success", false, "message", "连接失败: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }
}
