package com.jobmoa.hopefulreturn.staffschedule.repository;

import com.jobmoa.hopefulreturn.coursestaff.entity.SessionType;
import com.jobmoa.hopefulreturn.staffschedule.entity.StaffScheduleEntity;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffScheduleRepository extends JpaRepository<StaffScheduleEntity, Long> {

    // 단건/일괄 등록 시 UNIQUE(user_id, schedule_date, session_type) 중복 검사
    boolean existsByUserIdAndScheduleDateAndSessionType(
            Long userId, LocalDate scheduleDate, SessionType sessionType);

    // 내 캘린더 범위 조회(/me)
    List<StaffScheduleEntity> findByUserIdAndScheduleDateBetween(
            Long userId, LocalDate fromDate, LocalDate toDate);
}
