package com.pyojan.eDastakhat.exceptions;

public class UserCancelledException extends Exception {

    // Constructor that accepts a message
    public UserCancelledException(String message) {
        super(message);
    }

    // Constructor that accepts a message and a cause
    public UserCancelledException(String message, Throwable cause) {
        super(message, cause);
    }
}
