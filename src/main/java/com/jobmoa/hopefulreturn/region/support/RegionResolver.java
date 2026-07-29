package com.jobmoa.hopefulreturn.region.support;

import com.jobmoa.hopefulreturn.region.entity.RegionEntity;
import com.jobmoa.hopefulreturn.region.repository.RegionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 지역 필터의 상위/하위 지역 해석 공용 헬퍼.
 *
 * <p>course·course_participant 는 항상 하위(OPERATION) 지역에만 연결되므로, 상위(METROPOLITAN)
 * 지역을 선택하면 그 산하 하위 지역 전체로 확장해야 한다. 이 해석을 course·참여자·문자 조회가 공유한다.
 */
@Component
@RequiredArgsConstructor
public class RegionResolver {

    private final RegionRepository regionRepository;

    /**
     * 필터 대상 region_id 목록을 해석한다.
     *
     * <ul>
     *   <li>{@code regionId} 지정 → 해당 하위 지역 1건</li>
     *   <li>{@code parentRegionId} 지정 → 그 상위 지역의 하위 지역 전체</li>
     *   <li>둘 다 null → null(지역 필터 미적용)</li>
     * </ul>
     *
     * @return 필터 대상 region_id 목록. {@code null} 이면 지역 필터 미적용, 빈 목록이면 대상 없음(결과 0건).
     */
    public List<Long> resolveRegionIds(Long regionId, Long parentRegionId) {
        if (regionId != null) {
            return List.of(regionId);
        }
        if (parentRegionId != null) {
            return regionRepository.findByParentRegionId(parentRegionId).stream()
                    .map(RegionEntity::getRegionId)
                    .toList();
        }
        return null;
    }
}
