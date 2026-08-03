package com.jobmoa.hopefulreturn.role.service;

import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.role.entity.RoleEntity;
import com.jobmoa.hopefulreturn.role.model.dto.RoleResponseDto;
import com.jobmoa.hopefulreturn.role.repository.RoleRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    @Override
    public List<RoleResponseDto> findAll() {

        return roleRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public RoleResponseDto findById(Long roleId) {

        RoleEntity role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));

        return toDto(role);
    }

    private RoleResponseDto toDto(RoleEntity role) {

        return RoleResponseDto.builder()
                .roleId(role.getRoleId())
                .roleName(role.getRoleName().name())
                .description(role.getDescription())
                .build();
    }
}