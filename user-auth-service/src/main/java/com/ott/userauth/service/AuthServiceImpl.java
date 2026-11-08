package com.ott.userauth.service;

import com.ott.common.exception.UnauthorizedException;
import com.ott.userauth.component.JwtTokenProvider;
import com.ott.userauth.component.OtpStore;
import com.ott.userauth.component.RefreshTokenStore;
import com.ott.userauth.dto.*;
import com.ott.userauth.entity.User;
import com.ott.userauth.enums.AuthProvider;
import com.ott.userauth.enums.RoleType;
import com.ott.userauth.enums.SubscriptionTier;
import com.ott.userauth.exception.EmailAlreadyExistsException;
import com.ott.userauth.exception.InvalidRequestException;
import com.ott.userauth.exception.TooManyRequestsException;
import com.ott.userauth.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private RefreshTokenStore refreshTokenStore;

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private OtpStore otpStore;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ProfileService profileService;


    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {

        String email = Optional.ofNullable(request)
                .map(RegisterRequest::getEmail)
                .map(String::trim)
                .map(String::toLowerCase)
                .orElseThrow(() -> new InvalidRequestException(messageSource.getMessage(
                        "auth.request.invalid", null, LocaleContextHolder.getLocale())));

        String passwordHash = Optional.ofNullable(request.getPassword())
                .map(passwordEncoder::encode)
                .orElseThrow(() -> new InvalidRequestException(messageSource.getMessage(
                        "auth.request.invalid", null, LocaleContextHolder.getLocale())));

        String fullName = Optional.ofNullable(request.getFullName())
                .map(String::trim)
                .orElseThrow(() -> new InvalidRequestException(messageSource.getMessage(
                        "auth.request.invalid", null, LocaleContextHolder.getLocale())));

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(messageSource.getMessage(
                    "auth.email.exists", new Object[]{email}, LocaleContextHolder.getLocale()));
        }

        User user = modelMapper.map(request, User.class);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setFullName(fullName);
        user.setRole(RoleType.USER);
        user.setTier(SubscriptionTier.FREE_USER);
        user.setAuthProvider(AuthProvider.LOCAL);
        User saved = userRepository.save(user);

        // first profile for the new account, saved in the same transaction as the user
        profileService.createDefaultProfile(saved.getId(), saved.getFullName());

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {

        String email = Optional.ofNullable(request)
                .map(LoginRequest::getEmail)
                .map(String::trim)
                .map(String::toLowerCase)
                .orElseThrow(() -> new UnauthorizedException(messageSource.getMessage(
                        "auth.invalid.credentials", null, LocaleContextHolder.getLocale())));

        User user = userRepository.findByEmail(email)
                .filter(User::isActive)
                .orElseThrow(() -> new UnauthorizedException(messageSource.getMessage(
                        "auth.invalid.credentials", null, LocaleContextHolder.getLocale())));

        boolean passwordMatches = Optional.ofNullable(user.getPasswordHash())
                .flatMap(hash -> Optional.ofNullable(request.getPassword())
                        .map(raw -> passwordEncoder.matches(raw, hash)))
                .orElse(false);

        if (!passwordMatches) {
            throw new UnauthorizedException(messageSource.getMessage(
                    "auth.invalid.credentials", null, LocaleContextHolder.getLocale()));
        }

        return toResponse(user);
    }

    @Override
    public AuthResponse refresh(RefreshRequest request) {
        // consume() reads and deletes the token, so each refresh token works only once
        Long userId = Optional.ofNullable(request.getRefreshToken())
                .flatMap(refreshTokenStore::consume)
                .orElseThrow(() -> new UnauthorizedException(
                        messageSource.getMessage("auth.refresh.invalid", null, LocaleContextHolder.getLocale())));

        User user = userRepository.findById(userId)
                .filter(User::isActive)
                .orElseThrow(() -> new UnauthorizedException(
                        messageSource.getMessage("auth.refresh.invalid", null, LocaleContextHolder.getLocale())));

        return toResponse(user);
    }

    @Override
    public void logout(String authorizationHeader, RefreshRequest request) {
        String accessToken = Optional.ofNullable(authorizationHeader)
                .filter(header -> header.startsWith("Bearer "))
                .map(header -> header.substring(7))
                .orElseThrow(() -> new InvalidRequestException(
                        messageSource.getMessage("auth.token.missing", null, LocaleContextHolder.getLocale())));

        Optional.ofNullable(request)
                .map(RefreshRequest::getRefreshToken)
                .ifPresent(refreshTokenStore::delete);

        blacklistAccessToken(accessToken);
    }

    @Override
    public void requestOtp(OtpRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (otpStore.isRequestLimitReached(email) || otpStore.isOnCooldown(email)) {
            throw new TooManyRequestsException(
                    messageSource.getMessage("auth.otp.too.many", null, LocaleContextHolder.getLocale()));
        }

        // Generate a code for every email so the rate limits behave the same for known and unknown
        // emails. Only a registered, active user actually gets the email.
        String otp = otpStore.generateAndStore(email);
        userRepository.findByEmail(email)
                .filter(User::isActive)
                .ifPresent(user -> emailService.sendOtp(email, otp));
    }

    @Override
    public AuthResponse verifyOtp(OtpVerifyRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (!otpStore.verify(email, request.getOtp())) {
            throw new UnauthorizedException(
                    messageSource.getMessage("auth.otp.invalid", null, LocaleContextHolder.getLocale()));
        }

        User user = userRepository.findByEmail(email)
                .filter(User::isActive)
                .orElseThrow(() -> new UnauthorizedException(
                        messageSource.getMessage("auth.otp.invalid", null, LocaleContextHolder.getLocale())));

        // Receiving the code proves they own the email
        if (!user.isEmailVerified()) {
            user.setEmailVerified(true);
            userRepository.save(user);
        }

        return toResponse(user);
    }

    private void blacklistAccessToken(String token) {
        try {
            Claims claims = jwtTokenProvider.parse(token);
            refreshTokenStore.blacklist(
                    jwtTokenProvider.getJti(claims),
                    jwtTokenProvider.getRemainingSeconds(claims));
        } catch (JwtException e) {
            // expired or invalid token: nothing to blacklist
        }
    }

    private AuthResponse toResponse(User user) {
        AuthResponse response = new AuthResponse();
        response.setAccessToken(jwtTokenProvider.generateAccessToken(user));
        response.setTokenType("Bearer");
        response.setExpiresInSeconds(jwtTokenProvider.getAccessExpirySeconds());
        response.setRefreshToken(refreshTokenStore.create(user.getId()));
        return response;
    }



}
