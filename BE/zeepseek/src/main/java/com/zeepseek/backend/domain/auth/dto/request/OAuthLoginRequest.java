package com.zeepseek.backend.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OAuthLoginRequest {
    @NotBlank
    private String providerId;

    @NotBlank
    private String provider;
}