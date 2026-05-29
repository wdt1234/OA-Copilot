package com.oacopilot.service;

import com.oacopilot.config.AiProperties;
import com.oacopilot.mapper.AiConfigMapper;
import com.oacopilot.model.AiConfig;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AiConfigService {

    private final AiConfigMapper aiConfigMapper;
    private final AiProperties aiProperties;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AiConfigService(AiConfigMapper aiConfigMapper, AiProperties aiProperties) {
        this.aiConfigMapper = aiConfigMapper;
        this.aiProperties = aiProperties;
    }

    /**
     * 获取所有配置
     */
    public List<AiConfig> getAllConfigs() {
        return aiConfigMapper.findAll();
    }

    /**
     * 获取当前激活的配置
     */
    public AiConfig getActiveConfig() {
        return aiConfigMapper.findActive();
    }

    /**
     * 新增配置
     */
    public AiConfig addConfig(AiConfig config) {
        String now = LocalDateTime.now().format(FORMATTER);
        config.setCreateTime(now);
        config.setUpdateTime(now);
        if (config.getIsActive() == null) {
            config.setIsActive(0);
        }
        if (config.getTimeout() == null) {
            config.setTimeout(300);
        }
        aiConfigMapper.insert(config);
        return config;
    }

    /**
     * 更新配置
     */
    public AiConfig updateConfig(AiConfig config) {
        config.setUpdateTime(LocalDateTime.now().format(FORMATTER));
        aiConfigMapper.update(config);
        return config;
    }

    /**
     * 切换激活配置（实时生效）
     */
    public boolean switchConfig(Long id) {
        AiConfig config = aiConfigMapper.findById(id);
        if (config == null) {
            return false;
        }

        // 1. 数据库中取消所有激活
        aiConfigMapper.deactivateAll();

        // 2. 激活选中的配置
        aiConfigMapper.activate(id, LocalDateTime.now().format(FORMATTER));

        // 3. 实时更新内存中的配置
        aiProperties.setEnabled(true);
        aiProperties.setEndpoint(config.getEndpoint());
        aiProperties.setApiKey(config.getApiKey());
        aiProperties.setModel(config.getModel());
        aiProperties.setTimeout(config.getTimeout());

        return true;
    }

    /**
     * 删除配置
     */
    public boolean deleteConfig(Long id) {
        AiConfig config = aiConfigMapper.findById(id);
        if (config != null && config.getIsActive() == 1) {
            // 不能删除当前激活的配置
            return false;
        }
        return aiConfigMapper.deleteById(id) > 0;
    }

    /**
     * 掩码 API Key
     */
    public String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 10) {
            return "****";
        }
        return apiKey.substring(0, 6) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}
