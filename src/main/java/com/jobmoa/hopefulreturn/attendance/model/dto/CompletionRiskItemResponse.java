package com.jobmoa.hopefulreturn.attendance.model.dto;

public record CompletionRiskItemResponse(

        Long courseParticipantId,

        String participantName,

        long attendedMinutes,

        long requiredMinutes,

        double attendanceRate,

        CompletionRiskStatus riskStatus

) {
}