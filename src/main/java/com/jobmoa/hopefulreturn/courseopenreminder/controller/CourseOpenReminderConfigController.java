package com.jobmoa.hopefulreturn.courseopenreminder.controller;

import com.jobmoa.hopefulreturn.common.ApiResponse;
import com.jobmoa.hopefulreturn.courseopenreminder.model.dto.CourseOpenReminderConfigResponse;
import com.jobmoa.hopefulreturn.courseopenreminder.model.dto.CourseOpenReminderRunResult;
import com.jobmoa.hopefulreturn.courseopenreminder.model.dto.UpdateCourseOpenReminderConfigRequest;
import com.jobmoa.hopefulreturn.courseopenreminder.service.CourseOpenReminderConfigService;
import com.jobmoa.hopefulreturn.courseopenreminder.service.CourseOpenReminderScheduler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "CourseOpenReminder")
@RestController
@RequestMapping("/api/course-open-reminder")
@RequiredArgsConstructor
public class CourseOpenReminderConfigController {

    private final CourseOpenReminderConfigService configService;
    private final CourseOpenReminderScheduler scheduler;

    @Operation(summary = "개강 하루전 자동 문자 발송 설정 조회",
            description = "활성 여부·개강 며칠 전·발송 시각을 조회한다. 권한: ADMIN, HEAD_OFFICE")
    @GetMapping("/config")
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD_OFFICE')")
    public ApiResponse<CourseOpenReminderConfigResponse> getConfig() {
        return ApiResponse.success(configService.get());
    }

    @Operation(summary = "개강 하루전 자동 문자 발송 설정 수정",
            description = "활성 여부·개강 며칠 전(1~30)·발송 시각(HH:mm)을 수정한다. 권한: ADMIN, HEAD_OFFICE")
    @PutMapping("/config")
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD_OFFICE')")
    public ApiResponse<CourseOpenReminderConfigResponse> updateConfig(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody UpdateCourseOpenReminderConfigRequest request) {
        return ApiResponse.success(configService.update(request, userId));
    }

    @Operation(summary = "개강 하루전 자동 문자 즉시 발송(운영·테스트)",
            description = "발송 시각·하루 1회 가드·활성 여부를 무시하고 지금 배치를 실행한다. "
                    + "중복 발송 방지(이력 dedup)는 유지된다. 권한: ADMIN")
    @PostMapping("/run")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CourseOpenReminderRunResult> run() {
        return ApiResponse.success(scheduler.runNow());
    }
}
