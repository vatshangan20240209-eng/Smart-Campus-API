# Smart Campus Sensor & Room Management API

A RESTful API built with JAX-RS (Jersey) for managing campus rooms and IoT sensors.

---

## API Overview

Base URL: `http://localhost:8080/api/v1`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/v1 | API discovery / metadata |
| GET | /api/v1/rooms | List all rooms |
| POST | /api/v1/rooms | Create a room |
| GET | /api/v1/rooms/{id} | Get a room |
| DELETE | /api/v1/rooms/{id} | Delete a room (blocked if sensors exist) |
| GET | /api/v1/sensors | List sensors (optional ?type= filter) |
| POST | /api/v1/sensors | Register a sensor |
| GET | /api/v1/sensors/{id} | Get a sensor |
| DELETE | /api/v1/sensors/{id} | Delete a sensor |
| GET | /api/v1/sensors/{id}/readings | Get reading history |
| POST | /api/v1/sensors/{id}/readings | Add a reading |

---

## How to Build & Run

### Prerequisites
- Java 11+
- Apache Maven 3.6+
- NetBeans IDE 12+ (or any IDE with Maven support)

### Option A — NetBeans (Recommended)
1. Open NetBeans → File → Open Project → select the `smart-campus-api` folder
2. Right-click the project → Clean and Build
3. Right-click the project → Run (uses embedded Tomcat on port 8080)
4. API is available at: `http://localhost:8080/api/v1`

### Option B — Command Line
```bash
cd smart-campus-api
mvn clean package tomcat7:run
```

### Option C — Deploy to Tomcat
```bash
mvn clean package
# Copy target/smart-campus-api.war to Tomcat's webapps/ folder
# Then start Tomcat
```

---

## Sample curl Commands

### 1. API Discovery
```bash
curl -X GET http://localhost:8080/api/v1
```

### 2. Create a Room
```bash
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"name": "Library Quiet Study", "capacity": 50}'
```

