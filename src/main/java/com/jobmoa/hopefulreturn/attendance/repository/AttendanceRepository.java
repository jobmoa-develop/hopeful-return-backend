package com.jobmoa.hopefulreturn.attendance.repository;

import com.jobmoa.hopefulreturn.attendance.entity.AttendanceEntity;
import com.jobmoa.hopefulreturn.attendance.entity.AttendanceStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttendanceRepository extends JpaRepository<AttendanceEntity, Long> {

    List<AttendanceEntity> findByCourseParticipantId(Long courseParticipantId);

    List<AttendanceEntity> findByCourseParticipantIdAndDayNo(Long courseParticipantId, Integer dayNo);

    List<AttendanceEntity> findByStatus(AttendanceStatus status);

    @Query("select a.courseParticipantId as courseParticipantId, count(a) as attendedDays "
            + "from AttendanceEntity a "
            + "where a.courseParticipantId in :courseParticipantIds and a.status in :statuses "
            + "group by a.courseParticipantId")
    List<AttendanceDayCount> countAttendedDaysByCourseParticipantIdIn(
            @Param("courseParticipantIds") Collection<Long> courseParticipantIds,
            @Param("statuses") Collection<AttendanceStatus> statuses);
}
