package com.jobmoa.hopefulreturn.region.service;

import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.region.entity.RegionEntity;
import com.jobmoa.hopefulreturn.region.entity.RegionLevel;
import com.jobmoa.hopefulreturn.region.model.dto.RegionDetailResponse;
import com.jobmoa.hopefulreturn.region.model.dto.RegionListResponse;
import com.jobmoa.hopefulreturn.region.repository.RegionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegionServiceImpl implements RegionService {

    private final RegionRepository regionRepository;

    @Override
    public List<RegionListResponse> findAll() {
        return regionRepository.findAll(Sort.by(Sort.Direction.ASC, "regionId")).stream()
                .map(this::toListResponse)
                .toList();
    }

    @Override
    public RegionDetailResponse findById(Long regionId) {
        RegionEntity region = regionRepository.findById(regionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REGION_NOT_FOUND));
        return toDetailResponse(region);
    }

    private RegionListResponse toListResponse(RegionEntity region) {
        return new RegionListResponse(
                region.getRegionId(),
                region.getName(),
                toApiLevel(region.getLevel()),
                region.getParentRegionId());
    }

    private RegionDetailResponse toDetailResponse(RegionEntity region) {
        return new RegionDetailResponse(
                region.getRegionId(),
                region.getName(),
                toApiLevel(region.getLevel()),
                region.getParentRegionId(),
                extractParentRegionName(region),
                null,
                null);
    }

    private String extractParentRegionName(RegionEntity region) {
        return region.getParentRegion() == null ? null : region.getParentRegion().getName();
    }

    private String toApiLevel(RegionLevel level) {
        if (level == null) {
            return null;
        }
        return switch (level) {
            case METROPOLITAN -> "LEVEL1";
            case OPERATION -> "LEVEL2";
        };
    }
}
