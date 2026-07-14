package com.jobmoa.hopefulreturn.userrole.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleResponseDto {

    private Long userId;

    private String userName;

    private Long roleId;

    private String roleName;

    private String description;
}