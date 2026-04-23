package com.smartcampus.exception;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Global safety net - catches ALL unhandled exceptions and returns
 * a clean HTTP 500 response with NO stack trace exposed.
 *
 * Security rationale: exposing stack traces reveals internal package names,
 * library versions, file paths, and logic flow - all useful to attackers.
 * This mapper ensures no internal details leak to API consumers.
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable e) {
        // Log the full exception server-side for debugging
        LOG.log(Level.SEVERE, "Unhandled exception caught by global mapper", e);

        // Return a safe, generic response to the client
        Map<String, String> error = new HashMap<>();
        error.put("status", "500 Internal Server Error");
        error.put("error", "Internal Server Error");
        error.put("message", "An unexpected error occurred. Please contact the administrator.");

        return Response.status(500)
                .entity(error)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
