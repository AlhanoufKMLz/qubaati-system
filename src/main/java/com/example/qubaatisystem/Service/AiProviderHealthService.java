package com.example.qubaatisystem.Service;

import com.example.qubaatisystem.Api.ApiException;
import com.example.qubaatisystem.DTO.Out.AiHealthOutDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Central place to answer "is the AI provider usable?". Reads the standard Spring AI config
 * ({@code spring.ai.openai.api-key} / {@code spring.ai.openai.chat.options.model}) — never the old/manual
 * properties — and never exposes the key itself. Used by the explicit AI endpoints to fail fast (instead of
 * silently returning fallback content) and by {@code GET /api/v1/ai/health}.
 */
@Slf4j
@Service
public class AiProviderHealthService {

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    @Value("${spring.ai.openai.chat.options.model:}")
    private String model;

    private final ChatClient chatClient;

    public AiProviderHealthService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /** True only when a non-blank API key is configured. The key value is never returned or logged. */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /** The configured model name (or "unknown" when unset). Safe to expose. */
    public String getModel() {
        return (model == null || model.isBlank()) ? "unknown" : model;
    }

    /** Fail fast for explicit AI endpoints when the provider is not configured. */
    public void requireConfigured() {
        if (!isConfigured()) {
            throw new ApiException("AI provider is not configured. Set OPENAI_API_KEY.");
        }
    }

    /**
     * Health snapshot. When {@code probe} is true AND a key is configured, sends a tiny ChatClient request to
     * confirm the provider actually answers; a failure downgrades the status to AI_CALL_FAILED (never throws).
     */
    public AiHealthOutDTO health(boolean probe) {
        boolean configured = isConfigured();
        String status;
        if (!configured) {
            status = "MISSING_API_KEY";
        } else if (probe) {
            status = probeProvider() ? "READY" : "AI_CALL_FAILED";
        } else {
            status = "READY";
        }
        return new AiHealthOutDTO(configured, getModel(), "OpenAI via Spring AI", status);
    }

    private boolean probeProvider() {
        try {
            String reply = chatClient.prompt().user("ping").call().content();
            return reply != null;
        } catch (Exception e) {
            log.warn("AI health probe failed: {}", e.getMessage());
            return false;
        }
    }
}
