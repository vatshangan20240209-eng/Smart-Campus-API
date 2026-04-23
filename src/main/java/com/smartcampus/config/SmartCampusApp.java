package com.smartcampus.config;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;


/**
 * JAX-RS application configuration for WAR deployment.
 * This class is discovered by the servlet container and exposes the API under /api/v1.
 */
public class SmartCampusApp extends ResourceConfig {

    public SmartCampusApp() {
        packages("com.smartcampus");
        register(JacksonFeature.class);
    }
}
