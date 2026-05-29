package com.oacopilot.controller;

import com.oacopilot.service.AiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private final AiService aiService;

    public ChatController(AiService aiService) {
        this.aiService = aiService;
    }

    /**
     * 对话接口
     * @param request 包含 messages 和 promptType
     */
    @PostMapping
    public Map<String, Object> chat(@RequestBody ChatRequest request) {
        try {
            String reply = aiService.chat(request.getMessages(), request.getPromptType());
            return Map.of("success", true, "reply", reply);
        } catch (Exception e) {
            log.error("对话失败: {}", e.getMessage());
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    public static class ChatRequest {
        private List<Map<String, String>> messages;
        private String promptType;

        public List<Map<String, String>> getMessages() { return messages; }
        public void setMessages(List<Map<String, String>> messages) { this.messages = messages; }
        public String getPromptType() { return promptType; }
        public void setPromptType(String promptType) { this.promptType = promptType; }
    }
}
