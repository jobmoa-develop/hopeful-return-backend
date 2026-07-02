package com.jobmoa.hopefulreturn.region.service;

import com.jobmoa.hopefulreturn.region.model.dto.RegionRequestDto;
import com.jobmoa.hopefulreturn.region.model.dto.RegionResponseDto;
import java.util.List;

public interface RegionService {

    RegionResponseDto create(RegionRequestDto requestDto);

    RegionResponseDto findById(Long id);

    List<RegionResponseDto> findAll();

    RegionResponseDto update(Long id, RegionRequestDto requestDto);

    void delete(Long id);
}
