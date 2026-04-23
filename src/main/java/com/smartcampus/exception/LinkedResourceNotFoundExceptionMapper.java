package com.smartcampus.exception;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.HashMap;
import java.util.Map;

/**
 * Maps LinkedResourceNotFoundException -> HTTP 422 Unprocessable Entity
 *
 * 422 is more semantically accurate than 404 here because the request URI
 * itself is valid - the problem is inside the JSON payload (a roomId that
 * does not refer to an existing resource).
 */
@Provider
public class LinkedResourceNotFoundExceptionMapper implements ExceptionMapper<LinkedResourceNotFoundException> {

    @Override
    public Response toResponse(LinkedResourceNotFoundException e) {
        Map<String, String> error = new HashMap<>();
        error.put("status", "422 Unprocessable Entity");
        error.put("error", "Linked Resource Not Found");
        error.put("message", e.getMessage());
        return Response.status(422)
                .entity(error)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
