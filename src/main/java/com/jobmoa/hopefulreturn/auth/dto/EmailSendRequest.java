package com.jobmoa.hopefulreturn.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailSendRequest(@Email @NotBlank String email) {
}
