package com.jobmoa.hopefulreturn.participantsms.service;

import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantEntity;
import com.jobmoa.hopefulreturn.courseparticipant.repository.CourseParticipantRepository;
import com.jobmoa.hopefulreturn.participant.entity.ParticipantEntity;
import com.jobmoa.hopefulreturn.participantsms.entity.MessageFormat;
import com.jobmoa.hopefulreturn.participantsms.entity.ParticipantSmsEntity;
import com.jobmoa.hopefulreturn.participantsms.entity.ParticipantSmsImageEntity;
import com.jobmoa.hopefulreturn.participantsms.entity.SendStatus;
import com.jobmoa.hopefulreturn.participantsms.model.dto.ParticipantSmsDetailResponse;
import com.jobmoa.hopefulreturn.participantsms.model.dto.ParticipantSmsListResponse;
import com.jobmoa.hopefulreturn.participantsms.model.dto.ParticipantSmsPageResponse;
import com.jobmoa.hopefulreturn.participantsms.model.dto.ReservationCancelPreviewResponse;
import com.jobmoa.hopefulreturn.participantsms.model.dto.SendSmsRequest;
import com.jobmoa.hopefulreturn.participantsms.model.dto.SendSmsResponse;
import com.jobmoa.hopefulreturn.participantsms.repository.ParticipantSmsImageRepository;
import com.jobmoa.hopefulreturn.participantsms.repository.ParticipantSmsRepository;
import com.jobmoa.hopefulreturn.region.support.RegionResolver;
import com.jobmoa.hopefulreturn.sms.SmsMessageResult;
import com.jobmoa.hopefulreturn.sms.SmsSendCommand;
import com.jobmoa.hopefulreturn.sms.SmsSendResult;
import com.jobmoa.hopefulreturn.sms.SmsService;
import com.jobmoa.hopefulreturn.sms.support.SmsByteCalculator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ParticipantSmsServiceImpl implements ParticipantSmsService {

    private static final int SMS_MAX_BYTES = SmsByteCalculator.SMS_MAX_BYTES;
    private static final int LMS_MAX_BYTES = SmsByteCalculator.LMS_MAX_BYTES;
    private static final int SUBJECT_MAX_BYTES = SmsByteCalculator.SUBJECT_MAX_BYTES;
    private static final int BATCH_SIZE = 100;
    private static final String NAME_PLACEHOLDER = "{name}";
    private static final String REGION_PLACEHOLDER = "{region}";
    private static final String ROUND_PLACEHOLDER = "{round}";

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;
    private static final int RESULT_MESSAGE_MAX = 200;
    private static final DateTimeFormatter RESERVE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ParticipantSmsRepository participantSmsRepository;
    private final ParticipantSmsImageRepository participantSmsImageRepository;
    private final CourseParticipantRepository courseParticipantRepository;
    private final SmsService smsService;
    private final RegionResolver regionResolver;

    // 발송결과 폴링에서 이 시간(시)보다 오래된 PENDING 은 대상에서 제외(무한 조회 방지).
    @Value("${sens.result-poll.max-age-hours:24}")
    private long resultPollMaxAgeHours;

    @Override
    public SendSmsResponse send(Long userId, SendSmsRequest request) {
        List<Long> ids = request.courseParticipantIds();
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        LocalDateTime now = LocalDateTime.now();

        // 예약 발송: reserveTime 이 있으면 미래 시각인지 검증(형식은 @Pattern 이 1차 검증).
        boolean reserveRequested = StringUtils.hasText(request.reserveTime());
        LocalDateTime reserveAt = null;
        if (reserveRequested) {
            reserveAt = parseReserveTime(request.reserveTime());
            if (!reserveAt.isAfter(now)) {
                throw new BusinessException(ErrorCode.SMS_RESERVE_TIME_INVALID);
            }
        }

        Map<Long, CourseParticipantEntity> byId = courseParticipantRepository
                .findWithParticipantAndCourseByCourseParticipantIdIn(ids).stream()
                .collect(Collectors.toMap(CourseParticipantEntity::getCourseParticipantId, Function.identity()));

        // 요청 순서대로 수신자 구성 + {name}/{region}/{round} 치환, 치환 후 최대 바이트로 형식 판별
        List<Recipient> recipients = new ArrayList<>();
        int maxContentBytes = 0;
        for (Long id : ids) {
            CourseParticipantEntity cp = byId.get(id);
            if (cp == null) {
                throw new BusinessException(ErrorCode.COURSE_PARTICIPANT_NOT_FOUND);
            }
            ParticipantEntity participant = cp.getParticipant();
            String phone = participant == null ? null : participant.getPhone();
            if (!StringUtils.hasText(phone)) {
                throw new BusinessException(ErrorCode.INVALID_INPUT);
            }
            String name = participant == null ? null : participant.getName();
            var course = cp.getCourse();
            String region = course == null || course.getRegion() == null ? null : course.getRegion().getName();
            Integer round = course == null ? null : course.getCourseNumber();
            String substituted = substitute(request.content(), name, region, round);
            maxContentBytes = Math.max(maxContentBytes, byteLength(substituted));
            recipients.add(new Recipient(id, phone, substituted));
        }

        List<String> images = request.images() == null ? List.of() : request.images();
        boolean hasImages = !images.isEmpty();

        if (maxContentBytes > LMS_MAX_BYTES) {
            throw new BusinessException(ErrorCode.SMS_CONTENT_TOO_LONG);
        }
        // 이미지 있으면 MMS, 없으면 바이트 길이로 SMS(≤90)/LMS(≤2000) 판별
        MessageFormat format = hasImages
                ? MessageFormat.MMS
                : (maxContentBytes <= SMS_MAX_BYTES ? MessageFormat.SMS : MessageFormat.LMS);

        String subject = null;
        if (format != MessageFormat.SMS && StringUtils.hasText(request.title())) {
            if (byteLength(request.title()) > SUBJECT_MAX_BYTES) {
                throw new BusinessException(ErrorCode.SMS_CONTENT_TOO_LONG);
            }
            subject = request.title();
        }

        int successCount = 0;
        int failedCount = 0;
        List<Long> smsIds = new ArrayList<>();
        String responseReserveId = null;
        // SENS 한도(messages 100건) → 100건 단위로 분할 발송
        for (int start = 0; start < recipients.size(); start += BATCH_SIZE) {
            List<Recipient> batch = recipients.subList(start, Math.min(start + BATCH_SIZE, recipients.size()));
            List<SmsSendCommand.Recipient> commandRecipients = batch.stream()
                    .map(r -> new SmsSendCommand.Recipient(r.phone(), r.content()))
                    .toList();
            SendStatus status;
            List<String> fileIds;
            String requestId;
            try {
                SmsSendResult result = smsService.send(new SmsSendCommand(
                        format.name(),
                        subject,
                        request.content(),
                        commandRecipients,
                        format == MessageFormat.MMS ? images : null,
                        reserveRequested ? request.reserveTime() : null,
                        reserveRequested ? "Asia/Seoul" : null));
                requestId = result.requestId();
                fileIds = result.fileIds() == null ? List.of() : result.fileIds();
                if (!result.success()) {
                    status = SendStatus.FAIL;
                } else if (reserveRequested && StringUtils.hasText(requestId)) {
                    // 예약 접수 성공 — 예약시각 도래 시 폴러가 PENDING 으로 승격한다.
                    status = SendStatus.RESERVED;
                } else if (StringUtils.hasText(requestId)) {
                    // 접수 성공(202) → 실제 전달은 발송결과 조회 폴링으로 확정. requestId 로 후속 조회.
                    status = SendStatus.PENDING;
                } else {
                    // 미연동(NoOp) — requestId 없음, 폴링 대상 아님 → 예약 요청이어도 즉시 SUCCESS.
                    status = SendStatus.SUCCESS;
                }
            } catch (BusinessException e) {
                // 이미지 검증 등 입력 오류(400)는 요청 전체 실패로 전파, 발송 실패(SMS_SEND_FAILED)만 배치 FAIL 로 기록.
                // 예외를 여기서 잡아 전파하지 않으므로 트랜잭션은 유효 → FAIL 행이 정상 커밋된다.
                if (e.getErrorCode() != ErrorCode.SMS_SEND_FAILED) {
                    throw e;
                }
                log.warn("[SMS] 배치 발송 실패 — FAIL 로 기록. userId={}, batchSize={}", userId, batch.size(), e);
                status = SendStatus.FAIL;
                fileIds = List.of();
                requestId = null;
            }
            boolean reserved = status == SendStatus.RESERVED;
            // SENS 예약 취소 단위는 requestId(라이브 검증: DELETE reservations/{requestId}). 별도 reserveId 응답값은 없음.
            String reserveId = reserved ? requestId : null;
            if (reserved && StringUtils.hasText(reserveId) && responseReserveId == null) {
                responseReserveId = reserveId;
            }

            for (Recipient r : batch) {
                ParticipantSmsEntity row = ParticipantSmsEntity.builder()
                        .sentBy(userId)
                        .title(request.title())
                        .content(r.content())
                        .sendStatus(status)
                        .messageFormat(format)
                        // 예약건은 아직 발송 전이라 sent_at 없음(reserve_time 으로 표시). 승격 시 sent_at=승격시각.
                        .sentAt(reserved ? null : now)
                        .reserveTime(reserved ? reserveAt : null)
                        .reserveId(reserved ? reserveId : null)
                        .createdAt(now)
                        .courseParticipantId(r.courseParticipantId())
                        .requestId(requestId)
                        .build();
                ParticipantSmsEntity saved = participantSmsRepository.save(row);
                smsIds.add(saved.getSmsId());
                if (format == MessageFormat.MMS && !fileIds.isEmpty()) {
                    int order = 0;
                    for (String fileId : fileIds) {
                        participantSmsImageRepository.save(ParticipantSmsImageEntity.builder()
                                .smsId(saved.getSmsId())
                                .imageUrl(fileId)
                                .sortOrder(order++)
                                .createdAt(now)
                                .build());
                    }
                }
                // 접수 성공(PENDING·noop SUCCESS)은 successCount, 발송 실패(FAIL)만 failedCount.
                if (status == SendStatus.FAIL) {
                    failedCount++;
                } else {
                    successCount++;
                }
            }
        }

        String statusName = failedCount == 0 ? "success" : (successCount == 0 ? "fail" : "partial");
        return new SendSmsResponse(
                format.name(), recipients.size(), successCount, failedCount, statusName, smsIds, responseReserveId);
    }

    @Override
    public int pollPendingResults() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(resultPollMaxAgeHours);
        List<ParticipantSmsEntity> pending = participantSmsRepository
                .findBySendStatusAndRequestIdIsNotNullAndSentAtAfter(SendStatus.PENDING, cutoff);
        if (pending.isEmpty()) {
            return 0;
        }
        Map<String, List<ParticipantSmsEntity>> byRequest = pending.stream()
                .collect(Collectors.groupingBy(ParticipantSmsEntity::getRequestId));
        int updated = 0;
        for (Map.Entry<String, List<ParticipantSmsEntity>> entry : byRequest.entrySet()) {
            updated += applyResults(entry.getKey(), entry.getValue());
        }
        if (updated > 0) {
            log.info("[SMS] 발송결과 폴링 갱신 {}건 (PENDING 대상 {}건)", updated, pending.size());
        }
        return updated;
    }

    @Override
    public int promoteDueReservations() {
        LocalDateTime now = LocalDateTime.now();
        List<ParticipantSmsEntity> due = participantSmsRepository
                .findBySendStatusAndReserveTimeLessThanEqual(SendStatus.RESERVED, now);
        int promoted = 0;
        for (ParticipantSmsEntity row : due) {
            // 미연동(requestId 없음) 예약건은 승격해줄 폴링 대상이 아니므로 건너뛴다(정상적으로는 발생하지 않음).
            if (!StringUtils.hasText(row.getRequestId())) {
                continue;
            }
            // 승격 시 sent_at 은 승격 시각(now) — 폴링 cutoff(sentAtAfter) 정합성. reserve_time 은 예정시각으로 보존.
            row.setSentAt(now);
            row.setSendStatus(SendStatus.PENDING);
            participantSmsRepository.save(row);
            promoted++;
        }
        if (promoted > 0) {
            log.info("[SMS] 예약 발송 승격 {}건 (RESERVED→PENDING)", promoted);
        }
        return promoted;
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationCancelPreviewResponse previewReservationCancel(String reserveId) {
        if (!StringUtils.hasText(reserveId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        List<ParticipantSmsEntity> rows = participantSmsRepository.findByReserveIdWithParticipant(reserveId);
        List<ParticipantSmsEntity> reserved = rows.stream()
                .filter(r -> r.getSendStatus() == SendStatus.RESERVED)
                .toList();
        if (reserved.isEmpty()) {
            // 이미 발송(승격)되었거나 취소됨 — 취소 불가.
            throw new BusinessException(ErrorCode.SMS_RESERVATION_NOT_CANCELABLE);
        }
        List<String> names = reserved.stream()
                .map(r -> r.getCourseParticipant() == null || r.getCourseParticipant().getParticipant() == null
                        ? null : r.getCourseParticipant().getParticipant().getName())
                .filter(StringUtils::hasText)
                .toList();
        LocalDateTime reserveTime = reserved.get(0).getReserveTime();
        return new ReservationCancelPreviewResponse(reserveId, reserved.size(), reserveTime, names);
    }

    @Override
    public int cancelReservation(String reserveId) {
        if (!StringUtils.hasText(reserveId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        List<ParticipantSmsEntity> rows = participantSmsRepository.findByReserveIdWithParticipant(reserveId);
        List<ParticipantSmsEntity> reserved = rows.stream()
                .filter(r -> r.getSendStatus() == SendStatus.RESERVED)
                .toList();
        if (reserved.isEmpty()) {
            throw new BusinessException(ErrorCode.SMS_RESERVATION_NOT_CANCELABLE);
        }
        // SENS 예약취소 먼저 → 성공해야 DB 를 CANCELED 로 전이(역순이면 취소 실패 시 문자가 그대로 발송됨).
        smsService.cancelReservation(reserveId);
        for (ParticipantSmsEntity row : reserved) {
            row.setSendStatus(SendStatus.CANCELED);
            participantSmsRepository.save(row);
        }
        log.info("[SMS] 예약 취소 {}건 (reserveId={})", reserved.size(), reserveId);
        return reserved.size();
    }

    @Override
    public ParticipantSmsDetailResponse refreshResult(Long smsId) {
        ParticipantSmsEntity row = participantSmsRepository.findById(smsId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTICIPANT_SMS_NOT_FOUND));
        if (StringUtils.hasText(row.getRequestId())) {
            applyResults(row.getRequestId(), List.of(row));
        }
        return findById(smsId);
    }

    // SENS 발송결과 조회 → 수신번호(to)로 행 매칭 → messageId·결과·상태 갱신. 갱신 건수 반환.
    private int applyResults(String requestId, List<ParticipantSmsEntity> rows) {
        List<SmsMessageResult> results = smsService.lookupResults(requestId);
        if (results == null || results.isEmpty()) {
            return 0;
        }
        // 이미 messageId 가 매핑된 행은 messageId 로 정확 매칭.
        Map<String, SmsMessageResult> byMessageId = results.stream()
                .filter(r -> StringUtils.hasText(r.messageId()))
                .collect(Collectors.toMap(SmsMessageResult::messageId, Function.identity(), (a, b) -> a));
        // messageId 미매핑 행은 수신번호(to) 순서로 매칭(한 배치 내 동일 번호 중복 시 순서 기반).
        Map<String, Deque<SmsMessageResult>> byPhone = new HashMap<>();
        for (SmsMessageResult r : results) {
            byPhone.computeIfAbsent(normalizePhone(r.to()), k -> new ArrayDeque<>()).add(r);
        }
        Map<Long, String> phoneByCp = loadPhones(rows);

        int updated = 0;
        for (ParticipantSmsEntity row : rows) {
            SmsMessageResult r;
            if (StringUtils.hasText(row.getMessageId())) {
                r = byMessageId.get(row.getMessageId());
            } else {
                Deque<SmsMessageResult> queue =
                        byPhone.get(normalizePhone(phoneByCp.get(row.getCourseParticipantId())));
                r = queue == null ? null : queue.poll();
            }
            if (r != null && applyResult(row, r)) {
                updated++;
            }
        }
        return updated;
    }

    private boolean applyResult(ParticipantSmsEntity row, SmsMessageResult r) {
        SendStatus newStatus = switch (r.state()) {
            case SUCCESS -> SendStatus.SUCCESS;
            case FAIL -> SendStatus.FAIL;
            case PENDING -> SendStatus.PENDING;
        };
        row.setMessageId(r.messageId());
        row.setResultCode(r.resultCode());
        row.setResultMessage(truncate(r.resultName()));
        row.setCompleteTime(r.completeTime());
        boolean changed = row.getSendStatus() != newStatus;
        row.setSendStatus(newStatus);
        participantSmsRepository.save(row);
        return changed;
    }

    private Map<Long, String> loadPhones(List<ParticipantSmsEntity> rows) {
        List<Long> cpIds = rows.stream()
                .map(ParticipantSmsEntity::getCourseParticipantId)
                .distinct()
                .toList();
        return courseParticipantRepository.findWithParticipantByCourseParticipantIdIn(cpIds).stream()
                .filter(cp -> cp.getParticipant() != null && cp.getParticipant().getPhone() != null)
                .collect(Collectors.toMap(
                        CourseParticipantEntity::getCourseParticipantId,
                        cp -> cp.getParticipant().getPhone(),
                        (a, b) -> a));
    }

    private String normalizePhone(String phone) {
        return phone == null ? "" : phone.replaceAll("[^0-9]", "");
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= RESULT_MESSAGE_MAX ? value : value.substring(0, RESULT_MESSAGE_MAX);
    }

    @Override
    @Transactional(readOnly = true)
    public ParticipantSmsListResponse findByCourseParticipant(Long courseParticipantId) {
        if (!courseParticipantRepository.existsById(courseParticipantId)) {
            throw new BusinessException(ErrorCode.COURSE_PARTICIPANT_NOT_FOUND);
        }
        List<ParticipantSmsEntity> rows =
                participantSmsRepository.findByCourseParticipantIdOrderBySmsIdDesc(courseParticipantId);
        Map<Long, List<String>> imagesBySms = loadImages(rows);
        List<ParticipantSmsListResponse.Item> content = rows.stream()
                .map(row -> new ParticipantSmsListResponse.Item(
                        row.getSmsId(),
                        row.getMessageFormat() == null ? null : row.getMessageFormat().name(),
                        row.getTitle(),
                        row.getContent(),
                        row.getSendStatus() == null ? null : row.getSendStatus().name(),
                        row.getMessageId(),
                        row.getResultCode(),
                        row.getResultMessage(),
                        row.getCompleteTime(),
                        row.getSentAt(),
                        row.getReserveTime(),
                        row.getReserveId(),
                        senderName(row),
                        imagesBySms.getOrDefault(row.getSmsId(), List.of())))
                .toList();
        return new ParticipantSmsListResponse(content);
    }

    @Override
    @Transactional(readOnly = true)
    public ParticipantSmsDetailResponse findById(Long smsId) {
        ParticipantSmsEntity row = participantSmsRepository.findById(smsId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTICIPANT_SMS_NOT_FOUND));
        List<String> imageUrls = participantSmsImageRepository.findBySmsIdOrderBySortOrderAsc(smsId).stream()
                .map(ParticipantSmsImageEntity::getImageUrl)
                .toList();
        return new ParticipantSmsDetailResponse(
                row.getSmsId(),
                row.getCourseParticipantId(),
                row.getMessageFormat() == null ? null : row.getMessageFormat().name(),
                row.getTitle(),
                row.getContent(),
                row.getSendStatus() == null ? null : row.getSendStatus().name(),
                row.getMessageId(),
                row.getResultCode(),
                row.getResultMessage(),
                row.getCompleteTime(),
                row.getSentBy(),
                senderName(row),
                row.getSentAt(),
                row.getReserveTime(),
                row.getReserveId(),
                row.getCreatedAt(),
                imageUrls);
    }

    @Override
    @Transactional(readOnly = true)
    public ParticipantSmsPageResponse findSmsHistoryPage(
            Long effectiveSentBy,
            String sendStatus,
            Integer courseNumber,
            Integer localCourseNumber,
            Long regionId,
            Long parentRegionId,
            LocalDate sentDateFrom,
            LocalDate sentDateTo,
            String keyword,
            Integer page,
            Integer size) {
        // 정렬은 JPQL 의 order by coalesce(sent_at, reserve_time) desc 가 담당(예약건 sent_at=null 포함) → Pageable 은 무정렬.
        Pageable pageable = PageRequest.of(sanitizePage(page), sanitizeSize(size), Sort.unsorted());
        LocalDateTime dateFrom = sentDateFrom == null ? null : sentDateFrom.atStartOfDay();
        // 종료일 하루 포함: 다음날 0시 미만(< dateTo)
        LocalDateTime dateTo = sentDateTo == null ? null : sentDateTo.plusDays(1).atStartOfDay();

        // 상위 지역(서울) 선택 시 산하 하위 지역 전체로 확장한다. 산하가 없으면 결과 0건.
        List<Long> regionIds = regionResolver.resolveRegionIds(regionId, parentRegionId);
        if (regionIds != null && regionIds.isEmpty()) {
            return new ParticipantSmsPageResponse(List.of(), sanitizePage(page), sanitizeSize(size), 0, 0);
        }

        Page<ParticipantSmsEntity> result = participantSmsRepository.findPageByFilters(
                effectiveSentBy,
                parseSendStatus(sendStatus),
                courseNumber,
                localCourseNumber,
                regionIds,
                dateFrom,
                dateTo,
                normalize(keyword),
                pageable);

        List<ParticipantSmsPageResponse.Item> content = result.getContent().stream()
                .map(this::toHistoryItem)
                .toList();
        return new ParticipantSmsPageResponse(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    private ParticipantSmsPageResponse.Item toHistoryItem(ParticipantSmsEntity row) {
        CourseParticipantEntity cp = row.getCourseParticipant();
        ParticipantEntity participant = cp == null ? null : cp.getParticipant();
        var course = cp == null ? null : cp.getCourse();
        return new ParticipantSmsPageResponse.Item(
                row.getSmsId(),
                row.getCourseParticipantId(),
                participant == null ? null : participant.getName(),
                participant == null ? null : participant.getPhone(),
                course == null || course.getRegion() == null ? null : course.getRegion().getName(),
                course == null ? null : course.getCourseName(),
                course == null ? null : course.getCourseNumber(),
                row.getMessageFormat() == null ? null : row.getMessageFormat().name(),
                row.getTitle(),
                row.getContent(),
                row.getSendStatus() == null ? null : row.getSendStatus().name(),
                row.getMessageId(),
                row.getResultCode(),
                row.getResultMessage(),
                row.getCompleteTime(),
                row.getSentAt(),
                row.getReserveTime(),
                row.getReserveId(),
                senderName(row));
    }

    private SendStatus parseSendStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return SendStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
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

    private Map<Long, List<String>> loadImages(List<ParticipantSmsEntity> rows) {
        List<Long> smsIds = rows.stream().map(ParticipantSmsEntity::getSmsId).toList();
        if (smsIds.isEmpty()) {
            return Map.of();
        }
        return participantSmsImageRepository.findBySmsIdIn(smsIds).stream()
                .collect(Collectors.groupingBy(
                        ParticipantSmsImageEntity::getSmsId,
                        Collectors.mapping(ParticipantSmsImageEntity::getImageUrl, Collectors.toList())));
    }

    private String senderName(ParticipantSmsEntity row) {
        return row.getSender() == null ? null : row.getSender().getName();
    }

    // 수신자별 치환: {name}=성명, {region}=수강 지역명, {round}=회차. 값이 없으면 빈 문자열로 대체.
    private String substitute(String content, String name, String region, Integer round) {
        return content
                .replace(NAME_PLACEHOLDER, name == null ? "" : name)
                .replace(REGION_PLACEHOLDER, region == null ? "" : region)
                .replace(ROUND_PLACEHOLDER, round == null ? "" : String.valueOf(round));
    }

    private int byteLength(String value) {
        return SmsByteCalculator.byteLength(value);
    }

    // 예약 시각 파싱("yyyy-MM-dd HH:mm"). 형식 오류 시 SMS_RESERVE_TIME_INVALID.
    private LocalDateTime parseReserveTime(String value) {
        try {
            return LocalDateTime.parse(value.trim(), RESERVE_TIME_FORMAT);
        } catch (DateTimeParseException e) {
            throw new BusinessException(ErrorCode.SMS_RESERVE_TIME_INVALID);
        }
    }

    private record Recipient(Long courseParticipantId, String phone, String content) {
    }
}
