package com.jobmoa.hopefulreturn.attendance.model.dto;

import java.util.List;

public record CompletionRiskListResponse(

        List<CompletionRiskItemResponse> items

) {
}