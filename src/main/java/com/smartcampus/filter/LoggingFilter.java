package com.smartcampus.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * JAX-RS filter for API request and response logging.
 *
 * Implements both ContainerRequestFilter and ContainerResponseFilter
 * as a single cross-cutting concern class, avoiding the need to add
 * Logger.info() calls inside every single resource method.
 *
 * This approach is superior because:
 * - Single place to change logging format or level
 * - Resource classes stay clean (single responsibility)
 * - Cannot be accidentally omitted from a new endpoint
 */
@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOG = Logger.getLogger(LoggingFilter.class.getName());

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        LOG.info(String.format(
            "--> REQUEST:  %s %s",
            requestContext.getMethod(),
            requestContext.getUriInfo().getRequestUri()
        ));
    }

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        LOG.info(String.format(
            "<-- RESPONSE: %d %s",
            responseContext.getStatus(),
            requestContext.getUriInfo().getRequestUri()
        ));
    }
}
