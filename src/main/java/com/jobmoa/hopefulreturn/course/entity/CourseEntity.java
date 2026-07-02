package com.jobmoa.hopefulreturn.course.entity;

import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantEntity;
import com.jobmoa.hopefulreturn.coursestaff.entity.CourseStaffEntity;
import com.jobmoa.hopefulreturn.region.entity.RegionEntity;
import com.jobmoa.hopefulreturn.users.entity.UsersEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "course")
public class CourseEntity {

    @Id
    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "region_id", nullable = false)
    private Long regionId;

    @Column(name = "course_number", nullable = false)
    private Integer courseNumber;

    @Column(name = "course_name", length = 100)
    private String courseName;

    @Column(name = "recruit_start")
    private LocalDate recruitStart;

    @Column(name = "recruit_end")
    private LocalDate recruitEnd;

    @Column(name = "day1_date")
    private LocalDate day1Date;

    @Column(name = "day2_date")
    private LocalDate day2Date;

    @Column(name = "day3_date")
    private LocalDate day3Date;

    @Column(name = "day4_date")
    private LocalDate day4Date;

    @Column(name = "day5_date")
    private LocalDate day5Date;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Column(name = "minimum_capacity", nullable = false)
    private Integer minimumCapacity;

    @Column(name = "location", length = 100)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private CourseStatus status;

    @Column(name = "plan_submit_date")
    private LocalDate planSubmitDate;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "education_start_time")
    private LocalTime educationStartTime;

    @Column(name = "education_end_time")
    private LocalTime educationEndTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", insertable = false, updatable = false)
    private RegionEntity region;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", insertable = false, updatable = false)
    private UsersEntity creator;

    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
    private List<CourseParticipantEntity> courseParticipants;

    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
    private List<CourseStaffEntity> courseStaffs;
}
