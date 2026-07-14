package com.jobmoa.hopefulreturn.role.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleResponseDto {

    private Long roleId;

    private String roleName;

    private String description;
}