# System Architecture

Technical architecture and design decisions for the 5GLab Booking System.

## Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                           Client Layer                               │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                  │
│  │   Web App   │  │ Mobile App  │  │   Admin UI  │                  │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘                  │
└─────────┼────────────────┼────────────────┼─────────────────────────┘
          │                │                │
          ▼                ▼                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         API Gateway                                  │
│                    (Spring Boot Application)                         │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │                    Security Filter Chain                      │   │
│  │  ┌─────────────────┐  ┌─────────────────┐                    │   │
│  │  │  CORS Filter    │→│  JWT Auth Filter │→ Controllers       │   │
│  │  └─────────────────┘  └─────────────────┘                    │   │
│  └──────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        Data Layer                                    │
│  ┌─────────────────┐  ┌─────────────────┐                           │
│  │   PostgreSQL    │  │  (Future: Redis │                           │
│  │   Database      │  │   for caching)  │                           │
│  └─────────────────┘  └─────────────────┘                           │
└─────────────────────────────────────────────────────────────────────┘
```

## Authentication Flow

### Login Flow

```
┌────────┐      ┌─────────────┐      ┌───────────┐      ┌────────────┐
│ Client │      │ LoginController│   │ UserRepo  │      │ JwtService │
└───┬────┘      └──────┬──────┘      └─────┬─────┘      └─────┬──────┘
    │                  │                   │                  │
    │ POST /login      │                   │                  │
    │ {email,password} │                   │                  │
    │─────────────────>│                   │                  │
    │                  │                   │                  │
    │                  │ findByEmail()     │                  │
    │                  │──────────────────>│                  │
    │                  │                   │                  │
    │                  │      User         │                  │
    │                  │<──────────────────│                  │
    │                  │                   │                  │
    │                  │ Check: enabled?   │                  │
    │                  │ Check: locked?    │                  │
    │                  │ Verify password   │                  │
    │                  │                   │                  │
    │                  │ generateAccessToken()               │
    │                  │─────────────────────────────────────>│
    │                  │                                      │
    │                  │ generateRefreshToken()              │
    │                  │─────────────────────────────────────>│
    │                  │                                      │
    │                  │         tokens                       │
    │                  │<─────────────────────────────────────│
    │                  │                   │                  │
    │ 200 OK           │                   │                  │
    │ {accessToken}    │                   │                  │
    │ Set-Cookie:      │                   │                  │
    │ refreshToken=... │                   │                  │
    │<─────────────────│                   │                  │
    │                  │                   │                  │
```

### Token Refresh Flow

```
┌────────┐      ┌─────────────────┐      ┌──────────────┐
│ Client │      │ LoginController │      │ RefreshToken │
└───┬────┘      └────────┬────────┘      │  Repository  │
    │                    │               └──────┬───────┘
    │ POST /refresh      │                      │
    │ Cookie: refresh... │                      │
    │───────────────────>│                      │
    │                    │                      │
    │                    │ findByTokenId(jti)   │
    │                    │─────────────────────>│
    │                    │                      │
    │                    │    RefreshToken      │
    │                    │<─────────────────────│
    │                    │                      │
    │                    │ isActive()?          │
    │                    │ - Check revoked      │
    │                    │ - Check expired      │
    │                    │                      │
    │                    │ Generate new tokens  │
    │                    │ Revoke old token     │
    │                    │ Link old → new       │
    │                    │                      │
    │ 200 OK             │                      │
    │ {newAccessToken}   │                      │
    │ Set-Cookie: new... │                      │
    │<───────────────────│                      │
```

### Token Reuse Detection

When a refresh token is used after it has been rotated:

```
┌────────┐      ┌─────────────────┐      ┌──────────────┐
│Attacker│      │ LoginController │      │ RefreshToken │
└───┬────┘      └────────┬────────┘      │  Repository  │
    │                    │               └──────┬───────┘
    │ POST /refresh      │                      │
    │ Cookie: OLD_TOKEN  │                      │
    │───────────────────>│                      │
    │                    │                      │
    │                    │ findByTokenId(jti)   │
    │                    │─────────────────────>│
    │                    │                      │
    │                    │    RefreshToken      │
    │                    │    {                 │
    │                    │      revokedAt: X,   │
    │                    │      replacedBy: Y   │  ← Already rotated!
    │                    │    }                 │
    │                    │<─────────────────────│
    │                    │                      │
    │                    │ SECURITY ALERT!      │
    │                    │ Log incident         │
    │                    │                      │
    │ 401 Unauthorized   │                      │
    │ TOKEN_REUSE_DETECT │                      │
    │<───────────────────│                      │
