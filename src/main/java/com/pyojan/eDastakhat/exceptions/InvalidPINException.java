package com.pyojan.eDastakhat.exceptions;

public class InvalidPINException extends Exception {
    public InvalidPINException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidPINException(String message) {
        super(message);
    }
}
