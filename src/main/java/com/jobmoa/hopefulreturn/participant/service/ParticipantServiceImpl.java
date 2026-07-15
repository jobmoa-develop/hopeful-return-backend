package com.jobmoa.hopefulreturn.participant.service;

import com.jobmoa.hopefulreturn.attendance.entity.AttendanceStatus;
import com.jobmoa.hopefulreturn.attendance.repository.AttendanceDayCount;
import com.jobmoa.hopefulreturn.attendance.repository.AttendanceRepository;
import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantCounselorEntity;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantEntity;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantStatus;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantCreatedResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CreateCourseParticipantRequest;
import com.jobmoa.hopefulreturn.courseparticipant.repository.CourseParticipantCounselorRepository;
import com.jobmoa.hopefulreturn.courseparticipant.repository.CourseParticipantRepository;
import com.jobmoa.hopefulreturn.courseparticipant.service.CourseParticipantService;
import com.jobmoa.hopefulreturn.participant.entity.ParticipantEntity;
import com.jobmoa.hopefulreturn.participant.model.dto.CheckPhoneResponse;
import com.jobmoa.hopefulreturn.participant.model.dto.CreateParticipantRequest;
import com.jobmoa.hopefulreturn.participant.model.dto.EnrollmentSummary;
import com.jobmoa.hopefulreturn.participant.model.dto.ParticipantCreatedResponse;
import com.jobmoa.hopefulreturn.participant.model.dto.ParticipantDeletedResponse;
import com.jobmoa.hopefulreturn.participant.model.dto.ParticipantListResponse;
import com.jobmoa.hopefulreturn.participant.model.dto.ParticipantResponse;
import com.jobmoa.hopefulreturn.participant.model.dto.ParticipantUpdatedResponse;
import com.jobmoa.hopefulreturn.participant.model.dto.UpdateParticipantRequest;
import com.jobmoa.hopefulreturn.participant.repository.ParticipantRepository;
import com.jobmoa.hopefulreturn.participant.support.MatchKeyGenerator;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional
public class ParticipantServiceImpl implements ParticipantService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;

    private final ParticipantRepository participantRepository;
    private final CourseParticipantService courseParticipantService;
    private final CourseParticipantRepository courseParticipantRepository;
    private final CourseParticipantCounselorRepository courseParticipantCounselorRepository;
    private final AttendanceRepository attendanceRepository;

    @Override
    public ParticipantCreatedResponse create(CreateParticipantRequest request) {
        LocalDateTime now = LocalDateTime.now();
        ParticipantEntity participant = ParticipantEntity.builder()
                .name(request.name())
                .birthYear(request.birthYear())
                .phone(request.phone())
                .matchKey(MatchKeyGenerator.generate(request.name(), request.birthYear(), request.phone()))
                .createdAt(now)
                .updatedAt(now)
                .build();

        ParticipantEntity saved = participantRepository.save(participant);
        // 지역·회차를 함께 선택한 경우 같은 트랜잭션으로 수강 등록 — 실패 시 참여자 저장도 롤백된다.
        Long courseParticipantId = enrollIfRequested(request.enrollment(), saved.getParticipantId());
        return new ParticipantCreatedResponse(saved.getParticipantId(), saved.getMatchKey(), courseParticipantId);
    }

    private Long enrollIfRequested(CreateParticipantRequest.Enrollment enrollment, Long participantId) {
        if (enrollment == null) {
            return null;
        }
        CreateCourseParticipantRequest courseParticipantRequest = new CreateCourseParticipantRequest(
                enrollment.courseId(),
                participantId,
                enrollment.counselors(),
                enrollment.inflowType(),
                enrollment.applyDate(),
                enrollment.receptionDate(),
                enrollment.basicEducation());
        CourseParticipantCreatedResponse created =
                courseParticipantService.create(courseParticipantRequest, CourseParticipantStatus.CONFIRMED);
        return created.courseParticipantId();
    }

    @Override
    @Transactional(readOnly = true)
    public ParticipantListResponse findAll(Integer page, Integer size, String name, String phone) {
        Pageable pageable = PageRequest.of(
                sanitizePage(page),
                sanitizeSize(size),
                Sort.by(Sort.Direction.ASC, "participantId"));
        Page<ParticipantEntity> participants = findParticipants(pageable, normalize(name), normalize(phone));
        Map<Long, EnrollmentSummary> latestEnrollments = latestEnrollments(participants.getContent());
        List<ParticipantListResponse.Item> content = participants.getContent().stream()
                .map(participant -> toListItem(
                        participant, latestEnrollments.get(participant.getParticipantId())))
                .toList();

        return new ParticipantListResponse(
                content,
                participants.getNumber(),
                participants.getSize(),
                participants.getTotalElements(),
                participants.getTotalPages());
    }

    /**
     * 페이지 내 참여자들의 최신 수강건 요약을 배치 조회로 만든다 — 페이지당 3쿼리(수강건·상담사·출결)로 N+1을 막는다.
     * 최신 수강건 = 참여자별 courseParticipantId가 가장 큰 행.
     */
    private Map<Long, EnrollmentSummary> latestEnrollments(List<ParticipantEntity> participants) {
        if (participants.isEmpty()) {
            return Map.of();
        }
        List<Long> participantIds = participants.stream()
                .map(ParticipantEntity::getParticipantId)
                .toList();
        Map<Long, CourseParticipantEntity> latestByParticipant = new HashMap<>();
        for (CourseParticipantEntity cp : courseParticipantRepository.findWithCourseByParticipantIdIn(participantIds)) {
            latestByParticipant.merge(cp.getParticipantId(), cp, (a, b) ->
                    a.getCourseParticipantId() >= b.getCourseParticipantId() ? a : b);
        }
        if (latestByParticipant.isEmpty()) {
            return Map.of();
        }

        List<Long> courseParticipantIds = latestByParticipant.values().stream()
                .map(CourseParticipantEntity::getCourseParticipantId)
                .toList();
        Map<Long, List<CourseParticipantCounselorEntity>> counselorsByCp =
                courseParticipantCounselorRepository.findByCourseParticipantIdIn(courseParticipantIds).stream()
                        .collect(Collectors.groupingBy(CourseParticipantCounselorEntity::getCourseParticipantId));
        Map<Long, Long> attendedDaysByCp = attendanceRepository
                .countAttendedDaysByCourseParticipantIdIn(
                        courseParticipantIds, List.of(AttendanceStatus.ATTEND, AttendanceStatus.LATE))
                .stream()
                .collect(Collectors.toMap(
                        AttendanceDayCount::getCourseParticipantId, AttendanceDayCount::getAttendedDays));

        Map<Long, EnrollmentSummary> result = new HashMap<>();
        latestByParticipant.forEach((participantId, cp) -> result.put(participantId, EnrollmentSummary.from(
                cp,
                counselorsByCp.getOrDefault(cp.getCourseParticipantId(), List.of()),
                attendedDaysByCp.getOrDefault(cp.getCourseParticipantId(), 0L))));
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public CheckPhoneResponse checkPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        return participantRepository.findFirstByPhoneOrderByParticipantIdAsc(phone.trim())
                .map(existing -> new CheckPhoneResponse(true, existing.getParticipantId()))
                .orElseGet(() -> new CheckPhoneResponse(false, null));
    }

    @Override
    @Transactional(readOnly = true)
    public ParticipantResponse findById(Long participantId) {
        ParticipantEntity participant = findParticipant(participantId);
        return new ParticipantResponse(
                participant.getParticipantId(),
                participant.getName(),
                participant.getBirthYear(),
                participant.getPhone(),
                participant.getCreatedAt());
    }

    @Override
    public ParticipantUpdatedResponse update(Long participantId, UpdateParticipantRequest request) {
        ParticipantEntity participant = findParticipant(participantId);
        participant.setName(request.name());
        participant.setBirthYear(request.birthYear());
        participant.setPhone(request.phone());
        participant.setMatchKey(MatchKeyGenerator.generate(request.name(), request.birthYear(), request.phone()));
        participant.setUpdatedAt(LocalDateTime.now());
        participantRepository.save(participant);
        return new ParticipantUpdatedResponse(participant.getParticipantId(), true);
    }

    @Override
    public ParticipantDeletedResponse delete(Long participantId) {
        ParticipantEntity participant = findParticipant(participantId);
        participantRepository.delete(participant);
        return new ParticipantDeletedResponse(true);
    }

    private Page<ParticipantEntity> findParticipants(Pageable pageable, String name, String phone) {
        boolean hasName = StringUtils.hasText(name);
        boolean hasPhone = StringUtils.hasText(phone);

        if (hasName && hasPhone) {
            return participantRepository.findByNameContainingAndPhoneContaining(name, phone, pageable);
        }
        if (hasName) {
            return participantRepository.findByNameContaining(name, pageable);
        }
        if (hasPhone) {
            return participantRepository.findByPhoneContaining(phone, pageable);
        }
        return participantRepository.findAll(pageable);
    }

    private ParticipantEntity findParticipant(Long participantId) {
        return participantRepository.findById(participantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTICIPANT_NOT_FOUND));
    }

    private ParticipantListResponse.Item toListItem(
            ParticipantEntity participant, EnrollmentSummary latestEnrollment) {
        return new ParticipantListResponse.Item(
                participant.getParticipantId(),
                participant.getName(),
                participant.getBirthYear(),
                participant.getPhone(),
                participant.getMatchKey(),
                latestEnrollment);
    }

    private int sanitizePage(Integer page) {
        return page == null || page < 0 ? DEFAULT_PAGE : page;
    }

    private int sanitizeSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
