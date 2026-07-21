package com.jobmoa.hopefulreturn.dashboard.model.dto;

import java.util.List;

public record DashboardRegionStatsResponse(List<Item> content, Totals totals) {

    public record Item(
            Long regionId,
            String regionName,
            long plannedCount,
            long recruitingCount,
            long inProgressCount,
            long canceledCount,
            long completedParticipants,
            long incompleteParticipants
    ) {
    }

    public record Totals(
            long plannedCount,
            long recruitingCount,
            long inProgressCount,
            long canceledCount,
            long completedParticipants,
            long incompleteParticipants
    ) {
    }
}