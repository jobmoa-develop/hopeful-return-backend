package com.jobmoa.hopefulreturn.coursedailycounselor.entity;

import com.jobmoa.hopefulreturn.coursestaff.entity.CourseStaffEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 상담사(COUNSELOR) 일별 회차 배정. staff_schedule 의 UNIQUE(user, date, session) 제약에서
 * 벗어나 <b>같은 날 여러 회차</b> 중복 배정을 허용하기 위해 상담사 일자 정보만 분리 저장한다.
 * 역할·회차·인력 정보는 링크된 course_staff(role=COUNSELOR, session=FULL) 로스터 행이 보유한다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "course_daily_counselor")
public class CourseDailyCounselorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_daily_counselor_id", nullable = false)
    private Long courseDailyCounselorId;

    // 상담사 로스터(course_staff) 링크 — 회차·인력·역할·세션을 보유
    @Column(name = "course_staff_id", nullable = false)
    private Long courseStaffId;

    // 배정 날짜(교육일)
    @Column(name = "schedule_date", nullable = false)
    private LocalDate scheduleDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // 회차·인력 복원용 읽기 전용 연관(FK 컬럼은 위 courseStaffId 가 관리)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_staff_id", insertable = false, updatable = false)
    private CourseStaffEntity courseStaff;
}
