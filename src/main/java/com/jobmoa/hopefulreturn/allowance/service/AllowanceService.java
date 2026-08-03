package com.jobmoa.hopefulreturn.allowance.service;

import com.jobmoa.hopefulreturn.allowance.model.dto.AllowanceRequestDto;
import com.jobmoa.hopefulreturn.allowance.model.dto.AllowanceResponseDto;
import java.util.List;

public interface AllowanceService {

    AllowanceResponseDto create(AllowanceRequestDto requestDto);

    AllowanceResponseDto findById(Long id);

    List<AllowanceResponseDto> findAll();

    AllowanceResponseDto update(Long id, AllowanceRequestDto requestDto);

    void delete(Long id);
}
