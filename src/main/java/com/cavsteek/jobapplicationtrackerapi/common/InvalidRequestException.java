package com.cavsteek.jobapplicationtrackerapi.common;

public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }
}