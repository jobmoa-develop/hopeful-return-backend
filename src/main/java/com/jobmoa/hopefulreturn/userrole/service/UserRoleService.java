package com.jobmoa.hopefulreturn.userrole.service;

import com.jobmoa.hopefulreturn.userrole.model.dto.UserRoleRequestDto;
import com.jobmoa.hopefulreturn.userrole.model.dto.UserRoleResponseDto;
import java.util.List;

public interface UserRoleService {

    UserRoleResponseDto create(UserRoleRequestDto requestDto);

    UserRoleResponseDto findById(Long id);

    List<UserRoleResponseDto> findAll();

    UserRoleResponseDto update(Long id, UserRoleRequestDto requestDto);

    void delete(Long id);
}
