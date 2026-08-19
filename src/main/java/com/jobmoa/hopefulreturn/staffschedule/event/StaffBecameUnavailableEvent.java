package com.jobmoa.hopefulreturn.staffschedule.event;

import com.jobmoa.hopefulreturn.coursestaff.entity.SessionType;
import java.time.LocalDate;

/**
 * 인력이 '본인 배정된 회차의 날짜'를 가능→불가로 전환했을 때 발행되는 도메인 이벤트.
 *
 * <p>엔티티가 아닌 값(불변 record)만 담는다 — 발송 리스너가 별 스레드/별 트랜잭션에서 동작하므로
 * detached 엔티티의 LAZY 접근을 피하고 필요한 값은 courseStaffId 등으로 재조회한다.
 */
public record StaffBecameUnavailableEvent(
        Long staffScheduleId,
        Long courseStaffId,
        Long userId,
        LocalDate scheduleDate,
        SessionType sessionType,
        String memo) {
}
