package com.jobmoa.hopefulreturn.participant.repository;

/**
 * 참여자 목록의 한 행을 가리키는 참조 — (참여자, 수강건) 페어.
 *
 * <p>참여자 목록은 이제 수강건(course_participant)마다 1행이다. 참여자가 여러 회차에 등록됐으면
 * 같은 {@code participantId} 에 서로 다른 {@code courseParticipantId} 를 가진 여러 ref 가 나온다.
 * 수강건이 하나도 없는 참여자는 {@code courseParticipantId} 가 {@code null} 인 ref 1건으로 나온다
 * (LEFT JOIN 보존 — 등록 이력 없는 참여자도 목록에 노출).
 */
public record ParticipantEnrollmentRef(Long participantId, Long courseParticipantId) {
}
