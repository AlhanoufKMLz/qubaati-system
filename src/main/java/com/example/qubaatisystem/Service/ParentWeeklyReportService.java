package com.example.qubaatisystem.Service;

import com.example.qubaatisystem.Api.ApiException;
import com.example.qubaatisystem.DTO.Out.ChildLearningProfileOutDTO;
import com.example.qubaatisystem.DTO.Out.ParentWeeklyReportChildOutDTO;
import com.example.qubaatisystem.DTO.Out.ParentWeeklyReportOutDTO;
import com.example.qubaatisystem.DTO.Out.StudentOutDTO;
import com.example.qubaatisystem.Enum.ReportTriggerType;
import com.example.qubaatisystem.Model.Parent;
import com.example.qubaatisystem.Model.ParentWeeklyReport;
import com.example.qubaatisystem.Repository.ParentRepository;
import com.example.qubaatisystem.Repository.ParentWeeklyReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the "Parent Weekly Report" payload from existing dashboard/profile data, calls the n8n webhook via
 * {@link N8nWebhookService} (Basic Auth), and persists the result as a {@link ParentWeeklyReport} so it can be
 * viewed later (and forwarded to WhatsApp/email in a future iteration).
 *
 * <p>Reuses existing services only — no dashboard logic is duplicated. Missing per-child fields fall back to safe
 * defaults so a report is never blocked by sparse data.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParentWeeklyReportService {

    private static final String REPORT_TYPE = "PARENT_WEEKLY_REPORT";

    private final ParentRepository parentRepository;
    private final StudentService studentService;
    private final ChildLearningProfileService childLearningProfileService;
    private final N8nWebhookService n8nWebhookService;
    private final ParentWeeklyReportRepository reportRepository;
    private final ObjectMapper objectMapper;

    // ── Generation: single parent (manual demo) ───────────────────────────────

    /**
     * Builds + sends the report for ONE parent and stores the successful result. If n8n is not configured or the
     * call fails, a clear {@link ApiException} propagates (HTTP 400) — nothing is stored for the failed attempt.
     */
    @Transactional
    public ParentWeeklyReportOutDTO generateForParent(Integer parentId, ReportTriggerType triggerType) {
        Parent parent = requireParent(parentId);
        Map<String, Object> response = n8nWebhookService.generateParentWeeklyReport(buildPayload(parent));
        ParentWeeklyReport report = newReport(parent, triggerType);
        applySuccess(report, response);
        return toOut(reportRepository.save(report));
    }

    // ── Generation: all parents (batch / scheduled) ───────────────────────────

    /**
     * Generates a report for EVERY parent, continuing past failures. Each parent's outcome — success or failure —
     * is stored (failures carry success=false + errorMessage). Returns one result per parent.
     */
    public List<ParentWeeklyReportOutDTO> generateWeeklyReportsForAllParents(ReportTriggerType triggerType) {
        List<ParentWeeklyReportOutDTO> results = new ArrayList<>();
        List<Parent> parents = parentRepository.findAll();
        log.info("Generating parent weekly reports ({}) for {} parent(s)", triggerType, parents.size());
        for (Parent parent : parents) {
            results.add(generateAndSaveContinueOnError(parent, triggerType));
        }
        return results;
    }

    /** Batch helper: never throws — stores success or a failed row and returns the DTO either way. */
    private ParentWeeklyReportOutDTO generateAndSaveContinueOnError(Parent parent, ReportTriggerType triggerType) {
        ParentWeeklyReport report = newReport(parent, triggerType);
        try {
            Map<String, Object> response = n8nWebhookService.generateParentWeeklyReport(buildPayload(parent));
            applySuccess(report, response);
        } catch (Exception e) {
            report.setSuccess(false);
            report.setReportType(REPORT_TYPE);
            report.setErrorMessage(e.getMessage());
            log.warn("Weekly report failed for parent {}: {}", parent.getId(), e.getMessage());
        }
        return toOut(reportRepository.save(report));
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    public List<ParentWeeklyReportOutDTO> getReportsForParent(Integer parentId) {
        requireParent(parentId);
        return reportRepository.findParentWeeklyReportsByParentIdOrderByGeneratedAtDesc(parentId)
                .stream().map(this::toOut).toList();
    }

    public ParentWeeklyReportOutDTO getLatestReport(Integer parentId) {
        requireParent(parentId);
        ParentWeeklyReport report = reportRepository.findFirstParentWeeklyReportByParentIdOrderByGeneratedAtDesc(parentId);
        if (report == null) {
            throw new ApiException("No weekly report found for parent " + parentId);
        }
        return toOut(report);
    }

    public ParentWeeklyReportOutDTO getReportById(Integer reportId) {
        ParentWeeklyReport report = reportRepository.findParentWeeklyReportById(reportId);
        if (report == null) {
            throw new ApiException("Weekly report with id " + reportId + " not found");
        }
        return toOut(report);
    }

    // ── Payload building ───────────────────────────────────────────────────────

    private Map<String, Object> buildPayload(Parent parent) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", "PARENT_WEEKLY_REPORT");

        Map<String, Object> parentMap = new LinkedHashMap<>();
        parentMap.put("id", parent.getId());
        parentMap.put("name", parent.getFullName());
        payload.put("parent", parentMap);

        List<ParentWeeklyReportChildOutDTO> children = new ArrayList<>();
        for (StudentOutDTO child : studentService.getByParentId(parent.getId())) {
            children.add(buildChild(parent.getId(), child));
        }
        payload.put("children", children);
        payload.put("generatedAt", LocalDateTime.now().toString());
        return payload;
    }

    /** Per-child aggregation with safe fallbacks; one bad child never fails the whole report. */
    private ParentWeeklyReportChildOutDTO buildChild(Integer parentId, StudentOutDTO child) {
        ParentWeeklyReportChildOutDTO out = new ParentWeeklyReportChildOutDTO();
        out.setStudentId(child.getId());
        out.setStudentName(child.getFullName());
        out.setGradeLevel(child.getGrade() != null ? child.getGrade() : "UNKNOWN");
        out.setTotalPoints(child.getTotalPoints() != null ? child.getTotalPoints() : 0);
        out.setCompletedMissionsCount(child.getCompletedMissionsCount() != null ? child.getCompletedMissionsCount() : 0);

        // Safe fallbacks (used when the rich profile is missing/sparse).
        out.setAverageActivityScore(0);
        out.setCompletedMissionSessionsCount(0);
        out.setStrongSkills(new ArrayList<>());
        out.setWeakSkills(new ArrayList<>());
        out.setLearningStyle("UNKNOWN");
        out.setLatestInsight("No insight available yet.");
        out.setRecommendations(new ArrayList<>());

        try {
            ChildLearningProfileOutDTO p = childLearningProfileService.getLearningProfile(parentId, child.getId());
            if (p != null) {
                if (p.getAverageActivityScore() != null) {
                    out.setAverageActivityScore((int) Math.round(p.getAverageActivityScore()));
                }
                if (p.getCompletedMissionSessionsCount() != null) {
                    out.setCompletedMissionSessionsCount(p.getCompletedMissionSessionsCount());
                }
                out.setStrongSkills(deriveStrongSkills(p));
                if (p.getWeakSkills() != null) {
                    out.setWeakSkills(p.getWeakSkills());
                }
                if (notBlank(p.getLearningStylePrimary())) {
                    out.setLearningStyle(p.getLearningStylePrimary());
                }
                if (notBlank(p.getLatestMissionInsightSummary())) {
                    out.setLatestInsight(p.getLatestMissionInsightSummary());
                }
                if (p.getTopRecommendations() != null) {
                    out.setRecommendations(p.getTopRecommendations());
                }
            }
        } catch (Exception e) {
            log.warn("Using fallback child data for student {} (parent {}): {}", child.getId(), parentId, e.getMessage());
        }
        return out;
    }

    /** Strong skills = tracked skills that are not flagged as weak. */
    private List<String> deriveStrongSkills(ChildLearningProfileOutDTO p) {
        List<String> weak = p.getWeakSkills() != null ? p.getWeakSkills() : new ArrayList<>();
        List<String> strong = new ArrayList<>();
        if (p.getSkills() != null) {
            for (ChildLearningProfileOutDTO.SkillRow row : p.getSkills()) {
                if (row.getSkillName() != null && !weak.contains(row.getSkillName())) {
                    strong.add(row.getSkillName());
                }
            }
        }
        return strong;
    }

    // ── Mapping helpers ─────────────────────────────────────────────────────────

    private ParentWeeklyReport newReport(Parent parent, ReportTriggerType triggerType) {
        ParentWeeklyReport report = new ParentWeeklyReport();
        report.setParent(parent);
        report.setTriggerType(triggerType.name());
        report.setGeneratedAt(LocalDateTime.now());
        report.setSuccess(false); // flipped to true by applySuccess on a successful n8n call
        return report;
    }

    private void applySuccess(ParentWeeklyReport report, Map<String, Object> response) {
        Object success = response.get("success");
        report.setSuccess(success instanceof Boolean ? (Boolean) success : Boolean.TRUE);
        report.setReportType(asString(response.get("reportType"), REPORT_TYPE));
        report.setReportTitle(asString(response.get("reportTitle"), null));
        report.setSummary(asString(response.get("summary"), null));
        report.setSource(asString(response.get("source"), "n8n"));
        report.setReportJson(writeJson(response));
    }

    private String writeJson(Map<String, Object> response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.warn("Could not serialise n8n response to JSON: {}", e.getMessage());
            return null;
        }
    }

    private ParentWeeklyReportOutDTO toOut(ParentWeeklyReport r) {
        return new ParentWeeklyReportOutDTO(
                r.getId(),
                r.getParent() != null ? r.getParent().getId() : null,
                r.getTriggerType(),
                r.getSuccess(),
                r.getReportType(),
                r.getReportTitle(),
                r.getSummary(),
                r.getSource(),
                r.getGeneratedAt(),
                r.getErrorMessage(),
                // The entity stores the n8n payload as a LONGTEXT string; parse it back into a structured object
                // so the API returns clean nested JSON instead of an escaped string. Null on failed attempts.
                parseReportJson(r.getReportJson()));
    }

    /** Parses the stored report JSON string into a structured Map for the API response (no escaped strings). */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseReportJson(String reportJson) {
        if (reportJson == null || reportJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(reportJson, Map.class);
        } catch (Exception e) {
            // Never expose a stack trace; surface a small, safe diagnostic object instead.
            log.warn("Stored report JSON could not be parsed: {}", e.getMessage());
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("raw", reportJson);
            fallback.put("parseError", "Stored report JSON could not be parsed");
            return fallback;
        }
    }

    private Parent requireParent(Integer parentId) {
        Parent parent = parentRepository.findParentById(parentId);
        if (parent == null) {
            throw new ApiException("Parent with id " + parentId + " not found");
        }
        return parent;
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private String asString(Object value, String fallback) {
        return value != null ? String.valueOf(value) : fallback;
    }
}
