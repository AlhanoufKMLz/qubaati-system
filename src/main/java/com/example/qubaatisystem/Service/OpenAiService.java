package com.example.qubaatisystem.Service;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * Thin wrapper around OpenAI via Spring AI {@link ChatClient} (migrated from the old manual RestTemplate
 * integration). Returns {@code null} on any error — no/invalid key, network failure, or malformed JSON — so
 * the caller falls back to rule-based analysis. The API key and model come from Spring AI configuration
 * ({@code spring.ai.openai.*}); they are never read, logged, returned, or stored in this class.
 */
@Slf4j
@Service
public class OpenAiService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public OpenAiService(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    /**
     * Sends a chat request to OpenAI via Spring AI and parses the model's JSON content into
     * {@link AiAnalysisResult}. Returns {@code null} if the call fails (e.g. no API key configured), the
     * response is blank, or the JSON is malformed — so callers can apply their deterministic fallback.
     */
    public AiAnalysisResult analyze(String systemPrompt, String userPrompt) {
        try {
            String content = chatClient
                    .prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();
            if (content == null || content.isBlank()) {
                return null;
            }
            return objectMapper.readValue(content, AiAnalysisResult.class);
        } catch (Exception e) {
            log.warn("OpenAI call failed — using rule-based fallback: {}", e.getMessage());
            return null;
        }
    }

    // ── Inner DTO for the model's JSON output ───────────────────────────────

    @Data
    @NoArgsConstructor
    public static class AiAnalysisResult {
        private String summary;
        private List<String> strengths;
        private List<String> concerns;
        private List<String> recommendedActions;
    }
}
