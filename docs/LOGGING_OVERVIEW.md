# Logging Logic – Comprehensive Overview

This document describes how logging is configured, where it is used, and how admins can view logs via the API.

---

## 1. Logging Configuration

### 1.1 Framework and Config File

- **Framework**: Logback (Spring Boot default), configured via `logback-spring.xml`.
- **Location**: `src/main/resources/logback-spring.xml`.

### 1.2 Log File Location

- **Property**: `logging.file.name` (from `application.yml` or environment).
- **Default**: `${LOG_FILE_PATH:${java.io.tmpdir}/booking-spring.log}` (e.g. system temp dir or `logs/spring.log` if set).
- **Logback default** (when property not set): `${java.io.tmpdir}/booking-spring.log`.

So the same file is used by Spring Boot and by the admin log viewer when they share the same `logging.file.name`.

### 1.3 Appenders

| Appender | Type | Destination | Purpose |
|----------|------|-------------|---------|
| **CONSOLE** | `ConsoleAppender` | stdout | Development and container logs |
| **FILE** | `RollingFileAppender` | File from `logging.file.name` | Persistent logs, rotation, admin API |

Both use the same pattern (see below).

### 1.4 Log Format (Pattern)

Single pattern used for CONSOLE and FILE:

```
%d{yyyy-MM-dd'T'HH:mm:ss.SSSXXX} %5p %replace(---){'',''} [%15.15t] %-40.40logger{39} : %m%n%wEx
```

| Part | Meaning |
|------|---------|
| `%d{...}` | Timestamp (ISO with timezone) |
| `%5p` | Level (right-aligned, 5 chars) |
| `%replace(---){'',''}` | Placeholder (no process ID) |
| `[%15.15t]` | Thread name (15 chars) |
| `%-40.40logger{39}` | Logger name (left-aligned, 40 chars) |
| `: %m` | Message |
| `%n` | Newline |
| `%wEx` | Stack trace if present |

**Example line:**

```
2024-01-01T12:00:00.000+00:00  WARN  [http-nio-8080-exec-1] c._g.b.auth.controller.LoginController : Account user@example.com locked for 10 minutes after 3 failed attempts from IP 192.168.1.100
```

### 1.5 File Rotation (FILE appender)

- **Policy**: `SizeAndTimeBasedRollingPolicy`.
- **FileNamePattern**: `${LOG_FILE}.%d{yyyy-MM-dd}.%i.gz`
- **maxFileSize**: 10 MB.
- **maxHistory**: 30 days (or 30 files).
- **totalSizeCap**: 500 MB.

Logs are rolled by date and size, then compressed (`.gz`).

### 1.6 Log Levels

- **Root**: `INFO` → CONSOLE + FILE.
- **`com._glab`**: `DEBUG` (application code).
- **`org.springframework.security`**: `DEBUG` (security layer).

---

## 2. Where Logging Is Used in the Application

### 2.1 Authentication (LoginController and related)

| Event | Level | What is logged |
|-------|--------|-----------------|
| Empty credentials | WARN | Client IP |
| Unknown email | WARN | Email, IP |
| Disabled account login attempt | WARN | Email, IP |
| Locked account login attempt | WARN | Email, IP |
| Lockout expired (30 min) | DEBUG | Email |
| Lockout expired (10 min) | DEBUG | Email, failed count |
| Account locked 30 min | WARN | Email, failed count, IP |
| Account locked 10 min | WARN | Email, failed count, IP |
| Failed login (no lock yet) | WARN | Attempt number, email, IP |
| MFA setup required | INFO | Email, IP |
| MFA challenge issued | INFO | Email, IP |
| Successful login | INFO | Email, IP |
| Refresh without cookie | WARN | IP |
| Refresh with unknown token | WARN | IP |
| Refresh token reuse | ERROR | Email, IP (security) |
| Expired token refresh | DEBUG | Email, IP |
| Refresh for disabled account | WARN | Email, IP |
| Token refreshed | DEBUG | Email, IP |
| Logout | INFO | Email, IP |
| Password setup without token/password | WARN | IP |
| Password setup completed | INFO | Email, IP |

### 2.2 JWT (JwtService)

- Token expired: DEBUG.
- Token invalid: WARN (exception type and message).

### 2.3 Filters

- **JwtAuthenticationFilter**: WARN on JWT authentication failure.
- **RateLimitingFilter**: (no log calls in the snippet; rate limits are applied).

### 2.4 User and Admin

- **UserController**: INFO when admin creates user; DEBUG for fetch/availability checks.
- **AdminUserController**: INFO for admin update role, deactivate, hard delete.
- **UserExceptionHandler**: WARN for registration failures (email/username/role) and user not found.

### 2.5 Booking

- **AvailabilityService**: DEBUG for availability/workstations; WARN when lab not found.
- **ReservationService**: DEBUG for fetches and validation; INFO for create/recurring; WARN for validation/closed days; ERROR on notification email failure.
- **AdminWorkstationController**: INFO for create/update/archive workstation.
- **BookingExceptionHandler**: (handles exceptions; logging depends on implementation.)

### 2.6 Startup

- **DataInitializer**: INFO for created roles, test admin/professor users, anonymous system user.

---

## 3. Admin Log Viewing API

Admins can read and filter application logs via a dedicated REST endpoint. Access is restricted to users with the **ADMIN** role.

### 3.1 Endpoint

| Method | Path | Description |
|--------|------|-------------|
| **GET** | `/api/v1/admin/logs` | Paginated, filterable list of log entries from the log file |

