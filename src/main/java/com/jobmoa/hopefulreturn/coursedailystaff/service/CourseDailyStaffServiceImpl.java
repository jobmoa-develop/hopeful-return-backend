package com.jobmoa.hopefulreturn.coursedailystaff.service;

import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.course.entity.CourseEntity;
import com.jobmoa.hopefulreturn.course.entity.CourseStatus;
import com.jobmoa.hopefulreturn.course.repository.CourseRepository;
import com.jobmoa.hopefulreturn.coursedailycounselor.entity.CourseDailyCounselorEntity;
import com.jobmoa.hopefulreturn.coursedailycounselor.repository.CourseDailyCounselorRepository;
import com.jobmoa.hopefulreturn.coursedailystaff.exception.AssignConflictException;
import com.jobmoa.hopefulreturn.coursedailystaff.exception.AssignOnUnavailableDateException;
import com.jobmoa.hopefulreturn.coursedailystaff.model.dto.AssignConflict;
import com.jobmoa.hopefulreturn.coursedailystaff.model.dto.AssignUnavailable;
import com.jobmoa.hopefulreturn.coursedailystaff.model.dto.CourseDailyStaffCandidateResponse;
import com.jobmoa.hopefulreturn.coursedailystaff.model.dto.CourseDailyStaffCandidateResponse.Candidate;
import com.jobmoa.hopefulreturn.coursedailystaff.model.dto.CourseDailyStaffCandidateResponse.Candidate.Availability;
import com.jobmoa.hopefulreturn.coursedailystaff.model.dto.CourseDailyStaffCandidateResponse.Candidate.Busy;
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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
 *
 * PM(PROJECT_MANAGER)은 예외다. staff_schedule UNIQUE(user,date,session) 때문에 여러 회차에
 * 같은 날 동시 배정이 불가하므로, PM 은 <b>course_staff 단위(날짜 무관)</b>로만 저장하고 조회 시
 * 모든 교육일에 합성해 내려준다(모든 날짜 고정·여러 회차 중복 허용). 후보/충돌 검사에서도 제외된다.
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
    private final CourseDailyCounselorRepository courseDailyCounselorRepository;

    @Override
    @Transactional(readOnly = true)
    public CourseDailyStaffListResponse findAll(Long courseId) {
        List<CourseStaffEntity> roster = courseStaffRepository.findByCourseId(courseId);
        List<CourseDailyStaffListResponse.Item> assignments = new ArrayList<>();

        // 1) 비-PM·비-상담사 배정: course_staff 에 연결된 staff_schedule 행 복원.
        //    상담사는 staff_schedule 을 쓰지 않고 course_daily_counselor 에서 별도 복원한다(3).
        List<Long> staffScheduleStaffIds = roster.stream()
                .filter(cs -> cs.getStaffRole() != StaffRole.PROJECT_MANAGER
                        && cs.getStaffRole() != StaffRole.COUNSELOR)
                .map(CourseStaffEntity::getCourseStaffId)
                .toList();
        if (!staffScheduleStaffIds.isEmpty()) {
            staffScheduleRepository.findByCourseStaffIdIn(staffScheduleStaffIds).stream()
                    .sorted(Comparator.comparing(StaffScheduleEntity::getScheduleDate)
                            .thenComparing(StaffScheduleEntity::getStaffScheduleId))
                    .map(this::toListItem)
                    .forEach(assignments::add);
        }

        // 3) 상담사 배정: course_daily_counselor 에서 복원(같은 날 다중 회차 허용 저장소)
        for (CourseDailyCounselorEntity cdc : courseDailyCounselorRepository.findByCourseId(courseId)) {
            CourseStaffEntity cs = cdc.getCourseStaff();
            UsersEntity user = cs == null ? null : cs.getUser();
            assignments.add(new CourseDailyStaffListResponse.Item(
                    null, cdc.getScheduleDate(), StaffRole.COUNSELOR.name(), SessionType.FULL.name(),
                    cs == null ? null : cs.getUserId(),
                    user == null ? null : user.getName(),
                    user == null ? null : user.getPhone()));
        }

        // 2) PM 배정(course_staff 단위): 회차 전 교육일에 동일 인력으로 합성
        List<CourseStaffEntity> pmRoster = roster.stream()
                .filter(cs -> cs.getStaffRole() == StaffRole.PROJECT_MANAGER)
                .toList();
        if (!pmRoster.isEmpty()) {
            List<LocalDate> dates = courseRepository.findById(courseId)
                    .map(this::educationDates)
                    .orElse(List.of());
            for (CourseStaffEntity pm : pmRoster) {
                UsersEntity pmUser = usersRepository.findById(pm.getUserId()).orElse(null);
                String name = pmUser == null ? null : pmUser.getName();
                String phone = pmUser == null ? null : pmUser.getPhone();
                for (LocalDate date : dates) {
                    assignments.add(new CourseDailyStaffListResponse.Item(
                            null, date, StaffRole.PROJECT_MANAGER.name(),
                            SessionType.FULL.name(), pm.getUserId(), name, phone));
                }
            }
        }

        return new CourseDailyStaffListResponse(courseId, assignments);
    }

    @Override
    public SaveCourseDailyStaffResponse save(SaveCourseDailyStaffRequest request) {
        courseRepository.findById(request.courseId())
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));

        // 배정 대상 인력 존재 검증(삭제되지 않은 사용자만) + 이름 맵 확보
        List<Long> userIds = request.entries().stream()
                .map(SaveCourseDailyStaffRequest.Entry::userId)
                .distinct()
                .toList();
        Map<Long, String> nameById = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<UsersEntity> found = usersRepository.findAllById(userIds).stream()
                    .filter(user -> !Boolean.TRUE.equals(user.getDeleted()))
                    .toList();
            if (found.size() != userIds.size()) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND);
            }
            found.forEach(user -> nameById.put(user.getUserId(), user.getName()));
        }

        LocalDateTime now = LocalDateTime.now();

        // 회차 course_staff 로스터 로드 후 PM / 상담사 / 그 외(비-PM·비-상담사) 분리
        List<CourseStaffEntity> roster = courseStaffRepository.findByCourseId(request.courseId());
        List<CourseStaffEntity> pmRoster = roster.stream()
                .filter(cs -> cs.getStaffRole() == StaffRole.PROJECT_MANAGER)
                .toList();
        List<CourseStaffEntity> counselorRoster = roster.stream()
                .filter(cs -> cs.getStaffRole() == StaffRole.COUNSELOR)
                .toList();
        List<CourseStaffEntity> otherRoster = roster.stream()
                .filter(cs -> cs.getStaffRole() != StaffRole.PROJECT_MANAGER
                        && cs.getStaffRole() != StaffRole.COUNSELOR)
                .toList();

        // 요청 entry PM / 상담사 / 그 외 분리 (역할 파싱 — 잘못된 역할이면 INVALID_INPUT)
        List<SaveCourseDailyStaffRequest.Entry> pmEntries = new ArrayList<>();
        List<SaveCourseDailyStaffRequest.Entry> counselorEntries = new ArrayList<>();
        List<SaveCourseDailyStaffRequest.Entry> otherEntries = new ArrayList<>();
        for (SaveCourseDailyStaffRequest.Entry entry : request.entries()) {
            StaffRole role = parseStaffRole(entry.staffRole());
            if (role == StaffRole.PROJECT_MANAGER) {
                pmEntries.add(entry);
            } else if (role == StaffRole.COUNSELOR) {
                counselorEntries.add(entry);
            } else {
                otherEntries.add(entry);
            }
        }

        // 타 활성회차 중복 검사(비-PM·비-상담사만). 상담사는 여러 회차 중복 배정을 허용하므로 제외한다
        // (여러 지역 순회 상담). 미확인 + 충돌 시 409 로 충돌 목록 반환.
        if (!Boolean.TRUE.equals(request.confirmConflicts())) {
            List<AssignConflict> conflicts = detectConflicts(otherEntries, request.courseId(), nameById);
            if (!conflicts.isEmpty()) {
                throw new AssignConflictException(conflicts);
            }
        }

        // 근무 불가일 배정 차단(상담사 포함 전 배정). 어떤 변경도 하기 전(wipe 前)에 검증해 위반 시
        // 409 로 거부한다(override 없는 하드 블록). 상담사도 본인 불가일에는 배정할 수 없다.
        List<SaveCourseDailyStaffRequest.Entry> nonPmEntries = new ArrayList<>(otherEntries);
        nonPmEntries.addAll(counselorEntries);
        validateNotUnavailable(nonPmEntries, nameById);

        Map<String, CourseStaffEntity> rosterMap = new HashMap<>();
        for (CourseStaffEntity cs : otherRoster) {
            rosterMap.put(rosterKey(cs.getUserId(), cs.getStaffRole(), cs.getSessionType()), cs);
        }

        // 비-PM·비-상담사 기존 배정 행을 모두 배정 해제(course_staff_id NULL) — 이후 새 그리드로 재설정.
        // (PM·상담사는 staff_schedule 을 쓰지 않으므로 여기서 건드리지 않고 아래에서 별도 교체)
        List<Long> otherStaffIds = otherRoster.stream().map(CourseStaffEntity::getCourseStaffId).toList();
        if (!otherStaffIds.isEmpty()) {
            List<StaffScheduleEntity> existing = staffScheduleRepository.findByCourseStaffIdIn(otherStaffIds);
            for (StaffScheduleEntity row : existing) {
                row.setCourseStaffId(null);
                row.setUpdatedAt(now);
            }
            staffScheduleRepository.saveAll(existing);
        }

        // PM: course_staff 단위 교체 저장(staff_schedule 미사용 → UNIQUE 회피·여러 회차 허용)
        int savedPm = savePmRoster(request.courseId(), pmEntries, pmRoster, now);

        // 상담사: course_daily_counselor 단위 교체 저장(staff_schedule 미사용 → 같은 날 여러 회차 허용)
        int savedCounselor = saveCounselorRoster(request.courseId(), counselorEntries, counselorRoster, now);

        // 비-PM·비-상담사: 로스터 확보 + staff_schedule upsert(배정)
        Set<String> seen = new LinkedHashSet<>();
        int saved = 0;
        for (SaveCourseDailyStaffRequest.Entry entry : otherEntries) {
            StaffRole role = parseStaffRole(entry.staffRole());
            SessionType session = parseSessionType(entry.sessionType());
            // staff_schedule UNIQUE(user_id, schedule_date, session_type) 기준 셀 중복 제거
            String cellKey = entry.userId() + "|" + entry.scheduleDate() + "|" + session;
            if (!seen.add(cellKey)) {
                continue;
            }

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

        return new SaveCourseDailyStaffResponse(request.courseId(), saved + savedPm + savedCounselor);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseDailyStaffCandidateResponse findCandidates(Long courseId) {
        CourseEntity course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));

        List<LocalDate> dates = educationDates(course);
        if (dates.isEmpty()) {
            return new CourseDailyStaffCandidateResponse(courseId, List.of(), List.of());
        }

        LocalDate from = dates.get(0);
        LocalDate to = dates.get(dates.size() - 1);
        Set<LocalDate> dateSet = new LinkedHashSet<>(dates);

        // 근무 가용성: userId → 날짜 → (세션 → is_available). 배정 아님(cs NULL) 행 전체(true+false)를
        // 보존해, 구체 세션(AM/PM) 행이 FULL 행을 그 세션에 한해 override 하도록 한다(반일 가용/불가 반영).
        Map<Long, Map<LocalDate, Map<SessionType, Boolean>>> sessionAvailByUser =
                buildSessionAvailability(from, to, dateSet);

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
        Map<Long, String> phoneByUser = new HashMap<>();
        for (UsersEntity user : usersRepository.findAllById(userIds)) {
            if (!Boolean.TRUE.equals(user.getDeleted())) {
                nameByUser.put(user.getUserId(), user.getName());
                phoneByUser.put(user.getUserId(), user.getPhone());
            }
        }

        // 타 폐강되지 않은 회차의 기존 배정 → busy(중복 방지·경고용). PM(course_staff 단위)은
        // staff_schedule 을 쓰지 않아 자연히 busy 에 잡히지 않는다(면제).
        Map<Long, List<Busy>> busyByUser = new HashMap<>();
        for (StaffScheduleEntity row : staffScheduleRepository
                .findOtherActiveAssignments(userIds, from, to, courseId, CourseStatus.CANCELED)) {
            if (!dateSet.contains(row.getScheduleDate())) {
                continue;
            }
            CourseStaffEntity cs = row.getCourseStaff();
            busyByUser.computeIfAbsent(row.getUserId(), key -> new ArrayList<>())
                    .add(new Busy(
                            row.getScheduleDate(),
                            row.getSessionType() == null ? null : row.getSessionType().name(),
                            cs == null ? null : cs.getCourseId(),
                            cs == null || cs.getCourse() == null ? null : cs.getCourse().getCourseName(),
                            cs == null || cs.getStaffRole() == null ? null : cs.getStaffRole().name()));
        }

        List<Candidate> candidates = new ArrayList<>();
        for (Long userId : userIds) {
            String name = nameByUser.get(userId);
            if (name == null) {
                continue;
            }
            Map<LocalDate, Map<SessionType, Boolean>> userSessions =
                    sessionAvailByUser.getOrDefault(userId, Map.of());
            List<Availability> availability = new ArrayList<>();
            for (LocalDate date : dates) {
                Availability slot = availabilityFor(date, userSessions.getOrDefault(date, Map.of()));
                if (slot != null) {
                    availability.add(slot);
                }
            }
            candidates.add(new Candidate(
                    userId,
                    name,
                    phoneByUser.get(userId),
                    new ArrayList<>(rolesByUser.get(userId)),
                    availability,
                    busyByUser.getOrDefault(userId, List.of())));
        }

        return new CourseDailyStaffCandidateResponse(courseId, dates, candidates);
    }

    /**
     * PM 배정을 course_staff 단위로 교체 저장한다(staff_schedule 미사용). 기존 PM 로스터는
     * 전량 삭제(레거시로 연결된 staff_schedule 은 FK ON DELETE CASCADE 로 함께 정리)한 뒤,
     * 요청 인력으로 새 PM course_staff 행을 생성한다. 반환값은 PM 인력 수.
     * 주의: 기존 PM 의 staff_schedule 을 영속성 컨텍스트로 로드하지 않아야 CASCADE 삭제가
     * flush 충돌 없이 처리된다(그래서 상위 wipe 는 비-PM 만 대상으로 한다).
     */
    private int savePmRoster(Long courseId, List<SaveCourseDailyStaffRequest.Entry> pmEntries,
                             List<CourseStaffEntity> pmRoster, LocalDateTime now) {
        if (!pmRoster.isEmpty()) {
            courseStaffRepository.deleteAll(pmRoster);
        }
        Set<Long> pmUserIds = new LinkedHashSet<>();
        for (SaveCourseDailyStaffRequest.Entry entry : pmEntries) {
            pmUserIds.add(entry.userId());
        }
        for (Long userId : pmUserIds) {
            courseStaffRepository.save(CourseStaffEntity.builder()
                    .courseId(courseId)
                    .userId(userId)
                    .staffRole(StaffRole.PROJECT_MANAGER)
                    .sessionType(SessionType.FULL)
                    .createdAt(now)
                    .build());
        }
        return pmUserIds.size();
    }

    /**
     * 상담사 배정을 course_daily_counselor 단위로 교체 저장한다(staff_schedule 미사용 → 같은 날
     * 여러 회차 배정 허용). 회차의 상담사 course_staff 로스터를 요청 인력에 맞춰 확보/정리한 뒤,
     * 남은 로스터의 일별 배정을 전량 삭제하고 요청대로 재삽입한다. 반환값은 저장된 일별 배정 수.
     * 주의: 남은 로스터의 기존 일별 배정 삭제는 재삽입 전에 flush 해 UNIQUE(cs,date) 충돌을 피한다.
     */
    private int saveCounselorRoster(Long courseId,
                                    List<SaveCourseDailyStaffRequest.Entry> counselorEntries,
                                    List<CourseStaffEntity> counselorRoster, LocalDateTime now) {
        // 요청에 포함된 상담사(userId) 집합
        Set<Long> desiredUsers = new LinkedHashSet<>();
        for (SaveCourseDailyStaffRequest.Entry entry : counselorEntries) {
            desiredUsers.add(entry.userId());
        }

        // 기존 상담사 로스터: userId -> course_staff
        Map<Long, CourseStaffEntity> csByUser = new HashMap<>();
        for (CourseStaffEntity cs : counselorRoster) {
            csByUser.put(cs.getUserId(), cs);
        }

        // 요청에 없는 상담사 로스터 행 삭제(FK ON DELETE CASCADE 로 딸린 course_daily_counselor 정리)
        // → 참여자 상담사 드롭다운 등 로스터 기반 소비자와의 정합 유지.
        List<CourseStaffEntity> stale = counselorRoster.stream()
                .filter(cs -> !desiredUsers.contains(cs.getUserId()))
                .toList();
        if (!stale.isEmpty()) {
            courseStaffRepository.deleteAll(stale);
            // 삭제(및 FK ON DELETE CASCADE 로 딸린 course_daily_counselor 정리)를 먼저 flush 해
            // 이후 재삽입과의 순서를 확정한다(Hibernate 는 기본적으로 insert 를 delete 보다 먼저 flush).
            courseStaffRepository.flush();
            stale.forEach(cs -> csByUser.remove(cs.getUserId()));
        }

        // 필요한 상담사 로스터 확보(없으면 COUNSELOR·FULL 로 생성)
        for (Long userId : desiredUsers) {
            if (!csByUser.containsKey(userId)) {
                CourseStaffEntity cs = courseStaffRepository.save(CourseStaffEntity.builder()
                        .courseId(courseId)
                        .userId(userId)
                        .staffRole(StaffRole.COUNSELOR)
                        .sessionType(SessionType.FULL)
                        .createdAt(now)
                        .build());
                csByUser.put(userId, cs);
            }
        }

        // 남은 상담사 로스터의 기존 일별 배정 전량 삭제(재삽입 전 flush 로 UNIQUE(cs,date) 충돌 방지)
        List<Long> keepCsIds = csByUser.values().stream()
                .map(CourseStaffEntity::getCourseStaffId)
                .toList();
        if (!keepCsIds.isEmpty()) {
            courseDailyCounselorRepository.deleteByCourseStaffIdIn(keepCsIds);
            courseDailyCounselorRepository.flush();
        }

        // 요청대로 일별 배정 재삽입((course_staff, schedule_date) 기준 셀 중복 제거)
        Set<String> seen = new LinkedHashSet<>();
        int saved = 0;
        for (SaveCourseDailyStaffRequest.Entry entry : counselorEntries) {
            CourseStaffEntity cs = csByUser.get(entry.userId());
            if (cs == null) {
                continue;
            }
            String cellKey = cs.getCourseStaffId() + "|" + entry.scheduleDate();
            if (!seen.add(cellKey)) {
                continue;
            }
            courseDailyCounselorRepository.save(CourseDailyCounselorEntity.builder()
                    .courseStaffId(cs.getCourseStaffId())
                    .scheduleDate(entry.scheduleDate())
                    .createdAt(now)
                    .build());
            saved++;
        }
        return saved;
    }

    /**
     * 비-PM 배정 항목이 현재 회차가 아닌 폐강되지 않은 다른 회차에 같은 날짜·겹치는 시간대로 이미
     * 배정되어 있는지 검사한다. 겹침 규칙: FULL 은 전부, AM/PM 은 동일 세션만 충돌.
     */
    private List<AssignConflict> detectConflicts(List<SaveCourseDailyStaffRequest.Entry> entries,
                                                 Long courseId, Map<Long, String> nameById) {
        if (entries.isEmpty()) {
            return List.of();
        }
        List<Long> userIds = entries.stream()
                .map(SaveCourseDailyStaffRequest.Entry::userId).distinct().toList();
        List<LocalDate> dates = entries.stream()
                .map(SaveCourseDailyStaffRequest.Entry::scheduleDate).distinct().sorted().toList();
        LocalDate from = dates.get(0);
        LocalDate to = dates.get(dates.size() - 1);

        List<StaffScheduleEntity> otherRows = staffScheduleRepository
                .findOtherActiveAssignments(userIds, from, to, courseId, CourseStatus.CANCELED);
        if (otherRows.isEmpty()) {
            return List.of();
        }

        List<AssignConflict> conflicts = new ArrayList<>();
        for (SaveCourseDailyStaffRequest.Entry entry : entries) {
            SessionType session = parseSessionType(entry.sessionType());
            for (StaffScheduleEntity row : otherRows) {
                if (row.getUserId().equals(entry.userId())
                        && row.getScheduleDate().equals(entry.scheduleDate())
                        && sessionsOverlap(session, row.getSessionType())) {
                    CourseStaffEntity cs = row.getCourseStaff();
                    conflicts.add(new AssignConflict(
                            entry.userId(),
                            nameById.get(entry.userId()),
                            entry.scheduleDate(),
                            session.name(),
                            cs == null ? null : cs.getCourseId(),
                            cs == null || cs.getCourse() == null ? null : cs.getCourse().getCourseName(),
                            cs == null || cs.getStaffRole() == null ? null : cs.getStaffRole().name()));
                }
            }
        }
        return conflicts;
    }

    /**
     * 비-PM 배정 항목이 대상 인력의 근무 가용성과 어긋나면 배정을 거부한다. 판정은 후보 계산과 동일한
     * 세션 우선순위 해석({@link #isSessionAssignable})을 재사용한다 — 구체 세션(AM/PM) 가용/불가 행이
     * FULL 행을 그 세션에 한해 override 하므로, FULL 불가여도 'PM 가능' 행이 있으면 PM 배정은 허용된다.
     * FULL 배정은 AM·PM 모두 가용해야 한다. 위반 시 {@link AssignOnUnavailableDateException}(409)로
     * 불가 목록을 반환한다(하드 블록).
     */
    private void validateNotUnavailable(List<SaveCourseDailyStaffRequest.Entry> entries,
                                        Map<Long, String> nameById) {
        if (entries.isEmpty()) {
            return;
        }
        List<LocalDate> dates = entries.stream()
                .map(SaveCourseDailyStaffRequest.Entry::scheduleDate).distinct().sorted().toList();
        LocalDate from = dates.get(0);
        LocalDate to = dates.get(dates.size() - 1);

        Map<Long, Map<LocalDate, Map<SessionType, Boolean>>> sessionAvail =
                buildSessionAvailability(from, to, null);
        if (sessionAvail.isEmpty()) {
            return;
        }

        List<AssignUnavailable> violations = new ArrayList<>();
        for (SaveCourseDailyStaffRequest.Entry entry : entries) {
            Map<SessionType, Boolean> sessions = sessionAvail
                    .getOrDefault(entry.userId(), Map.of())
                    .getOrDefault(entry.scheduleDate(), Map.of());
            if (sessions.isEmpty()) {
                continue;
            }
            SessionType session = parseSessionType(entry.sessionType());
            if (!isSessionAssignable(sessions, session)) {
                violations.add(new AssignUnavailable(
                        entry.userId(),
                        nameById.get(entry.userId()),
                        entry.scheduleDate(),
                        session.name()));
            }
        }
        if (!violations.isEmpty()) {
            throw new AssignOnUnavailableDateException(violations);
        }
    }

    /**
     * 배정 아님(course_staff_id NULL) staff_schedule 행을 userId → 날짜 → (세션 → is_available) 로
     * 모은다(is_available 원본 보존 → 구체 세션이 FULL 을 override 가능). dateFilter 가 주어지면 해당
     * 날짜만 포함한다(null 이면 조회 범위 전부).
     */
    private Map<Long, Map<LocalDate, Map<SessionType, Boolean>>> buildSessionAvailability(
            LocalDate from, LocalDate to, Set<LocalDate> dateFilter) {
        Map<Long, Map<LocalDate, Map<SessionType, Boolean>>> map = new HashMap<>();
        for (StaffScheduleEntity row : staffScheduleRepository
                .findByScheduleDateBetweenAndCourseStaffIdIsNull(from, to)) {
            if (dateFilter != null && !dateFilter.contains(row.getScheduleDate())) {
                continue;
            }
            map.computeIfAbsent(row.getUserId(), key -> new HashMap<>())
                    .computeIfAbsent(row.getScheduleDate(), key -> new EnumMap<>(SessionType.class))
                    .put(row.getSessionType(), row.getIsAvailable());
        }
        return map;
    }

    /**
     * 세션 우선순위 해석: 해당 세션(AM/PM) 행이 있으면 그 is_available, 없으면 FULL 행의 is_available,
     * 둘 다 없으면 가용(true). 구체 세션 행이 FULL 행을 그 세션에 한해 override 한다.
     */
    private boolean isSessionFree(Map<SessionType, Boolean> sessions, SessionType session) {
        Boolean specific = sessions.get(session);
        if (specific != null) {
            return specific;
        }
        Boolean full = sessions.get(SessionType.FULL);
        if (full != null) {
            return full;
        }
        return true;
    }

    /** 배정 항목 세션이 실제 배정 가능한지 — FULL 은 AM·PM 모두 가용해야 하고, AM/PM 은 각 세션만. */
    private boolean isSessionAssignable(Map<SessionType, Boolean> sessions, SessionType session) {
        if (session == SessionType.FULL) {
            return isSessionFree(sessions, SessionType.AM) && isSessionFree(sessions, SessionType.PM);
        }
        return isSessionFree(sessions, session);
    }

    /** 시간대 겹침: FULL 은 모든 세션과 겹치고, 그 외에는 동일 세션일 때만 겹친다. */
    private boolean sessionsOverlap(SessionType a, SessionType b) {
        if (a == null || b == null) {
            return false;
        }
        return a == SessionType.FULL || b == SessionType.FULL || a == b;
    }

    /**
     * 세션별 가용성(세션 → is_available)을 반영해 해당 날짜의 가용 세션을 산출한다.
     *   - AM·PM 모두 가용 → FULL(종일)
     *   - 한쪽만 가용 → 그 세션(AM 또는 PM)
     *   - 모두 불가 → null(후보 미노출)
     * 각 세션 가용 여부는 {@link #isSessionFree} 의 우선순위 해석(구체 세션 > FULL > 기본 가용)을 따른다.
     */
    private Availability availabilityFor(LocalDate date, Map<SessionType, Boolean> sessions) {
        boolean amFree = isSessionFree(sessions, SessionType.AM);
        boolean pmFree = isSessionFree(sessions, SessionType.PM);
        if (amFree && pmFree) {
            return new Availability(date, SessionType.FULL.name());
        }
        if (amFree) {
            return new Availability(date, SessionType.AM.name());
        }
        if (pmFree) {
            return new Availability(date, SessionType.PM.name());
        }
        return null;
    }

    private List<LocalDate> educationDates(CourseEntity course) {
        return Stream.of(
                        course.getDay1Date(), course.getDay2Date(), course.getDay3Date(),
                        course.getDay4Date(), course.getDay5Date())
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
    }

    private CourseDailyStaffListResponse.Item toListItem(StaffScheduleEntity schedule) {
        CourseStaffEntity cs = schedule.getCourseStaff();
        return new CourseDailyStaffListResponse.Item(
                schedule.getStaffScheduleId(),
                schedule.getScheduleDate(),
                cs == null || cs.getStaffRole() == null ? null : cs.getStaffRole().name(),
                schedule.getSessionType() == null ? null : schedule.getSessionType().name(),
                schedule.getUserId(),
                schedule.getUser() == null ? null : schedule.getUser().getName(),
                schedule.getUser() == null ? null : schedule.getUser().getPhone());
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