```

## Database Schema

### Entity Relationship Diagram

```
┌─────────────────────┐       ┌─────────────────────┐
│       account       │       │        role         │
├─────────────────────┤       ├─────────────────────┤
│ id (PK)             │       │ id (PK)             │
│ username            │       │ name                │
│ email               │       └──────────┬──────────┘
│ password            │                  │
│ first_name          │                  │
│ last_name           │                  │
│ degree              │                  │
│ role_id (FK)────────┼──────────────────┘
│ enabled             │
│ failed_login_count  │
│ locked_until        │
│ last_login          │
│ last_login_ip       │
│ mfa_enabled         │
│ totp_secret         │
│ password_changed_at │
│ created_timestamp   │
│ last_modified       │
└──────────┬──────────┘
           │
           │ 1:N
           ▼
┌─────────────────────┐
│   refresh_token     │
├─────────────────────┤
│ id (PK)             │
│ token_id (unique)   │
│ user_id (FK)        │
│ expires_at          │
│ revoked_at          │
│ replaced_by_token_id│
│ created_at          │
└─────────────────────┘

┌─────────────────────┐
│ password_setup_token│
├─────────────────────┤
│ id (PK)             │
│ token_hash (unique) │
│ user_id (FK)        │
│ purpose             │
│ expires_at          │
│ used_at             │
│ created_at          │
└─────────────────────┘
```

## Security Architecture

### JWT Token Structure

#### Access Token Claims

```json
{
  "sub": "user@example.com",
  "userId": 1,
  "role": "USER",
  "iat": 1703836800,
  "exp": 1703837700,
  "iss": "booking-system"
}
```

#### Refresh Token Claims

```json
{
  "sub": "user@example.com",
  "jti": "550e8400-e29b-41d4-a716-446655440000",
  "iat": 1703836800,
  "exp": 1704441600,
  "iss": "booking-system"
}
```

### Key Management

```
┌──────────────────────────────────────────┐
│           JwtKeyProvider                  │
├──────────────────────────────────────────┤
│ - Loads RSA keys from filesystem         │
│ - Supports classpath or absolute paths   │
│ - Validates key format on startup        │
│                                          │
│ Private Key: Sign tokens                 │
│ Public Key: Verify tokens                │
└──────────────────────────────────────────┘
```

### Security Filter Chain

```
Request
   │
   ▼
