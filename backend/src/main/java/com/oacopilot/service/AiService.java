package com.oacopilot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oacopilot.config.AiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final KnowledgeService knowledgeService;

    public AiService(AiProperties aiProperties, ObjectMapper objectMapper, KnowledgeService knowledgeService) {
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
        this.knowledgeService = knowledgeService;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * 对话接口（支持多轮对话 + Prompt 类型）
     * @param messages 对话历史 [{role: "user", content: "..."}, ...]
     * @param promptType prompt 类型：oa/dee/空（通用）
     * @return AI 回复
     */
    public String chat(List<Map<String, String>> messages, String promptType) {
        if (!aiProperties.isEnabled()) {
            throw new RuntimeException("AI 未启用（ai.enabled=false）");
        }

        try {
            List<Map<String, String>> fullMessages = new ArrayList<>();

            // 根据 promptType 添加系统提示
            String systemPrompt = getSystemPrompt(promptType);
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                fullMessages.add(Map.of("role", "system", "content", systemPrompt));
            }

            // 添加对话历史
            fullMessages.addAll(messages);

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "model", aiProperties.getModel(),
                    "messages", fullMessages,
                    "temperature", 0.7
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(aiProperties.getEndpoint()))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("Authorization", "Bearer " + aiProperties.getApiKey())
                    .timeout(Duration.ofSeconds(aiProperties.getTimeout()))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                String detail = response.body();
                if (detail.length() > 200) detail = detail.substring(0, 200) + "...";
                log.error("AI API 返回错误: status={}, body={}", response.statusCode(), response.body());
                throw new RuntimeException("API 返回 HTTP " + response.statusCode() + ": " + detail);
            }

            JsonNode json = objectMapper.readTree(response.body());
            String content = json.path("choices").get(0).path("message").path("content").asText();
            if (content == null || content.isEmpty()) {
                throw new RuntimeException("AI 返回空内容");
            }
            return content;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 对话失败: {}", e.getMessage());
            throw new RuntimeException("AI 调用异常: " + e.getMessage());
        }
    }

    /**
     * 根据 promptType 获取系统提示
     */
    private String getSystemPrompt(String promptType) {
        if (promptType == null || promptType.isEmpty()) {
            return "你是 OA Copilot 智能助手，可以回答各种问题。";
        }
        return switch (promptType) {
            case "oa" -> loadPromptFile("knowledge/prompts/dashboard/oa_assistant.md");
            case "dee" -> loadPromptFile("knowledge/prompts/dashboard/dee_assistant.md");
            default -> "你是 OA Copilot 智能助手，可以回答各种问题。";
        };
    }

    /**
     * 加载 Prompt 文件
     */
    private String loadPromptFile(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            if (!resource.exists()) {
                log.warn("Prompt 文件不存在: {}", path);
                return null;
            }
            try (InputStream is = resource.getInputStream()) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.error("加载 Prompt 文件失败: {}", path, e);
            return null;
        }
    }

    /**
     * 调用 AI 生成内容
     * @param systemPrompt 系统提示词
     * @param userPrompt 用户输入
     * @return AI 返回内容
     * @throws RuntimeException 调用失败时抛出具体原因
     */
    public String generate(String systemPrompt, String userPrompt) {
        if (!aiProperties.isEnabled()) {
            throw new RuntimeException("AI 未启用（ai.enabled=false）");
        }

        try {
            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "model", aiProperties.getModel(),
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    ),
                    "temperature", 0.3
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(aiProperties.getEndpoint()))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("Authorization", "Bearer " + aiProperties.getApiKey())
                    .timeout(Duration.ofSeconds(aiProperties.getTimeout()))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                String detail = response.body();
                if (detail.length() > 200) detail = detail.substring(0, 200) + "...";
                log.error("AI API 返回错误: status={}, body={}", response.statusCode(), response.body());
                throw new RuntimeException("API 返回 HTTP " + response.statusCode() + ": " + detail);
            }

            JsonNode json = objectMapper.readTree(response.body());
            String content = json.path("choices").get(0).path("message").path("content").asText();
            if (content == null || content.isEmpty()) {
                log.warn("AI 返回空内容");
                throw new RuntimeException("AI 返回空内容，响应: " + response.body().substring(0, Math.min(200, response.body().length())));
            }
            log.info("AI 返回成功，原始长度: {}", content.length());
            return stripMarkdown(content);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI API 调用失败: {}", e.getMessage());
            throw new RuntimeException("AI 调用异常: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    /**
     * 去除 AI 返回中的 markdown 代码块包裹
     */
    private String stripMarkdown(String content) {
        if (content == null) return null;
        content = content.trim();
        // 匹配 ```sql ... ```, ```json ... ```, ```java ... ``` 等
        if (content.startsWith("```") && content.endsWith("```")) {
            int firstNewline = content.indexOf('\n');
            if (firstNewline > 0) {
                content = content.substring(firstNewline + 1);
            }
            content = content.substring(0, content.length() - 3).trim();
        }
        return content;
    }

    /**
     * 生成 SQL（基于知识资产）
     * @param prompt 用户输入
     * @param formCode 表单代码（如 guazhang_apply），为 null 时使用通用 Prompt
     */
    public String generateSql(String prompt, String formCode) {
        String system = knowledgeService.buildSqlSystemPrompt(formCode);
        log.info("SQL 生成 - formCode: [{}], system prompt 长度: {}", formCode, system.length());
        return generate(system, prompt);
    }

    /**
     * 生成 SQL（兼容旧调用）
     */
    public String generateSql(String prompt) {
        return generateSql(prompt, null);
    }

    /**
     * 生成 DEE 模板
     */
    public String generateDeeTemplate(String templateType, String description) {
        // 优先从知识资产加载，失败则回退硬编码
        String system = knowledgeService.getDeePrompt(templateType);
        if (system == null) {
            system = switch (templateType) {
            case "workflow" -> """
                    你是 DEE 集成平台专家。
                    请生成 workflow 流程节点定义 JSON。
                    包含 workflowId, workflowName, steps 数组。
                    只返回 JSON，不要解释。""";
            case "token" -> """
                    你是 DEE 集成平台专家。
                    请生成 token 认证接口调用配置 JSON。
                    包含 url, method, headers, body, responseMapping。
                    只返回 JSON，不要解释。""";
            case "json" -> """
                    你是 DEE 集成平台专家。
                    请生成字段映射模板 JSON。
                    包含 formId, mappings 数组（source/target/type）。
                    只返回 JSON，不要解释。""";
            case "java" -> """
                    你是 DEE 集成平台专家。
                    请生成 DEE 工作流回调 Handler Java 代码。
                    包含 handle 方法，接收 DeeContext，返回 Map。
                    只返回 Java 代码，不要解释。""";
            default -> "请根据描述生成 DEE 模板。只返回内容，不要解释。";
        };
        }
        return generate(system, description);
    }

    /**
     * 生成字段映射推荐
     */
    public String generateFieldMapping(String formId, String inputFields) {
        String system = """
                你是致远 OA 字段映射专家。
                OA 表单字段格式为 formmain_xxx / fieldxxxx。
                请根据表单 ID 和字段列表，推断每个字段的业务含义。
                返回 JSON 格式：{ "formId": "...", "formName": "...", "fields": [{ "source", "target", "label", "type" }] }
                只返回 JSON，不要解释。""";
        String userPrompt = "表单ID: " + formId + "\n字段列表:\n" + inputFields;
        return generate(system, userPrompt);
    }
}
