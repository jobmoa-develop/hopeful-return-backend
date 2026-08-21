package com.jobmoa.hopefulreturn.region.controller;

import com.jobmoa.hopefulreturn.common.ApiResponse;
import com.jobmoa.hopefulreturn.region.model.dto.RegionDetailResponse;
import com.jobmoa.hopefulreturn.region.model.dto.RegionListResponse;
import com.jobmoa.hopefulreturn.region.service.RegionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Region")
@RestController
@RequestMapping("/api/regions")
@RequiredArgsConstructor
public class RegionController {

    private final RegionService regionService;

    @Operation(summary = "지역 목록 조회", description = "권한: 로그인 사용자 전체(지역명·계층은 비민감 참조데이터로 여러 필터에서 공용)")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<RegionListResponse>> findAll() {
        return ApiResponse.success(regionService.findAll());
    }

    @Operation(summary = "지역 상세 조회", description = "권한: ADMIN, HEAD_OFFICE")
    @GetMapping("/{regionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD_OFFICE')")
    public ApiResponse<RegionDetailResponse> findById(@PathVariable Long regionId) {
        return ApiResponse.success(regionService.findById(regionId));
    }
}
