package com.smartcampus.exception;

/**
 * Thrown when attempting to delete a room that still has sensors assigned.
 * Maps to HTTP 409 Conflict.
 */
public class RoomNotEmptyException extends RuntimeException {
    public RoomNotEmptyException() {
        super("Room still has active sensors assigned to it.");
    }
    public RoomNotEmptyException(String message) {
        super(message);
    }
}
