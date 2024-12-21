package com.example.billing.exception;

import org.springframework.http.HttpStatus;

import com.example.billing.enums.ErrorCodesEnum;

public class TechnicalException extends Exception {

private static final long serialVersionUID = 4593615040723830877L;
	
    private String code;
    private HttpStatus httpStatus;

    public TechnicalException(String code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public TechnicalException(String message, Throwable cause) {
        super(message, cause);
    }

    public TechnicalException(ErrorCodesEnum errorCode) {
        super(errorCode.getDefaultMessage());
        this.code = errorCode.getCode();
        this.httpStatus = errorCode.getDefaultHttpStatus();
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }
    
}
