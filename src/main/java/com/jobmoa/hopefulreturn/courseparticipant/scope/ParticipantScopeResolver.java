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
 *   <li>상담사(COUNSELOR): 개별 배정된 상담 건(course_participant_counselor) — 현행 유지.</li>
 *   <li>진행자(STAFF): 배정 회차(course_staff) 의 전체 참여자 — 신규.</li>
 * </ul>
 *
 * 두 역할을 함께 가진 경우 합집합이며, FE 우회가 불가하도록 서버측에서 강제한다.
 * 상담사는 STAFF 경로를 타지 않으므로 course_staff 에 COUNSELOR 로 배치돼 있어도
 * 회차 전체가 노출되지 않는다(상담사 현행 유지 보장).
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

        // 진행자(STAFF) — 배정 회차(course_staff)의 전체 참여자.
        if (AuthScopeSupport.hasCourseAssignedScope(authentication)) {
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

        // 상담사(COUNSELOR) — 개별 배정된 상담 건. 참여자 id 는 수강건에서 역참조한다.
        if (AuthScopeSupport.hasRole(authentication, "ROLE_COUNSELOR")) {
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
