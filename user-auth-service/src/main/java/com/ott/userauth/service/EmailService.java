package com.ott.userauth.service;

public interface EmailService {

    void sendOtp(String to, String otp);
}