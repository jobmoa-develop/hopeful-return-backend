package com.jobmoa.hopefulreturn.dashboard.model.dto;

import java.time.LocalDate;
import java.util.List;

public record DashboardCalendarResponse(List<Item> content) {

    public record Item(
            String id,
            LocalDate date,
            String kind,       // "TASK" | "ALERT"
            String taskType,   // "RECRUIT_START" | "RECRUIT_END" | "PLAN_SUBMIT" | null(ALERT)
            String title,
            String meta,
            String sourceType, // "COURSE" | "COURSE_PARTICIPANT_GROUP"
            Long sourceId,
            String severity,   // "danger" | "warn" | null(TASK)
            Long count         // ALERT일 때만, TASK면 null
    ) {
    }
}