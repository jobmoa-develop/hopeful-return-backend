package com.jobmoa.hopefulreturn.region.service;

import com.jobmoa.hopefulreturn.region.model.dto.RegionRequestDto;
import com.jobmoa.hopefulreturn.region.model.dto.RegionResponseDto;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RegionServiceImpl implements RegionService {

    @Override
    public RegionResponseDto create(RegionRequestDto requestDto) {
        return null;
    }

    @Override
    public RegionResponseDto findById(Long id) {
        return null;
    }

    @Override
    public List<RegionResponseDto> findAll() {
        return null;
    }

    @Override
    public RegionResponseDto update(Long id, RegionRequestDto requestDto) {
        return null;
    }

    @Override
    public void delete(Long id) {
    }
}
