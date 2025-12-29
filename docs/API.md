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

#### Validation Errors

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `USER_EMAIL_NOT_VALID` | 400 | Email format is invalid |

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

