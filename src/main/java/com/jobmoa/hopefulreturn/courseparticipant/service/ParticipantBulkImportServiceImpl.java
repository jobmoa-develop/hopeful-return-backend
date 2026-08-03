package com.jobmoa.hopefulreturn.courseparticipant.service;

import com.jobmoa.hopefulreturn.course.entity.CourseEntity;
import com.jobmoa.hopefulreturn.course.repository.CourseRepository;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantEntity;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantStatus;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.BulkImportCommitRequest;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.BulkImportCourseGroup;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.BulkImportParsedRow;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.BulkImportPreviewResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.BulkImportResultResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.BulkImportRowResult;
import com.jobmoa.hopefulreturn.courseparticipant.repository.CourseParticipantRepository;
import com.jobmoa.hopefulreturn.courseparticipant.support.ParticipantExcelParser;
import com.jobmoa.hopefulreturn.courseparticipant.support.PhoneNormalizer;
import com.jobmoa.hopefulreturn.participant.entity.ParticipantEntity;
import com.jobmoa.hopefulreturn.participant.repository.ParticipantRepository;
import com.jobmoa.hopefulreturn.participant.support.MatchKeyGenerator;
import com.jobmoa.hopefulreturn.region.entity.RegionEntity;
import com.jobmoa.hopefulreturn.region.entity.RegionLevel;
import com.jobmoa.hopefulreturn.region.repository.RegionRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class ParticipantBulkImportServiceImpl implements ParticipantBulkImportService {

    /** 교육과정명 끝의 "…_16회차" 에서 회차 번호를 뽑는다. */
    private static final Pattern ROUND_PATTERN = Pattern.compile("(\\d+)\\s*회차");
    /** 시군구 마지막 토큰의 행정구역 접미사(운영지역명 매칭용으로 제거). */
    private static final Pattern REGION_SUFFIX =
            Pattern.compile("(특별자치시|특별자치도|광역시|특별시|자치시|자치도|시|군|구|도)$");
    private static final String NO_COURSE_NAME = "(교육과정명 없음)";

    private static final String OUTCOME_INVALID = "INVALID";
    private static final String OUTCOME_UNMAPPED = "SKIPPED_UNMAPPED";
    private static final String OUTCOME_DUPLICATE = "SKIPPED_DUPLICATE";

    private final ParticipantExcelParser parser;
    private final ParticipantRepository participantRepository;
    private final CourseParticipantRepository courseParticipantRepository;
    private final CourseRepository courseRepository;
    private final RegionRepository regionRepository;

    @Override
    @Transactional(readOnly = true)
    public BulkImportPreviewResponse preview(MultipartFile file) {
        List<BulkImportParsedRow> rows = parser.parse(file);

        Map<String, List<BulkImportParsedRow>> grouped = new LinkedHashMap<>();
        for (BulkImportParsedRow row : rows) {
            grouped.computeIfAbsent(groupKey(row), k -> new ArrayList<>()).add(row);
        }

        List<BulkImportCourseGroup> groups = new ArrayList<>();
        for (Map.Entry<String, List<BulkImportParsedRow>> entry : grouped.entrySet()) {
            List<BulkImportParsedRow> groupRows = entry.getValue();
            BulkImportParsedRow first = groupRows.get(0);
            Integer round = extractRound(entry.getKey());
            int invalidCount = (int) groupRows.stream().filter(r -> r.error() != null).count();
            Long suggested = suggestCourseId(first.sido(), first.sigungu(), round);
            groups.add(new BulkImportCourseGroup(
                    entry.getKey(), first.sido(), first.sigungu(), round,
                    groupRows.size(), invalidCount, suggested, groupRows));
        }

        int invalidRows = (int) rows.stream().filter(r -> r.error() != null).count();
        return new BulkImportPreviewResponse(rows.size(), rows.size() - invalidRows, invalidRows, groups);
    }

    @Override
    public BulkImportResultResponse commit(BulkImportCommitRequest request) {
        List<BulkImportCommitRequest.Item> items =
                request == null || request.items() == null ? List.of() : request.items();

        LocalDateTime now = LocalDateTime.now();
        int registered = 0;
        int duplicate = 0;
        int unmapped = 0;
        int invalid = 0;
        int created = 0;
        int reused = 0;
        List<BulkImportRowResult> details = new ArrayList<>();

        for (BulkImportCommitRequest.Item item : items) {
            // 클라이언트 값을 신뢰하지 않고 서버가 다시 정규화·검증한다.
            String phone = PhoneNormalizer.normalize(item.phone());
            String error = validate(item.name(), phone);
            if (error != null) {
                invalid++;
                details.add(rowResult(item, OUTCOME_INVALID, error));
                continue;
            }
            if (item.targetCourseId() == null || !courseRepository.existsById(item.targetCourseId())) {
                unmapped++;
                details.add(rowResult(item, OUTCOME_UNMAPPED, "매핑된 회차가 없습니다."));
                continue;
            }

            ParticipantResolution resolution = resolveParticipant(item.name(), item.birthYear(), phone, now);
            if (resolution.created()) {
                created++;
            } else {
                reused++;
            }

            if (courseParticipantRepository.existsByCourseIdAndParticipantId(
                    item.targetCourseId(), resolution.participantId())) {
                duplicate++;
                details.add(rowResult(item, OUTCOME_DUPLICATE, "이미 해당 회차에 등록된 참여자입니다."));
                continue;
            }

            enroll(item, resolution.participantId(), now);
            registered++;
        }

        return new BulkImportResultResponse(
                registered, duplicate, unmapped, invalid, created, reused, details);
    }

    private String validate(String name, String phone) {
        if (!StringUtils.hasText(name)) {
            return "교육생명이 없습니다.";
        }
        if (!StringUtils.hasText(phone) || phone.length() < 9 || phone.length() > 20) {
            return "휴대폰번호가 올바르지 않습니다.";
        }
        return null;
    }

    /**
     * matchKey → 전화 순으로 기존 참여자를 찾고(중복 방지), 없으면 새로 생성한다.
     * 같은 요청 내 동일인은 첫 행 생성 후 이후 행에서 재사용된다(트랜잭션 내 flush).
     */
    private ParticipantResolution resolveParticipant(String name, Integer birthYear, String phone, LocalDateTime now) {
        String matchKey = MatchKeyGenerator.generate(name, birthYear, phone);
        Optional<ParticipantEntity> existing = participantRepository.findByMatchKey(matchKey);
        if (existing.isEmpty()) {
            existing = participantRepository.findFirstByPhoneOrderByParticipantIdAsc(phone);
        }
        if (existing.isPresent()) {
            return new ParticipantResolution(existing.get().getParticipantId(), false);
        }
        ParticipantEntity participant = ParticipantEntity.builder()
                .name(name)
                .birthYear(birthYear)
                .phone(phone)
                .matchKey(matchKey)
                .createdAt(now)
                .updatedAt(now)
                .build();
        ParticipantEntity saved = participantRepository.save(participant);
        return new ParticipantResolution(saved.getParticipantId(), true);
    }

    private void enroll(BulkImportCommitRequest.Item item, Long participantId, LocalDateTime now) {
        CourseParticipantEntity entity = CourseParticipantEntity.builder()
                .courseId(item.targetCourseId())
                .participantId(participantId)
                .applyDate(item.applyDate())
                .receptionDate(item.receptionDate())
                .status(parseStatus(item.status()))
                .contactAttempt(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
        courseParticipantRepository.save(entity);
    }

    /** 상태 문자열을 enum 으로 파싱한다. 값이 없거나 알 수 없으면 APPLIED(접수). */
    private CourseParticipantStatus parseStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return CourseParticipantStatus.APPLIED;
        }
        try {
            return CourseParticipantStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return CourseParticipantStatus.APPLIED;
        }
    }

    private BulkImportRowResult rowResult(BulkImportCommitRequest.Item item, String outcome, String reason) {
        return new BulkImportRowResult(item.rowNumber(), item.name(), item.sourceCourseName(), outcome, reason);
    }

    private String groupKey(BulkImportParsedRow row) {
        return StringUtils.hasText(row.sourceCourseName()) ? row.sourceCourseName().trim() : NO_COURSE_NAME;
    }

    private Integer extractRound(String courseName) {
        if (!StringUtils.hasText(courseName)) {
            return null;
        }
        Matcher matcher = ROUND_PATTERN.matcher(courseName);
        Integer last = null;
        while (matcher.find()) {
            try {
                last = Integer.valueOf(matcher.group(1));
            } catch (NumberFormatException ignored) {
                // 숫자 범위를 벗어나면 무시하고 계속 탐색한다.
            }
        }
        return last;
    }

    /**
     * 시군구·회차번호로 내부 회차를 best-effort 추천한다. 지역·번호 체계가 정부식과 달라 대개 null.
     * 애매하면(지역 다중/미존재, 회차 다중) null 을 반환해 운영자 매핑에 맡긴다.
     */
    private Long suggestCourseId(String sido, String sigungu, Integer round) {
        if (round == null) {
            return null;
        }
        Long regionId = resolveOperationRegionId(sigungu);
        if (regionId == null) {
            return null;
        }
        List<CourseEntity> matches = courseRepository.findByRegionIdAndCourseNumber(regionId, round);
        return matches.size() == 1 ? matches.get(0).getCourseId() : null;
    }

    private Long resolveOperationRegionId(String sigungu) {
        if (!StringUtils.hasText(sigungu)) {
            return null;
        }
        String[] tokens = sigungu.trim().split("\\s+");
        String lastToken = tokens[tokens.length - 1];
        String stripped = REGION_SUFFIX.matcher(lastToken).replaceAll("");
        for (String candidate : new String[] {stripped, lastToken}) {
            if (!StringUtils.hasText(candidate)) {
                continue;
            }
            List<RegionEntity> operations = regionRepository.findByName(candidate).stream()
                    .filter(region -> region.getLevel() == RegionLevel.OPERATION)
                    .toList();
            if (operations.size() == 1) {
                return operations.get(0).getRegionId();
            }
        }
        return null;
    }

    /** 참여자 find-or-create 결과 — id 와 신규 생성 여부. */
    private record ParticipantResolution(Long participantId, boolean created) {
    }
}
