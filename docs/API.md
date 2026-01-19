# API Documentation

Complete REST API reference for the 5GLab Booking System.

**Base URL**: `http://localhost:8080/api/v1`

**Interactive Docs**: [Swagger UI](http://localhost:8080/swagger-ui.html)

---

## Authentication Endpoints

All authentication endpoints are under `/api/v1/auth`.

### POST /auth/login

Authenticate a user and receive access tokens.

#### Request

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "yourPassword123"
}
```

#### Response (200 OK) - No MFA

```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "email": "user@example.com",
    "role": "PROFESSOR"
  }
}
```

A `refreshToken` cookie is also set (httpOnly, Secure).

#### Response (200 OK) - MFA Required

If the user has MFA enabled, returns an MFA challenge instead:

```json
{
  "mfaToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

Use this `mfaToken` with the `/auth/mfa/verify` endpoint to complete login.

#### Error Responses

| Status | Code | Description |
|--------|------|-------------|
| 401 | `AUTH_INVALID_CREDENTIALS` | Email or password incorrect |
| 403 | `AUTH_ACCOUNT_DISABLED` | Account is disabled |
| 403 | `AUTH_MFA_SETUP_REQUIRED` | Admin/Lab Manager must set up MFA first |
| 423 | `AUTH_ACCOUNT_LOCKED` | Account is temporarily locked |

#### Example Error Response

```json
{
  "status": "AUTH_INVALID_CREDENTIALS",
  "message": "Invalid credentials"
}
```

---

### POST /auth/refresh

Refresh the access token using the refresh token cookie.

#### Request

```http
POST /api/v1/auth/refresh
Cookie: refreshToken=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
```

No request body required. The refresh token is read from the httpOnly cookie.

#### Response (200 OK)

```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "email": "user@example.com",
    "role": "PROFESSOR"
  }
}
```

A new `refreshToken` cookie is set (token rotation).

#### Error Responses

| Status | Code | Description |
|--------|------|-------------|
| 401 | `AUTH_INVALID_REFRESH_TOKEN` | Token missing or invalid |
| 401 | `AUTH_REFRESH_TOKEN_EXPIRED` | Token has expired |
| 401 | `AUTH_REFRESH_TOKEN_REUSE_DETECTED` | Security alert: token reuse detected |
| 403 | `AUTH_ACCOUNT_DISABLED` | Account was disabled |

---

### POST /auth/logout

Revoke the refresh token and clear the cookie.

#### Request

```http
POST /api/v1/auth/logout
Cookie: refreshToken=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
```

#### Response (204 No Content)

No response body. The `refreshToken` cookie is cleared.

---

### POST /auth/setup-password

Set up initial password or reset forgotten password using a setup token.

#### Request

```http
POST /api/v1/auth/setup-password
Content-Type: application/json

{
  "token": "abc123def456...",
  "newPassword": "SecurePassword123!"
}
```

#### Response (200 OK)

```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "email": "user@example.com",
    "role": "PROFESSOR"
  }
}
```

The user is automatically logged in after setting the password.

#### Error Responses

| Status | Code | Description |
|--------|------|-------------|
| 400 | `AUTH_PASSWORD_TOKEN_INVALID` | Token not found or already used |
| 400 | `AUTH_PASSWORD_TOKEN_EXPIRED` | Token has expired |

---

## MFA Endpoints

Multi-Factor Authentication endpoints for setting up and verifying 2FA.

> **Note**: MFA is **mandatory** for Admin and Lab Manager roles. Professors can optionally enable MFA.

### POST /auth/mfa/setup

Start MFA setup by generating a TOTP secret and QR code.

**Requires Authentication**: Yes (Bearer token)

#### Request

```http
POST /api/v1/auth/mfa/setup
Authorization: Bearer <access_token>
```

#### Response (200 OK)

```json
{
  "secret": "JBSWY3DPEHPK3PXP",
  "qrCodeDataUri": "data:image/png;base64,iVBORw0KGgo...",
  "otpAuthUri": "otpauth://totp/5GLab%20Booking:user@example.com?secret=JBSWY3DPEHPK3PXP&issuer=5GLab%20Booking"
}
```

Display the QR code to the user for scanning with an authenticator app.

#### Error Responses

| Status | Code | Description |
|--------|------|-------------|
| 409 | `AUTH_MFA_ALREADY_ENABLED` | MFA is already enabled |

---

### POST /auth/mfa/setup/verify

Complete MFA setup by verifying the first TOTP code.

**Requires Authentication**: Yes (Bearer token)

#### Request

```http
POST /api/v1/auth/mfa/setup/verify
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "secret": "JBSWY3DPEHPK3PXP",
  "code": "123456"
}
```

#### Response (200 OK)

```json
{
  "enabled": true,
  "backupCodes": [
    "ABCD-1234",
    "EFGH-5678",
    "IJKL-9012",
    "..."
  ],
  "message": "MFA has been enabled. Please save your backup codes in a safe place."
}
```

> ⚠️ **Important**: Backup codes are shown **only once**. Users must save them securely.

#### Error Responses

| Status | Code | Description |
|--------|------|-------------|
| 401 | `AUTH_MFA_INVALID_CODE` | TOTP code is incorrect |
| 409 | `AUTH_MFA_ALREADY_ENABLED` | MFA is already enabled |

---

### POST /auth/mfa/verify

Verify MFA code during login (after password verification).

**Requires Authentication**: No (uses `mfaToken` from login response)

#### Request

```http
POST /api/v1/auth/mfa/verify
Content-Type: application/json

{
  "mfaToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "code": "123456",
  "codeType": "TOTP"
}
```

**Code Types**:
- `TOTP` - 6-digit code from authenticator app (default)
- `EMAIL` - 6-digit code sent via email
- `BACKUP` - One-time backup code (e.g., "ABCD-1234")

#### Response (200 OK)

```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "email": "user@example.com",
    "role": "ADMIN"
  }
}
```

A `refreshToken` cookie is also set.

#### Error Responses

| Status | Code | Description |
|--------|------|-------------|
| 401 | `AUTH_MFA_TOKEN_INVALID` | MFA token is invalid |
| 401 | `AUTH_MFA_TOKEN_EXPIRED` | MFA token has expired (5 min limit) |
| 401 | `AUTH_MFA_INVALID_CODE` | Verification code is incorrect |

---

### POST /auth/mfa/email-code

Request an email OTP as an alternative to TOTP.

**Requires Authentication**: No (uses `mfaToken` from login response)

#### Request

```http
POST /api/v1/auth/mfa/email-code
Content-Type: application/json

{
  "mfaToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

#### Response (200 OK)

```json
{
  "sent": true,
  "message": "Verification code sent to your email"
}
```

If rate-limited:

```json
{
  "sent": false,
  "message": "Please wait before requesting another code"
}
```

#### Error Responses

| Status | Code | Description |
|--------|------|-------------|
| 401 | `AUTH_MFA_TOKEN_INVALID` | MFA token is invalid or expired |

---

### POST /auth/mfa/disable

Disable MFA for the current user.

**Requires Authentication**: Yes (Bearer token)

> **Note**: Admins and Lab Managers **cannot** disable MFA.

#### Request

```http
POST /api/v1/auth/mfa/disable
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "code": "123456"
}
```

#### Response (200 OK)

```json
{
  "mfaEnabled": false,
  "message": "MFA has been disabled"
}
```

#### Error Responses

| Status | Code | Description |
|--------|------|-------------|
| 400 | `AUTH_MFA_NOT_ENABLED` | MFA is not enabled |
| 401 | `AUTH_MFA_INVALID_CODE` | TOTP code is incorrect |
| 403 | `AUTH_MFA_REQUIRED` | Cannot disable (role requires MFA) |

---

### GET /auth/mfa/status

Get MFA status for the current user.

**Requires Authentication**: Yes (Bearer token)

#### Request

```http
GET /api/v1/auth/mfa/status
Authorization: Bearer <access_token>
```

#### Response (200 OK)

```json
{
  "mfaEnabled": true,
  "mfaRequired": true,
  "canDisable": false
}
```

---

## Lab Booking Endpoints

Endpoints for discovering buildings/labs and creating reservations.

### Building Endpoints

#### GET /buildings

List all buildings.

**Requires Authentication**: Yes (Bearer token)

##### Request

```http
GET /api/v1/buildings
Authorization: Bearer <access_token>
```

##### Response (200 OK)

```json
[
  {
    "id": 1,
    "name": "Main Engineering Building",
    "description": "Primary engineering facility",
    "address": "123 University Ave",
    "city": "Warsaw",
    "createdAt": "2026-01-15T10:00:00Z",
    "lastModifiedAt": "2026-01-15T10:00:00Z"
  }
]
```

---

#### GET /buildings/{buildingId}/labs

List all labs in a specific building.

**Requires Authentication**: Yes (Bearer token)

##### Request

```http
GET /api/v1/buildings/1/labs
Authorization: Bearer <access_token>
```

##### Response (200 OK)

```json
[
  {
    "id": 1,
    "name": "Computer Lab A",
    "description": "General purpose computer lab",
    "capacity": 30,
    "defaultOpenTime": "08:00:00",
    "defaultCloseTime": "20:00:00",
    "createdAt": "2026-01-15T10:00:00Z",
    "lastModifiedAt": "2026-01-15T10:00:00Z",
    "building": {
      "id": 1,
      "name": "Main Engineering Building"
    }
  }
]
```

---

### Lab Endpoints

#### GET /labs/{labId}

Get detailed information about a specific lab.

**Requires Authentication**: Yes (Bearer token)

##### Request

```http
GET /api/v1/labs/1
Authorization: Bearer <access_token>
```

##### Response (200 OK)

```json
{
  "id": 1,
  "name": "Computer Lab A",
  "description": "General purpose computer lab",
  "capacity": 30,
  "defaultOpenTime": "08:00:00",
  "defaultCloseTime": "20:00:00",
  "createdAt": "2026-01-15T10:00:00Z",
  "lastModifiedAt": "2026-01-15T10:00:00Z",
  "building": {
    "id": 1,
    "name": "Main Engineering Building"
  }
}
```

##### Error Responses

| Status | Code | Description |
|--------|------|-------------|
| 404 | `BOOKING_LAB_NOT_FOUND` | Lab with the specified ID does not exist |

---

#### GET /labs/{labId}/availability

Get weekly availability for a lab, including operating hours, closed days, and existing reservations.

**Requires Authentication**: Yes (Bearer token)

##### Request

```http
GET /api/v1/labs/1/availability?week=2026-01-19
Authorization: Bearer <access_token>
```

**Query Parameters**:
- `week` (optional) - ISO date (YYYY-MM-DD) for the week start. Defaults to current week if not provided.

##### Response (200 OK)

```json
{
  "labId": 1,
  "labName": "Computer Lab A",
  "weekStart": "2026-01-19",
  "weekEnd": "2026-01-25",
  "operatingHours": [
    { "dayOfWeek": 1, "open": "08:00:00", "close": "20:00:00", "closed": false },
    { "dayOfWeek": 2, "open": "08:00:00", "close": "20:00:00", "closed": false },
    { "dayOfWeek": 3, "open": "08:00:00", "close": "20:00:00", "closed": false },
    { "dayOfWeek": 4, "open": "08:00:00", "close": "20:00:00", "closed": false },
    { "dayOfWeek": 5, "open": "08:00:00", "close": "20:00:00", "closed": false },
    { "dayOfWeek": 6, "open": "10:00:00", "close": "16:00:00", "closed": false },
    { "dayOfWeek": 0, "open": null, "close": null, "closed": true }
  ],
  "closedDays": [
    { "date": "2026-01-21", "reason": "Maintenance" }
  ],
  "reservations": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "date": "2026-01-20",
      "startTime": "10:00:00",
      "endTime": "12:00:00",
      "status": "APPROVED",
      "wholeLab": false,
      "workstationIds": [1, 2, 3],
      "userName": "John Doe"
    },
    {
      "id": "550e8400-e29b-41d4-a716-446655440001",
      "date": "2026-01-20",
      "startTime": "14:00:00",
      "endTime": "16:00:00",
      "status": "PENDING",
      "wholeLab": true,
      "workstationIds": [],
      "userName": "Jane Smith"
    }
  ]
}
```

**Notes**:
- `dayOfWeek` uses ISO standard: 1 = Monday, 7 = Sunday, 0 = Sunday (alternate)
- `operatingHours` shows per-day schedule; if `closed` is true, the lab is closed that day
- `closedDays` lists specific dates when the lab is closed (holidays, maintenance)
- `reservations` includes both PENDING and APPROVED reservations for frontend to display

##### Error Responses

| Status | Code | Description |
|--------|------|-------------|
| 404 | `BOOKING_LAB_NOT_FOUND` | Lab with the specified ID does not exist |

---

#### GET /labs/{labId}/availability/current

Get current availability status for a lab (what's happening right now).

**Requires Authentication**: Yes (Bearer token)

##### Request

```http
GET /api/v1/labs/1/availability/current
Authorization: Bearer <access_token>
```

##### Response (200 OK)

```json
{
  "labId": 1,
  "labName": "Computer Lab A",
  "isOpen": true,
  "currentReservations": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "date": "2026-01-19",
      "startTime": "10:00:00",
      "endTime": "12:00:00",
      "status": "APPROVED",
      "wholeLab": false,
      "workstationIds": [1, 2, 3],
      "userName": "John Doe"
    }
  ]
}
```

##### Error Responses

| Status | Code | Description |
|--------|------|-------------|
| 404 | `BOOKING_LAB_NOT_FOUND` | Lab with the specified ID does not exist |

---

#### GET /labs/{labId}/workstations

List all workstations in a lab.

**Requires Authentication**: Yes (Bearer token)

##### Request

```http
GET /api/v1/labs/1/workstations
Authorization: Bearer <access_token>
```

##### Response (200 OK)

```json
{
  "labId": 1,
  "labName": "Computer Lab A",
  "workstations": [
    {
      "id": 1,
      "identifier": "WS-01",
      "description": "Window seat with dual monitors",
      "active": true
    },
    {
      "id": 2,
      "identifier": "WS-02",
      "description": null,
      "active": true
    },
    {
      "id": 3,
      "identifier": "WS-03",
      "description": "Near power outlet",
      "active": false
    }
  ]
}
```

**Notes**:
- `active` indicates if the workstation is available for booking
- Inactive workstations cannot be selected for reservations

##### Error Responses

| Status | Code | Description |
|--------|------|-------------|
| 404 | `BOOKING_LAB_NOT_FOUND` | Lab with the specified ID does not exist |

---

### Reservation Endpoints

#### POST /reservations

Create a new reservation (single or recurring).

**Requires Authentication**: Yes (Bearer token)

##### Request - Single Reservation

```http
POST /api/v1/reservations
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "labId": 1,
  "startTime": "2026-01-20T10:00:00+01:00",
  "endTime": "2026-01-20T12:00:00+01:00",
  "description": "Project work session",
  "wholeLab": false,
  "workstationIds": [1, 2]
}
```

##### Request - Recurring Reservation

```http
POST /api/v1/reservations
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "labId": 1,
  "startTime": "2026-01-20T10:00:00+01:00",
  "endTime": "2026-01-20T12:00:00+01:00",
  "description": "Weekly team meeting",
  "wholeLab": true,
  "workstationIds": [],
  "recurring": {
    "patternType": "WEEKLY",
    "endDate": "2026-03-20",
    "occurrences": null
  }
}
```

**Fields**:
- `labId` (required) - ID of the lab to book
- `startTime` (required) - ISO 8601 datetime with timezone
- `endTime` (required) - ISO 8601 datetime with timezone
- `description` (optional) - Purpose of the reservation
- `wholeLab` (optional, default: false) - Book the entire lab
- `workstationIds` (required if `wholeLab` is false) - Array of workstation IDs to reserve
- `recurring` (optional) - Recurring pattern configuration:
  - `patternType` - One of: `WEEKLY`, `BIWEEKLY`, `MONTHLY`, `CUSTOM`
  - `intervalDays` - Custom interval in days (required for `CUSTOM` pattern)
  - `endDate` - End date for recurrence (ISO date string)
  - `occurrences` - Number of occurrences (alternative to `endDate`)

##### Response - Single Reservation (201 Created)

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "labId": 1,
  "labName": "Computer Lab A",
  "startTime": "2026-01-20T10:00:00+01:00",
  "endTime": "2026-01-20T12:00:00+01:00",
  "description": "Project work session",
  "status": "PENDING",
  "wholeLab": false,
  "workstationIds": [1, 2],
  "recurringGroupId": null,
  "createdAt": "2026-01-19T14:30:00Z"
}
```

