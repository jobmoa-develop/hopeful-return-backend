package com.jobmoa.hopefulreturn.users.service;

import com.jobmoa.hopefulreturn.users.model.dto.CheckLoginIdResponse;
import com.jobmoa.hopefulreturn.users.model.dto.CreateUserRequest;
import com.jobmoa.hopefulreturn.users.model.dto.UpdateUserRequest;
import com.jobmoa.hopefulreturn.users.model.dto.UserListResponse;
import com.jobmoa.hopefulreturn.users.model.dto.UserResponse;

public interface UsersService {

    UserResponse create(CreateUserRequest request);

    UserResponse findById(Long userId);

    UserListResponse findAll(Integer page, Integer size, String name, String roleName, Boolean enabled);

    void update(Long userId, UpdateUserRequest request);

    void delete(Long userId);

    void updateSmsPermission(Long userId, boolean canSendSms);

    void updateEmailPermission(Long userId, boolean canSendEmail);

    CheckLoginIdResponse checkLoginId(String loginId);

    String resetPassword(Long userId);
}
