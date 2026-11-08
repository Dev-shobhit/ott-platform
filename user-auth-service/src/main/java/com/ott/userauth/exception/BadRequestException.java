package com.ott.userauth.exception;

import com.ott.common.exception.OttException;
import org.springframework.http.HttpStatus;

public class BadRequestException extends OttException {

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }
}
