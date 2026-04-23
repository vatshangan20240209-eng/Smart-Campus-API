package com.smartcampus.resource;

import com.smartcampus.config.DataStore;
import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.*;

/**
 * Part 4 - Sub-Resource for Sensor Readings
 * Handles /api/v1/sensors/{sensorId}/readings
 *
 * This class is NOT annotated with @Path at the class level.
 * It is instantiated and returned by SensorResource's sub-resource locator method.
 * JAX-RS then dispatches GET/POST on this class for the /readings path.
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    /**
     * GET /api/v1/sensors/{sensorId}/readings
     * Returns the full reading history for this sensor.
     */
    @GET
    public Response getReadings() {
        Sensor sensor = DataStore.getSensors().get(sensorId);
        if (sensor == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Not Found");
            error.put("message", "Sensor with ID '" + sensorId + "' does not exist.");
            return Response.status(404).entity(error).build();
        }

        List<SensorReading> history = DataStore.getReadingsForSensor(sensorId);
        return Response.ok(history).build();
    }

    /**
     * POST /api/v1/sensors/{sensorId}/readings
     * Appends a new reading to this sensor's history.
     *
     * Side effect: updates the parent Sensor's currentValue field
     * to keep the API data consistent.
     *
     * State constraint: throws SensorUnavailableException (-> 403)
     * if the sensor is in MAINTENANCE status.
     */
    @POST
    public Response addReading(SensorReading reading) {
        Sensor sensor = DataStore.getSensors().get(sensorId);
        if (sensor == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Not Found");
            error.put("message", "Sensor with ID '" + sensorId + "' does not exist.");
            return Response.status(404).entity(error).build();
        }

        // Block readings for sensors under maintenance
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(
                "Sensor '" + sensorId + "' is under MAINTENANCE and cannot accept new readings."
            );
        }

        if (reading == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Bad Request");
            error.put("message", "Reading body is required with a 'value' field.");
            return Response.status(400).entity(error).build();
        }

        // Assign generated ID and current timestamp
        reading.setId("READ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        reading.setTimestamp(System.currentTimeMillis());

        // Store the reading
        DataStore.getReadingsForSensor(sensorId).add(reading);

        // Side effect: keep parent sensor's currentValue in sync
        sensor.setCurrentValue(reading.getValue());

        return Response.status(201).entity(reading).build();
    }

    /**
     * GET /api/v1/sensors/{sensorId}/readings/{readingId}
     * Returns a single reading by its ID.
     */
    @GET
    @Path("/{readingId}")
    public Response getReading(@PathParam("readingId") String readingId) {
        List<SensorReading> history = DataStore.getReadingsForSensor(sensorId);
        return history.stream()
                .filter(r -> readingId.equals(r.getId()))
                .findFirst()
                .map(r -> Response.ok(r).build())
                .orElse(Response.status(404).entity(
                    Map.of("error", "Not Found", "message", "Reading '" + readingId + "' not found.")
                ).build());
    }
}
