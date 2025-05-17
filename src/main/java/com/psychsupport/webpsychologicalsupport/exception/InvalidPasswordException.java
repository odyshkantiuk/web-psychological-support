package com.psychsupport.webpsychologicalsupport.exception;

import java.util.List;

public class InvalidPasswordException extends RuntimeException {
    
    private final List<String> validationErrors;
    
    public InvalidPasswordException(String message, List<String> validationErrors) {
        super(message);
        this.validationErrors = validationErrors;
    }
    
    public List<String> getValidationErrors() {
        return validationErrors;
    }
    
    @Override
    public String getMessage() {
        return super.getMessage() + ": " + String.join(", ", validationErrors);
    }
}