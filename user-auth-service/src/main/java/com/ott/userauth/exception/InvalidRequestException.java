package com.ott.userauth.exception;

import com.ott.common.exception.OttException;
import org.springframework.http.HttpStatus;

public class InvalidRequestException extends OttException {

    public InvalidRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", message);
    }
}