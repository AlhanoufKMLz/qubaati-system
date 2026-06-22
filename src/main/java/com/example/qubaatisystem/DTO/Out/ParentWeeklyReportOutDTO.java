package com.example.qubaatisystem.DTO.Out;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * API response for a stored parent weekly report. Mirrors the persisted {@code ParentWeeklyReport}; the flat
 * fields support quick listing, and the full n8n payload is returned as a structured {@code report} object (the
 * stored JSON string is parsed before responding — never an escaped JSON string). No hidden teacher/grading
 * fields are exposed.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParentWeeklyReportOutDTO {

    private Integer id;
    private Integer parentId;
    private String triggerType;
    private Boolean success;
    private String reportType;
    private String reportTitle;
    private String summary;
    private String source;
    private LocalDateTime generatedAt;
    private String errorMessage;

    // The full n8n response as a structured object (includes children[]). Null on failed attempts. The entity
    // still stores it as a LONGTEXT string; it is parsed here so the API returns clean nested JSON.
    private Map<String, Object> report;
}
