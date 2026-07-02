package com.jobmoa.hopefulreturn.region.repository;

import com.jobmoa.hopefulreturn.region.entity.RegionEntity;
import com.jobmoa.hopefulreturn.region.entity.RegionLevel;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionRepository extends JpaRepository<RegionEntity, Long> {

    List<RegionEntity> findByParentRegionId(Long parentRegionId);

    List<RegionEntity> findByLevel(RegionLevel level);

    List<RegionEntity> findByName(String name);
}
