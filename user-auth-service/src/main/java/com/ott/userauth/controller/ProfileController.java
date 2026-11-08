package com.ott.userauth.controller;

import com.ott.userauth.dto.ProfileDto;
import com.ott.userauth.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("api/v1/profiles")
public class ProfileController {

    // Set by the gateway after it validates the JWT (Step 9). Until then, send it by hand when testing.
    private static final String USER_ID_HEADER = "X-User-Id";

    @Autowired
    private ProfileService profileService;

    @GetMapping
    public ResponseEntity<Map<String, Object>>profileList(@RequestHeader(USER_ID_HEADER) Long userId) {
        return ResponseEntity.ok(profileService.getProfiles(userId));
    }

    @PostMapping
    public ResponseEntity<ProfileDto> create(@RequestHeader(USER_ID_HEADER) Long userId, @Valid @RequestBody ProfileDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(profileService.createProfile(userId, dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProfileDto> update(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @PathVariable Long id,
            @Valid @RequestBody ProfileDto dto) {
        return ResponseEntity.ok(profileService.updateProfile(userId, id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@RequestHeader(USER_ID_HEADER) Long userId, @PathVariable Long id) {
        profileService.deleteProfile(userId, id);
        return ResponseEntity.ok("Profile has been deleted");
    }
}