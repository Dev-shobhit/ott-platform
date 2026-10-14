package com.ott.common.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends  OttException {

    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message);
    }
}
