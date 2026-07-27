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
import com.jobmoa.hopefulreturn.participantsms.model.dto.SendSmsRequest;
import com.jobmoa.hopefulreturn.participantsms.model.dto.SendSmsResponse;
import com.jobmoa.hopefulreturn.participantsms.repository.ParticipantSmsImageRepository;
import com.jobmoa.hopefulreturn.participantsms.repository.ParticipantSmsRepository;
import com.jobmoa.hopefulreturn.sms.SmsSendCommand;
import com.jobmoa.hopefulreturn.sms.SmsSendResult;
import com.jobmoa.hopefulreturn.sms.SmsService;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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
public class ParticipantSmsServiceImpl implements ParticipantSmsService {

    private static final Charset EUC_KR = Charset.forName("EUC-KR");
    private static final int SMS_MAX_BYTES = 90;
    private static final int LMS_MAX_BYTES = 2000;
    private static final int SUBJECT_MAX_BYTES = 40;
    private static final int BATCH_SIZE = 100;
    private static final String NAME_PLACEHOLDER = "{name}";

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;

    private final ParticipantSmsRepository participantSmsRepository;
    private final ParticipantSmsImageRepository participantSmsImageRepository;
    private final CourseParticipantRepository courseParticipantRepository;
    private final SmsService smsService;

    @Override
    public SendSmsResponse send(Long userId, SendSmsRequest request) {
        List<Long> ids = request.courseParticipantIds();
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        LocalDateTime now = LocalDateTime.now();

        Map<Long, CourseParticipantEntity> byId = courseParticipantRepository
                .findWithParticipantByCourseParticipantIdIn(ids).stream()
                .collect(Collectors.toMap(CourseParticipantEntity::getCourseParticipantId, Function.identity()));

        // 요청 순서대로 수신자 구성 + {name} 치환, 치환 후 최대 바이트로 형식 판별
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
            String substituted = substitute(request.content(), name);
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
        // SENS 한도(messages 100건) → 100건 단위로 분할 발송
        for (int start = 0; start < recipients.size(); start += BATCH_SIZE) {
            List<Recipient> batch = recipients.subList(start, Math.min(start + BATCH_SIZE, recipients.size()));
            List<SmsSendCommand.Recipient> commandRecipients = batch.stream()
                    .map(r -> new SmsSendCommand.Recipient(r.phone(), r.content()))
                    .toList();
            SmsSendResult result = smsService.send(new SmsSendCommand(
                    format.name(),
                    subject,
                    request.content(),
                    commandRecipients,
                    format == MessageFormat.MMS ? images : null));
            SendStatus status = result.success() ? SendStatus.SUCCESS : SendStatus.FAIL;
            List<String> fileIds = result.fileIds() == null ? List.of() : result.fileIds();

            for (Recipient r : batch) {
                ParticipantSmsEntity row = ParticipantSmsEntity.builder()
                        .sentBy(userId)
                        .title(request.title())
                        .content(r.content())
                        .sendStatus(status)
                        .messageFormat(format)
                        .sentAt(now)
                        .createdAt(now)
                        .courseParticipantId(r.courseParticipantId())
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
                if (status == SendStatus.SUCCESS) {
                    successCount++;
                } else {
                    failedCount++;
                }
            }
        }

        String statusName = failedCount == 0 ? "success" : "partial";
        return new SendSmsResponse(format.name(), recipients.size(), successCount, failedCount, statusName, smsIds);
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
                        row.getSentAt(),
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
                row.getSentBy(),
                senderName(row),
                row.getSentAt(),
                row.getCreatedAt(),
                imageUrls);
    }

    @Override
    @Transactional(readOnly = true)
    public ParticipantSmsPageResponse findSmsHistoryPage(
            Long effectiveSentBy,
            String sendStatus,
            Integer courseNumber,
            Long regionId,
            LocalDate sentDateFrom,
            LocalDate sentDateTo,
            String keyword,
            Integer page,
            Integer size) {
        Pageable pageable = PageRequest.of(
                sanitizePage(page),
                sanitizeSize(size),
                Sort.by(Sort.Direction.DESC, "sentAt"));
        LocalDateTime dateFrom = sentDateFrom == null ? null : sentDateFrom.atStartOfDay();
        // 종료일 하루 포함: 다음날 0시 미만(< dateTo)
        LocalDateTime dateTo = sentDateTo == null ? null : sentDateTo.plusDays(1).atStartOfDay();

        Page<ParticipantSmsEntity> result = participantSmsRepository.findPageByFilters(
                effectiveSentBy,
                parseSendStatus(sendStatus),
                courseNumber,
                regionId,
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
                row.getSentAt(),
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

    private String substitute(String content, String name) {
        return content.replace(NAME_PLACEHOLDER, name == null ? "" : name);
    }

    private int byteLength(String value) {
        return value == null ? 0 : value.getBytes(EUC_KR).length;
    }

    private record Recipient(Long courseParticipantId, String phone, String content) {
    }
}
