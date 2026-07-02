package com.jobmoa.hopefulreturn.attendance.repository;

import com.jobmoa.hopefulreturn.attendance.entity.AttendanceEntity;
import com.jobmoa.hopefulreturn.attendance.entity.AttendanceStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRepository extends JpaRepository<AttendanceEntity, Long> {

    List<AttendanceEntity> findByCourseParticipantId(Long courseParticipantId);

    List<AttendanceEntity> findByCourseParticipantIdAndDayNo(Long courseParticipantId, Integer dayNo);

    List<AttendanceEntity> findByStatus(AttendanceStatus status);
}
