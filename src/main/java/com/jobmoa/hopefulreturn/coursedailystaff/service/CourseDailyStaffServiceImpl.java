package com.jobmoa.hopefulreturn.coursedailystaff.service;

import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.course.entity.CourseEntity;
import com.jobmoa.hopefulreturn.course.repository.CourseRepository;
import com.jobmoa.hopefulreturn.coursedailystaff.model.dto.CourseDailyStaffCandidateResponse;
import com.jobmoa.hopefulreturn.coursedailystaff.model.dto.CourseDailyStaffCandidateResponse.Candidate;
import com.jobmoa.hopefulreturn.coursedailystaff.model.dto.CourseDailyStaffCandidateResponse.Candidate.Availability;
import com.jobmoa.hopefulreturn.coursedailystaff.model.dto.CourseDailyStaffListResponse;
import com.jobmoa.hopefulreturn.coursedailystaff.model.dto.SaveCourseDailyStaffRequest;
import com.jobmoa.hopefulreturn.coursedailystaff.model.dto.SaveCourseDailyStaffResponse;
import com.jobmoa.hopefulreturn.coursestaff.entity.CourseStaffEntity;
import com.jobmoa.hopefulreturn.coursestaff.entity.SessionType;
import com.jobmoa.hopefulreturn.coursestaff.entity.StaffRole;
import com.jobmoa.hopefulreturn.coursestaff.repository.CourseStaffRepository;
import com.jobmoa.hopefulreturn.role.entity.RoleName;
import com.jobmoa.hopefulreturn.staffschedule.entity.StaffScheduleEntity;
import com.jobmoa.hopefulreturn.staffschedule.repository.StaffScheduleRepository;
import com.jobmoa.hopefulreturn.userrole.entity.UserRoleEntity;
import com.jobmoa.hopefulreturn.userrole.repository.UserRoleRepository;
import com.jobmoa.hopefulreturn.users.entity.UsersEntity;
import com.jobmoa.hopefulreturn.users.repository.UsersRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회차 날짜별 인력 배정. 배정 행을 course_staff(회차·역할·직원)에 연결한 staff_schedule 로 표현한다.
 *   - 저장: course_staff 로스터 확보 → staff_schedule 에 course_staff_id·is_available=true 로 upsert.
 *   - 조회: 회차 course_staff id 에 연결된 staff_schedule 행을 join 해 그리드 복원.
 *   - 후보: 역할 자격자 중 해당 날짜 근무 불가일(course_staff_id NULL·is_available=false)이 없는 사람.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CourseDailyStaffServiceImpl implements CourseDailyStaffService {

    private final CourseStaffRepository courseStaffRepository;
    private final CourseRepository courseRepository;
    private final UsersRepository usersRepository;
    private final StaffScheduleRepository staffScheduleRepository;
    private final UserRoleRepository userRoleRepository;

    @Override
    @Transactional(readOnly = true)
    public CourseDailyStaffListResponse findAll(Long courseId) {
        List<Long> courseStaffIds = courseStaffRepository.findByCourseId(courseId).stream()
                .map(CourseStaffEntity::getCourseStaffId)
                .toList();
        if (courseStaffIds.isEmpty()) {
            return new CourseDailyStaffListResponse(courseId, List.of());
        }
        List<CourseDailyStaffListResponse.Item> assignments = staffScheduleRepository
                .findByCourseStaffIdIn(courseStaffIds).stream()
                .sorted(Comparator.comparing(StaffScheduleEntity::getScheduleDate)
                        .thenComparing(StaffScheduleEntity::getStaffScheduleId))
                .map(this::toListItem)
                .toList();
        return new CourseDailyStaffListResponse(courseId, assignments);
    }

    @Override
    public SaveCourseDailyStaffResponse save(SaveCourseDailyStaffRequest request) {
        courseRepository.findById(request.courseId())
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));

        // 배정 대상 인력 존재 검증(삭제되지 않은 사용자만)
        List<Long> userIds = request.entries().stream()
                .map(SaveCourseDailyStaffRequest.Entry::userId)
                .distinct()
                .toList();
        if (!userIds.isEmpty()) {
            long found = usersRepository.findAllById(userIds).stream()
                    .filter(user -> !Boolean.TRUE.equals(user.getDeleted()))
                    .count();
            if (found != userIds.size()) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND);
            }
        }

        LocalDateTime now = LocalDateTime.now();

        // 1) 회차 course_staff 로스터 로드 (userId|role|session → entity)
        List<CourseStaffEntity> roster = courseStaffRepository.findByCourseId(request.courseId());
        Map<String, CourseStaffEntity> rosterMap = new HashMap<>();
        for (CourseStaffEntity cs : roster) {
            rosterMap.put(rosterKey(cs.getUserId(), cs.getStaffRole(), cs.getSessionType()), cs);
        }

        // 2) 이 회차의 기존 배정 행을 모두 배정 해제(course_staff_id NULL) — 이후 새 그리드로 재설정
        List<Long> courseStaffIds = roster.stream().map(CourseStaffEntity::getCourseStaffId).toList();
        if (!courseStaffIds.isEmpty()) {
            List<StaffScheduleEntity> existing = staffScheduleRepository.findByCourseStaffIdIn(courseStaffIds);
            for (StaffScheduleEntity row : existing) {
                row.setCourseStaffId(null);
                row.setUpdatedAt(now);
            }
            staffScheduleRepository.saveAll(existing);
        }

        // 3) 각 entry: 로스터 확보 + staff_schedule upsert(배정)
        Set<String> seen = new LinkedHashSet<>();
        int saved = 0;
        for (SaveCourseDailyStaffRequest.Entry entry : request.entries()) {
            StaffRole role = parseStaffRole(entry.staffRole());
            SessionType session = parseSessionType(entry.sessionType());
            // staff_schedule UNIQUE(user_id, schedule_date, session_type) 기준 셀 중복 제거
            String cellKey = entry.userId() + "|" + entry.scheduleDate() + "|" + session;
            if (!seen.add(cellKey)) {
                continue;
            }

            // course_staff 로스터 확보(없으면 생성)
            String rKey = rosterKey(entry.userId(), role, session);
            CourseStaffEntity cs = rosterMap.get(rKey);
            if (cs == null) {
                cs = courseStaffRepository.save(CourseStaffEntity.builder()
                        .courseId(request.courseId())
                        .userId(entry.userId())
                        .staffRole(role)
                        .sessionType(session)
                        .createdAt(now)
                        .build());
                rosterMap.put(rKey, cs);
            }

            // staff_schedule upsert(배정 행): course_staff_id 부착 + is_available=true
            StaffScheduleEntity row = staffScheduleRepository
                    .findByUserIdAndScheduleDateAndSessionType(entry.userId(), entry.scheduleDate(), session)
                    .orElse(null);
            if (row == null) {
                row = StaffScheduleEntity.builder()
                        .userId(entry.userId())
                        .scheduleDate(entry.scheduleDate())
                        .sessionType(session)
                        .isAvailable(true)
                        .courseStaffId(cs.getCourseStaffId())
                        .createdAt(now)
                        .build();
            } else {
                row.setCourseStaffId(cs.getCourseStaffId());
                row.setIsAvailable(true);
                row.setUpdatedAt(now);
            }
            staffScheduleRepository.save(row);
            saved++;
        }

        return new SaveCourseDailyStaffResponse(request.courseId(), saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseDailyStaffCandidateResponse findCandidates(Long courseId) {
        CourseEntity course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));

        List<LocalDate> dates = Stream.of(
                        course.getDay1Date(), course.getDay2Date(), course.getDay3Date(),
                        course.getDay4Date(), course.getDay5Date())
                .filter(date -> date != null)
                .distinct()
                .sorted()
                .toList();
        if (dates.isEmpty()) {
            return new CourseDailyStaffCandidateResponse(courseId, List.of(), List.of());
        }

        LocalDate from = dates.get(0);
        LocalDate to = dates.get(dates.size() - 1);
        Set<LocalDate> dateSet = new LinkedHashSet<>(dates);

        // 근무 불가일: userId → 불가 날짜 집합(회차 교육일 범위)
        Map<Long, Set<LocalDate>> unavailableByUser = new HashMap<>();
        for (StaffScheduleEntity row : staffScheduleRepository
                .findByScheduleDateBetweenAndIsAvailableFalseAndCourseStaffIdIsNull(from, to)) {
            if (dateSet.contains(row.getScheduleDate())) {
                unavailableByUser.computeIfAbsent(row.getUserId(), key -> new HashSet<>())
                        .add(row.getScheduleDate());
            }
        }

        // 역할 자격자 풀: 배정 가능 역할(StaffRole)로 매핑되는 user_role
        Map<Long, LinkedHashSet<String>> rolesByUser = new LinkedHashMap<>();
        for (UserRoleEntity userRole : userRoleRepository.findAll()) {
            RoleName roleName = userRole.getRole() == null ? null : userRole.getRole().getRoleName();
            StaffRole staffRole = toAssignableRole(roleName);
            if (staffRole != null) {
                rolesByUser.computeIfAbsent(userRole.getUserId(), key -> new LinkedHashSet<>())
                        .add(staffRole.name());
            }
        }
        if (rolesByUser.isEmpty()) {
            return new CourseDailyStaffCandidateResponse(courseId, dates, List.of());
        }

        List<Long> userIds = new ArrayList<>(rolesByUser.keySet());
        Map<Long, String> nameByUser = new HashMap<>();
        for (UsersEntity user : usersRepository.findAllById(userIds)) {
            if (!Boolean.TRUE.equals(user.getDeleted())) {
                nameByUser.put(user.getUserId(), user.getName());
            }
        }

        List<Candidate> candidates = new ArrayList<>();
        for (Long userId : userIds) {
            String name = nameByUser.get(userId);
            if (name == null) {
                continue;
            }
            Set<LocalDate> unavailable = unavailableByUser.getOrDefault(userId, Set.of());
            List<Availability> availability = dates.stream()
                    .filter(date -> !unavailable.contains(date))
                    .map(date -> new Availability(date, SessionType.FULL.name()))
                    .toList();
            candidates.add(new Candidate(
                    userId,
                    name,
                    new ArrayList<>(rolesByUser.get(userId)),
                    availability));
        }

        return new CourseDailyStaffCandidateResponse(courseId, dates, candidates);
    }

    private CourseDailyStaffListResponse.Item toListItem(StaffScheduleEntity schedule) {
        CourseStaffEntity cs = schedule.getCourseStaff();
        return new CourseDailyStaffListResponse.Item(
                schedule.getStaffScheduleId(),
                schedule.getScheduleDate(),
                cs == null || cs.getStaffRole() == null ? null : cs.getStaffRole().name(),
                schedule.getSessionType() == null ? null : schedule.getSessionType().name(),
                schedule.getUserId(),
                schedule.getUser() == null ? null : schedule.getUser().getName());
    }

    private String rosterKey(Long userId, StaffRole role, SessionType session) {
        return userId + "|" + role + "|" + session;
    }

    /**
     * 시스템 사용자 역할(RoleName)을 회차 배정 역할(StaffRole)로 매핑한다.
     * 행정허브(OPERATOR)는 배정 역할 ADMIN_STAFF 로 노출한다.
     * HEAD_OFFICE·REGIONAL_MANAGER·ADMIN 은 배정 대상이 아니므로 null.
     */
    private StaffRole toAssignableRole(RoleName roleName) {
        if (roleName == null) {
            return null;
        }
        return switch (roleName) {
            case LECTURER -> StaffRole.LECTURER;
            case COUNSELOR -> StaffRole.COUNSELOR;
            case STAFF -> StaffRole.STAFF;
            case PROJECT_MANAGER -> StaffRole.PROJECT_MANAGER;
            case PROJECT_LEADER -> StaffRole.PROJECT_LEADER;
            case OPERATOR -> StaffRole.ADMIN_STAFF;
            default -> null;
        };
    }

    private StaffRole parseStaffRole(String staffRole) {
        try {
            return StaffRole.valueOf(staffRole.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private SessionType parseSessionType(String sessionType) {
        try {
            return SessionType.valueOf(sessionType.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }
}
