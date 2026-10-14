package com.ott.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class OttException extends RuntimeException{

    private final HttpStatus status;
    private final String code;

    protected OttException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

}
