package com.jobmoa.hopefulreturn.users.service;

import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.role.entity.RoleEntity;
import com.jobmoa.hopefulreturn.role.entity.RoleName;
import com.jobmoa.hopefulreturn.role.repository.RoleRepository;
import com.jobmoa.hopefulreturn.userrole.entity.UserRoleEntity;
import com.jobmoa.hopefulreturn.userrole.repository.UserRoleRepository;
import com.jobmoa.hopefulreturn.users.entity.UsersEntity;
import com.jobmoa.hopefulreturn.users.model.dto.CheckLoginIdResponse;
import com.jobmoa.hopefulreturn.users.model.dto.CreateUserRequest;
import com.jobmoa.hopefulreturn.users.model.dto.UpdateUserRequest;
import com.jobmoa.hopefulreturn.users.model.dto.UserListResponse;
import com.jobmoa.hopefulreturn.users.model.dto.UserResponse;
import com.jobmoa.hopefulreturn.users.repository.UsersRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional
public class UsersServiceImpl implements UsersService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;

    private final UsersRepository usersRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String TEMP_PASSWORD_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final int TEMP_PASSWORD_LENGTH = 10;
    private final java.security.SecureRandom secureRandom = new java.security.SecureRandom();

    @Override
    public String resetPassword(Long userId) {
        UsersEntity user = findActiveUser(userId);

        String tempPassword = generateTempPassword();
        user.setPassword(passwordEncoder.encode(tempPassword));
        user.setRefreshToken(null); // 기존 세션 무효화
        user.setUpdatedAt(LocalDateTime.now());
        usersRepository.save(user);

        return tempPassword;
    }

    private String generateTempPassword() {
        StringBuilder sb = new StringBuilder(TEMP_PASSWORD_LENGTH);
        for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
            sb.append(TEMP_PASSWORD_CHARS.charAt(secureRandom.nextInt(TEMP_PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }

    @Override
    public UserResponse create(CreateUserRequest request) {
        if (usersRepository.existsByLoginIdAndDeletedFalse(request.loginId())) {
            throw new BusinessException(ErrorCode.LOGIN_ID_ALREADY_EXISTS);
        }

        List<RoleEntity> roles = findRoles(request.roleNames());
        LocalDateTime now = LocalDateTime.now();
        UsersEntity user = UsersEntity.builder()
                .loginId(request.loginId())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .phone(request.phone())
                .email(request.email())
                .enabled(true)
                .locked(false)
                .deleted(false)
                .canSendSms(false)
                .canSendEmail(false)
                .createdAt(now)
                .updatedAt(now)
                .build();

        UsersEntity savedUser = usersRepository.save(user);
        saveUserRoles(savedUser.getUserId(), roles);

        return new UserResponse(
                savedUser.getUserId(),
                savedUser.getLoginId(),
                savedUser.getName(),
                savedUser.getPhone(),
                savedUser.getEmail(),
                roles.stream().map(r -> r.getRoleName().name()).toList(),
                savedUser.getEnabled(),
                savedUser.getLocked(),
                null,
                Boolean.TRUE.equals(savedUser.getCanSendSms()),
                Boolean.TRUE.equals(savedUser.getCanSendEmail()));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findById(Long userId) {
        UsersEntity user = findActiveUser(userId);
        return toResponse(user, true);
    }

    @Override
    @Transactional(readOnly = true)
    public UserListResponse findAll(Integer page, Integer size, String name, String roleName, Boolean enabled) {
        Pageable pageable = PageRequest.of(
                sanitizePage(page),
                sanitizeSize(size),
                Sort.by(Sort.Direction.ASC, "userId"));
        Page<UsersEntity> users = findUsers(pageable, normalize(name), normalize(roleName), enabled);
        List<UserListResponse.Item> content = users.getContent().stream()
                .map(this::toListItem)
                .toList();

        return new UserListResponse(
                content,
                users.getNumber(),
                users.getSize(),
                users.getTotalElements(),
                users.getTotalPages());
    }

    @Override
    public void update(Long userId, UpdateUserRequest request) {
        UsersEntity user = findActiveUser(userId);
        List<RoleEntity> roles = findRoles(request.roleNames());

        user.setName(request.name());
        user.setPhone(request.phone());
        user.setEmail(request.email());
        user.setEnabled(request.enabled());
        user.setLocked(request.locked());
        user.setUpdatedAt(LocalDateTime.now());

        usersRepository.save(user);
        userRoleRepository.deleteByUserId(user.getUserId());
        saveUserRoles(user.getUserId(), roles);
    }

    @Override
    public void delete(Long userId) {
        UsersEntity user = findActiveUser(userId);
        user.setDeleted(true);
        user.setUpdatedAt(LocalDateTime.now());
        usersRepository.save(user);
    }

    @Override
    public void updateSmsPermission(Long userId, boolean canSendSms) {
        UsersEntity user = findActiveUser(userId);
        user.setCanSendSms(canSendSms);
        user.setUpdatedAt(LocalDateTime.now());
        usersRepository.save(user);
    }

    @Override
    public void updateEmailPermission(Long userId, boolean canSendEmail) {
        UsersEntity user = findActiveUser(userId);
        user.setCanSendEmail(canSendEmail);
        user.setUpdatedAt(LocalDateTime.now());
        usersRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public CheckLoginIdResponse checkLoginId(String loginId) {
        if (!StringUtils.hasText(loginId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        return new CheckLoginIdResponse(usersRepository.existsByLoginIdAndDeletedFalse(loginId));
    }

    private Page<UsersEntity> findUsers(Pageable pageable, String name, String roleName, Boolean enabled) {
        boolean hasName = StringUtils.hasText(name);
        boolean hasRole = StringUtils.hasText(roleName);

        if (!hasRole) {
            if (hasName && enabled != null) {
                return usersRepository.findByDeletedFalseAndNameContainingAndEnabled(name, enabled, pageable);
            }
            if (hasName) {
                return usersRepository.findByDeletedFalseAndNameContaining(name, pageable);
            }
            if (enabled != null) {
                return usersRepository.findByDeletedFalseAndEnabled(enabled, pageable);
            }
            return usersRepository.findByDeletedFalse(pageable);
        }

        RoleEntity role = findRole(roleName);
        List<Long> userIds = userRoleRepository.findByRoleId(role.getRoleId()).stream()
                .map(UserRoleEntity::getUserId)
                .toList();
        if (userIds.isEmpty()) {
            return Page.empty(pageable);
        }

        if (hasName && enabled != null) {
            return usersRepository.findByDeletedFalseAndUserIdInAndNameContainingAndEnabled(
                    userIds,
                    name,
                    enabled,
                    pageable);
        }
        if (hasName) {
            return usersRepository.findByDeletedFalseAndUserIdInAndNameContaining(userIds, name, pageable);
        }
        if (enabled != null) {
            return usersRepository.findByDeletedFalseAndUserIdInAndEnabled(userIds, enabled, pageable);
        }
        return usersRepository.findByDeletedFalseAndUserIdIn(userIds, pageable);
    }

    private UsersEntity findActiveUser(Long userId) {
        return usersRepository.findByUserIdAndDeletedFalse(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private RoleEntity findRole(String roleName) {
        RoleName parsedRoleName = parseRoleName(roleName);
        return roleRepository.findByRoleName(parsedRoleName)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));
    }

    private List<RoleEntity> findRoles(List<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_ROLE_NAME);
        }
        return roleNames.stream()
                .distinct()
                .map(this::findRole)
                .toList();
    }

    private RoleName parseRoleName(String roleName) {
        try {
            return RoleName.valueOf(roleName.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ErrorCode.INVALID_ROLE_NAME);
        }
    }

    private void saveUserRoles(Long userId, List<RoleEntity> roles) {
        roles.forEach(role -> {
            UserRoleEntity userRole = UserRoleEntity.builder()
                    .userId(userId)
                    .roleId(role.getRoleId())
                    .build();
            userRoleRepository.save(userRole);
        });
    }

    private UserResponse toResponse(UsersEntity user, boolean includeCreatedAt) {
        return new UserResponse(
                user.getUserId(),
                user.getLoginId(),
                user.getName(),
                user.getPhone(),
                user.getEmail(),
                extractRoleNames(user),
                user.getEnabled(),
                user.getLocked(),
                includeCreatedAt ? user.getCreatedAt() : null,
                Boolean.TRUE.equals(user.getCanSendSms()),
                Boolean.TRUE.equals(user.getCanSendEmail()));
    }

    private UserListResponse.Item toListItem(UsersEntity user) {
        return new UserListResponse.Item(
                user.getUserId(),
                user.getLoginId(),
                user.getName(),
                extractRoleNames(user),
                user.getEnabled(),
                Boolean.TRUE.equals(user.getCanSendSms()),
                Boolean.TRUE.equals(user.getCanSendEmail()));
    }

    private List<String> extractRoleNames(UsersEntity user) {
        if (user.getUserRoles() == null || user.getUserRoles().isEmpty()) {
            return List.of();
        }
        return user.getUserRoles().stream()
                .map(UserRoleEntity::getRole)
                .filter(role -> role != null && role.getRoleName() != null)
                .map(role -> role.getRoleName().name())
                .toList();
    }

    private int sanitizePage(Integer page) {
        return page == null || page < 0 ? DEFAULT_PAGE : page;
    }

    private int sanitizeSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}