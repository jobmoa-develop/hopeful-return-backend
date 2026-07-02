package com.jobmoa.hopefulreturn.users.service;

import com.jobmoa.hopefulreturn.users.model.dto.UsersRequestDto;
import com.jobmoa.hopefulreturn.users.model.dto.UsersResponseDto;
import java.util.List;

public interface UsersService {

    UsersResponseDto create(UsersRequestDto requestDto);

    UsersResponseDto findById(Long id);

    List<UsersResponseDto> findAll();

    UsersResponseDto update(Long id, UsersRequestDto requestDto);

    void delete(Long id);
}
