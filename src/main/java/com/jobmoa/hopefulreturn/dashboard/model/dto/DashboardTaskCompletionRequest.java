package com.jobmoa.hopefulreturn.dashboard.model.dto;

import jakarta.validation.constraints.NotBlank;

public record DashboardTaskCompletionRequest(@NotBlank String taskId) {
}