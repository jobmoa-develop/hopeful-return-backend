package com.jobmoa.hopefulreturn.staffschedule.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.jobmoa.hopefulreturn.coursestaff.entity.SessionType;
import com.jobmoa.hopefulreturn.role.entity.RoleName;
import com.jobmoa.hopefulreturn.staffschedule.entity.StaffScheduleEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link StaffScheduleRepository#findCounselorUnavailabilities} 검증 — 상담사(COUNSELOR) 역할 사용자
 * 본인의 근무 불가일(course_staff_id NULL·is_available=false)만 반환하는지 실 DB로 확인한다.
 * 시드/역할은 V4 기준(counsel01/7=COUNSELOR, staff01/9=STAFF)을 사용하며 @Transactional 로 롤백된다.
 *
 * 실행 조건: 실제 MSSQL 필요 → {@code DB_PASSWORD} 환경변수가 있을 때만 활성화(무DB CI 자동 스킵).
 */
@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class StaffScheduleRepositoryCounselorUnavailabilityTest {

    private static final Long COUNSELOR_USER_ID = 7L; // counsel01 (COUNSELOR)
    private static final Long STAFF_USER_ID = 9L;     // staff01 (STAFF, 비상담사)

    @Autowired
    private StaffScheduleRepository staffScheduleRepository;
    @PersistenceContext
    private EntityManager entityManager;

    private void seedUnavail(Long userId, LocalDate date, SessionType st, boolean available) {
        staffScheduleRepository.saveAndFlush(StaffScheduleEntity.builder()
                .userId(userId).scheduleDate(date).sessionType(st)
                .isAvailable(available).memo("시드")
                .createdAt(LocalDateTime.now())
                .build());
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("COUNSELOR 본인의 불가(is_available=false)만 반환하고, 가능·비상담사 행은 제외한다")
    void returnsOnlyCounselorUnavailable() {
        // 실 DB 기존 데이터와 충돌을 피하려 먼 미래의 고유 날짜를 사용한다.
        LocalDate date = LocalDate.of(2099, 1, 15);
        seedUnavail(COUNSELOR_USER_ID, date, SessionType.AM, false);   // 상담사 불가 → 포함
        seedUnavail(COUNSELOR_USER_ID, date, SessionType.PM, true);    // 상담사 가능 → 제외
        seedUnavail(STAFF_USER_ID, date, SessionType.FULL, false);     // 비상담사 불가 → 제외
        flushAndClear();

        List<StaffScheduleEntity> rows = staffScheduleRepository.findCounselorUnavailabilities(
                date, date, null, RoleName.COUNSELOR);

        assertThat(rows).extracting(StaffScheduleEntity::getUserId).containsOnly(COUNSELOR_USER_ID);
        assertThat(rows).allMatch(r -> Boolean.FALSE.equals(r.getIsAvailable()));
        assertThat(rows).extracting(StaffScheduleEntity::getSessionType).containsExactly(SessionType.AM);

        // 이름 LIKE 양성 — 반환된 상담사명의 부분 문자열로 재조회하면 여전히 잡힌다.
        String name = rows.get(0).getUser().getName();
        assertThat(staffScheduleRepository.findCounselorUnavailabilities(
                date, date, "%" + name + "%", RoleName.COUNSELOR)).isNotEmpty();
    }

    @Test
    @DisplayName("이름 LIKE 패턴이 일치하지 않으면 0건을 반환한다")
    void filtersByNonMatchingName() {
        LocalDate date = LocalDate.of(2099, 1, 16);
        seedUnavail(COUNSELOR_USER_ID, date, SessionType.AM, false);
        flushAndClear();

        assertThat(staffScheduleRepository.findCounselorUnavailabilities(
                date, date, "%절대없는이름XYZ%", RoleName.COUNSELOR)).isEmpty();
    }
}
