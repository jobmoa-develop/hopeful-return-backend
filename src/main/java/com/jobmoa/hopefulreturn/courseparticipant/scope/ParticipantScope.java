package com.jobmoa.hopefulreturn.courseparticipant.scope;

import java.util.Set;

/**
 * 로그인 사용자가 조회 가능한 수강건/참여자 스코프.
 * 두 집합이 모두 {@code null} 이면 제한 없음(관리자급) 을 뜻한다.
 * 값이 있으면 그 집합에 포함된 대상만 조회 가능하며, 빈 집합은 "조회 가능 대상 없음" 을 뜻한다.
 *
 * @param courseParticipantIds 조회 가능한 수강건(course_participant) id 집합. null = 제한 없음.
 * @param participantIds       조회 가능한 참여자(participant) id 집합. null = 제한 없음.
 */
public record ParticipantScope(Set<Long> courseParticipantIds, Set<Long> participantIds) {

    /** 스코프 제한이 없는(관리자급) 사용자. */
    public static final ParticipantScope UNRESTRICTED = new ParticipantScope(null, null);

    /** 제한 없음(관리자급) 여부. */
    public boolean unrestricted() {
        return courseParticipantIds == null;
    }

    /** 지정한 수강건을 조회할 수 있는지. 제한 없음이면 항상 true. */
    public boolean allowsCourseParticipant(Long courseParticipantId) {
        return courseParticipantIds == null || courseParticipantIds.contains(courseParticipantId);
    }

    /** 지정한 참여자를 조회할 수 있는지. 제한 없음이면 항상 true. */
    public boolean allowsParticipant(Long participantId) {
        return participantIds == null || participantIds.contains(participantId);
    }
}
