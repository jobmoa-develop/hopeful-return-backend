package com.jobmoa.hopefulreturn.region.support;

import com.jobmoa.hopefulreturn.region.entity.RegionEntity;
import com.jobmoa.hopefulreturn.region.repository.RegionRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
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

    /**
     * 프론트 RegionSelect 드롭다운(및 groupRegionsByParent)과 동일한 표시 순서로,
     * 하위(OPERATION) 지역의 regionId → 순서 인덱스 맵을 만든다.
     *
     * <p>순서 규칙: 상위 지역을 regionId 오름차순으로 순회하며, 각 상위 지역 밑의
     * 하위 지역들을 regionId 오름차순으로 이어붙인다. 단순 "전체 regionId 오름차순"과는 다르다 —
     * 예를 들어 서울(부모 regionId=1) 하위에 양천(10)·강북(20), 충남(부모 regionId=2) 하위에
     * 천안(15)이 있으면 결과 순서는 [양천, 강북, 천안]이지 [양천, 천안, 강북]이 아니다.
     *
     * <p>참여자 목록 등에서 최신 수강건의 지역을 이 맵으로 정렬하면, 프론트 드롭다운에 보이는
     * 지역 순서와 동일한 순서로 참여자를 정렬할 수 있다. 상위(METROPOLITAN) 지역 자체는
     * course에 직접 연결되지 않으므로 이 맵에는 포함하지 않는다.
     *
     * @return 하위 지역 regionId → 0부터 시작하는 순서 인덱스
     */
    public Map<Long, Integer> buildChildRegionDisplayOrder() {
        List<RegionEntity> all = regionRepository.findAll(Sort.by(Sort.Direction.ASC, "regionId"));

        List<RegionEntity> parents = all.stream()
                .filter(r -> r.getParentRegionId() == null)
                .toList();

        // LinkedHashMap: 상위 지역이 처음 등장한 순서(=regionId 오름차순)를 그대로 유지하고,
        // 각 그룹 내부 리스트도 regionId 오름차순(all이 이미 그 순서)을 그대로 유지한다.
        Map<Long, List<RegionEntity>> childrenByParent = all.stream()
                .filter(r -> r.getParentRegionId() != null)
                .collect(Collectors.groupingBy(
                        RegionEntity::getParentRegionId,
                        LinkedHashMap::new,
                        Collectors.toList()));

        Map<Long, Integer> order = new LinkedHashMap<>();
        int idx = 0;
        for (RegionEntity parent : parents) {
            List<RegionEntity> children = childrenByParent.getOrDefault(parent.getRegionId(), List.of());
            for (RegionEntity child : children) {
                order.put(child.getRegionId(), idx++);
            }
        }
        return order;
    }
}