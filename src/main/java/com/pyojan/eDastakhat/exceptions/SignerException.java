package com.pyojan.eDastakhat.exceptions;

/**
 * Custom exception class for handling errors during the signing process.
 */
public class SignerException extends Exception {

    /**
     * Constructs a new SignerException with the specified detail message.
     *
     * @param message the detail message
     */
    public SignerException(String message) {
        super(message);
    }

    /**
     * Constructs a new SignerException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of the exception
     */
    public SignerException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new SignerException with the specified cause.
     *
     * @param cause the cause of the exception
     */
    public SignerException(Throwable cause) {
        super(cause);
    }
}