##### Response - Recurring Reservation (201 Created)

```json
{
  "recurringGroupId": "660e8400-e29b-41d4-a716-446655440000",
  "patternType": "WEEKLY",
  "totalOccurrences": 9,
  "reservations": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "labId": 1,
      "labName": "Computer Lab A",
      "startTime": "2026-01-20T10:00:00+01:00",
      "endTime": "2026-01-20T12:00:00+01:00",
      "description": "Weekly team meeting",
      "status": "PENDING",
      "wholeLab": true,
      "workstationIds": [],
      "recurringGroupId": "660e8400-e29b-41d4-a716-446655440000",
      "createdAt": "2026-01-19T14:30:00Z"
    }
  ]
}
```

**Notes**:
- All new reservations are created with `PENDING` status
- Email notifications are sent to the user and lab manager(s) upon submission
- Recurring reservations create individual `Reservation` records linked by `recurringGroupId`
- The system does NOT block conflicting reservations - lab managers review and approve/reject

##### Error Responses

| Status | Code | Description |
|--------|------|-------------|
| 400 | `BOOKING_INVALID_TIME_RANGE` | End time before start time, or in the past |
| 400 | `BOOKING_OUTSIDE_OPERATING_HOURS` | Reservation outside lab operating hours |
| 400 | `BOOKING_LAB_CLOSED` | Lab is closed on the requested date |
| 400 | `BOOKING_WORKSTATION_NOT_IN_LAB` | Workstation doesn't belong to the lab |
| 400 | `BOOKING_WORKSTATION_INACTIVE` | Workstation is not active |
| 400 | `BOOKING_NO_WORKSTATIONS_SELECTED` | `wholeLab` is false but no workstations provided |
| 400 | `BOOKING_INVALID_RECURRING_PATTERN` | Invalid recurring configuration |
| 400 | `BOOKING_NO_VALID_OCCURRENCES` | Recurring pattern produces no valid dates |
| 404 | `BOOKING_LAB_NOT_FOUND` | Lab with the specified ID does not exist |
| 404 | `BOOKING_WORKSTATION_NOT_FOUND` | Workstation with the specified ID does not exist |

