package com.chatgemma.service.exception;

public class UserNotApprovedException extends RuntimeException {
    public UserNotApprovedException(String message) {
        super(message);
    }

    public UserNotApprovedException(String message, Throwable cause) {
        super(message, cause);
    }
}