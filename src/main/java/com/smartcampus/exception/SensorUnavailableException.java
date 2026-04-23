package com.smartcampus.exception;

/**
 * Thrown when a POST reading is attempted on a sensor in MAINTENANCE status.
 * Maps to HTTP 403 Forbidden.
 */
public class SensorUnavailableException extends RuntimeException {
    public SensorUnavailableException() {
        super("Sensor is currently under maintenance and cannot accept new readings.");
    }
    public SensorUnavailableException(String message) {
        super(message);
    }
}