---

#### GET /reservations/{id}

Get a reservation by ID.

**Requires Authentication**: Yes (Bearer token)

##### Request

```http
GET /api/v1/reservations/550e8400-e29b-41d4-a716-446655440000
Authorization: Bearer <access_token>
```

##### Response (200 OK)

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "labId": 1,
  "labName": "Computer Lab A",
  "startTime": "2026-01-20T10:00:00+01:00",
  "endTime": "2026-01-20T12:00:00+01:00",
  "description": "Project work session",
  "status": "PENDING",
  "wholeLab": false,
  "workstationIds": [1, 2],
  "recurringGroupId": null,
  "createdAt": "2026-01-19T14:30:00Z"
}
```

##### Error Responses

| Status | Code | Description |
|--------|------|-------------|
| 404 | `BOOKING_RESERVATION_NOT_FOUND` | Reservation with the specified ID does not exist |

---

#### GET /reservations/me

Get the current user's reservations.

**Requires Authentication**: Yes (Bearer token)

##### Request

```http
GET /api/v1/reservations/me
Authorization: Bearer <access_token>
```

##### Response (200 OK)

```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "labId": 1,
    "labName": "Computer Lab A",
    "startTime": "2026-01-20T10:00:00+01:00",
    "endTime": "2026-01-20T12:00:00+01:00",
    "description": "Project work session",
    "status": "PENDING",
    "wholeLab": false,
    "workstationIds": [1, 2],
    "recurringGroupId": null,
    "createdAt": "2026-01-19T14:30:00Z"
  }
]
```

---

### Reservation Status Values

| Status | Description |
|--------|-------------|
| `PENDING` | Awaiting lab manager review |
| `APPROVED` | Approved by lab manager |
| `REJECTED` | Rejected by lab manager |
| `CANCELLED` | Cancelled by the user |

---

### Recurrence Pattern Types

| Pattern | Description |
|---------|-------------|
| `WEEKLY` | Repeats every week on the same day |
| `BIWEEKLY` | Repeats every two weeks |
| `MONTHLY` | Repeats monthly on the same day of month |
| `CUSTOM` | Repeats every N days (specify `intervalDays`) |

---

## User Management Endpoints

User management endpoints for admin operations.

### POST /users

Register a new user (Admin only).

**Requires Authentication**: Yes (Bearer token, ADMIN role)

#### Request

```http
POST /api/v1/users
Authorization: Bearer <admin_access_token>
Content-Type: application/json

