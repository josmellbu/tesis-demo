package com.example.billing.exception;

import org.springframework.http.HttpStatus;

/**
 * Custom business rule exception that carries HTTP status information
 * 
 * @author josmellbu
 */
public class BusinessRuleException extends Exception {
 
    private long id;
    private String code;  
    private HttpStatus httpStatus;
   
    public BusinessRuleException(long id, String code, String message, HttpStatus httpStatus) {
        super(message);
        this.id = id;
        this.code = code;
        this.httpStatus = httpStatus;
    }
    
    public BusinessRuleException(String code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }
   
    public BusinessRuleException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = HttpStatus.INTERNAL_SERVER_ERROR; // Default status
    }
    
    // Convenience constructors for common scenarios
    public static BusinessRuleException notFound(String message) {
        return new BusinessRuleException("NOT_FOUND", message, HttpStatus.NOT_FOUND);
    }
    
    public static BusinessRuleException badRequest(String message) {
        return new BusinessRuleException("BAD_REQUEST", message, HttpStatus.BAD_REQUEST);
    }
    
    public static BusinessRuleException conflict(String message) {
        return new BusinessRuleException("CONFLICT", message, HttpStatus.CONFLICT);
    }
    
    // Getters and setters
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
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