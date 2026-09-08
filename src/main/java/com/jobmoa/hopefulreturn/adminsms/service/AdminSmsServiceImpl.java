package com.jobmoa.hopefulreturn.adminsms.service;

import com.jobmoa.hopefulreturn.adminsms.entity.AdminSmsEntity;
import com.jobmoa.hopefulreturn.adminsms.entity.AdminSmsImageEntity;
import com.jobmoa.hopefulreturn.adminsms.model.dto.AdminReservationCancelPreviewResponse;
import com.jobmoa.hopefulreturn.adminsms.model.dto.AdminSendSmsRequest;
import com.jobmoa.hopefulreturn.adminsms.model.dto.AdminSendSmsResponse;
import com.jobmoa.hopefulreturn.adminsms.model.dto.AdminSmsDetailResponse;
import com.jobmoa.hopefulreturn.adminsms.model.dto.AdminSmsPageResponse;
import com.jobmoa.hopefulreturn.adminsms.repository.AdminSmsImageRepository;
import com.jobmoa.hopefulreturn.adminsms.repository.AdminSmsRepository;
import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.participantsms.entity.MessageFormat;
import com.jobmoa.hopefulreturn.participantsms.entity.SendStatus;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
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
public class AdminSmsServiceImpl implements AdminSmsService {

    private static final int SMS_MAX_BYTES = SmsByteCalculator.SMS_MAX_BYTES;
    private static final int LMS_MAX_BYTES = SmsByteCalculator.LMS_MAX_BYTES;
    private static final int SUBJECT_MAX_BYTES = SmsByteCalculator.SUBJECT_MAX_BYTES;
    private static final int BATCH_SIZE = 100;

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;
    private static final int RESULT_MESSAGE_MAX = 200;
    private static final DateTimeFormatter RESERVE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    // 국내 휴대폰 번호(정규화 후): 010/011/016/017/018/019 + 7~8자리.
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^01[016789]\\d{7,8}$");

    private final AdminSmsRepository adminSmsRepository;
    private final AdminSmsImageRepository adminSmsImageRepository;
    private final SmsService smsService;

    // 발송결과 폴링에서 이 시간(시)보다 오래된 PENDING 은 대상에서 제외(무한 조회 방지).
    @Value("${sens.result-poll.max-age-hours:24}")
    private long resultPollMaxAgeHours;

