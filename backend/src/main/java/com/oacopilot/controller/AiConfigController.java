package com.oacopilot.controller;

import com.oacopilot.model.AiConfig;
import com.oacopilot.service.AiConfigService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai-configs")
public class AiConfigController {

    private final AiConfigService aiConfigService;

    public AiConfigController(AiConfigService aiConfigService) {
        this.aiConfigService = aiConfigService;
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
}
