# KV Store

A production-grade version-controlled Key-Value Store API built with Java 21 and Spring Boot 3.

[![CI/CD Pipeline](https://github.com/jukyeong-git/kv-store/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/jukyeong-git/kv-store/actions/workflows/ci-cd.yml)

## Live Demo

- **API Docs (Swagger UI):** https://jukyeong-git.github.io
- **API Base URL:** https://kv-store-uebl.onrender.com

> Note: Deployed on Render free tier. Cold start may occur after 15 minutes of inactivity.

---

## Features

- Automatic version management on every save
- Point-in-time value retrieval via UNIX timestamp (UTC)
- Concurrency-safe with Redis distributed lock
- DB-level fallback lock when Redis is unavailable
- Java 21 Virtual Threads for non-blocking request handling

---

## Tech Stack

| Category      | Technology                          |
|---------------|-------------------------------------|
| Language      | Java 21                             |
| Framework     | Spring Boot 3.5                     |
| Database      | PostgreSQL (Supabase)               |
| Cache / Lock  | Redis (Upstash)                     |
| Deployment    | Docker + Render                     |
| CI/CD         | GitHub Actions                      |
| Coverage      | JaCoCo                              |
| API Docs      | SpringDoc OpenAPI (Swagger UI)      |

---

## API Endpoints

| Method | Endpoint                          | Description                              |
|--------|-----------------------------------|------------------------------------------|
| POST   | `/object`                         | Save a key-value pair (auto-versioning)  |
| GET    | `/object/{key}`                   | Get the latest value for a key           |
| GET    | `/object/{key}?timestamp={unix}`  | Get the value at a specific point in time|
| GET    | `/object/get_all_records`         | Get all keys with their latest values    |

### Example

```bash
# Save a value
POST /object
{
  "key": "mykey",
  "value": "hello world"
}

# Response
{
  "key": "mykey",
  "value": "hello world",
  "version": 1,
  "createdAt": 1751800200,
  "createdAtFormatted": "2025-07-06T18:00:00Z"
}

# Get latest value
GET /object/mykey

# Get value at a specific time
GET /object/mykey?timestamp=1751800200

# Get all records
GET /object/get_all_records
```

---

## Architecture

```
Client (Postman / Swagger UI)
        ↓
Render (Spring Boot)
kv-store-uebl.onrender.com
        ↓              ↓
Supabase           Upstash Redis
PostgreSQL         (Distributed Lock)
(Data Storage)
```

### Concurrency Design

50+ concurrent requests are handled safely using a Redis distributed lock:

```
Request 1: Acquire lock ✅ → Read latest version → Increment → Save → Release lock
Request 2: Wait for lock ⏳
Request 3: Wait for lock ⏳
...
→ Versions 1~51 are stored correctly with no duplicates or gaps
```

If Redis is unavailable, the system automatically falls back to a DB-level pessimistic lock.

---

## Design Decisions

### Why Redis distributed lock instead of DB pessimistic lock?

DB pessimistic locks hold DB connections during the wait, risking connection pool exhaustion under high concurrency. Redis handles the wait externally, keeping DB connections free until needed.

### Why both Redis lock and DB unique constraint?

Redis is the primary concurrency guard. The `UNIQUE (kv_key, version)` constraint is a last-resort safety net in case of Redis failure or direct DB access without the lock.

### Why not use idempotency keys?

The core requirement is that every request creates a new version. Idempotency keys would prevent duplicate versions, which conflicts with the spec. This decision is intentional and documented here.

### Why store `createdAt` as UNIX timestamp?

UNIX timestamps in UTC are timezone-agnostic and allow efficient range queries. A human-readable `createdAtFormatted` field (ISO 8601) is also included in the response for readability.

---

## Running Locally

### Prerequisites

- Java 21
- Docker (for Redis)

### Setup

```bash
# Clone the repository
git clone https://github.com/jukyeong-git/kv-store.git
cd kv-store

# Start Redis
docker run -d -p 6379:6379 --name redis redis:latest

# Run the application (local profile uses H2 in-memory DB)
./gradlew bootRun
```

### Access

```
Swagger UI:  http://localhost:8080/swagger-ui.html
H2 Console:  http://localhost:8080/h2-console
             JDBC URL: jdbc:h2:mem:kvstore
```

---

## Running Tests

```bash
# Run all tests
./gradlew test

# Run tests with coverage report
./gradlew test jacocoTestReport

# View coverage report
open build/reports/jacoco/test/html/index.html
```

### Test Coverage

| Layer       | Tests                                               |
|-------------|-----------------------------------------------------|
| Service     | Version management, timestamp queries, concurrency  |
| Controller  | Request validation, response structure, HTTP status |
| Repository  | Query correctness, unique constraint enforcement    |
| Exception   | 404, 415, 400 error handling                        |

---

## CI/CD

Every push to `main` triggers the pipeline:

```
Push to main
     ↓
GitHub Actions
     ↓
Build → Test → JaCoCo Coverage Report
     ↓
Deploy to Render (only if all tests pass)
```

Coverage reports are uploaded as GitHub Actions artifacts on every run.

---

## Database Schema

```sql
CREATE TABLE kv_store (
    id         BIGSERIAL    PRIMARY KEY,
    kv_key     VARCHAR(255) NOT NULL,
    kv_value   TEXT         NOT NULL,
    version    INTEGER      NOT NULL,
    created_at BIGINT       NOT NULL,
    CONSTRAINT uq_key_version UNIQUE (kv_key, version)
);
```

---

## Future Improvements

- **Kafka integration**: Publish version change events to downstream services
- **CQRS pattern**: Separate read/write services for higher throughput
- **Cloud migration**: Planning to migrate to a better cloud environment
- **Metrics**: Expose Prometheus metrics via Spring Actuator

---

## Author

**Ryan Jukyeong Kim**
- GitHub: [@jukyeong-git](https://github.com/jukyeong-git)
