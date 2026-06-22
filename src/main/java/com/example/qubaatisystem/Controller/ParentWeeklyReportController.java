package com.example.qubaatisystem.Controller;

import com.example.qubaatisystem.DTO.Out.ParentWeeklyReportOutDTO;
import com.example.qubaatisystem.Enum.ReportTriggerType;
import com.example.qubaatisystem.Security.SecurityOwnershipService;
import com.example.qubaatisystem.Service.ParentWeeklyReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Parent weekly report endpoints (Student 1 / parent area). Parent-owner-or-admin on the {@code {parentId}}
 * routes; ADMIN-only on the cross-parent batch. Triggers the n8n integration and exposes the stored reports.
 */
@RestController
@RequestMapping("/api/v1/parents")
@RequiredArgsConstructor
public class ParentWeeklyReportController {

    private final ParentWeeklyReportService parentWeeklyReportService;
    private final SecurityOwnershipService security;

    // Manual demo trigger for ONE parent — generates via n8n and stores the result.
    @Deprecated // legacy profile-id route — prefer the /me equivalent
    @PostMapping("/{parentId}/weekly-report/generate")
    public ResponseEntity<?> generate(@PathVariable Integer parentId) {
        security.assertCurrentParentOrAdmin(parentId);
        return ResponseEntity.status(200)
                .body(parentWeeklyReportService.generateForParent(parentId, ReportTriggerType.MANUAL));
    }

    // All stored reports for a parent (newest first).
    @Deprecated // legacy profile-id route — prefer the /me equivalent
    @GetMapping("/{parentId}/weekly-reports")
    public ResponseEntity<?> list(@PathVariable Integer parentId) {
        security.assertCurrentParentOrAdmin(parentId);
        return ResponseEntity.status(200).body(parentWeeklyReportService.getReportsForParent(parentId));
    }

    // The most recent stored report for a parent.
    @Deprecated // legacy profile-id route — prefer the /me equivalent
    @GetMapping("/{parentId}/weekly-reports/latest")
    public ResponseEntity<?> latest(@PathVariable Integer parentId) {
        security.assertCurrentParentOrAdmin(parentId);
        return ResponseEntity.status(200).body(parentWeeklyReportService.getLatestReport(parentId));
    }

    // A single stored report by id — parent owner (checked against the report's parent) or admin.
    @GetMapping("/weekly-reports/{reportId}")
    public ResponseEntity<?> byId(@PathVariable Integer reportId) {
        ParentWeeklyReportOutDTO report = parentWeeklyReportService.getReportById(reportId);
        security.assertCurrentParentOrAdmin(report.getParentId());
        return ResponseEntity.status(200).body(report);
    }

    // Manual batch trigger for ALL parents (continues past failures). triggerType = BATCH_MANUAL. ADMIN only.
    @PostMapping("/weekly-reports/generate-all")
    public ResponseEntity<?> generateAll() {
        security.assertAdmin();
        return ResponseEntity.status(200)
                .body(parentWeeklyReportService.generateWeeklyReportsForAllParents(ReportTriggerType.BATCH_MANUAL));
    }

    // ---------- current-parent ("me") endpoints — no parentId in the path ----------

    @PostMapping("/me/weekly-report/generate")
    public ResponseEntity<?> generateMine() {
        return ResponseEntity.status(200)
                .body(parentWeeklyReportService.generateForParent(security.getCurrentParentId(), ReportTriggerType.MANUAL));
    }

    @GetMapping("/me/weekly-reports")
    public ResponseEntity<?> listMine() {
        return ResponseEntity.status(200).body(parentWeeklyReportService.getReportsForParent(security.getCurrentParentId()));
    }

    @GetMapping("/me/weekly-reports/latest")
    public ResponseEntity<?> latestMine() {
        return ResponseEntity.status(200).body(parentWeeklyReportService.getLatestReport(security.getCurrentParentId()));
    }
}
