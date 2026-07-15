package com.jobmoa.hopefulreturn.attendanceleave.repository;

import com.jobmoa.hopefulreturn.attendanceleave.entity.AttendanceLeaveEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceLeaveRepository extends JpaRepository<AttendanceLeaveEntity, Long> {

    List<AttendanceLeaveEntity> findByAttendanceId(Long attendanceId);

    List<AttendanceLeaveEntity> findByAttendanceIdIn(List<Long> attendanceIds);
}