{
  "email": "newuser@example.com",
  "username": "johsmi",
  "firstName": "John",
  "lastName": "Smith",
  "degree": "DR",
  "roleName": "PROFESSOR"
}
```

**Fields**:
- `email` (required) - Valid email address
- `username` (required) - 3-30 chars, alphanumeric and underscores only
- `firstName` (required) - User's first name
- `lastName` (required) - User's last name
- `degree` (optional) - One of: `INZ`, `MGR`, `MGR_INZ`, `DR`, `DR_INZ`, `DR_HAB`, `PROF`
- `roleName` (required) - One of: `ADMIN`, `LAB_MANAGER`, `PROFESSOR`

> **Note**: Frontend should suggest username as `firstName.substring(0,3) + lastName.substring(0,3)` lowercase. Admin can edit before submitting.

#### Response (201 Created)

```json
{
  "id": 1,
  "email": "newuser@example.com",
  "username": "johsmi",
  "firstName": "John",
  "lastName": "Smith",
  "degree": "DR",
  "role": "PROFESSOR",
  "enabled": false,
  "createdAt": "2026-01-13T12:00:00Z"
}
```

The user is created with `enabled=false`. An email is sent to the user with a password setup link.

#### Error Responses

| Status | Code | Description |
|--------|------|-------------|
| 403 | - | Not authorized (not ADMIN role) |
| 409 | `USER_EMAIL_ALREADY_EXISTS` | Email already registered |
| 409 | `USER_USERNAME_ALREADY_EXISTS` | Username already taken |
| 400 | `USER_INVALID_ROLE` | Invalid role specified |

---

### GET /users/{id}

Get a user by ID.

**Requires Authentication**: Yes (Bearer token)

#### Request

```http
GET /api/v1/users/1
Authorization: Bearer <access_token>
```

#### Response (200 OK)

```json
{
  "id": 1,
  "email": "user@example.com",
  "username": "johsmi",
  "firstName": "John",
  "lastName": "Smith",
  "degree": "DR",
  "role": "PROFESSOR",
  "enabled": true,
  "createdAt": "2026-01-13T12:00:00Z"
}
```

#### Error Responses

| Status | Description |
|--------|-------------|
| 404 | User not found |

---

### GET /users/check-username

Check if a username is available (Admin only).

**Requires Authentication**: Yes (Bearer token, ADMIN role)

#### Request

```http
GET /api/v1/users/check-username?username=johsmi
Authorization: Bearer <admin_access_token>
```

#### Response (200 OK)

```json
{
  "available": true
}
```

---

### GET /users/check-email

Check if an email is available (Admin only).

**Requires Authentication**: Yes (Bearer token, ADMIN role)

#### Request

```http
GET /api/v1/users/check-email?email=user@example.com
Authorization: Bearer <admin_access_token>
```

#### Response (200 OK)

```json
{
  "available": false
}
```

---

## Using Access Tokens

### Authorization Header

Include the access token in the `Authorization` header for protected endpoints:

```http
GET /api/v1/protected-resource
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Token Expiration

