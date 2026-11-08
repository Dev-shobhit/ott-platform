package com.ott.userauth.service;

import com.ott.userauth.dto.*;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshRequest request);

    void logout(String authorizationHeader, RefreshRequest request);

    void requestOtp(OtpRequest request);

    AuthResponse verifyOtp(OtpVerifyRequest request);
}