### 3. Create a Sensor (use a real roomId from step 2)
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"type": "CO2", "status": "ACTIVE", "currentValue": 400.0, "roomId": "ROOM-XXXXXXXX"}'
```

### 4. Filter Sensors by Type
```bash
curl -X GET "http://localhost:8080/api/v1/sensors?type=CO2"
```

### 5. Post a Sensor Reading (use real sensorId from step 3)
```bash
curl -X POST http://localhost:8080/api/v1/sensors/SENS-XXXXXXXX/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 520.5}'
```

### 6. Get Reading History
```bash
curl -X GET http://localhost:8080/api/v1/sensors/SENS-XXXXXXXX/readings
```

### 7. Try to Delete a Room with Sensors (expects 409 Conflict)
```bash
curl -X DELETE http://localhost:8080/api/v1/rooms/ROOM-XXXXXXXX
```

### 8. Try to Add Reading to MAINTENANCE Sensor (expects 403 Forbidden)
```bash
# First set sensor to MAINTENANCE via POST, then try adding a reading
curl -X POST http://localhost:8080/api/v1/sensors/SENS-XXXXXXXX/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 100.0}'
```

---

## Conceptual Report — Question Answers

### Part 1.1 — JAX-RS Resource Lifecycle

By default, JAX-RS creates a new instance of a resource class for each incoming HTTP request. This is known as request-scoped behavior. Because of this, instance variables are not shared between requests and cannot be used to store data permanently.

This directly affects how data is managed. If data were stored in instance variables inside resource classes, it would be lost after each request is completed. To avoid this, all shared data in this project is stored in a static DataStore class using ConcurrentHashMap. Since ConcurrentHashMap is thread-safe, it allows multiple clients to access and modify data at the same time without causing race conditions. In contrast, using a regular HashMap could lead to data loss or ConcurrentModificationException when accessed concurrently.

### Part 1.2 — HATEOAS
HATEOAS means including navigable links in API responses so that clients can discover what actions are available without relying on fixed documentation. For example, when a list of rooms is returned, the response might include a _links section with URLs like /api/v1/rooms/{id} and /api/v1/sensors.

This approach is useful for client developers because they don’t have to hard-code URLs or constantly refer back to documentation. Instead, the API becomes self-describing. A client can start at /api/v1 and follow the links provided to move through the system. This reduces the dependency between the client and server, which makes it easier to update or expand the API later on.
### Part 2.1 — Returning IDs vs Full Objects

Returning only IDs in a room list is more efficient in terms of bandwidth, especially for large collections. However, it forces the client to make additional requests to get the full details for each room, leading to the N+1 problem. On the other hand, returning full objects increases the size of the response, but allows the client to display all the necessary information with a single request.

A balanced approach is to return full objects when dealing with moderately sized collections, and apply pagination when the dataset becomes very large.

### Part 2.2 — DELETE Idempotency

DELETE is idempotent in this implementation. The first request removes the room and returns a 200 response. If the same request is sent again, the room no longer exists, so the server returns a 404. In both situations, the final state of the system is the same—the room is not present. There are no additional side effects from repeating the request. Therefore, this behavior satisfies the REST requirement for idempotency, since performing the operation multiple times has the same result as performing it once.

### Part 3.1 — @Consumes and Content-Type Mismatch

If a client sends a request with Content-Type: text/plain or application/xml to an endpoint that only accepts JSON (@Consumes(MediaType.APPLICATION_JSON)), JAX-RS will automatically return an HTTP 415 Unsupported Media Type error. This happens before the method is even executed. The framework checks whether the request’s Content-Type matches what the endpoint expects, and if it doesn’t, the request is rejected immediately. This helps prevent the method from receiving data it cannot properly process.
### Part 3.2 — @QueryParam vs Path Parameter for Filtering

Using @QueryParam (for example, GET /sensors?type=CO2) is better for filtering because it shows that type is just an optional filter on a collection, not a separate resource. In contrast, using a path like /sensors/type/CO2 suggests a different resource structure, which can be misleading.

Query parameters are also more flexible since you can combine multiple filters easily (e.g., ?type=CO2&status=ACTIVE) without changing the overall URL format. In REST, path segments are usually meant to identify specific resources, while query parameters are meant for filtering or modifying the results.

### Part 4.1 — Sub-Resource Locator Pattern

The sub-resource locator pattern is used to pass the handling of a nested path to a separate class. In this project, SensorResource manages the /sensors endpoint and delegates /sensors/{id}/readings to SensorReadingResource. This helps keep each class focused on a single responsibility.

In larger APIs with many nested resources, placing all endpoints in one class can lead to a difficult-to-manage “God class.” By separating them into different resource classes, the code becomes easier to test, maintain, and extend. It also allows multiple team members to work on different parts of the API at the same time without conflicts.

### Part 5.2 — Why 422 is More Accurate than 404

HTTP 404 means the requested URI could not be found, while HTTP 422 means the request is syntactically correct but cannot be processed because of its content.
For example, if a client sends a POST request to create a sensor with a roomId that doesn’t exist, the endpoint /api/v1/sensors is still valid and accessible. The issue is within the request body, where it refers to a resource that isn’t there. Returning a 404 in this case could make it seem like the endpoint itself is missing, which isn’t true.
Using 422 is more accurate because it clearly indicates that the problem lies in the request data, making it easier for the client to understand and fix the error.

### Part 5.4 — Cybersecurity Risk of Exposing Stack Traces

Exposing raw Java stack traces to API users is a serious security risk. A stack trace can reveal a lot of sensitive internal details, such as:

Internal package and class names, which can confirm the technology stack being used (like Java and specific frameworks)
Library versions, making it easier for attackers to find known vulnerabilities (CVEs)
File paths on the server, exposing the system’s directory structure
The flow of business logic, showing which methods were executed and giving insight into the application’s design
Database or service names, especially if the error comes from the data layer, where things like table names or connection details might appear

In this project, a global ExceptionMapper<Throwable> is used to catch any unhandled exceptions. It logs the full stack trace on the server for developers to review, but only returns a simple 500 error message to the client, without exposing any of these internal details.
### Part 5.5 — Why Filters are Better than Manual Logging

Using a JAX-RS filter for logging is a good way to handle cross-cutting concerns. The alternative—adding Logger.info() calls in every resource method—has several drawbacks. It’s easy to forget to include logging in new endpoints, which creates gaps. If the log format needs to change, every method has to be updated. It also makes resource classes cluttered with code that isn’t part of the core business logic.
With a filter, logging is handled in one place. It automatically intercepts every request and response, no matter which endpoint processes them. This means it only needs to be added once, is easier to maintain, and won’t be accidentally left out.
