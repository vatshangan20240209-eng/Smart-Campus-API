package com.smartcampus.config;

import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central in-memory data store for the Smart Campus API.
 *
 * Uses ConcurrentHashMap to ensure thread safety when multiple requests
 * read/write simultaneously (avoids race conditions and data corruption).
 * No database is used - all data is stored in memory only.
 */
public class DataStore {

    // All maps are static so they are shared across ALL resource class instances
    private static final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private static final Map<String, Sensor> sensors = new ConcurrentHashMap<>();
    private static final Map<String, List<SensorReading>> readings = new ConcurrentHashMap<>();

    // Private constructor - this is a utility class, not instantiated
    private DataStore() {}

    public static Map<String, Room> getRooms() {
        return rooms;
    }

    public static Map<String, Sensor> getSensors() {
        return sensors;
    }

    public static Map<String, List<SensorReading>> getReadings() {
        return readings;
    }

    /**
     * Helper: get or create a readings list for a sensor
     */
    public static List<SensorReading> getReadingsForSensor(String sensorId) {
        return readings.computeIfAbsent(sensorId, k -> new CopyOnWriteArrayList<>());
    }
}
