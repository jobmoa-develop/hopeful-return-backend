package com.jobmoa.hopefulreturn.region.service;

import com.jobmoa.hopefulreturn.region.model.dto.RegionDetailResponse;
import com.jobmoa.hopefulreturn.region.model.dto.RegionListResponse;
import java.util.List;

public interface RegionService {

    List<RegionListResponse> findAll();

    RegionDetailResponse findById(Long regionId);
}
