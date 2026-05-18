package com.oacopilot.controller;

import com.oacopilot.config.AiProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    private final AiProperties aiProperties;

    public HealthController(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
    }

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "ok");
        result.put("app", "OA Integration Copilot");
        result.put("version", "0.0.1");

        Map<String, Object> ai = new LinkedHashMap<>();
        ai.put("enabled", aiProperties.isEnabled());
        ai.put("model", aiProperties.getModel());
        ai.put("endpoint", aiProperties.getEndpoint());
        result.put("ai", ai);

        return result;
    }
}
