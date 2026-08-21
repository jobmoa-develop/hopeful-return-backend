package com.jobmoa.hopefulreturn.coursedailystaff.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.course.entity.CourseEntity;
import com.jobmoa.hopefulreturn.course.entity.CourseStatus;
import com.jobmoa.hopefulreturn.course.repository.CourseRepository;
import com.jobmoa.hopefulreturn.coursedailycounselor.entity.CourseDailyCounselorEntity;
import com.jobmoa.hopefulreturn.coursedailycounselor.repository.CourseDailyCounselorRepository;
import com.jobmoa.hopefulreturn.coursedailystaff.exception.AssignConflictException;
import com.jobmoa.hopefulreturn.coursedailystaff.exception.AssignOnUnavailableDateException;
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
    @Mock
    private CourseDailyCounselorRepository courseDailyCounselorRepository;

    @InjectMocks
    private CourseDailyStaffServiceImpl service;

    private UsersEntity user(Long id, String name) {
        return UsersEntity.builder().userId(id).name(name).deleted(false).build();
    }

    private UsersEntity deletedUser(Long id, String name) {
        return UsersEntity.builder().userId(id).name(name).deleted(true).build();
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
                .findByScheduleDateBetweenAndCourseStaffIdIsNull(D1, D2))
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

    @Test
    @DisplayName("배정 목록 조회는 계정이 삭제된 강사(비-PM)의 배정 행을 제외한다")
    void findAll_excludesDeletedLecturer() {
        CourseStaffEntity cs = CourseStaffEntity.builder()
                .courseStaffId(8L).courseId(COURSE_ID).userId(6L)
                .staffRole(StaffRole.LECTURER).sessionType(SessionType.AM).build();
        when(courseStaffRepository.findByCourseId(COURSE_ID)).thenReturn(List.of(cs));
        StaffScheduleEntity schedule = StaffScheduleEntity.builder()
                .staffScheduleId(50L).userId(6L).scheduleDate(D1).sessionType(SessionType.AM)
                .isAvailable(true).courseStaffId(8L).user(deletedUser(6L, "이강사")).courseStaff(cs).build();
        when(staffScheduleRepository.findByCourseStaffIdIn(anyList())).thenReturn(List.of(schedule));

        CourseDailyStaffListResponse response = service.findAll(COURSE_ID);

        assertThat(response.assignments()).isEmpty();
    }

    @Test
    @DisplayName("배정 목록 조회는 계정이 삭제된 PM 합성을 제외한다")
    void findAll_excludesDeletedPm() {
        CourseStaffEntity pm = CourseStaffEntity.builder()
                .courseStaffId(200L).courseId(COURSE_ID).userId(50L)
                .staffRole(StaffRole.PROJECT_MANAGER).sessionType(SessionType.FULL).build();
        when(courseStaffRepository.findByCourseId(COURSE_ID)).thenReturn(List.of(pm));
        CourseEntity course = new CourseEntity();
        course.setDay1Date(D1);
        course.setDay2Date(D2);
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        when(usersRepository.findById(50L)).thenReturn(Optional.of(deletedUser(50L, "박문순")));

        CourseDailyStaffListResponse response = service.findAll(COURSE_ID);

        assertThat(response.assignments()).isEmpty();
    }

    @Test
    @DisplayName("배정 목록 조회는 계정이 삭제된 상담사(course_daily_counselor)를 제외한다")
    void findAll_excludesDeletedCounselor() {
        when(courseStaffRepository.findByCourseId(COURSE_ID)).thenReturn(List.of());
        CourseStaffEntity cs = CourseStaffEntity.builder()
                .courseStaffId(300L).courseId(COURSE_ID).userId(7L)
                .staffRole(StaffRole.COUNSELOR).sessionType(SessionType.FULL)
                .user(deletedUser(7L, "김상담")).build();
        CourseDailyCounselorEntity cdc = CourseDailyCounselorEntity.builder()
                .courseDailyCounselorId(1L).courseStaffId(300L).scheduleDate(D1).courseStaff(cs).build();
        when(courseDailyCounselorRepository.findByCourseId(COURSE_ID)).thenReturn(List.of(cdc));

        CourseDailyStaffListResponse response = service.findAll(COURSE_ID);

        assertThat(response.assignments()).isEmpty();
    }

    // 타 회차 배정 행(배정: course_staff_id NOT NULL, course fetch 포함) 목킹용
    private StaffScheduleEntity assignedRow(Long userId, LocalDate date, SessionType session,
                                            Long otherCourseId, StaffRole role, String courseName) {
        CourseEntity c = new CourseEntity();
        c.setCourseId(otherCourseId);
        c.setCourseName(courseName);
        c.setStatus(CourseStatus.OPEN);
        CourseStaffEntity cs = CourseStaffEntity.builder()
                .courseStaffId(500L).courseId(otherCourseId).userId(userId)
                .staffRole(role).sessionType(session).course(c).build();
        return StaffScheduleEntity.builder()
                .staffScheduleId(700L).userId(userId).scheduleDate(date).sessionType(session)
                .isAvailable(true).courseStaffId(500L).courseStaff(cs).build();
    }

    @Test
    @DisplayName("후보 조회는 폐강 아닌 다른 회차 배정을 busy로 표시한다(날짜·세션·회차·역할 포함)")
    void findCandidates_marksBusyFromOtherActiveCourse() {
        CourseEntity course = new CourseEntity();
        course.setDay1Date(D1);
        course.setDay2Date(D2);
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        when(staffScheduleRepository
                .findByScheduleDateBetweenAndCourseStaffIdIsNull(D1, D2))
                .thenReturn(List.of());
        when(userRoleRepository.findAll()).thenReturn(List.of(userRole(6L, RoleName.LECTURER)));
        when(usersRepository.findAllById(anyList())).thenReturn(List.of(user(6L, "이강사")));
        when(staffScheduleRepository.findOtherActiveAssignments(
                anyList(), eq(D1), eq(D2), eq(COURSE_ID), eq(CourseStatus.CANCELED)))
                .thenReturn(List.of(assignedRow(6L, D1, SessionType.AM, 99L, StaffRole.LECTURER, "타회차5기")));

        CourseDailyStaffCandidateResponse response = service.findCandidates(COURSE_ID);

        CourseDailyStaffCandidateResponse.Candidate c = response.candidates().stream()
                .filter(x -> x.userId().equals(6L)).findFirst().orElseThrow();
        assertThat(c.busy()).hasSize(1);
        CourseDailyStaffCandidateResponse.Candidate.Busy b = c.busy().get(0);
        assertThat(b.scheduleDate()).isEqualTo(D1);
        assertThat(b.sessionType()).isEqualTo("AM");
        assertThat(b.courseId()).isEqualTo(99L);
        assertThat(b.courseName()).isEqualTo("타회차5기");
        assertThat(b.staffRole()).isEqualTo("LECTURER");
    }

    @Test
    @DisplayName("저장 시 미확인 상태에서 타 활성회차 중복이면 ASSIGN_CONFLICT 예외(저장 안 함)")
    void save_conflict_throwsWhenNotConfirmed() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(new CourseEntity()));
        when(usersRepository.findAllById(anyList())).thenReturn(List.of(user(6L, "이강사")));
        when(courseStaffRepository.findByCourseId(COURSE_ID)).thenReturn(List.of());
        when(staffScheduleRepository.findOtherActiveAssignments(
                anyList(), eq(D1), eq(D1), eq(COURSE_ID), eq(CourseStatus.CANCELED)))
                .thenReturn(List.of(assignedRow(6L, D1, SessionType.AM, 99L, StaffRole.LECTURER, "타회차5기")));

        assertThatThrownBy(() -> service.save(new SaveCourseDailyStaffRequest(
                COURSE_ID, List.of(new SaveCourseDailyStaffRequest.Entry(D1, "LECTURER", "AM", 6L)))))
                .isInstanceOf(AssignConflictException.class);
        verify(staffScheduleRepository, never()).save(any());
    }

    @Test
    @DisplayName("저장 시 confirmConflicts=true면 충돌 검사 없이 현재 회차로 이동 저장한다")
    void save_conflict_proceedsWhenConfirmed() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(new CourseEntity()));
        when(usersRepository.findAllById(anyList())).thenReturn(List.of(user(6L, "이강사")));
        when(courseStaffRepository.findByCourseId(COURSE_ID)).thenReturn(List.of());
        when(courseStaffRepository.save(any(CourseStaffEntity.class))).thenAnswer(inv -> {
            CourseStaffEntity cs = inv.getArgument(0);
            cs.setCourseStaffId(100L);
            return cs;
        });
        when(staffScheduleRepository.findByUserIdAndScheduleDateAndSessionType(6L, D1, SessionType.AM))
                .thenReturn(Optional.empty());
        when(staffScheduleRepository.save(any(StaffScheduleEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        SaveCourseDailyStaffResponse response = service.save(new SaveCourseDailyStaffRequest(
                COURSE_ID, List.of(new SaveCourseDailyStaffRequest.Entry(D1, "LECTURER", "AM", 6L)), true));

        assertThat(response.saved()).isEqualTo(1);
        verify(staffScheduleRepository, never())
                .findOtherActiveAssignments(anyList(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("강사 AM/PM은 같은 날 다른 회차와 세션이 겹치지 않아 충돌이 아니다")
    void save_lecturerDifferentSession_noConflict() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(new CourseEntity()));
        when(usersRepository.findAllById(anyList())).thenReturn(List.of(user(6L, "이강사")));
        when(courseStaffRepository.findByCourseId(COURSE_ID)).thenReturn(List.of());
        when(courseStaffRepository.save(any(CourseStaffEntity.class))).thenAnswer(inv -> {
            CourseStaffEntity cs = inv.getArgument(0);
            cs.setCourseStaffId(100L);
            return cs;
        });
        when(staffScheduleRepository.findByUserIdAndScheduleDateAndSessionType(6L, D1, SessionType.PM))
                .thenReturn(Optional.empty());
        when(staffScheduleRepository.save(any(StaffScheduleEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        // 타 회차엔 같은 날 AM 배정 → PM 요청과 세션 겹치지 않음
        when(staffScheduleRepository.findOtherActiveAssignments(
                anyList(), eq(D1), eq(D1), eq(COURSE_ID), eq(CourseStatus.CANCELED)))
                .thenReturn(List.of(assignedRow(6L, D1, SessionType.AM, 99L, StaffRole.LECTURER, "타회차")));

        SaveCourseDailyStaffResponse response = service.save(new SaveCourseDailyStaffRequest(
                COURSE_ID, List.of(new SaveCourseDailyStaffRequest.Entry(D1, "LECTURER", "PM", 6L))));

        assertThat(response.saved()).isEqualTo(1);
        verify(staffScheduleRepository).save(any());
    }

    @Test
    @DisplayName("PM 저장은 course_staff 단위로만 저장하고 staff_schedule은 쓰지 않는다")
    void save_pm_storesCourseStaffOnly() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(new CourseEntity()));
        when(usersRepository.findAllById(anyList())).thenReturn(List.of(user(50L, "박문순")));
        when(courseStaffRepository.findByCourseId(COURSE_ID)).thenReturn(List.of());
        when(courseStaffRepository.save(any(CourseStaffEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        SaveCourseDailyStaffResponse response = service.save(new SaveCourseDailyStaffRequest(
                COURSE_ID, List.of(
                new SaveCourseDailyStaffRequest.Entry(D1, "PROJECT_MANAGER", "FULL", 50L),
                new SaveCourseDailyStaffRequest.Entry(D2, "PROJECT_MANAGER", "FULL", 50L))));

        assertThat(response.saved()).isEqualTo(1); // PM 인력 1명
        verify(staffScheduleRepository, never()).save(any());
        ArgumentCaptor<CourseStaffEntity> captor = ArgumentCaptor.forClass(CourseStaffEntity.class);
        verify(courseStaffRepository).save(captor.capture());
        assertThat(captor.getValue().getStaffRole()).isEqualTo(StaffRole.PROJECT_MANAGER);
        assertThat(captor.getValue().getSessionType()).isEqualTo(SessionType.FULL);
        assertThat(captor.getValue().getUserId()).isEqualTo(50L);
    }

    @Test
    @DisplayName("배정 조회는 PM(course_staff)을 회차 전 교육일에 합성해 반환한다")
    void findAll_synthesizesPmOnAllDates() {
        CourseStaffEntity pm = CourseStaffEntity.builder()
                .courseStaffId(200L).courseId(COURSE_ID).userId(50L)
                .staffRole(StaffRole.PROJECT_MANAGER).sessionType(SessionType.FULL).build();
        when(courseStaffRepository.findByCourseId(COURSE_ID)).thenReturn(List.of(pm));
        CourseEntity course = new CourseEntity();
        course.setDay1Date(D1);
        course.setDay2Date(D2);
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        when(usersRepository.findById(50L)).thenReturn(Optional.of(user(50L, "박문순")));

        CourseDailyStaffListResponse response = service.findAll(COURSE_ID);

        assertThat(response.assignments()).hasSize(2);
        assertThat(response.assignments()).allSatisfy(it -> {
            assertThat(it.staffRole()).isEqualTo("PROJECT_MANAGER");
            assertThat(it.sessionType()).isEqualTo("FULL");
            assertThat(it.userId()).isEqualTo(50L);
            assertThat(it.name()).isEqualTo("박문순");
        });
        assertThat(response.assignments()).extracting("scheduleDate").containsExactlyInAnyOrder(D1, D2);
        verify(staffScheduleRepository, never()).findByCourseStaffIdIn(anyList());
    }

    // 근무 불가일(course_staff_id NULL·is_available=false) 목킹용
    private StaffScheduleEntity unavailableRow(Long userId, LocalDate date, SessionType session) {
        return sessionRow(userId, date, session, false);
    }

    // 가용/불가 세션 행(course_staff_id NULL) 목킹용 — is_available 지정
    private StaffScheduleEntity sessionRow(Long userId, LocalDate date, SessionType session,
                                           boolean isAvailable) {
        return StaffScheduleEntity.builder()
                .userId(userId).scheduleDate(date).sessionType(session).isAvailable(isAvailable).build();
    }

    @Test
    @DisplayName("불가일(AM)에 AM 배정 저장 시 ASSIGN_ON_UNAVAILABLE_DATE로 거부하고 저장하지 않는다")
    void save_unavailableDate_throwsAndDoesNotSave() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(new CourseEntity()));
        when(usersRepository.findAllById(anyList())).thenReturn(List.of(user(6L, "이강사")));
        when(courseStaffRepository.findByCourseId(COURSE_ID)).thenReturn(List.of());
        when(staffScheduleRepository
                .findByScheduleDateBetweenAndCourseStaffIdIsNull(D1, D1))
                .thenReturn(List.of(unavailableRow(6L, D1, SessionType.AM)));

        assertThatThrownBy(() -> service.save(new SaveCourseDailyStaffRequest(
                COURSE_ID, List.of(new SaveCourseDailyStaffRequest.Entry(D1, "LECTURER", "AM", 6L)))))
                .isInstanceOf(AssignOnUnavailableDateException.class);
        verify(staffScheduleRepository, never()).save(any());
    }

    @Test
    @DisplayName("FULL 배정은 AM만 불가여도 세션이 겹쳐 거부된다(ASSIGN_ON_UNAVAILABLE_DATE)")
    void save_fullBlockedWhenHalfUnavailable() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(new CourseEntity()));
        when(usersRepository.findAllById(anyList())).thenReturn(List.of(user(6L, "이강사")));
        when(courseStaffRepository.findByCourseId(COURSE_ID)).thenReturn(List.of());
        when(staffScheduleRepository
                .findByScheduleDateBetweenAndCourseStaffIdIsNull(D1, D1))
                .thenReturn(List.of(unavailableRow(6L, D1, SessionType.AM)));

        assertThatThrownBy(() -> service.save(new SaveCourseDailyStaffRequest(
                COURSE_ID, List.of(new SaveCourseDailyStaffRequest.Entry(D1, "LECTURER", "FULL", 6L)))))
                .isInstanceOf(AssignOnUnavailableDateException.class);
        verify(staffScheduleRepository, never()).save(any());
    }

    @Test
    @DisplayName("AM 불가여도 같은 날 PM 배정은 세션이 겹치지 않아 정상 저장된다")
    void save_pmSessionAllowedWhenOtherHalfUnavailable() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(new CourseEntity()));
        when(usersRepository.findAllById(anyList())).thenReturn(List.of(user(6L, "이강사")));
        when(courseStaffRepository.findByCourseId(COURSE_ID)).thenReturn(List.of());
        when(staffScheduleRepository
                .findByScheduleDateBetweenAndCourseStaffIdIsNull(D1, D1))
                .thenReturn(List.of(unavailableRow(6L, D1, SessionType.AM)));
        when(courseStaffRepository.save(any(CourseStaffEntity.class))).thenAnswer(inv -> {
            CourseStaffEntity cs = inv.getArgument(0);
            cs.setCourseStaffId(100L);
            return cs;
        });
        when(staffScheduleRepository.findByUserIdAndScheduleDateAndSessionType(6L, D1, SessionType.PM))
                .thenReturn(Optional.empty());
        when(staffScheduleRepository.save(any(StaffScheduleEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        SaveCourseDailyStaffResponse response = service.save(new SaveCourseDailyStaffRequest(
                COURSE_ID, List.of(new SaveCourseDailyStaffRequest.Entry(D1, "LECTURER", "PM", 6L))));

        assertThat(response.saved()).isEqualTo(1);
        verify(staffScheduleRepository).save(any());
    }

    @Test
    @DisplayName("AM만 불가로 등록한 강사는 그 날 PM 후보로 노출되고 AM에서는 제외된다(availability=PM)")
    void findCandidates_sessionAware_amUnavailableShowsPmOnly() {
        CourseEntity course = new CourseEntity();
        course.setDay1Date(D1);
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        when(staffScheduleRepository
                .findByScheduleDateBetweenAndCourseStaffIdIsNull(D1, D1))
                .thenReturn(List.of(unavailableRow(6L, D1, SessionType.AM)));
        when(userRoleRepository.findAll()).thenReturn(List.of(userRole(6L, RoleName.LECTURER)));
        when(usersRepository.findAllById(anyList())).thenReturn(List.of(user(6L, "이강사")));

        CourseDailyStaffCandidateResponse response = service.findCandidates(COURSE_ID);

        CourseDailyStaffCandidateResponse.Candidate lecturer = response.candidates().stream()
                .filter(c -> c.userId().equals(6L)).findFirst().orElseThrow();
        assertThat(lecturer.availability()).hasSize(1);
        assertThat(lecturer.availability().get(0).scheduleDate()).isEqualTo(D1);
        assertThat(lecturer.availability().get(0).sessionType()).isEqualTo("PM");
    }

    @Test
    @DisplayName("종일(FULL) 불가 + 오후(PM) 가능 행이 공존하면 그 인력은 오후 후보로 노출된다(availability=PM)")
    void findCandidates_fullUnavailableButPmAvailable_showsPmCandidate() {
        CourseEntity course = new CourseEntity();
        course.setDay1Date(D1);
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        // FULL 불가 + PM 가능 — 구체 세션(PM)이 FULL 을 그 세션에 한해 override
        when(staffScheduleRepository
                .findByScheduleDateBetweenAndCourseStaffIdIsNull(D1, D1))
                .thenReturn(List.of(
                        sessionRow(6L, D1, SessionType.FULL, false),
                        sessionRow(6L, D1, SessionType.PM, true)));
        when(userRoleRepository.findAll()).thenReturn(List.of(userRole(6L, RoleName.LECTURER)));
        when(usersRepository.findAllById(anyList())).thenReturn(List.of(user(6L, "이강사")));

        CourseDailyStaffCandidateResponse response = service.findCandidates(COURSE_ID);

        CourseDailyStaffCandidateResponse.Candidate lecturer = response.candidates().stream()
                .filter(c -> c.userId().equals(6L)).findFirst().orElseThrow();
        assertThat(lecturer.availability()).hasSize(1);
        assertThat(lecturer.availability().get(0).scheduleDate()).isEqualTo(D1);
        assertThat(lecturer.availability().get(0).sessionType()).isEqualTo("PM");
    }

    @Test
    @DisplayName("종일(FULL) 불가만 있으면(세션 override 없음) 그 날 후보 가용일이 전혀 없다")
    void findCandidates_fullUnavailableOnly_showsNoAvailability() {
        CourseEntity course = new CourseEntity();
        course.setDay1Date(D1);
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        when(staffScheduleRepository
                .findByScheduleDateBetweenAndCourseStaffIdIsNull(D1, D1))
                .thenReturn(List.of(sessionRow(6L, D1, SessionType.FULL, false)));
        when(userRoleRepository.findAll()).thenReturn(List.of(userRole(6L, RoleName.LECTURER)));
        when(usersRepository.findAllById(anyList())).thenReturn(List.of(user(6L, "이강사")));

        CourseDailyStaffCandidateResponse response = service.findCandidates(COURSE_ID);

        CourseDailyStaffCandidateResponse.Candidate lecturer = response.candidates().stream()
                .filter(c -> c.userId().equals(6L)).findFirst().orElseThrow();
        assertThat(lecturer.availability()).isEmpty();
    }

    @Test
    @DisplayName("종일(FULL) 불가여도 오후(PM) 가능 행이 있으면 PM 배정 저장이 하드블록되지 않는다")
    void save_pmAllowedWhenFullUnavailableButPmAvailable() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(new CourseEntity()));
        when(usersRepository.findAllById(anyList())).thenReturn(List.of(user(6L, "이강사")));
        when(courseStaffRepository.findByCourseId(COURSE_ID)).thenReturn(List.of());
        when(staffScheduleRepository
                .findByScheduleDateBetweenAndCourseStaffIdIsNull(D1, D1))
                .thenReturn(List.of(
                        sessionRow(6L, D1, SessionType.FULL, false),
                        sessionRow(6L, D1, SessionType.PM, true)));
        when(courseStaffRepository.save(any(CourseStaffEntity.class))).thenAnswer(inv -> {
            CourseStaffEntity cs = inv.getArgument(0);
            cs.setCourseStaffId(100L);
            return cs;
        });
        when(staffScheduleRepository.findByUserIdAndScheduleDateAndSessionType(6L, D1, SessionType.PM))
                .thenReturn(Optional.empty());
        when(staffScheduleRepository.save(any(StaffScheduleEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        SaveCourseDailyStaffResponse response = service.save(new SaveCourseDailyStaffRequest(
                COURSE_ID, List.of(new SaveCourseDailyStaffRequest.Entry(D1, "LECTURER", "PM", 6L))));

        assertThat(response.saved()).isEqualTo(1);
        verify(staffScheduleRepository).save(any());
    }

    @Test
    @DisplayName("가용일 기등록(available) 행이 있으면 신규 INSERT 없이 기존 행을 UPDATE해 배정한다")
    void save_upsertsExistingAvailabilityRow() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(new CourseEntity()));
        when(usersRepository.findAllById(anyList())).thenReturn(List.of(user(6L, "이강사")));
        when(courseStaffRepository.findByCourseId(COURSE_ID)).thenReturn(List.of());
        when(courseStaffRepository.save(any(CourseStaffEntity.class))).thenAnswer(inv -> {
            CourseStaffEntity cs = inv.getArgument(0);
            cs.setCourseStaffId(100L);
            return cs;
        });
        // 사용자가 등록한 가용일 행(course_staff_id NULL·is_available=true)
        StaffScheduleEntity existing = StaffScheduleEntity.builder()
                .staffScheduleId(77L).userId(6L).scheduleDate(D1).sessionType(SessionType.AM)
                .isAvailable(true).build();
        when(staffScheduleRepository.findByUserIdAndScheduleDateAndSessionType(6L, D1, SessionType.AM))
                .thenReturn(Optional.of(existing));
        when(staffScheduleRepository.save(any(StaffScheduleEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        service.save(new SaveCourseDailyStaffRequest(
                COURSE_ID, List.of(new SaveCourseDailyStaffRequest.Entry(D1, "LECTURER", "AM", 6L))));

        ArgumentCaptor<StaffScheduleEntity> captor = ArgumentCaptor.forClass(StaffScheduleEntity.class);
        verify(staffScheduleRepository).save(captor.capture());
        StaffScheduleEntity saved = captor.getValue();
        assertThat(saved.getStaffScheduleId()).isEqualTo(77L);   // 기존 행 재사용(신규 아님)
        assertThat(saved.getCourseStaffId()).isEqualTo(100L);     // 배정 연결
        assertThat(saved.getIsAvailable()).isTrue();              // 가용 유지
    }

    // ── 상담사 다중 회차 배정 ───────────────────────────────────────────────────

    private CourseStaffEntity counselorRoster(Long courseStaffId, Long userId, String name) {
        return CourseStaffEntity.builder()
                .courseStaffId(courseStaffId).courseId(COURSE_ID).userId(userId)
                .staffRole(StaffRole.COUNSELOR).sessionType(SessionType.FULL)
                .user(user(userId, name)).build();
    }

    @Test
    @DisplayName("상담사 배정은 staff_schedule 이 아니라 course_daily_counselor 에 저장한다")
    void save_counselor_storedInCounselorTable() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(new CourseEntity()));
        when(usersRepository.findAllById(anyList())).thenReturn(List.of(user(7L, "김상담")));
        when(courseStaffRepository.findByCourseId(COURSE_ID)).thenReturn(List.of());
        when(courseStaffRepository.save(any(CourseStaffEntity.class))).thenAnswer(inv -> {
            CourseStaffEntity cs = inv.getArgument(0);
            cs.setCourseStaffId(300L);
            return cs;
        });
        when(courseDailyCounselorRepository.save(any(CourseDailyCounselorEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        SaveCourseDailyStaffResponse response = service.save(new SaveCourseDailyStaffRequest(
                COURSE_ID, List.of(new SaveCourseDailyStaffRequest.Entry(D1, "COUNSELOR", "FULL", 7L))));

        assertThat(response.saved()).isEqualTo(1);
        verify(staffScheduleRepository, never()).save(any());
        ArgumentCaptor<CourseDailyCounselorEntity> captor =
                ArgumentCaptor.forClass(CourseDailyCounselorEntity.class);
        verify(courseDailyCounselorRepository).save(captor.capture());
        assertThat(captor.getValue().getCourseStaffId()).isEqualTo(300L);
        assertThat(captor.getValue().getScheduleDate()).isEqualTo(D1);
    }

    @Test
    @DisplayName("상담사는 타 회차 중복 검사(detectConflicts) 대상이 아니어서 같은 날 다른 회차 배정이 허용된다")
    void save_counselor_notConflictChecked() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(new CourseEntity()));
        when(usersRepository.findAllById(anyList())).thenReturn(List.of(user(7L, "김상담")));
        when(courseStaffRepository.findByCourseId(COURSE_ID)).thenReturn(List.of());
        when(courseStaffRepository.save(any(CourseStaffEntity.class))).thenAnswer(inv -> {
            CourseStaffEntity cs = inv.getArgument(0);
            cs.setCourseStaffId(300L);
            return cs;
        });
        when(courseDailyCounselorRepository.save(any(CourseDailyCounselorEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        SaveCourseDailyStaffResponse response = service.save(new SaveCourseDailyStaffRequest(
                COURSE_ID, List.of(new SaveCourseDailyStaffRequest.Entry(D1, "COUNSELOR", "FULL", 7L))));

        assertThat(response.saved()).isEqualTo(1);
        // 상담사만 있는 저장에선 타 회차 중복 조회 자체가 일어나지 않는다.
        verify(staffScheduleRepository, never())
                .findOtherActiveAssignments(anyList(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("상담사도 본인 근무 불가일에는 배정할 수 없다(ASSIGN_ON_UNAVAILABLE_DATE)")
    void save_counselor_unavailableStillBlocks() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(new CourseEntity()));
        when(usersRepository.findAllById(anyList())).thenReturn(List.of(user(7L, "김상담")));
        when(courseStaffRepository.findByCourseId(COURSE_ID)).thenReturn(List.of());
        when(staffScheduleRepository
                .findByScheduleDateBetweenAndCourseStaffIdIsNull(D1, D1))
                .thenReturn(List.of(unavailableRow(7L, D1, SessionType.FULL)));

        assertThatThrownBy(() -> service.save(new SaveCourseDailyStaffRequest(
                COURSE_ID, List.of(new SaveCourseDailyStaffRequest.Entry(D1, "COUNSELOR", "FULL", 7L)))))
                .isInstanceOf(AssignOnUnavailableDateException.class);
        verify(courseDailyCounselorRepository, never()).save(any());
    }

    @Test
    @DisplayName("배정 목록 조회는 상담사를 course_daily_counselor 에서 복원한다")
    void findAll_reconstructsCounselorFromDailyTable() {
        when(courseStaffRepository.findByCourseId(COURSE_ID)).thenReturn(List.of());
        CourseStaffEntity cs = counselorRoster(300L, 7L, "김상담");
        CourseDailyCounselorEntity cdc = CourseDailyCounselorEntity.builder()
                .courseDailyCounselorId(1L).courseStaffId(300L).scheduleDate(D1).courseStaff(cs).build();
        when(courseDailyCounselorRepository.findByCourseId(COURSE_ID)).thenReturn(List.of(cdc));

        CourseDailyStaffListResponse response = service.findAll(COURSE_ID);

        assertThat(response.assignments()).hasSize(1);
        CourseDailyStaffListResponse.Item item = response.assignments().get(0);
        assertThat(item.staffRole()).isEqualTo("COUNSELOR");
        assertThat(item.sessionType()).isEqualTo("FULL");
        assertThat(item.scheduleDate()).isEqualTo(D1);
        assertThat(item.userId()).isEqualTo(7L);
        assertThat(item.name()).isEqualTo("김상담");
    }

    @Test
    @DisplayName("상담사 저장 시 요청에 없는 기존 상담사 로스터 행은 삭제한다(로스터 정합)")
    void save_counselor_removesStaleRoster() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(new CourseEntity()));
        when(usersRepository.findAllById(anyList())).thenReturn(List.of(user(7L, "김상담")));
        // 기존 상담사 로스터엔 8L(요청에 없음) → 삭제 대상
        when(courseStaffRepository.findByCourseId(COURSE_ID))
                .thenReturn(List.of(counselorRoster(800L, 8L, "이상담")));
        when(courseStaffRepository.save(any(CourseStaffEntity.class))).thenAnswer(inv -> {
            CourseStaffEntity cs = inv.getArgument(0);
            cs.setCourseStaffId(300L);
            return cs;
        });
        when(courseDailyCounselorRepository.save(any(CourseDailyCounselorEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.save(new SaveCourseDailyStaffRequest(
                COURSE_ID, List.of(new SaveCourseDailyStaffRequest.Entry(D1, "COUNSELOR", "FULL", 7L))));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CourseStaffEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(courseStaffRepository).deleteAll(captor.capture());
        assertThat(captor.getValue()).extracting(CourseStaffEntity::getCourseStaffId).containsExactly(800L);
    }
}