- **Security**: `@PreAuthorize("hasRole('ADMIN')")` on `LogController` → requires valid JWT with ADMIN role.
- **Authentication**: Bearer token in `Authorization` header.

### 3.2 Query Parameters

| Parameter | Required | Default | Description |
|-----------|----------|---------|-------------|
| `level` | No | — | Filter by level: `DEBUG`, `INFO`, `WARN`, `ERROR` (case-insensitive) |
| `dateFrom` | No | — | Start date (ISO date, e.g. `2026-01-01`) |
| `dateTo` | No | — | End date (ISO date) |
| `search` | No | — | Substring search in the raw log line (case-insensitive) |
| `page` | No | `0` | Page index (0-based) |
| `size` | No | `100` | Page size (capped between 1 and 500) |

### 3.3 Example Request

```http
GET /api/v1/admin/logs?level=ERROR&dateFrom=2026-01-01&dateTo=2026-01-31&search=authentication&page=0&size=100
Authorization: Bearer <admin_access_token>
```

### 3.4 Response (200 OK)

JSON array of log entry objects. Each entry can contain:

| Field | Type | Description |
|-------|------|-------------|
| `timestamp` | string | Parsed timestamp from log line (ISO) |
| `level` | string | Parsed level (e.g. INFO, WARN, ERROR) |
| `logger` | string | Logger name (e.g. `c._g.b.auth.controller.LoginController`) |
| `message` | string | Log message text |
| `raw` | string | Full log line (always present; used for non-parseable lines) |

Lines that do not match the expected pattern are returned with only `raw` set.

**Example response body:**

```json
[
  {
    "timestamp": "2026-01-20T10:30:00.000+00:00",
    "level": "ERROR",
    "logger": "c._g.b.auth.controller.LoginController",
    "message": "SECURITY: Refresh token reuse detected for user user@example.com from IP 192.168.1.100",
    "raw": "2026-01-20T10:30:00.000+00:00  ERROR ... [http-nio-8080-exec-1] c._g.b.auth.controller.LoginController : SECURITY: Refresh token reuse..."
  }
]
```

### 3.5 Behaviour of the Log API

- **Data source**: The same file as the FILE appender (`logging.file.name`). If the file is missing or not readable, the API returns an empty list (and logs a WARN in the application log).
- **Order**: Entries are returned in **reverse chronological order** (newest first) after filtering.
- **Parsing**: Lines are matched with a regex that expects the Logback pattern above. Non-matching lines are still returned as entries with only `raw` set.
- **Filtering**: Applied in order: level → dateFrom/dateTo → search; then pagination (page/size) is applied on the filtered list.
- **Pagination**: `page` and `size` are applied after filtering; `size` is clamped to 1–500.

---

## 4. Implementation Components

### 4.1 LogController

- **Class**: `com._glab.booking_system.admin.controller.LogController`
- **Mapping**: `@RequestMapping("/api/v1/admin/logs")`, `@PreAuthorize("hasRole('ADMIN')")`
- **Method**: `GET` → delegates to `LogService.getLogEntries(...)` with query params, clamps `size` to 1–500, returns `ResponseEntity<List<LogEntryResponse>>`.

### 4.2 LogService

- **Class**: `com._glab.booking_system.admin.service.LogService`
- **Role**: Reads the log file from `logging.file.name`, parses lines with a fixed regex, filters by level/date/search, reverses order, then paginates.
- **Log path**: Injected via `@Value("${logging.file.name:logs/spring.log}")`; resolution tries the path as-is and then `user.dir` + path if the first does not exist.
- **Error handling**: On read error, logs WARN and returns an empty list.

### 4.3 LogEntryResponse

- **Class**: `com._glab.booking_system.admin.response.LogEntryResponse`
- **Fields**: `timestamp`, `level`, `logger`, `message`, `raw` (Lombok getters/setters, builder).

### 4.4 Regex (LogService)

The pattern used to parse one log line (aligned with the Logback pattern) is:

- Group 1: timestamp (`yyyy-MM-dd'T'HH:mm:ss.SSS...`)
- Group 2: level
- Group 3: thread (inside `[...]`) — parsed but **not** exposed in `LogEntryResponse`
- Group 4: logger name
- Group 5: message

---

## 5. Security and Deployment Notes

- **Admin-only**: Log viewing is restricted to ADMIN; method security enforces this regardless of path configuration.
- **Sensitive data**: Logs may contain emails, IPs, and error details; restrict access to the log file and to the admin API.
- **File vs API**: In production, ensure `logging.file.name` points to a path the app can read and that the admin log API is only reachable by authenticated admins over a secure channel (HTTPS, network restrictions as needed).
- **Rotation**: The admin API reads the **current** log file only; it does not read rotated/archived files (e.g. `.gz`). For long-term analysis, use the rotated files separately (e.g. external log aggregation).

---

## 6. Summary

| Aspect | Detail |
|--------|--------|
| Config | `logback-spring.xml` + `application.yml` `logging.file.name` |
| Output | Console + rolling file (10 MB, 30 days, 500 MB cap) |
| Levels | Root INFO; `com._glab` and `org.springframework.security` DEBUG |
| Format | ISO timestamp, level, thread, logger, message (+ exception) |
| Usage | Auth, JWT, filters, user/admin, booking, startup |
| Admin API | `GET /api/v1/admin/logs` (ADMIN only), optional level/date/search + pagination |
| Response | List of `LogEntryResponse` (timestamp, level, logger, message, raw), newest first |

This covers the logging setup, where logs are produced, and how admins view them via the log endpoint.