| Token Type | Default Lifetime | Purpose |
|------------|-----------------|---------|
| Access Token | 15 minutes | API authorization |
| Refresh Token | 7 days | Obtain new access tokens |

When the access token expires, use the `/auth/refresh` endpoint to get a new one.

---

## Error Response Format

All API errors follow this format:

```json
{
  "status": "ERROR_CODE",
  "message": "Human-readable error description"
}
```

### Error Codes Reference

#### Authentication Errors

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `AUTH_INVALID_CREDENTIALS` | 401 | Invalid email or password |
| `AUTH_INVALID_TOKEN` | 401 | JWT token is invalid |
| `AUTH_EXPIRED_TOKEN` | 401 | JWT token has expired |
| `AUTH_ACCOUNT_DISABLED` | 403 | User account is disabled |
| `AUTH_ACCOUNT_LOCKED` | 423 | Account temporarily locked due to failed attempts |

#### Refresh Token Errors

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `AUTH_INVALID_REFRESH_TOKEN` | 401 | Refresh token not found or invalid |
| `AUTH_REFRESH_TOKEN_EXPIRED` | 401 | Refresh token has expired |
| `AUTH_REFRESH_TOKEN_REUSE_DETECTED` | 401 | Possible token theft detected |

#### Password Setup Errors

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `AUTH_PASSWORD_TOKEN_INVALID` | 400 | Password setup token invalid or used |
| `AUTH_PASSWORD_TOKEN_EXPIRED` | 400 | Password setup token has expired |

