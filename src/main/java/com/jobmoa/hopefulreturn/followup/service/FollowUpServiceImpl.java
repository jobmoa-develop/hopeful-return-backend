package com.jobmoa.hopefulreturn.followup.service;

import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.coursestaff.entity.CourseStaffEntity;
import com.jobmoa.hopefulreturn.coursestaff.entity.StaffRole;
import com.jobmoa.hopefulreturn.coursestaff.repository.CourseStaffRepository;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantEntity;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantListResponse;
import com.jobmoa.hopefulreturn.courseparticipant.repository.CourseParticipantCounselorRepository;
import com.jobmoa.hopefulreturn.courseparticipant.repository.CourseParticipantRepository;
import com.jobmoa.hopefulreturn.courseparticipant.service.CourseParticipantService;
import com.jobmoa.hopefulreturn.followup.entity.FollowUpEntity;
import com.jobmoa.hopefulreturn.followup.model.dto.*;
import com.jobmoa.hopefulreturn.followup.repository.FollowUpRepository;
import com.jobmoa.hopefulreturn.followupcounsel.entity.FollowUpCounselEntity;
import com.jobmoa.hopefulreturn.followupcounsel.repository.FollowUpCounselRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FollowUpServiceImpl implements FollowUpService {

    private static final Set<String> NATIONAL_PROGRAM_BRANCHES = Set.of(
            "남부", "서부", "부천", "수원", "인천서부", "의정부", "인천남부",
            "동대문", "광명", "안양", "북부", "성남", "천호", "관악");

    private static final String COMPLETED_STATUS = "COMPLETED";

    private final FollowUpRepository followUpRepository;
    private final FollowUpCounselRepository followUpCounselRepository;
    private final CourseParticipantRepository courseParticipantRepository;
    private final CourseParticipantCounselorRepository courseParticipantCounselorRepository;
    private final CourseParticipantService courseParticipantService;
    private final CourseStaffRepository courseStaffRepository; // ← 추가

    @Override
    public CreateFollowUpResponse create(CreateFollowUpRequest request) {
        validateCourseParticipantExists(request.courseParticipantId());
        validateBranch(request.nationalProgramBranch());

        FollowUpEntity entity = FollowUpEntity.builder()
                .courseParticipantId(request.courseParticipantId())
                .employmentDate(request.employmentDate())
                .forestProgramDate(request.forestProgramDate())
                .nationalProgramDate(request.nationalProgramDate())
                .nationalProgramBranch(request.nationalProgramBranch())
                .build();

        FollowUpEntity saved = followUpRepository.save(entity);
        return new CreateFollowUpResponse(
                saved.getFollowupId(),
                saved.getCourseParticipantId(),
                LocalDateTime.now());
    }

    /**
     * 상담사(COUNSELOR)의 사후관리 허용 범위 — 개별 상담 슬롯 배정(course_participant_counselor)이
     * 아니라, "본인이 COUNSELOR로 배정된 회차(course_staff)에 속한 참여자 전체"로 계산한다.
     * 개별 세션(PRE/POST 등)에 지정되지 않았어도 담당 회차의 참여자는 모두 조회/수정 가능해진다.
     * counselorScopeId 가 null(관리자급)이면 제한 없음.
     */
    private Set<Long> resolveCounselorAllowedCourseParticipantIds(Long counselorScopeId) {
        if (counselorScopeId == null) {
            return null;
        }
        Set<Long> myCounselorCourseIds = courseStaffRepository.findByUserId(counselorScopeId).stream()
                .filter(cs -> cs.getStaffRole() == StaffRole.COUNSELOR)
                .map(CourseStaffEntity::getCourseId)
                .collect(Collectors.toSet());
        if (myCounselorCourseIds.isEmpty()) {
            return Set.of();
        }
        return courseParticipantRepository.findByCourseIdIn(myCounselorCourseIds).stream()
                .map(CourseParticipantEntity::getCourseParticipantId)
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional(readOnly = true)
    public FollowUpListResponse findAll(
            String name, Long regionId, Integer courseNumber, Long counselorScopeId, Integer page, Integer size) {
        Set<Long> allowedCourseParticipantIds = resolveCounselorAllowedCourseParticipantIds(counselorScopeId);
        CourseParticipantListResponse cpPage = courseParticipantService.findAll(
                null, regionId, courseNumber, COMPLETED_STATUS, name, allowedCourseParticipantIds,
                null, null, page, size);
        List<CourseParticipantListResponse.Item> cps = cpPage.content();
        List<Long> cpIds = cps.stream().map(CourseParticipantListResponse.Item::courseParticipantId).toList();

        Map<Long, LocalDate> completionByCp = new HashMap<>();
        Map<Long, FollowUpEntity> snapshotByCp = new HashMap<>();
        Map<Long, Long> counselCountByCp = new HashMap<>();
        Map<Long, LocalDate> lastCounselByCp = new HashMap<>();
        if (!cpIds.isEmpty()) {
            for (CourseParticipantEntity cp : courseParticipantRepository.findAllById(cpIds)) {
                completionByCp.put(cp.getCourseParticipantId(), cp.getCompletionDate());
            }
            for (FollowUpEntity fu : followUpRepository.findByCourseParticipantIdIn(cpIds)) {
                FollowUpEntity prev = snapshotByCp.get(fu.getCourseParticipantId());
                if (prev == null || fu.getFollowupId() > prev.getFollowupId()) {
                    snapshotByCp.put(fu.getCourseParticipantId(), fu);
                }
            }
            for (FollowUpCounselEntity c : followUpCounselRepository.findByCourseParticipantIdIn(cpIds)) {
                Long cpId = c.getCourseParticipantId();
                counselCountByCp.merge(cpId, 1L, Long::sum);
                LocalDate d = c.getCounselDate();
                if (d != null) {
                    LocalDate cur = lastCounselByCp.get(cpId);
                    if (cur == null || d.isAfter(cur)) {
                        lastCounselByCp.put(cpId, d);
                    }
                }
            }
        }

        List<FollowUpListResponse.Item> content = cps.stream().map(cp -> {
            Long cpId = cp.courseParticipantId();
            FollowUpEntity snap = snapshotByCp.get(cpId);
            return new FollowUpListResponse.Item(
                    snap == null ? null : snap.getFollowupId(),
                    cpId,
                    cp.participantName(),
                    cp.matchKey(),
                    cp.regionName(),
                    cp.localCourseNumber(),
                    completionByCp.get(cpId),
                    snap == null ? null : snap.getEmploymentDate(),
                    snap == null ? null : snap.getForestProgramDate(),
                    snap == null ? null : snap.getNationalProgramDate(),
                    snap == null ? null : snap.getNationalProgramBranch(),
                    counselCountByCp.getOrDefault(cpId, 0L),
                    lastCounselByCp.get(cpId));
        }).toList();

        return new FollowUpListResponse(
                content, cpPage.page(), cpPage.size(), cpPage.totalElements(), cpPage.totalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public FollowUpDetailResponse findById(Long followUpId, Long counselorScopeId) {
        FollowUpEntity entity = findEntity(followUpId);
        verifyCounselorScope(entity.getCourseParticipantId(), counselorScopeId);
        return toDetailResponse(entity);
    }

    @Override
    public UpdateFollowUpResponse update(Long followUpId, UpdateFollowUpRequest request) {
        validateBranch(request.nationalProgramBranch());
        FollowUpEntity entity = findEntity(followUpId);
        entity.setEmploymentDate(request.employmentDate());
        entity.setForestProgramDate(request.forestProgramDate());
        entity.setNationalProgramDate(request.nationalProgramDate());
        entity.setNationalProgramBranch(request.nationalProgramBranch());
        followUpRepository.save(entity);
        return new UpdateFollowUpResponse(entity.getFollowupId(), LocalDateTime.now());
    }

    @Override
    public DeleteFollowUpResponse delete(Long followUpId) {
        FollowUpEntity entity = findEntity(followUpId);
        followUpRepository.delete(entity);
        return new DeleteFollowUpResponse("사후관리 정보가 삭제되었습니다.");
    }

    private void validateCourseParticipantExists(Long courseParticipantId) {
        if (!courseParticipantRepository.existsById(courseParticipantId)) {
            throw new BusinessException(ErrorCode.COURSE_PARTICIPANT_NOT_FOUND);
        }
    }

    private void validateBranch(String branch) {
        if (branch != null && !NATIONAL_PROGRAM_BRANCHES.contains(branch)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    /**
     * 상담사(COUNSELOR 전용) 스코프 가드 — "회차(course_staff) 배정" 기준으로 판단한다.
     * 개별 course_participant_counselor 배정 여부와 무관하게, 본인이 COUNSELOR로 배정된
     * 회차의 참여자면 상세 조회/수정 통과.
     */
    private void verifyCounselorScope(Long courseParticipantId, Long counselorScopeId) {
        if (counselorScopeId == null) {
            return;
        }
        CourseParticipantEntity cp = courseParticipantRepository.findById(courseParticipantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_PARTICIPANT_NOT_FOUND));
        boolean assignedToMyCourse = courseStaffRepository.findByUserId(counselorScopeId).stream()
                .anyMatch(cs -> cs.getStaffRole() == StaffRole.COUNSELOR
                        && cs.getCourseId().equals(cp.getCourseId()));
        if (!assignedToMyCourse) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    private FollowUpEntity findEntity(Long followUpId) {
        return followUpRepository.findById(followUpId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FOLLOW_UP_NOT_FOUND));
    }

    private FollowUpDetailResponse toDetailResponse(FollowUpEntity entity) {
        return new FollowUpDetailResponse(
                entity.getFollowupId(),
                entity.getCourseParticipantId(),
                entity.getEmploymentDate(),
                entity.getForestProgramDate(),
                entity.getNationalProgramDate(),
                entity.getNationalProgramBranch());
    }

    @Override
    @Transactional(readOnly = true)
    public FollowUpStatsResponse getStats(Long regionId, Integer courseNumber, Long counselorScopeId) {
        Set<Long> allowedCourseParticipantIds = resolveCounselorAllowedCourseParticipantIds(counselorScopeId);

        List<Long> cpIds = courseParticipantService.findAllIds(
                null, regionId, courseNumber, COMPLETED_STATUS, null, allowedCourseParticipantIds, null, null);

        long total = cpIds.size();
        if (total == 0) {
            return new FollowUpStatsResponse(0, 0, 0, 0, 0.0, 0.0, 0.0);
        }

        Map<Long, FollowUpEntity> snapshotByCp = new HashMap<>();
        for (FollowUpEntity fu : followUpRepository.findByCourseParticipantIdIn(cpIds)) {
            FollowUpEntity prev = snapshotByCp.get(fu.getCourseParticipantId());
            if (prev == null || fu.getFollowupId() > prev.getFollowupId()) {
                snapshotByCp.put(fu.getCourseParticipantId(), fu);
            }
        }

        long employed = snapshotByCp.values().stream().filter(f -> f.getEmploymentDate() != null).count();
        long forest = snapshotByCp.values().stream().filter(f -> f.getForestProgramDate() != null).count();
        long national = snapshotByCp.values().stream().filter(f -> f.getNationalProgramDate() != null).count();

        return new FollowUpStatsResponse(
                total, employed, forest, national,
                rate(employed, total), rate(forest, total), rate(national, total));
    }

    private double rate(long part, long total) {
        return Math.round(part * 10000.0 / total) / 100.0;
    }
}