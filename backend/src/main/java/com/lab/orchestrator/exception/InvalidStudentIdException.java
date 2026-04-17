package com.lab.orchestrator.exception;

/**
 * Thrown when a provided studentId fails validation.
 *
 * <p>We use a dedicated exception so global error handling can consistently return HTTP 400
 * instead of accidentally mapping validation failures to 500.</p>
 */
public class InvalidStudentIdException extends IllegalArgumentException {

    public InvalidStudentIdException(String message) {
        super(message);
    }

    public InvalidStudentIdException(String message, Throwable cause) {
        super(message, cause);
    }
}

