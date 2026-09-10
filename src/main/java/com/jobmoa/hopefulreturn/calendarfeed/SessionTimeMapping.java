package com.jobmoa.hopefulreturn.calendarfeed;

import java.time.LocalTime;

/**
 * AM/PM/FULL 세션 타입을 고정 시각대로 매핑한다.
 * 배정 회차는 CourseEntity 의 교육 시각을 우선 사용하고, 시각이 없는 개인 가용성 블록에만 이 매핑을 폴백으로 쓴다.
 * (종일 이벤트 대신 시각 이벤트로 내보내 AM/PM 구분을 캘린더에서 유지한다.)
 */
public final class SessionTimeMapping {

    public static final LocalTime AM_START = LocalTime.of(9, 0);
    public static final LocalTime AM_END = LocalTime.of(13, 0);
    public static final LocalTime PM_START = LocalTime.of(13, 0);
    public static final LocalTime PM_END = LocalTime.of(18, 0);
    public static final LocalTime FULL_START = LocalTime.of(9, 0);
    public static final LocalTime FULL_END = LocalTime.of(18, 0);

    private SessionTimeMapping() {
    }

    public static LocalTime startOf(String sessionType) {
        if ("AM".equalsIgnoreCase(sessionType)) {
            return AM_START;
        }
        if ("PM".equalsIgnoreCase(sessionType)) {
            return PM_START;
        }
        return FULL_START;
    }

    public static LocalTime endOf(String sessionType) {
        if ("AM".equalsIgnoreCase(sessionType)) {
            return AM_END;
        }
        if ("PM".equalsIgnoreCase(sessionType)) {
            return PM_END;
        }
        return FULL_END;
    }
}