    @Override
    public AdminSendSmsResponse send(Long userId, AdminSendSmsRequest request) {
        // 정규화(숫자만) → 휴대폰 형식 검증 → 중복제거(첫 등장 순서 유지).
        List<String> phones = normalizeValidateDedup(request.recipients());
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

        // 치환 없음 — 모든 수신자 공통 본문. 본문 바이트로 형식 판별.
        int contentBytes = byteLength(request.content());
        List<String> images = request.images() == null ? List.of() : request.images();
        boolean hasImages = !images.isEmpty();

        if (contentBytes > LMS_MAX_BYTES) {
            throw new BusinessException(ErrorCode.SMS_CONTENT_TOO_LONG);
        }
        // 이미지 있으면 MMS, 없으면 바이트 길이로 SMS(≤90)/LMS(≤2000) 판별.
        MessageFormat format = hasImages
                ? MessageFormat.MMS
                : (contentBytes <= SMS_MAX_BYTES ? MessageFormat.SMS : MessageFormat.LMS);

        String subject = null;
        if (format != MessageFormat.SMS && StringUtils.hasText(request.title())) {
            if (byteLength(request.title()) > SUBJECT_MAX_BYTES) {
                throw new BusinessException(ErrorCode.SMS_CONTENT_TOO_LONG);
            }
            subject = request.title();
        }

        int successCount = 0;
        int failedCount = 0;
        List<Long> adminSmsIds = new ArrayList<>();
        String responseReserveId = null;
        // SENS 한도(messages 100건) → 100건 단위로 분할 발송.
        for (int start = 0; start < phones.size(); start += BATCH_SIZE) {
            List<String> batch = phones.subList(start, Math.min(start + BATCH_SIZE, phones.size()));
            List<SmsSendCommand.Recipient> commandRecipients = batch.stream()
                    .map(phone -> new SmsSendCommand.Recipient(phone, request.content()))
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
                if (e.getErrorCode() != ErrorCode.SMS_SEND_FAILED) {
                    throw e;
                }
                log.warn("[AdminSMS] 배치 발송 실패 — FAIL 로 기록. userId={}, batchSize={}", userId, batch.size(), e);
                status = SendStatus.FAIL;
                fileIds = List.of();
                requestId = null;
            }
            boolean reserved = status == SendStatus.RESERVED;
            // SENS 예약 취소 단위는 requestId. 별도 reserveId 응답값은 없음.
            String reserveId = reserved ? requestId : null;
            if (reserved && StringUtils.hasText(reserveId) && responseReserveId == null) {
                responseReserveId = reserveId;
            }

            for (String phone : batch) {
                AdminSmsEntity row = AdminSmsEntity.builder()
                        .sentBy(userId)
                        .toPhone(phone)
                        .title(request.title())
                        .content(request.content())
                        .sendStatus(status)
                        .messageFormat(format)
                        // 예약건은 아직 발송 전이라 sent_at 없음(reserve_time 으로 표시). 승격 시 sent_at=승격시각.
                        .sentAt(reserved ? null : now)
                        .reserveTime(reserved ? reserveAt : null)
                        .reserveId(reserved ? reserveId : null)
                        .createdAt(now)
                        .requestId(requestId)
                        .build();
                AdminSmsEntity saved = adminSmsRepository.save(row);
                adminSmsIds.add(saved.getAdminSmsId());
                if (format == MessageFormat.MMS && !fileIds.isEmpty()) {
                    int order = 0;
                    for (String fileId : fileIds) {
                        adminSmsImageRepository.save(AdminSmsImageEntity.builder()
                                .adminSmsId(saved.getAdminSmsId())
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
        return new AdminSendSmsResponse(
                format.name(), phones.size(), successCount, failedCount, statusName, adminSmsIds, responseReserveId);
    }

    // 수신번호 정규화(숫자만)+휴대폰 형식 검증+중복제거. 비거나 유효번호가 없거나 잘못된 번호가 섞이면 INVALID_INPUT.
    private List<String> normalizeValidateDedup(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String value : raw) {
            String normalized = normalizePhone(value);
            if (!MOBILE_PATTERN.matcher(normalized).matches()) {
                // 잘못된 번호는 조용히 버리지 않고 요청 전체를 거절(사용자에게 명확히 알림).
                throw new BusinessException(ErrorCode.INVALID_INPUT);
            }
            result.add(normalized);
        }
        if (result.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        return new ArrayList<>(result);
    }

    @Override
    public int pollPendingResults() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(resultPollMaxAgeHours);
        List<AdminSmsEntity> pending = adminSmsRepository
                .findBySendStatusAndRequestIdIsNotNullAndSentAtAfter(SendStatus.PENDING, cutoff);
        if (pending.isEmpty()) {
            return 0;
        }
        Map<String, List<AdminSmsEntity>> byRequest = pending.stream()
                .collect(Collectors.groupingBy(AdminSmsEntity::getRequestId));
        int updated = 0;
        for (Map.Entry<String, List<AdminSmsEntity>> entry : byRequest.entrySet()) {
            updated += applyResults(entry.getKey(), entry.getValue());
        }
        if (updated > 0) {
            log.info("[AdminSMS] 발송결과 폴링 갱신 {}건 (PENDING 대상 {}건)", updated, pending.size());
        }
        return updated;
    }

    @Override
    public int promoteDueReservations() {
        LocalDateTime now = LocalDateTime.now();
        List<AdminSmsEntity> due = adminSmsRepository
                .findBySendStatusAndReserveTimeLessThanEqual(SendStatus.RESERVED, now);
        int promoted = 0;
        for (AdminSmsEntity row : due) {
            // 미연동(requestId 없음) 예약건은 승격해줄 폴링 대상이 아니므로 건너뛴다.
            if (!StringUtils.hasText(row.getRequestId())) {
                continue;
            }
            row.setSentAt(now);
            row.setSendStatus(SendStatus.PENDING);
            adminSmsRepository.save(row);
            promoted++;
        }
        if (promoted > 0) {
            log.info("[AdminSMS] 예약 발송 승격 {}건 (RESERVED→PENDING)", promoted);
        }
        return promoted;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminReservationCancelPreviewResponse previewReservationCancel(String reserveId) {
        List<AdminSmsEntity> reserved = findReserved(reserveId);
        List<String> phones = reserved.stream().map(AdminSmsEntity::getToPhone).toList();
        LocalDateTime reserveTime = reserved.get(0).getReserveTime();
        return new AdminReservationCancelPreviewResponse(reserveId, reserved.size(), reserveTime, phones);
    }

    @Override
    public int cancelReservation(String reserveId) {
        List<AdminSmsEntity> reserved = findReserved(reserveId);
        // SENS 예약취소 먼저 → 성공해야 DB 를 CANCELED 로 전이(역순이면 취소 실패 시 문자가 그대로 발송됨).
        smsService.cancelReservation(reserveId);
        for (AdminSmsEntity row : reserved) {
            row.setSendStatus(SendStatus.CANCELED);
            adminSmsRepository.save(row);
        }
        log.info("[AdminSMS] 예약 취소 {}건 (reserveId={})", reserved.size(), reserveId);
        return reserved.size();
    }

    // reserveId 로 묶인 RESERVED 행 조회. 없으면(이미 발송/취소) 취소 불가.
    private List<AdminSmsEntity> findReserved(String reserveId) {
        if (!StringUtils.hasText(reserveId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        List<AdminSmsEntity> reserved = adminSmsRepository.findByReserveId(reserveId).stream()
                .filter(r -> r.getSendStatus() == SendStatus.RESERVED)
                .toList();
        if (reserved.isEmpty()) {
            throw new BusinessException(ErrorCode.SMS_RESERVATION_NOT_CANCELABLE);
        }
        return reserved;
    }

    @Override
    public AdminSmsDetailResponse refreshResult(Long adminSmsId) {
        AdminSmsEntity row = adminSmsRepository.findById(adminSmsId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTICIPANT_SMS_NOT_FOUND));
        if (StringUtils.hasText(row.getRequestId())) {
            applyResults(row.getRequestId(), List.of(row));
        }
        return findById(adminSmsId);
    }

    // SENS 발송결과 조회 → 수신번호(to)로 행 매칭 → messageId·결과·상태 갱신. 갱신 건수 반환.
    private int applyResults(String requestId, List<AdminSmsEntity> rows) {
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

        int updated = 0;
        for (AdminSmsEntity row : rows) {
            SmsMessageResult r;
            if (StringUtils.hasText(row.getMessageId())) {
                r = byMessageId.get(row.getMessageId());
            } else {
                Deque<SmsMessageResult> queue = byPhone.get(normalizePhone(row.getToPhone()));
                r = queue == null ? null : queue.poll();
            }
            if (r != null && applyResult(row, r)) {
                updated++;
            }
        }
        return updated;
    }

    private boolean applyResult(AdminSmsEntity row, SmsMessageResult r) {
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
        adminSmsRepository.save(row);
        return changed;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminSmsDetailResponse findById(Long adminSmsId) {
        AdminSmsEntity row = adminSmsRepository.findById(adminSmsId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTICIPANT_SMS_NOT_FOUND));
        List<String> imageUrls = adminSmsImageRepository.findByAdminSmsIdOrderBySortOrderAsc(adminSmsId).stream()
                .map(AdminSmsImageEntity::getImageUrl)
                .toList();
        return new AdminSmsDetailResponse(
                row.getAdminSmsId(),
                row.getToPhone(),
                row.getRecipientLabel(),
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
    public AdminSmsPageResponse findSmsHistoryPage(
            Long effectiveSentBy,
            String sendStatus,
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

        Page<AdminSmsEntity> result = adminSmsRepository.findPageByFilters(
                effectiveSentBy,
                parseSendStatus(sendStatus),
                dateFrom,
                dateTo,
                normalize(keyword),
                pageable);

        List<AdminSmsPageResponse.Item> content = result.getContent().stream()
                .map(this::toHistoryItem)
                .toList();
        return new AdminSmsPageResponse(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    private AdminSmsPageResponse.Item toHistoryItem(AdminSmsEntity row) {
        return new AdminSmsPageResponse.Item(
                row.getAdminSmsId(),
                row.getToPhone(),
                row.getRecipientLabel(),
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

    private String senderName(AdminSmsEntity row) {
        return row.getSender() == null ? null : row.getSender().getName();
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
}
