package com.jobmoa.hopefulreturn.userrole.service;

import com.jobmoa.hopefulreturn.userrole.model.dto.UserRoleResponseDto;
import java.util.List;

public interface UserRoleService {

    /**
     * 사용자 권한 전체 조회
     */
    List<UserRoleResponseDto> findAll();

    /**
     * 특정 사용자의 권한 조회
     */
    List<UserRoleResponseDto> findByUserId(Long userId);

}