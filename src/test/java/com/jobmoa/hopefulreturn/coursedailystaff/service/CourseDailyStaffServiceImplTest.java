package com.jobmoa.hopefulreturn.coursedailystaff.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.course.entity.CourseEntity;
import com.jobmoa.hopefulreturn.course.repository.CourseRepository;
import com.jobmoa.hopefulreturn.coursedailystaff.model.dto.CourseDailyStaffCandidateResponse;
import com.jobmoa.hopefulreturn.coursedailystaff.model.dto.CourseDailyStaffListResponse;
import com.jobmoa.hopefulreturn.coursedailystaff.model.dto.SaveCourseDailyStaffRequest;
import com.jobmoa.hopefulreturn.coursedailystaff.model.dto.SaveCourseDailyStaffResponse;
import com.jobmoa.hopefulreturn.coursestaff.entity.CourseStaffEntity;
import com.jobmoa.hopefulreturn.coursestaff.entity.SessionType;
import com.jobmoa.hopefulreturn.coursestaff.entity.StaffRole;
import com.jobmoa.hopefulreturn.coursestaff.repository.CourseStaffRepository;
import com.jobmoa.hopefulreturn.role.entity.RoleEntity;
import com.jobmoa.hopefulreturn.role.entity.RoleName;
import com.jobmoa.hopefulreturn.staffschedule.entity.StaffScheduleEntity;
import com.jobmoa.hopefulreturn.staffschedule.repository.StaffScheduleRepository;
import com.jobmoa.hopefulreturn.userrole.entity.UserRoleEntity;
import com.jobmoa.hopefulreturn.userrole.repository.UserRoleRepository;
import com.jobmoa.hopefulreturn.users.entity.UsersEntity;
import com.jobmoa.hopefulreturn.users.repository.UsersRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 회차 날짜별 인력 배정 서비스 단위 테스트. staff_schedule(course_staff_id 연결) 모델 기준으로
 * 저장(로스터 확보·staff_schedule 배정 upsert·중복 제거), 미존재 검증, 조회(연결 행 복원),
 * 후보(근무 불가일 제외)를 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class CourseDailyStaffServiceImplTest {

    private static final Long COURSE_ID = 15L;
    private static final LocalDate D1 = LocalDate.of(2026, 8, 18);
    private static final LocalDate D2 = LocalDate.of(2026, 8, 19);

    @Mock
    private CourseStaffRepository courseStaffRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private UsersRepository usersRepository;
    @Mock
    private StaffScheduleRepository staffScheduleRepository;
    @Mock
    private UserRoleRepository userRoleRepository;

    @InjectMocks
    private CourseDailyStaffServiceImpl service;

    private UsersEntity user(Long id, String name) {
        return UsersEntity.builder().userId(id).name(name).deleted(false).build();
    }

    private UserRoleEntity userRole(Long userId, RoleName roleName) {
        return UserRoleEntity.builder()
                .userId(userId)
                .role(RoleEntity.builder().roleName(roleName).build())
                .build();
    }

    @Test
    @DisplayName("저장 시 course_staff 로스터를 확보하고 staff_schedule에 course_staff_id·is_available=true로 배정한다(중복 제거)")
    void save_attachesAssignmentAndDedupes() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(new CourseEntity()));
        when(usersRepository.findAllById(anyList())).thenReturn(List.of(user(6L, "이강사")));
        when(courseStaffRepository.findByCourseId(COURSE_ID)).thenReturn(List.of()); // 로스터 없음
        when(courseStaffRepository.save(any(CourseStaffEntity.class))).thenAnswer(inv -> {
            CourseStaffEntity cs = inv.getArgument(0);
            cs.setCourseStaffId(100L);
            return cs;
        });
        when(staffScheduleRepository.findByUserIdAndScheduleDateAndSessionType(6L, D1, SessionType.AM))
                .thenReturn(Optional.empty());
        when(staffScheduleRepository.save(any(StaffScheduleEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // 동일 (user·date·session) 중복 1건 포함 → 1건만 저장
        SaveCourseDailyStaffResponse response = service.save(new SaveCourseDailyStaffRequest(
                COURSE_ID, List.of(
                new SaveCourseDailyStaffRequest.Entry(D1, "LECTURER", "AM", 6L),
                new SaveCourseDailyStaffRequest.Entry(D1, "LECTURER", "AM", 6L))));

        assertThat(response.courseId()).isEqualTo(COURSE_ID);
        assertThat(response.saved()).isEqualTo(1);
        ArgumentCaptor<StaffScheduleEntity> captor = ArgumentCaptor.forClass(StaffScheduleEntity.class);
        verify(staffScheduleRepository).save(captor.capture());
        assertThat(captor.getValue().getCourseStaffId()).isEqualTo(100L);
        assertThat(captor.getValue().getIsAvailable()).isTrue();
        assertThat(captor.getValue().getScheduleDate()).isEqualTo(D1);
        assertThat(captor.getValue().getSessionType()).isEqualTo(SessionType.AM);
    }

    @Test
    @DisplayName("저장 시 회차가 없으면 COURSE_NOT_FOUND 예외")
    void save_courseNotFound() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.save(new SaveCourseDailyStaffRequest(COURSE_ID, List.of())))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COURSE_NOT_FOUND);
        verify(staffScheduleRepository, never()).save(any());
    }

    @Test
    @DisplayName("저장 시 배정 인력이 존재하지 않으면 USER_NOT_FOUND 예외")
    void save_userNotFound() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(new CourseEntity()));
        when(usersRepository.findAllById(anyList())).thenReturn(List.of()); // 없음

        assertThatThrownBy(() -> service.save(new SaveCourseDailyStaffRequest(
                COURSE_ID, List.of(new SaveCourseDailyStaffRequest.Entry(D1, "LECTURER", "AM", 99L)))))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("저장 시 잘못된 역할이면 INVALID_INPUT 예외")
    void save_invalidRole() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(new CourseEntity()));
        when(usersRepository.findAllById(anyList())).thenReturn(List.of(user(6L, "이강사")));
        when(courseStaffRepository.findByCourseId(COURSE_ID)).thenReturn(List.of());

        assertThatThrownBy(() -> service.save(new SaveCourseDailyStaffRequest(
                COURSE_ID, List.of(new SaveCourseDailyStaffRequest.Entry(D1, "MANAGER", "AM", 6L)))))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("후보 조회는 역할 자격자 중 근무 불가일이 있는 날짜를 제외한다(OPERATOR→ADMIN_STAFF)")
    void findCandidates_excludesUnavailableDates() {
        CourseEntity course = new CourseEntity();
        course.setDay1Date(D1);
        course.setDay2Date(D2);
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        // 6번은 D2 근무 불가
        when(staffScheduleRepository
                .findByScheduleDateBetweenAndIsAvailableFalseAndCourseStaffIdIsNull(D1, D2))
                .thenReturn(List.of(StaffScheduleEntity.builder()
                        .userId(6L).scheduleDate(D2).sessionType(SessionType.FULL)
                        .isAvailable(false).build()));
        when(userRoleRepository.findAll()).thenReturn(List.of(
                userRole(6L, RoleName.LECTURER),
                userRole(7L, RoleName.OPERATOR)));   // 행정허브 → ADMIN_STAFF
        when(usersRepository.findAllById(anyList())).thenReturn(List.of(
                user(6L, "이강사"), user(7L, "김행정")));

        CourseDailyStaffCandidateResponse response = service.findCandidates(COURSE_ID);

        assertThat(response.dates()).containsExactly(D1, D2);
        assertThat(response.candidates()).hasSize(2);

        CourseDailyStaffCandidateResponse.Candidate lecturer = response.candidates().stream()
                .filter(c -> c.userId().equals(6L)).findFirst().orElseThrow();
        assertThat(lecturer.staffRoles()).containsExactly("LECTURER");
        // D2 불가 → D1만 가용
        assertThat(lecturer.availability()).extracting("scheduleDate").containsExactly(D1);

        CourseDailyStaffCandidateResponse.Candidate admin = response.candidates().stream()
                .filter(c -> c.userId().equals(7L)).findFirst().orElseThrow();
        assertThat(admin.staffRoles()).containsExactly("ADMIN_STAFF");
        assertThat(admin.availability()).hasSize(2);
    }

    @Test
    @DisplayName("후보 조회 시 회차 교육일이 없으면 빈 결과를 반환한다")
    void findCandidates_noDates() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(new CourseEntity()));

        CourseDailyStaffCandidateResponse response = service.findCandidates(COURSE_ID);

        assertThat(response.dates()).isEmpty();
        assertThat(response.candidates()).isEmpty();
    }

    @Test
    @DisplayName("배정 목록 조회는 course_staff에 연결된 staff_schedule 행을 복원한다")
    void findAll_reconstructsFromLinkedSchedules() {
        CourseStaffEntity cs = CourseStaffEntity.builder()
                .courseStaffId(8L).courseId(COURSE_ID).userId(6L)
                .staffRole(StaffRole.LECTURER).sessionType(SessionType.AM).build();
        when(courseStaffRepository.findByCourseId(COURSE_ID)).thenReturn(List.of(cs));
        StaffScheduleEntity schedule = StaffScheduleEntity.builder()
                .staffScheduleId(50L).userId(6L).scheduleDate(D1).sessionType(SessionType.AM)
                .isAvailable(true).courseStaffId(8L).user(user(6L, "이강사")).courseStaff(cs).build();
        when(staffScheduleRepository.findByCourseStaffIdIn(anyList())).thenReturn(List.of(schedule));

        CourseDailyStaffListResponse response = service.findAll(COURSE_ID);

        assertThat(response.assignments()).hasSize(1);
        CourseDailyStaffListResponse.Item item = response.assignments().get(0);
        assertThat(item.staffRole()).isEqualTo("LECTURER");
        assertThat(item.sessionType()).isEqualTo("AM");
        assertThat(item.scheduleDate()).isEqualTo(D1);
        assertThat(item.name()).isEqualTo("이강사");
    }
}
