package com.jobmoa.hopefulreturn.role.service;

import com.jobmoa.hopefulreturn.role.model.dto.RoleRequestDto;
import com.jobmoa.hopefulreturn.role.model.dto.RoleResponseDto;
import java.util.List;

public interface RoleService {

    RoleResponseDto create(RoleRequestDto requestDto);

    RoleResponseDto findById(Long id);

    List<RoleResponseDto> findAll();

    RoleResponseDto update(Long id, RoleRequestDto requestDto);

    void delete(Long id);
}
