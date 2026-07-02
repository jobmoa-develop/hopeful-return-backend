package com.jobmoa.hopefulreturn.userrole.service;

import com.jobmoa.hopefulreturn.userrole.model.dto.UserRoleRequestDto;
import com.jobmoa.hopefulreturn.userrole.model.dto.UserRoleResponseDto;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class UserRoleServiceImpl implements UserRoleService {

    @Override
    public UserRoleResponseDto create(UserRoleRequestDto requestDto) {
        return null;
    }

    @Override
    public UserRoleResponseDto findById(Long id) {
        return null;
    }

    @Override
    public List<UserRoleResponseDto> findAll() {
        return null;
    }

    @Override
    public UserRoleResponseDto update(Long id, UserRoleRequestDto requestDto) {
        return null;
    }

    @Override
    public void delete(Long id) {
    }
}