#### MFA Errors

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `AUTH_MFA_REQUIRED` | 403 | MFA cannot be disabled for this role |
| `AUTH_MFA_SETUP_REQUIRED` | 403 | Must set up MFA before proceeding |
| `AUTH_MFA_INVALID_CODE` | 401 | TOTP/email/backup code is invalid |
| `AUTH_MFA_TOKEN_INVALID` | 401 | MFA token is invalid |
| `AUTH_MFA_TOKEN_EXPIRED` | 401 | MFA token has expired (5 min) |
| `AUTH_MFA_RATE_LIMITED` | 429 | Too many OTP requests |
| `AUTH_MFA_ALREADY_ENABLED` | 409 | MFA is already enabled |
| `AUTH_MFA_NOT_ENABLED` | 400 | MFA is not enabled |

#### User Management Errors

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `USER_EMAIL_ALREADY_EXISTS` | 409 | Email already registered |
| `USER_USERNAME_ALREADY_EXISTS` | 409 | Username already taken |
| `USER_INVALID_ROLE` | 400 | Invalid role specified |

#### Validation Errors

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `USER_EMAIL_NOT_VALID` | 400 | Email format is invalid |

#### Booking Errors

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `BOOKING_LAB_NOT_FOUND` | 404 | Lab with the specified ID does not exist |
| `BOOKING_BUILDING_NOT_FOUND` | 404 | Building with the specified ID does not exist |
| `BOOKING_WORKSTATION_NOT_FOUND` | 404 | Workstation with the specified ID does not exist |
| `BOOKING_RESERVATION_NOT_FOUND` | 404 | Reservation with the specified ID does not exist |
| `BOOKING_INVALID_TIME_RANGE` | 400 | Invalid time range (end before start, or in the past) |
| `BOOKING_OUTSIDE_OPERATING_HOURS` | 400 | Reservation outside lab operating hours |
| `BOOKING_LAB_CLOSED` | 400 | Lab is closed on the requested date |
| `BOOKING_WORKSTATION_NOT_IN_LAB` | 400 | Workstation doesn't belong to the specified lab |
| `BOOKING_WORKSTATION_INACTIVE` | 400 | Workstation is not active |
| `BOOKING_NO_WORKSTATIONS_SELECTED` | 400 | No workstations selected for non-whole-lab booking |
| `BOOKING_INVALID_RECURRING_PATTERN` | 400 | Invalid recurring pattern configuration |
| `BOOKING_NO_VALID_OCCURRENCES` | 400 | Recurring pattern produces no valid dates |
| `BOOKING_NOT_AUTHORIZED` | 403 | User not authorized for this booking action |

