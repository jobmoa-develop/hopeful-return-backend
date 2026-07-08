package com.jobmoa.hopefulreturn.users.entity;

import com.jobmoa.hopefulreturn.course.entity.CourseEntity;
import com.jobmoa.hopefulreturn.coursestaff.entity.CourseStaffEntity;
import com.jobmoa.hopefulreturn.participantmemo.entity.ParticipantMemoEntity;
import com.jobmoa.hopefulreturn.participantsms.entity.ParticipantSmsEntity;
import com.jobmoa.hopefulreturn.userrole.entity.UserRoleEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
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
@Table(name = "users")
public class UsersEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "login_id", nullable = false, length = 100)
    private String loginId;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    @Column(name = "locked")
    private Boolean locked;

    @Column(name = "deleted")
    private Boolean deleted;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "refresh_token", length = 1000)
    private String refreshToken;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<UserRoleEntity> userRoles;

    @OneToMany(mappedBy = "creator", fetch = FetchType.LAZY)
    private List<CourseEntity> createdCourses;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<CourseStaffEntity> courseStaffs;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<ParticipantMemoEntity> participantMemos;

    @OneToMany(mappedBy = "sender", fetch = FetchType.LAZY)
    private List<ParticipantSmsEntity> sentParticipantSms;
}
