package com.jobmoa.hopefulreturn.region.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.jobmoa.hopefulreturn.region.entity.RegionEntity;
import com.jobmoa.hopefulreturn.region.repository.RegionRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegionResolverTest {

    @Mock
    private RegionRepository regionRepository;

    @InjectMocks
    private RegionResolver regionResolver;

    @Test
    @DisplayName("하위 지역(regionId) 지정 → 해당 지역 1건, 리포지토리 미조회")
    void resolvesSingleRegion() {
        List<Long> result = regionResolver.resolveRegionIds(11L, null);

        assertThat(result).containsExactly(11L);
        verifyNoInteractions(regionRepository);
    }

    @Test
    @DisplayName("상위 지역(parentRegionId) 지정 → 산하 하위 지역 전체로 확장")
    void resolvesChildRegions() {
        RegionEntity yangcheon = RegionEntity.builder().regionId(11L).name("양천").build();
        RegionEntity gangnam = RegionEntity.builder().regionId(12L).name("강남").build();
        when(regionRepository.findByParentRegionId(1L)).thenReturn(List.of(yangcheon, gangnam));

        List<Long> result = regionResolver.resolveRegionIds(null, 1L);

        assertThat(result).containsExactly(11L, 12L);
    }

    @Test
    @DisplayName("regionId 가 우선 — parentRegionId 가 함께 와도 regionId 만 사용")
    void regionIdTakesPrecedence() {
        List<Long> result = regionResolver.resolveRegionIds(11L, 1L);

        assertThat(result).containsExactly(11L);
        verifyNoInteractions(regionRepository);
    }

    @Test
    @DisplayName("둘 다 null → null(지역 필터 미적용)")
    void noFilter() {
        assertThat(regionResolver.resolveRegionIds(null, null)).isNull();
        verifyNoInteractions(regionRepository);
    }

    @Test
    @DisplayName("상위 지역에 하위가 없으면 빈 목록(결과 0건)")
    void emptyChildren() {
        when(regionRepository.findByParentRegionId(99L)).thenReturn(List.of());

        assertThat(regionResolver.resolveRegionIds(null, 99L)).isEmpty();
    }
}
