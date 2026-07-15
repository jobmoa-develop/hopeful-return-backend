package com.jobmoa.hopefulreturn.role.service;

import com.jobmoa.hopefulreturn.role.model.dto.RoleResponseDto;
import java.util.List;

public interface RoleService {

    /**
     * 권한 목록 조회
     */
    List<RoleResponseDto> findAll();

    /**
     * 권한 상세 조회
     */
    RoleResponseDto findById(Long roleId);

}