---

## Security Features

### Account Lockout Policy

The system implements a tiered lockout policy to prevent brute-force attacks:

| Failed Attempts | Lockout Duration |
|-----------------|------------------|
| 3 | 10 minutes |
| 6+ | 30 minutes |

After a successful login, the failed attempt counter resets.

### Refresh Token Rotation

Each time you refresh your access token:
1. The old refresh token is invalidated
2. A new refresh token is issued
3. The old token is linked to the new one for audit

**Token Reuse Detection**: If an already-rotated token is used, it indicates potential token theft. The system will reject the request and log a security alert.

### IP Tracking

All authentication events are logged with:
- Client IP address (supports `X-Forwarded-For` for proxied requests)
- Timestamp
- Success/failure status

---

## API Usage Examples

### cURL Examples

#### Login
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123"}' \
  -c cookies.txt
```

#### Refresh Token
```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -b cookies.txt \
  -c cookies.txt
```

#### Logout
```bash
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -b cookies.txt
```

#### Access Protected Resource
```bash
curl -X GET http://localhost:8080/api/v1/protected \
  -H "Authorization: Bearer <access_token>"
```

### JavaScript (Fetch API)

```javascript
// Login
const loginResponse = await fetch('/api/v1/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  credentials: 'include', // Important for cookies
  body: JSON.stringify({
    email: 'user@example.com',
    password: 'password123'
  })
});

const { accessToken, user } = await loginResponse.json();

// Store access token
localStorage.setItem('accessToken', accessToken);

// Make authenticated request
const response = await fetch('/api/v1/protected', {
  headers: {
    'Authorization': `Bearer ${accessToken}`
  }
});

// Refresh token (when access token expires)
const refreshResponse = await fetch('/api/v1/auth/refresh', {
  method: 'POST',
  credentials: 'include' // Cookie is sent automatically
});

// Logout
await fetch('/api/v1/auth/logout', {
  method: 'POST',
  credentials: 'include'
});
localStorage.removeItem('accessToken');
```

---

## Rate Limiting

Currently, rate limiting is not implemented. Consider using a reverse proxy (nginx, Cloudflare) for rate limiting in production.

---

## Versioning

The API uses URL versioning: `/api/v1/...`

Breaking changes will be introduced in new versions (`/api/v2/...`) while maintaining backward compatibility for existing versions.

