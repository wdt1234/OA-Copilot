package com.oacopilot.controller;

import com.oacopilot.service.SettingsService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    /**
     * 获取系统设置
     */
    @GetMapping
    public Map<String, Object> getSettings() {
        return settingsService.getSettings();
    }

    /**
     * 更新 AI 设置
     */
    @PutMapping("/ai")
    public Map<String, Object> updateAiSettings(@RequestBody Map<String, Object> settings) {
        return settingsService.updateAiSettings(settings);
    }
}
