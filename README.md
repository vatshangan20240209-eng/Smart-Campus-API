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

By default, JAX-RS creates a **new instance** of each resource class for every incoming HTTP request (request-scoped). This means instance variables are never shared between requests and cannot be used to store data persistently.

This has a direct impact on data management: if data were stored as instance variables inside resource classes, it would be lost after every request. To solve this, all shared state in this project is stored in the static `DataStore` class using `ConcurrentHashMap`. Because `ConcurrentHashMap` is thread-safe, simultaneous requests from multiple clients cannot corrupt data through race conditions. A regular `HashMap` would risk data loss or `ConcurrentModificationException` under concurrent access.

### Part 1.2 — HATEOAS

HATEOAS (Hypermedia as the Engine of Application State) means embedding navigable links inside API responses, so clients can discover available actions without consulting static documentation. For example, a response listing rooms might include a `_links` object pointing to `/api/v1/rooms/{id}` and `/api/v1/sensors`.

This benefits client developers because they do not need to hard-code URLs or keep documentation in sync. The API becomes self-describing — a client can start at `/api/v1` and navigate the entire system from the links returned. This reduces coupling between client and server, making API evolution easier.

### Part 2.1 — Returning IDs vs Full Objects

Returning only IDs in a room list is bandwidth-efficient for large collections, but forces the client to make N additional requests to fetch each room's details (the N+1 problem). Returning full objects increases payload size but allows the client to render the entire list in a single request. The best approach is to return full objects for moderate-sized collections, and use pagination for very large ones.

### Part 2.2 — DELETE Idempotency

Yes, DELETE is idempotent in this implementation. The first call removes the room and returns 200. A second identical call finds no room and returns 404. In both cases, the server state is the same: the room does not exist. No repeated side effects occur. This satisfies the REST idempotency requirement — the result of applying the operation multiple times is identical to applying it once.

### Part 3.1 — @Consumes and Content-Type Mismatch

If a client sends a request with `Content-Type: text/plain` or `application/xml` to an endpoint annotated with `@Consumes(MediaType.APPLICATION_JSON)`, JAX-RS automatically returns **HTTP 415 Unsupported Media Type** before the method body is even executed. The framework matches the incoming `Content-Type` header against the declared `@Consumes` value and rejects non-matching requests at the infrastructure level. This protects the method from receiving unparseable data.

### Part 3.2 — @QueryParam vs Path Parameter for Filtering

Using `@QueryParam` (e.g. `GET /sensors?type=CO2`) is superior for filtering because it clearly communicates that `type` is an optional modifier on a collection, not a distinct resource. Path parameters (e.g. `/sensors/type/CO2`) imply a separate resource hierarchy exists at that path, which is semantically misleading. Query parameters are also composable — multiple filters can be combined (`?type=CO2&status=ACTIVE`) without changing the URL structure. REST convention reserves path segments for resource identity, not filtering criteria.

### Part 4.1 — Sub-Resource Locator Pattern

The sub-resource locator pattern delegates handling of a nested path to a dedicated class. In this project, `SensorResource` handles `/sensors` and locates `SensorReadingResource` for `/sensors/{id}/readings`. This keeps each class focused on a single responsibility. In large APIs with dozens of nested resources, putting every endpoint into one class creates an unmanageable "God class." Separate resource classes are independently testable, easier to maintain, and can be developed in parallel by different team members.

### Part 5.2 — Why 422 is More Accurate than 404

HTTP 404 means the requested URI was not found. HTTP 422 means the request was syntactically valid but semantically unprocessable. When a client POSTs a sensor with a `roomId` that does not exist, the URI `/api/v1/sensors` is perfectly valid — it exists. The problem is inside the request payload: a reference to a non-existent resource. Using 404 would mislead the client into thinking the endpoint itself is missing. 422 correctly signals that the payload's content is the issue, helping clients diagnose and fix their request.

### Part 5.4 — Cybersecurity Risk of Exposing Stack Traces

Exposing raw Java stack traces to API consumers is a significant security risk. A stack trace reveals:
- **Internal package and class names** — confirms the technology stack (Java, specific frameworks)
- **Library versions** — allows attackers to look up known CVEs for those exact versions
- **File paths on the server** — reveals deployment directory structure
- **Business logic flow** — shows which methods were called, exposing internal architecture
- **Database or service names** — if the error originated in a data layer, connection strings or table names may appear

The global `ExceptionMapper<Throwable>` in this project catches all unhandled exceptions, logs the full trace server-side for developers, and returns only a generic 500 message to the client — containing none of the above information.

### Part 5.5 — Why Filters are Better than Manual Logging

Using a JAX-RS filter for logging is a cross-cutting concern approach. The alternative — adding `Logger.info()` calls inside every resource method — means: forgetting to add it to a new endpoint leaves blind spots; changing the log format requires editing every method; and resource classes become cluttered with non-business logic. A single filter class intercepts every request and response automatically, regardless of which endpoint handles them. It is added once, maintained in one place, and cannot be accidentally omitted.
