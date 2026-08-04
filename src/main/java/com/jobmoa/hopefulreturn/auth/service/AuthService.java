package com.jobmoa.hopefulreturn.auth.service;

import com.jobmoa.hopefulreturn.auth.model.dto.ChangePasswordRequest;
import com.jobmoa.hopefulreturn.auth.model.dto.LoginRequest;
import com.jobmoa.hopefulreturn.auth.model.dto.LoginResponse;
import com.jobmoa.hopefulreturn.auth.model.dto.MeResponse;
import com.jobmoa.hopefulreturn.auth.model.dto.RefreshResponse;
import com.jobmoa.hopefulreturn.auth.model.dto.UpdateMyProfileRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request, HttpServletResponse response);

    RefreshResponse refresh(HttpServletRequest request);

    void logout(HttpServletRequest request, HttpServletResponse response);

    MeResponse me();

    MeResponse updateMyProfile(UpdateMyProfileRequest request);

    void changePassword(ChangePasswordRequest request);
}