package com.jobmoa.hopefulreturn.courseparticipant.scope;

import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantCounselorEntity;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantEntity;
import com.jobmoa.hopefulreturn.courseparticipant.repository.CourseParticipantCounselorRepository;
import com.jobmoa.hopefulreturn.courseparticipant.repository.CourseParticipantRepository;
import com.jobmoa.hopefulreturn.coursestaff.entity.CourseStaffEntity;
import com.jobmoa.hopefulreturn.coursestaff.repository.CourseStaffRepository;
import com.jobmoa.hopefulreturn.security.AuthScopeSupport;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인 사용자가 참여자관리(수강생/참여자)에서 조회 가능한 스코프를 계산한다.
 *
 * <ul>
 *   <li>관리자급(ADMIN/HEAD_OFFICE/REGIONAL_MANAGER/PROJECT_MANAGER/PROJECT_LEADER/OPERATOR):
 *       제한 없음({@link ParticipantScope#UNRESTRICTED}).</li>
 *   <li>진행자(STAFF): 배정 회차(course_staff) 의 전체 참여자.</li>
 *   <li>상담사(COUNSELOR): 배정 회차(course_staff) 의 전체 참여자 + 개별 배정된 상담 건
 *       (course_participant_counselor) 의 합집합. 회차 전체 스코프로 확대(권한 개편).</li>
 * </ul>
 *
 * 여러 역할을 함께 가진 경우 합집합이며, FE 우회가 불가하도록 서버측에서 강제한다.
 */
@Component
@RequiredArgsConstructor
public class ParticipantScopeResolver {

    private final CourseParticipantCounselorRepository courseParticipantCounselorRepository;
    private final CourseStaffRepository courseStaffRepository;
    private final CourseParticipantRepository courseParticipantRepository;

    @Transactional(readOnly = true)
    public ParticipantScope resolve(Authentication authentication, Long userId) {
        if (AuthScopeSupport.hasUnrestrictedScope(authentication)) {
            return ParticipantScope.UNRESTRICTED;
        }
        if (userId == null) {
            return new ParticipantScope(Set.of(), Set.of());
        }

        Set<Long> courseParticipantIds = new HashSet<>();
        Set<Long> participantIds = new HashSet<>();

        boolean staffScope = AuthScopeSupport.hasCourseAssignedScope(authentication);
        boolean counselorScope = AuthScopeSupport.hasRole(authentication, "ROLE_COUNSELOR");

        // 진행자(STAFF)/상담사(COUNSELOR) 공통 — 배정 회차(course_staff)의 전체 참여자.
        // 상담사도 배정 회차 전체를 조회한다(권한 개편: 개별 배정건 → 회차 전체로 확대).
        if (staffScope || counselorScope) {
            Set<Long> courseIds = courseStaffRepository.findByUserId(userId).stream()
                    .map(CourseStaffEntity::getCourseId)
                    .collect(Collectors.toSet());
            if (!courseIds.isEmpty()) {
                for (CourseParticipantEntity cp : courseParticipantRepository.findByCourseIdIn(courseIds)) {
                    courseParticipantIds.add(cp.getCourseParticipantId());
                    participantIds.add(cp.getParticipantId());
                }
            }
        }

        // 상담사(COUNSELOR) — 개별 배정된 상담 건도 합집합으로 포함한다.
        // course_staff 에 배치되지 않은 회차의 개별 슬롯 배정건이 유실되지 않도록 유지.
        if (counselorScope) {
            Set<Long> counselorCpIds = courseParticipantCounselorRepository.findByCounselorId(userId).stream()
                    .map(CourseParticipantCounselorEntity::getCourseParticipantId)
                    .collect(Collectors.toSet());
            if (!counselorCpIds.isEmpty()) {
                for (CourseParticipantEntity cp : courseParticipantRepository.findAllById(counselorCpIds)) {
                    courseParticipantIds.add(cp.getCourseParticipantId());
                    participantIds.add(cp.getParticipantId());
                }
            }
        }

        return new ParticipantScope(courseParticipantIds, participantIds);
    }
}
