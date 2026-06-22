package com.example.qubaatisystem.Enum;

/** How a ParentWeeklyReport generation was triggered. */
public enum ReportTriggerType {
    MANUAL,        // single-parent manual demo endpoint
    SCHEDULED,     // weekly scheduler (disabled by default)
    BATCH_MANUAL   // manual "generate for all parents" trigger
}
