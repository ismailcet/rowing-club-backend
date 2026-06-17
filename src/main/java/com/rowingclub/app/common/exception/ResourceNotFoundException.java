package com.rowingclub.app.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resource, String field, Object value) {
        super(resource + " bulunamadı: " + field + " = " + value, HttpStatus.NOT_FOUND);
    }
}