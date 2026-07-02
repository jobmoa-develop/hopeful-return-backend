package com.jobmoa.hopefulreturn.allowance.service;

import com.jobmoa.hopefulreturn.allowance.model.dto.AllowanceRequestDto;
import com.jobmoa.hopefulreturn.allowance.model.dto.AllowanceResponseDto;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AllowanceServiceImpl implements AllowanceService {

    @Override
    public AllowanceResponseDto create(AllowanceRequestDto requestDto) {
        return null;
    }

    @Override
    public AllowanceResponseDto findById(Long id) {
        return null;
    }

    @Override
    public List<AllowanceResponseDto> findAll() {
        return null;
    }

    @Override
    public AllowanceResponseDto update(Long id, AllowanceRequestDto requestDto) {
        return null;
    }

    @Override
    public void delete(Long id) {
    }
}
