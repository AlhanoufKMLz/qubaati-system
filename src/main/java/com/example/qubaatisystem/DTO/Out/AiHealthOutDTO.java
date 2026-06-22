package com.example.qubaatisystem.DTO.Out;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI provider health/config snapshot. Never exposes the API key — only whether one is configured, the model
 * name, the provider label, and a coarse status (READY / MISSING_API_KEY / AI_CALL_FAILED).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiHealthOutDTO {

    private boolean configured;
    private String model;
    private String provider;
    private String status;
}
