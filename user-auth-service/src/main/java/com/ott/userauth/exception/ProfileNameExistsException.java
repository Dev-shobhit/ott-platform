package com.ott.userauth.exception;

import com.ott.common.exception.OttException;
import org.springframework.http.HttpStatus;

public class ProfileNameExistsException extends OttException {

    public ProfileNameExistsException(String message) {
        super(HttpStatus.CONFLICT, message, "PROFILE_NAME_EXISTS");
    }
}