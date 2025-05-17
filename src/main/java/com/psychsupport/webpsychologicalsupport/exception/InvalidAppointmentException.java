package com.psychsupport.webpsychologicalsupport.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidAppointmentException extends RuntimeException {
    public InvalidAppointmentException(String message) {
        super(message);
    }
}