package com.smartcampus.exception;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.HashMap;
import java.util.Map;

/**
 * Maps RoomNotEmptyException -> HTTP 409 Conflict
 */
@Provider
public class RoomNotEmptyExceptionMapper implements ExceptionMapper<RoomNotEmptyException> {

    @Override
    public Response toResponse(RoomNotEmptyException e) {
        Map<String, String> error = new HashMap<>();
        error.put("status", "409 Conflict");
        error.put("error", "Room Not Empty");
        error.put("message", e.getMessage());
        return Response.status(409)
                .entity(error)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
