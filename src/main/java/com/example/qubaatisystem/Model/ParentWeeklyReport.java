package com.example.qubaatisystem.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A generated (or attempted) parent weekly report. The full n8n JSON response is kept in {@code reportJson}
 * (TEXT) so it can be re-rendered later or forwarded to WhatsApp/email in a future iteration. Failed attempts
 * are also stored (success=false + errorMessage) when generated through the batch flow.
 */
@Entity
@Table(name = "parent_weekly_report")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParentWeeklyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = false)
    private Parent parent;

    // MANUAL / SCHEDULED / BATCH_MANUAL (stored as String; mirrors ReportTriggerType)
    @Column(nullable = false, length = 20)
    private String triggerType;

    @Column(nullable = false)
    private Boolean success;

    @Column(length = 50)
    private String reportType;

    @Column(length = 255)
    private String reportTitle;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(length = 50)
    private String source;

    @Column(nullable = false, updatable = false)
    private LocalDateTime generatedAt;

    // Full n8n response JSON (success) — kept for later viewing / WhatsApp / email.
    @Column(columnDefinition = "LONGTEXT")
    private String reportJson;

    // Populated only when success=false.
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
}
