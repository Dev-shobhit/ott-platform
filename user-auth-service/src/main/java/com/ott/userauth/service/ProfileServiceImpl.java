package com.ott.userauth.service.impl;

import com.ott.userauth.dto.ProfileDto;
import com.ott.userauth.entity.Profile;
import com.ott.userauth.exception.InvalidRequestException;
import com.ott.userauth.exception.ProfileNameExistsException;
import com.ott.userauth.repository.ProfileRepository;
import com.ott.userauth.service.ProfileService;
import com.ott.common.exception.ResourceNotFoundException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ProfileServiceImpl implements ProfileService {

    private static final String DEFAULT_AVATAR = "avatar_1";
    private static final String DEFAULT_PROFILE_NAME = "My Profile";
    private static final int MAX_NAME_LENGTH = 50;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private MessageSource messageSource;

    @Value("${profile.max-per-user}")
    private int maxProfiles;

    @Override
    public Map<String, Object> getProfiles(Long userId) {
        List<ProfileDto> profiles = Optional.ofNullable(userId)
                .map(profileRepository::findByUserIdOrderByCreatedAtAsc)
                .orElse(Collections.emptyList())
                .stream()
                .map(this::toDto)
                .toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("profiles", profiles);
        response.put("count", profiles.size());
        response.put("maxAllowed", maxProfiles);
        return response;
    }

    @Override
    public ProfileDto createProfile(Long userId, ProfileDto dto) {
        String name = resolveName(dto);

        if (profileRepository.countByUserId(userId) >= maxProfiles) {
            throw new InvalidRequestException(msg("profile.limit.exceeded", maxProfiles));
        }
        if (profileRepository.existsByUserIdAndNameIgnoreCase(userId, name)) {
            throw new ProfileNameExistsException(msg("profile.name.exists", name));
        }

        return toDto(profileRepository.save(buildProfile(userId, name, dto.getAvatarKey(), dto.isKids())));
    }

    @Override
    public ProfileDto updateProfile(Long userId, Long profileId, ProfileDto dto) {
        String name = resolveName(dto);
        Profile profile = findOwned(userId, profileId);

        if (profileRepository.existsByUserIdAndNameIgnoreCaseAndIdNot(userId, name, profileId)) {
            throw new ProfileNameExistsException(msg("profile.name.exists", name));
        }

        profile.setName(name);
        profile.setAvatarKey(avatarOrDefault(dto.getAvatarKey()));
        profile.setKids(dto.isKids());
        return toDto(profileRepository.save(profile));
    }

    @Override
    public void deleteProfile(Long userId, Long profileId) {
        Profile profile = findOwned(userId, profileId);

        if (profileRepository.countByUserId(userId) <= 1) {
            throw new InvalidRequestException(msg("profile.last.delete"));
        }
        profileRepository.delete(profile);
    }

    @Override
    public void createDefaultProfile(Long userId, String fullName) {
        String name = Optional.ofNullable(fullName)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .orElse(DEFAULT_PROFILE_NAME);

        if (name.length() > MAX_NAME_LENGTH) {
            name = name.substring(0, MAX_NAME_LENGTH);
        }
        profileRepository.save(buildProfile(userId, name, null, false));
    }

    // Always look up by id AND owner, so another user's profile is simply "not found"
    private Profile findOwned(Long userId, Long profileId) {
        return Optional.ofNullable(userId)
                .flatMap(owner -> Optional.ofNullable(profileId)
                        .flatMap(id -> profileRepository.findByIdAndUserId(id, owner)))
                .orElseThrow(() -> new ResourceNotFoundException(msg("profile.not.found")));
    }

    private String resolveName(ProfileDto dto) {
        return Optional.ofNullable(dto)
                .map(ProfileDto::getName)
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .orElseThrow(() -> new InvalidRequestException(msg("profile.name.required")));
    }

    // Fields are set one by one on purpose: nothing from the request can ever set id or userId
    private Profile buildProfile(Long userId, String name, String avatarKey, boolean kids) {
        Profile profile = new Profile();
        profile.setUserId(userId);
        profile.setName(name);
        profile.setAvatarKey(avatarOrDefault(avatarKey));
        profile.setKids(kids);
        return profile;
    }

    private String avatarOrDefault(String avatarKey) {
        return Optional.ofNullable(avatarKey)
                .filter(key -> !key.isBlank())
                .orElse(DEFAULT_AVATAR);
    }

    private ProfileDto toDto(Profile profile) {
        return modelMapper.map(profile, ProfileDto.class);
    }

    private String msg(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }
}