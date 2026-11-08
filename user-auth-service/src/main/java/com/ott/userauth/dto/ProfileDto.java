package com.ott.userauth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileDto {

    // Output only: a client can't set it in a request body
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @NotBlank
    @Size(max = 50)
    private String name;

    // Optional on create: the service falls back to "avatar_1"
    @Size(max = 50)
    private String avatarKey;

    private boolean kids;
}