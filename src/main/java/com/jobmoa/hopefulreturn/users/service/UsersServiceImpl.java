package com.jobmoa.hopefulreturn.users.service;

import com.jobmoa.hopefulreturn.users.model.dto.UsersRequestDto;
import com.jobmoa.hopefulreturn.users.model.dto.UsersResponseDto;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class UsersServiceImpl implements UsersService {

    @Override
    public UsersResponseDto create(UsersRequestDto requestDto) {
        return null;
    }

    @Override
    public UsersResponseDto findById(Long id) {
        return null;
    }

    @Override
    public List<UsersResponseDto> findAll() {
        return null;
    }

    @Override
    public UsersResponseDto update(Long id, UsersRequestDto requestDto) {
        return null;
    }

    @Override
    public void delete(Long id) {
    }
}
