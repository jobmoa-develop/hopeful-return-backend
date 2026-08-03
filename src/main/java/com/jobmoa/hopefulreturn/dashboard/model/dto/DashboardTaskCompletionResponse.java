package com.jobmoa.hopefulreturn.dashboard.model.dto;

import java.util.Set;

public record DashboardTaskCompletionResponse(Set<String> completedTaskIds) {
}