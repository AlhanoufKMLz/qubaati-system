package com.example.qubaatisystem.DTO.Out;

import com.example.qubaatisystem.Enum.ActivityStatus;
import com.example.qubaatisystem.Enum.ActivityType;
import com.example.qubaatisystem.Enum.DifficultyLevel;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Detailed activity response used ONLY by the AI generate/refine endpoints, so the AI-generated
 * questions and options are returned immediately. Existing CRUD endpoints keep using the simpler
 * {@code ActivityOutDTO}. User-facing text (title/description and nested question/option text) is
 * localized to Arabic in the response when {@code language=ar}; stored DB values remain English.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActivityDetailsOutDTO {

    private Integer id;
    private String title;
    private String description;
    private ActivityType type;
    private ActivityStatus status;
    private DifficultyLevel difficulty;
    private Integer maxScore;
    private LocalDateTime createdAt;
    private List<QuestionDetailsOutDTO> questions;

    /**
     * Optional display-only metadata: Arabic labels for the (stable, English) JSON keys, populated only
     * when {@code language=ar}. JSON key names themselves never change. Omitted from English responses
     * (null) via {@link JsonInclude}.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, String> fieldLabels;
}
