package com.example.qubaatisystem.Service;

import com.example.qubaatisystem.Enum.ReportTriggerType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Weekly scheduler for parent reports. The {@code @Scheduled} method is always registered (so the cron can be
 * tuned via {@code n8n.parent-weekly-report.cron}) but is a NO-OP unless
 * {@code n8n.parent-weekly-report.scheduler-enabled=true}. It stays disabled by default so normal testing never
 * makes unexpected external n8n calls.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ParentWeeklyReportScheduler {

    @Value("${n8n.parent-weekly-report.scheduler-enabled:false}")
    private boolean schedulerEnabled;

    private final ParentWeeklyReportService parentWeeklyReportService;

    @Scheduled(cron = "${n8n.parent-weekly-report.cron:0 0 8 * * MON}")
    public void generateWeeklyReports() {
        if (!schedulerEnabled) {
            log.debug("Parent weekly report scheduler is disabled — skipping run.");
            return;
        }
        log.info("Scheduled parent weekly report run starting.");
        parentWeeklyReportService.generateWeeklyReportsForAllParents(ReportTriggerType.SCHEDULED);
    }
}
