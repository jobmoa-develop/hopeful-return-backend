package com.jobmoa.hopefulreturn.web;

import com.jobmoa.hopefulreturn.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 기본 동작/연결 확인용 샘플 엔드포인트.
 * 도메인 확정 전, 프론트-백엔드 연결과 공통 응답 포맷을 확인하는 시작점.
 */
@RestController
@RequestMapping("/api")
public class PingController {

    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.success("pong");
    }
}
