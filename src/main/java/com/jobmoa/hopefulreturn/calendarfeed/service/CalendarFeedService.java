package com.jobmoa.hopefulreturn.calendarfeed.service;

import com.jobmoa.hopefulreturn.calendarfeed.model.dto.CalendarFeedStatusResponse;
import java.util.Optional;

/**
 * 내 일정 Google Calendar ICS 구독 피드 서비스.
 * 관리(상태/발급/해제)는 인증 사용자 기준, 피드 생성은 비밀 토큰 기준으로 동작한다.
 */
public interface CalendarFeedService {

    /** 현재 사용자의 연동 상태(+발급 URL) 조회. */
    CalendarFeedStatusResponse getStatus(Long userId);

    /** 토큰을 새로 발급(재발급 포함)해 연동을 활성화하고 상태를 반환. 기존 URL 은 무효화된다. */
    CalendarFeedStatusResponse enable(Long userId);

    /** 토큰을 제거해 연동을 해제한다. */
    void disable(Long userId);

    /** 비밀 토큰으로 사용자를 찾아 ICS 텍스트를 생성한다. 토큰이 유효하지 않으면 empty. */
    Optional<String> buildIcsFeed(String token);
}