┌─────────────────────┐
│    CORS Filter      │  ← Cross-origin configuration
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│   CSRF Disabled     │  ← Stateless API (JWT-based)
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ JwtAuthFilter       │  ← Extract & validate JWT
│ - Extract from      │
│   Authorization     │
│   header            │
│ - Validate token    │
│ - Set Security      │
│   Context           │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ Authorization       │  ← Role-based access
│ - /auth/** permitAll│
│ - /** authenticated │
└──────────┬──────────┘
           │
           ▼
     Controller
```

## Lockout Policy Implementation

### Tiered Lockout Logic

```java
// Pseudocode
if (passwordMismatch) {
    failedCount++;
    
    if (failedCount >= 6) {
        lockUntil = now + 30 minutes;
    } else if (failedCount >= 3) {
        lockUntil = now + 10 minutes;
    }
}

if (successfulLogin) {
    failedCount = 0;
    lockUntil = null;
}
```

### State Diagram

```
                    ┌──────────────┐
                    │   ACTIVE     │
                    │ failed = 0   │
                    └──────┬───────┘
                           │
              Failed Login │
                           ▼
                    ┌──────────────┐
                    │   WARNING    │
                    │ failed = 1-2 │
                    └──────┬───────┘
                           │
              3rd Failed   │
                           ▼
                    ┌──────────────┐
                    │   LOCKED     │◄────────────────┐
                    │   10 min     │                 │
                    └──────┬───────┘                 │
                           │                         │
              Timeout      │         6th Failed      │
                           ▼                         │
                    ┌──────────────┐                 │
                    │  UNLOCKED    │─────────────────┘
                    │ failed = 3-5 │
                    └──────┬───────┘
                           │
              6th Failed   │
                           ▼
                    ┌──────────────┐
                    │   LOCKED     │
                    │   30 min     │
                    └──────────────┘
```

## Module Structure

```
com._glab.booking_system
│
├── auth/                          # Authentication Module
│   ├── config/
│   │   ├── JwtKeyProvider        # RSA key management
│   │   ├── JwtProperties         # JWT configuration
│   │   └── SecurityConfig        # Spring Security config
│   │
│   ├── controller/
│   │   └── LoginController       # Auth endpoints
│   │
│   ├── exception/
│   │   ├── AuthenticationFailedException
│   │   ├── AccountLockedException
│   │   ├── AccountDisabledException
│   │   ├── InvalidRefreshTokenException
│   │   ├── RefreshTokenExpiredException
│   │   ├── RefreshTokenReuseException
│   │   ├── InvalidPasswordSetupTokenException
│   │   └── ExpiredPasswordSetupTokenException
│   │
│   ├── filter/
│   │   └── JwtAuthenticationFilter
│   │
│   ├── model/
│   │   ├── RefreshToken          # Refresh token entity
│   │   ├── PasswordSetupToken    # Password setup entity
│   │   └── TokenPurpose          # ACCOUNT_SETUP, PASSWORD_RESET
│   │
│   ├── repository/
│   │   ├── RefreshTokenRepository
│   │   └── PasswordSetupTokenRepository
│   │
│   └── service/
│       ├── JwtService            # Token generation/validation
│       ├── PasswordSetupTokenService
│       └── CustomUserDetailsService
│
├── user/                          # User Module
│   ├── model/
│   │   ├── User                  # User entity
│   │   ├── Role                  # Role entity
│   │   ├── RoleName              # USER, ADMIN, MODERATOR
│   │   └── Degree                # Academic degree enum
│   │
│   └── repository/
│       ├── UserRepository
│       └── RoleRepository
│
├── ErrorResponse                  # Global error format
├── ErrorResponseCode              # Error code enum
└── BookingSystemApplication       # Main class
```

## Configuration Management

### Profile-Based Configuration

| Profile | Purpose | Database | JWT Keys |
|---------|---------|----------|----------|
| `default` | Production | External PostgreSQL | File-based |
| `dev` | Development | Docker PostgreSQL | File-based |
| `test` | Testing | Testcontainers | Generated in-memory |

### Configuration Hierarchy

```
application.yml          # Base configuration
    │
    ├── application-dev.yml     # Development overrides
    │
    └── application-test.yml    # Test overrides
```

## Logging Strategy

### Security Audit Logs

All authentication events are logged with severity levels:

| Event | Level | Information Logged |
|-------|-------|-------------------|
| Successful login | INFO | Email, IP |
| Failed login | WARN | Email, IP, attempt count |
| Account locked | WARN | Email, IP, duration |
| Token refresh | DEBUG | Email, IP |
| Token reuse detected | ERROR | Email, IP (security incident) |
| Logout | INFO | Email, IP |

### Log Format

```
2024-01-01T12:00:00.000Z  WARN 12345 --- [http-nio-8080-exec-1] c._g.b.auth.controller.LoginController : Account user@example.com locked for 10 minutes after 3 failed attempts from IP 192.168.1.100
```

## Future Considerations

### Planned Enhancements

1. **MFA Support**
   - TOTP-based two-factor authentication
   - `mfaEnabled` and `totpSecret` fields already in User entity

2. **Rate Limiting**
   - Redis-based rate limiting per IP/user
   - Configurable thresholds

3. **GeoIP Analysis**
   - IP geolocation for login anomaly detection
   - Suspicious location alerts

4. **Session Management**
   - Concurrent session limits
   - Session listing and remote logout

5. **Password Policies**
   - Minimum complexity requirements
   - Password history (prevent reuse)
   - Expiration policies

### Scalability Considerations

- **Horizontal Scaling**: Stateless JWT design allows multiple app instances
- **Database Scaling**: Read replicas for token validation
- **Caching**: Redis for frequently accessed user data
- **Token Storage**: Consider moving refresh tokens to Redis for faster lookups

