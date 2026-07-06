package com.jobmoa.hopefulreturn.coursestaff.controller;

import com.jobmoa.hopefulreturn.common.ApiResponse;
import com.jobmoa.hopefulreturn.coursestaff.model.dto.CourseStaffListResponse;
import com.jobmoa.hopefulreturn.coursestaff.model.dto.CreateCourseStaffRequest;
import com.jobmoa.hopefulreturn.coursestaff.model.dto.CreateCourseStaffResponse;
import com.jobmoa.hopefulreturn.coursestaff.model.dto.DeleteCourseStaffResponse;
import com.jobmoa.hopefulreturn.coursestaff.model.dto.UpdateCourseStaffRequest;
import com.jobmoa.hopefulreturn.coursestaff.model.dto.UpdateCourseStaffResponse;
import com.jobmoa.hopefulreturn.coursestaff.service.CourseStaffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "CourseStaff")
@RestController
@RequestMapping("/api/course-staffs")
@RequiredArgsConstructor
public class CourseStaffController {

    private final CourseStaffService courseStaffService;

    @Operation(summary = "강좌 담당자 등록", description = "권한: ADMIN, OPERATOR, REGIONAL_MANAGER")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'REGIONAL_MANAGER')")
    public ApiResponse<CreateCourseStaffResponse> create(@Valid @RequestBody CreateCourseStaffRequest request) {
        return ApiResponse.success(courseStaffService.create(request));
    }

    @Operation(summary = "강좌 담당자 수정", description = "권한: ADMIN, OPERATOR, REGIONAL_MANAGER")
    @PutMapping("/{courseStaffId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'REGIONAL_MANAGER')")
    public ApiResponse<UpdateCourseStaffResponse> update(
            @PathVariable Long courseStaffId,
            @Valid @RequestBody UpdateCourseStaffRequest request) {
        return ApiResponse.success(courseStaffService.update(courseStaffId, request));
    }

    @Operation(summary = "강좌 담당자 삭제", description = "권한: ADMIN, OPERATOR, REGIONAL_MANAGER")
    @DeleteMapping("/{courseStaffId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'REGIONAL_MANAGER')")
    public ApiResponse<DeleteCourseStaffResponse> delete(@PathVariable Long courseStaffId) {
        return ApiResponse.success(courseStaffService.delete(courseStaffId));
    }

    @Operation(summary = "강좌 담당자 목록 조회", description = "권한: 로그인 사용자")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<CourseStaffListResponse> findAll(@RequestParam Long courseId) {
        return ApiResponse.success(courseStaffService.findAll(courseId));
    }
}
