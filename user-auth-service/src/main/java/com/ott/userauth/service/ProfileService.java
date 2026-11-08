package com.ott.userauth.service;

import com.ott.userauth.dto.ProfileDto;

import java.util.Map;

public interface ProfileService {

    Map<String, Object> getProfiles(Long userId);

    ProfileDto createProfile(Long userId, ProfileDto dto);

    ProfileDto updateProfile(Long userId, Long profileId, ProfileDto dto);

    void deleteProfile(Long userId, Long profileId);

    void createDefaultProfile(Long userId, String fullName);
}
