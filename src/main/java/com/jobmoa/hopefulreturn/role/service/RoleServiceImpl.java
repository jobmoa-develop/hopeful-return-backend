package com.jobmoa.hopefulreturn.role.service;

import com.jobmoa.hopefulreturn.role.model.dto.RoleRequestDto;
import com.jobmoa.hopefulreturn.role.model.dto.RoleResponseDto;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RoleServiceImpl implements RoleService {

    @Override
    public RoleResponseDto create(RoleRequestDto requestDto) {
        return null;
    }

    @Override
    public RoleResponseDto findById(Long id) {
        return null;
    }

    @Override
    public List<RoleResponseDto> findAll() {
        return null;
    }

    @Override
    public RoleResponseDto update(Long id, RoleRequestDto requestDto) {
        return null;
    }

    @Override
    public void delete(Long id) {
    }
}
