package com.jobmoa.hopefulreturn.coursestaffsms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "강좌 담당자 안내 문자 발송 내역(전역·페이지)")
public record CourseStaffSmsPageResponse(
        List<Item> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    @Schema(description = "발송 이력 항목")
    public record Item(
            Long courseStaffSmsId,
            Long courseId,
            String regionName,
            String courseName,
            Integer courseNumber,
            Integer localCourseNumber,
            Long userId,
            String userName,
            String userPhone,
            Long sentBy,
            String sentByName,
            String notifyType,
            String content,
            String sendStatus,
            LocalDateTime sentAt
    ) {
    }
}