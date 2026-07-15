package com.jobmoa.hopefulreturn.userrole.service;

import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.userrole.entity.UserRoleEntity;
import com.jobmoa.hopefulreturn.userrole.model.dto.UserRoleResponseDto;
import com.jobmoa.hopefulreturn.userrole.repository.UserRoleRepository;
import com.jobmoa.hopefulreturn.users.repository.UsersRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserRoleServiceImpl implements UserRoleService {

    private final UserRoleRepository userRoleRepository;
    private final UsersRepository usersRepository;

    @Override
    public List<UserRoleResponseDto> findAll() {

        return userRoleRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public List<UserRoleResponseDto> findByUserId(Long userId) {

        // 사용자가 존재하는지 먼저 확인
        if (!usersRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 사용자는 존재하지만 권한이 없으면 빈 리스트 반환
        return userRoleRepository.findByUserId(userId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private UserRoleResponseDto toDto(UserRoleEntity entity) {

        return UserRoleResponseDto.builder()
                .userId(entity.getUser().getUserId())
                .userName(entity.getUser().getName())
                .roleId(entity.getRole().getRoleId())
                .roleName(entity.getRole().getRoleName().name())
                .description(entity.getRole().getDescription())
                .build();
    }
}