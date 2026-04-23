package com.smartcampus.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

/**
 * Part 1 - Discovery Endpoint
 * GET /api/v1
 *
 * Returns API metadata including version, contact info, and links to
 * primary resource collections (HATEOAS principle).
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @GET
    public Response discover() {
        Map<String, Object> info = new HashMap<>();
        info.put("name", "Smart Campus Sensor & Room Management API");
        info.put("version", "1.0");
        info.put("description", "RESTful API for managing campus rooms and IoT sensors");
        info.put("contact", "admin@smartcampus.ac.uk");
        info.put("status", "operational");

        // HATEOAS: provide navigable links so clients can discover endpoints
        Map<String, String> links = new HashMap<>();
        links.put("self", "/api/v1");
        links.put("rooms", "/api/v1/rooms");
        links.put("sensors", "/api/v1/sensors");
        info.put("_links", links);

        Map<String, String> resources = new HashMap<>();
        resources.put("rooms", "/api/v1/rooms");
        resources.put("sensors", "/api/v1/sensors");
        info.put("resources", resources);

        return Response.ok(info).build();
    }
}
