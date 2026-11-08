package com.ott.userauth.exception;

import com.ott.common.exception.OttException;
import org.springframework.http.HttpStatus;

public class TooManyRequestsException extends OttException {

    public TooManyRequestsException(String message) {
        super(HttpStatus.TOO_MANY_REQUESTS, message, "TOO_MANY_REQUESTS");
    }
}