package com.jobmoa.hopefulreturn.counselingschedule.service;

import com.jobmoa.hopefulreturn.counselingschedule.model.dto.CounselingScheduleResponse;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantCounselorEntity;
import com.jobmoa.hopefulreturn.courseparticipant.repository.CourseParticipantCounselorRepository;
import com.jobmoa.hopefulreturn.region.support.RegionResolver;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CounselingScheduleServiceImpl implements CounselingScheduleService {

    /** from/to 미지정 시 기본 조회 범위(당월 1일 ~ +DEFAULT_RANGE_DAYS). */
    private static final int DEFAULT_RANGE_DAYS = 31;

    private final CourseParticipantCounselorRepository counselorRepository;
    private final RegionResolver regionResolver;

    @Override
    public CounselingScheduleResponse findSchedules(
            LocalDate from, LocalDate to,
            Long regionId, Long parentRegionId, Integer courseNumber, String counselorName) {
        // 상위 지역(서울)=parentRegionId 는 산하 하위 지역 전체로 확장. 참여자·문자 조회와 동일 규칙.
        List<Long> regionIds = regionResolver.resolveRegionIds(regionId, parentRegionId);
        // 상위 지역인데 산하 하위 지역이 없으면 대상 없음 → IN () 회피를 위해 즉시 0건 반환.
        if (regionIds != null && regionIds.isEmpty()) {
            return CounselingScheduleResponse.from(List.of());
        }

        LocalDate fromDate = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate toDate = to != null ? to : fromDate.plusDays(DEFAULT_RANGE_DAYS);
        // 뒤집힌 범위는 교정한다(방어).
        if (toDate.isBefore(fromDate)) {
            LocalDate tmp = fromDate;
            fromDate = toDate;
            toDate = tmp;
        }
        // 부분일치 LIKE 패턴을 미리 만들어 넘긴다(concat 사용 시 varchar 캐스팅으로 한글 유실 회피).
        String counselorPattern =
                StringUtils.hasText(counselorName) ? "%" + counselorName.trim() + "%" : null;

        // 종료일 포함을 위해 +1일 자정을 상한(exclusive)으로 넘긴다.
        List<CourseParticipantCounselorEntity> rows = counselorRepository.findCounselingSchedules(
                fromDate.atStartOfDay(), toDate.plusDays(1).atStartOfDay(),
                regionIds, courseNumber, counselorPattern);
        return CounselingScheduleResponse.from(rows);
    }
}
