package com.smartcampus.resource;

import com.smartcampus.config.DataStore;
import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Sensor;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Part 3 - Sensor Operations
 * Handles /api/v1/sensors and delegates /api/v1/sensors/{id}/readings
 * to SensorReadingResource via a sub-resource locator.
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    /**
     * GET /api/v1/sensors
     * GET /api/v1/sensors?type=CO2
     *
     * Optional @QueryParam("type") filters results by sensor type.
     * Query param approach is superior to path-based filtering (/sensors/type/CO2)
     * because it clearly signals "optional filter on a collection" rather than
     * implying a separate sub-resource hierarchy.
     */
    @GET
    public Response getSensors(@QueryParam("type") String type) {
        List<Sensor> result = new ArrayList<>(DataStore.getSensors().values());

        if (type != null && !type.isEmpty()) {
            result = result.stream()
                    .filter(s -> s.getType() != null && s.getType().equalsIgnoreCase(type))
                    .collect(Collectors.toList());
        }

        return Response.ok(result).build();
    }

    /**
     * POST /api/v1/sensors
     * Registers a new sensor. Validates that the referenced roomId exists.
     *
     * @Consumes(APPLICATION_JSON): If client sends wrong content-type,
     * JAX-RS returns 415 Unsupported Media Type automatically.
     * If the body cannot be parsed as JSON, JAX-RS returns 400 Bad Request.
     */
    @POST
    public Response createSensor(Sensor sensor) {
        if (sensor == null || sensor.getRoomId() == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Bad Request");
            error.put("message", "Sensor must include a roomId.");
            return Response.status(400).entity(error).build();
        }

        // Validate that the referenced room actually exists
        if (!DataStore.getRooms().containsKey(sensor.getRoomId())) {
            throw new LinkedResourceNotFoundException(
                "Cannot register sensor: room with ID '" + sensor.getRoomId() + "' does not exist."
            );
        }

        // Generate unique ID
        if (sensor.getId() == null || sensor.getId().isEmpty()) {
            sensor.setId("SENS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }

        // Default status to ACTIVE if not set
        if (sensor.getStatus() == null || sensor.getStatus().isEmpty()) {
            sensor.setStatus("ACTIVE");
        }

        DataStore.getSensors().put(sensor.getId(), sensor);

        // Link sensor ID back to the room
        DataStore.getRooms().get(sensor.getRoomId()).getSensorIds().add(sensor.getId());

        return Response.status(201).entity(sensor).build();
    }

    /**
     * GET /api/v1/sensors/{sensorId}
     * Returns details of a single sensor.
     */
    @GET
    @Path("/{sensorId}")
    public Response getSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = DataStore.getSensors().get(sensorId);
        if (sensor == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Not Found");
            error.put("message", "Sensor with ID '" + sensorId + "' does not exist.");
            return Response.status(404).entity(error).build();
        }
        return Response.ok(sensor).build();
    }

    /**
     * DELETE /api/v1/sensors/{sensorId}
     * Deletes a sensor and removes it from its parent room's sensorIds list.
     */
    @DELETE
    @Path("/{sensorId}")
    public Response deleteSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = DataStore.getSensors().get(sensorId);
        if (sensor == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Not Found");
            error.put("message", "Sensor with ID '" + sensorId + "' does not exist.");
            return Response.status(404).entity(error).build();
        }

        // Remove sensor ID from parent room
        String roomId = sensor.getRoomId();
        if (roomId != null && DataStore.getRooms().containsKey(roomId)) {
            DataStore.getRooms().get(roomId).getSensorIds().remove(sensorId);
        }

        DataStore.getSensors().remove(sensorId);

        Map<String, String> result = new HashMap<>();
        result.put("message", "Sensor '" + sensorId + "' has been successfully deleted.");
        return Response.ok(result).build();
    }

    /**
     * Part 4 - Sub-Resource Locator
     * Delegates /api/v1/sensors/{sensorId}/readings to SensorReadingResource.
     *
     * No HTTP method annotation here - JAX-RS sees this as a locator,
     * not an endpoint itself. The actual GET/POST are defined in SensorReadingResource.
     *
     * Benefit: SensorReadingResource is a separate, focused class.
     * Large APIs can have dozens of sub-resources without one class becoming
     * unmanageable. Each class has a single responsibility.
     */
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingsResource(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId);
    }
}
