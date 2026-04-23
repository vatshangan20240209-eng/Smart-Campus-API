package com.smartcampus.resource;

import com.smartcampus.config.DataStore;
import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.*;

/**
 * Part 2 - Room Management
 * Handles all operations on /api/v1/rooms
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    /**
     * GET /api/v1/rooms
     * Returns a list of all rooms.
     */
    @GET
    public Response getAllRooms() {
        Collection<Room> allRooms = DataStore.getRooms().values();
        return Response.ok(new ArrayList<>(allRooms)).build();
    }

    /**
     * POST /api/v1/rooms
     * Creates a new room. Returns 201 Created with the new room object.
     *
     * @Consumes(APPLICATION_JSON): If client sends wrong content type (e.g. text/plain),
     * JAX-RS automatically returns 415 Unsupported Media Type before the method is called.
     */
    @POST
    public Response createRoom(Room room) {
        if (room == null || room.getName() == null || room.getName().isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Bad Request");
            error.put("message", "Room name is required.");
            return Response.status(400).entity(error).build();
        }

        // Generate a unique ID if not provided
        if (room.getId() == null || room.getId().isEmpty()) {
            room.setId("ROOM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }

        DataStore.getRooms().put(room.getId(), room);

        return Response.status(201).entity(room).build();
    }

    /**
     * GET /api/v1/rooms/{roomId}
     * Returns details of a specific room.
     */
    @GET
    @Path("/{roomId}")
    public Response getRoom(@PathParam("roomId") String roomId) {
        Room room = DataStore.getRooms().get(roomId);
        if (room == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Not Found");
            error.put("message", "Room with ID '" + roomId + "' does not exist.");
            return Response.status(404).entity(error).build();
        }
        return Response.ok(room).build();
    }

    /**
     * DELETE /api/v1/rooms/{roomId}
     * Deletes a room only if it has no sensors assigned.
     *
     * Idempotency: DELETE is idempotent by REST convention. First call removes
     * the room and returns 204. Subsequent calls find nothing and return 404.
     * The server state is the same after either: room does not exist.
     * No side effects occur on repeated calls.
     *
     * Business constraint: throws RoomNotEmptyException (-> 409) if sensors exist.
     */
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = DataStore.getRooms().get(roomId);

        if (room == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Not Found");
            error.put("message", "Room with ID '" + roomId + "' does not exist.");
            return Response.status(404).entity(error).build();
        }

        // Safety check: prevent data orphans
        if (!room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(
                "Cannot delete room '" + roomId + "'. It still has " +
                room.getSensorIds().size() + " sensor(s) assigned: " +
                room.getSensorIds().toString()
            );
        }

        DataStore.getRooms().remove(roomId);

        Map<String, String> result = new HashMap<>();
        result.put("message", "Room '" + roomId + "' has been successfully deleted.");
        return Response.ok(result).build();
    }
}
