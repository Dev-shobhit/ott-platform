package com.ott.userauth.exception;

import com.ott.common.exception.OttException;
import org.springframework.http.HttpStatus;

public class EmailAlreadyExistsException extends OttException {

    public EmailAlreadyExistsException(String message) {
        super(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", message);
    }
}