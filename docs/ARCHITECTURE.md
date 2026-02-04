# System Architecture

Technical architecture and design decisions for the 5GLab Booking System.

## Overview

```mermaid
---
title: System Architecture Overview
---
flowchart TB
    subgraph Client["Client Layer"]
        WebApp["Web App"]
        MobileApp["Mobile App"]
        AdminUI["Admin UI"]
    end

    subgraph API["API Gateway (Spring Boot Application)"]
        subgraph Security["Security Filter Chain"]
            CORS["CORS Filter"] --> JWT["JWT Auth Filter"] --> Controllers
        end
    end

    subgraph Data["Data Layer"]
        PostgreSQL["PostgreSQL Database"]
        Redis["(Future: Redis for caching)"]
    end

    WebApp --> API
    MobileApp --> API
    AdminUI --> API
    API --> Data
```

## Authentication Flow

### Login Flow

```mermaid
sequenceDiagram
    participant Client
    participant LoginController
    participant UserRepo
    participant JwtService

    Client->>LoginController: POST /login {email, password}
    LoginController->>UserRepo: findByEmail()
    UserRepo-->>LoginController: User
    
    Note over LoginController: Check: enabled?<br/>Check: locked?<br/>Verify password
    
    LoginController->>JwtService: generateAccessToken()
    LoginController->>JwtService: generateRefreshToken()
    JwtService-->>LoginController: tokens
    
    LoginController-->>Client: 200 OK {accessToken}<br/>Set-Cookie: refreshToken=...
```

### Token Refresh Flow

```mermaid
sequenceDiagram
    participant Client
    participant LoginController
    participant RefreshTokenRepository

    Client->>LoginController: POST /refresh<br/>Cookie: refresh...
    LoginController->>RefreshTokenRepository: findByTokenId(jti)
    RefreshTokenRepository-->>LoginController: RefreshToken
    
    Note over LoginController: isActive()?<br/>- Check revoked<br/>- Check expired
    Note over LoginController: Generate new tokens<br/>Revoke old token<br/>Link old → new
    
    LoginController-->>Client: 200 OK {newAccessToken}<br/>Set-Cookie: new...
```

### MFA Login Flow

When MFA is enabled for a user:

```mermaid
sequenceDiagram
    participant Client
    participant LoginController
    participant MfaService
    participant MfaController

    Client->>LoginController: POST /login {email, password}
    Note over LoginController: Verify password (success)
    
    LoginController->>MfaService: Check MFA enabled?
    MfaService-->>LoginController: mfaEnabled=true
    
    LoginController->>MfaService: generateMfaToken()
    LoginController-->>Client: 200 OK {mfaToken}
    
    Client->>MfaController: POST /mfa/verify {mfaToken, code}
    MfaController->>MfaService: parseMfaToken()
    MfaController->>MfaService: verifyTotp()
    
    MfaController-->>Client: 200 OK {accessToken}<br/>Set-Cookie: refreshToken=...
```

### MFA Setup Flow

```mermaid
sequenceDiagram
    participant Client
    participant MfaController
    participant MfaService

    Client->>MfaController: POST /mfa/setup (with Bearer)
    MfaController->>MfaService: generateSecret()
    MfaController->>MfaService: generateQrCode()
    MfaController-->>Client: 200 OK {secret, qrCode}
    
    Note over Client: User scans QR
    
    Client->>MfaController: POST /setup/verify {secret, code}
    MfaController->>MfaService: verifyTotp()
    MfaController->>MfaService: generateBackupCodes()
    MfaController-->>Client: 200 OK {backupCodes}
```

### User Registration Flow (Admin Only)

```mermaid
sequenceDiagram
    participant Admin
    participant UserController
    participant UserService
    participant EmailService
    participant User

    Admin->>UserController: POST /users {email, username, firstName, ...}
    Note over UserController: @PreAuthorize(ADMIN)
    
    UserController->>UserService: registerUser()
    Note over UserService: Validate email<br/>Validate username<br/>Create User (enabled=false)<br/>Generate token (48h expiry)
    
    UserService->>EmailService: sendSetupEmail()
    EmailService-->>User: Email with setup link
    
    UserController-->>Admin: 201 Created {id, email, ...}
```

### Token Reuse Detection

When a refresh token is used after it has been rotated:

```mermaid
sequenceDiagram
    participant Attacker
    participant LoginController
    participant RefreshTokenRepository

    Attacker->>LoginController: POST /refresh<br/>Cookie: OLD_TOKEN
    LoginController->>RefreshTokenRepository: findByTokenId(jti)
    RefreshTokenRepository-->>LoginController: RefreshToken {revokedAt: X, replacedBy: Y}
    
    Note over LoginController: ⚠️ Already rotated!<br/>SECURITY ALERT!<br/>Log incident
    
    LoginController-->>Attacker: 401 Unauthorized<br/>TOKEN_REUSE_DETECTED
```

## Database Schema

### User & Authentication Entities

```mermaid
erDiagram
    role {
        int id PK
        string name
    }

    account {
        int id PK
        string username
        string email
        string password
        string first_name
        string last_name
        string degree
        int role_id FK
        boolean enabled
        int failed_login_count
        datetime locked_until
        datetime last_login
        string last_login_ip
        boolean mfa_enabled
        string totp_secret
        datetime password_changed_at
        datetime created_timestamp
        datetime last_modified
    }

    refresh_token {
        int id PK
        string token_id UK
        int user_id FK
        datetime expires_at
        datetime revoked_at
        string replaced_by_token_id
        datetime created_at
    }

    password_setup_token {
        int id PK
        string token_hash UK
        int user_id FK
        string purpose
        datetime expires_at
        datetime used_at
        datetime created_at
    }

    email_otp {
        int id PK
        int user_id FK
        string code_hash
        datetime expires_at
        datetime used_at
        datetime created_at
    }

    role ||--o{ account : "has"
    account ||--o{ refresh_token : "has"
    account ||--o{ password_setup_token : "has"
    account ||--o{ email_otp : "has"
```

### Booking Entities

```mermaid
erDiagram
    building {
        int id PK
        string name
        string description
        string address
        string city
        datetime created_at
        datetime last_modified_at
    }

    lab {
        int id PK
        int building_id FK
        string name
        string description
        int capacity
        time default_open_time
        time default_close_time
        datetime created_at
        datetime last_modified_at
    }

    workstation {
        int id PK
        int lab_id FK
        string identifier
        string description
        boolean active
        datetime created_at
    }

    lab_operating_hours {
        int id PK
        int lab_id FK
        int day_of_week
        time open_time
        time close_time
        boolean is_closed
    }

    lab_closed_day {
        int id PK
        int lab_id FK
        date specific_date
        int recurring_day
        string reason
    }

    lab_manager {
        int id PK
        int user_id FK
        int lab_id FK
        boolean is_primary
        datetime assigned_at
    }

    reservation {
        uuid id PK
        int user_id FK
        int lab_id FK
        datetime start_time
        datetime end_time
        string description
        string status
        boolean whole_lab
        uuid recurring_group_id
        datetime created_at
    }

    reservation_workstation {
        uuid reservation_id PK_FK
        int workstation_id PK_FK
    }

    recurring_pattern {
        uuid id PK
        uuid reservation_id FK
        string pattern_type
        int interval_days
        date end_date
        int occurrences
    }

    reservation_edit_proposal {
        uuid id PK
        uuid reservation_id FK
        int edited_by FK "account"
        string original_status
        datetime original_start_time
        datetime original_end_time
        string original_description
        boolean original_whole_lab
        json original_workstation_ids
        datetime proposed_start_time
        datetime proposed_end_time
        string proposed_description
        boolean proposed_whole_lab
        json proposed_workstation_ids
        string resolution "PENDING/APPROVED/REJECTED"
        int resolved_by FK "account"
        datetime resolved_at
        datetime created_at
    }

    building ||--o{ lab : "contains"
    lab ||--o{ workstation : "has"
    lab ||--o{ lab_operating_hours : "has"
    lab ||--o{ lab_closed_day : "has"
    lab ||--o{ lab_manager : "managed by"
    lab ||--o{ reservation : "has"
    account ||--o{ lab_manager : "is"
    account ||--o{ reservation : "makes"
    reservation ||--o{ reservation_workstation : "includes"
    workstation ||--o{ reservation_workstation : "reserved in"
    reservation ||--o| recurring_pattern : "has"
    reservation ||--o{ reservation_edit_proposal : "has"
```

### Booking Enums

```java
public enum ReservationStatus {
    PENDING,               // Awaiting lab manager review
    APPROVED,              // Approved by lab manager
    REJECTED,              // Rejected by lab manager
    CANCELLED,             // Cancelled by user
    PENDING_EDIT_APPROVAL  // Edited, awaiting approval
}

public enum RecurrenceType {
    WEEKLY,     // Every week
    BIWEEKLY,   // Every two weeks
    MONTHLY,    // Monthly on same day
    CUSTOM      // Every N days (intervalDays)
}
```

## Security Architecture

### JWT Token Structure

#### Access Token Claims

```json
{
  "sub": "user@example.com",
  "userId": 1,
  "role": "PROFESSOR",
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

```mermaid
classDiagram
    class JwtKeyProvider {
        -PrivateKey privateKey
        -PublicKey publicKey
        +loadKeys() void
        +getPrivateKey() PrivateKey
        +getPublicKey() PublicKey
    }
    note for JwtKeyProvider "- Loads RSA keys from filesystem\n- Supports classpath or absolute paths\n- Validates key format on startup\n\nPrivate Key: Sign tokens\nPublic Key: Verify tokens"
```

### Security Filter Chain

```mermaid
flowchart TB
    Request([Request])
    
    Request --> CORS
    
    subgraph CORS["CORS Filter"]
        cors_note["Cross-origin configuration"]
    end
    
    CORS --> CSRF
    
    subgraph CSRF["CSRF Disabled"]
        csrf_note["Stateless API (JWT-based)"]
    end
    
    CSRF --> JWT
    
    subgraph JWT["JwtAuthFilter"]
        jwt_note["- Extract from Authorization header\n- Validate token\n- Set Security Context"]
    end
    
    JWT --> Auth
    
    subgraph Auth["Authorization"]
        auth_note["- /auth/** permitAll\n- /** authenticated"]
    end
    
    Auth --> Controller([Controller])
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

```mermaid
---
title: Account Lockout State Machine
---
stateDiagram-v2
    [*] --> ACTIVE
    
    ACTIVE: failed = 0
    WARNING: failed = 1-2
    LOCKED_10: LOCKED (10 min)<br/>failed = 3-5
    LOCKED_30: LOCKED (30 min)<br/>failed ≥ 6
    
    ACTIVE --> WARNING: Failed Login
    WARNING --> WARNING: Failed Login (count < 3)
    WARNING --> LOCKED_10: 3rd Failed
    LOCKED_10 --> ACTIVE: Timeout (counter resets to 0)
    LOCKED_10 --> LOCKED_30: 6th Failed (before timeout)
    LOCKED_30 --> ACTIVE: Timeout (counter resets to 0)
    
    ACTIVE --> ACTIVE: Successful Login
    WARNING --> ACTIVE: Successful Login (counter resets)
```

## Module Structure

```
com._glab.booking_system
│
├── auth/                          # Authentication Module
│   ├── config/
│   │   ├── JwtKeyProvider        # RSA key management
│   │   ├── JwtProperties         # JWT configuration
│   │   ├── AppProperties         # App-wide config (mail, frontend URL)
│   │   └── SecurityConfig        # Spring Security config
│   │
│   ├── controller/
│   │   ├── LoginController       # Login/refresh/logout endpoints
│   │   └── MfaController         # MFA setup/verify endpoints
│   │
│   ├── exception/
│   │   ├── AuthenticationFailedException
│   │   ├── AccountLockedException
│   │   ├── AccountDisabledException
│   │   ├── InvalidRefreshTokenException
│   │   ├── RefreshTokenExpiredException
│   │   ├── RefreshTokenReuseException
│   │   ├── InvalidPasswordSetupTokenException
│   │   ├── ExpiredPasswordSetupTokenException
│   │   ├── MfaRequiredException
│   │   ├── MfaSetupRequiredException
│   │   ├── InvalidMfaCodeException
│   │   ├── InvalidMfaTokenException
│   │   ├── MfaTokenExpiredException
│   │   ├── MfaRateLimitedException
│   │   ├── MfaAlreadyEnabledException
│   │   ├── MfaNotEnabledException
│   │   └── MfaVerificationFailedException
│   │
│   ├── filter/
│   │   └── JwtAuthenticationFilter
│   │
│   ├── model/
│   │   ├── RefreshToken          # Refresh token entity
│   │   ├── PasswordSetupToken    # Password setup entity
│   │   ├── EmailOtp              # Email OTP entity
│   │   ├── TokenPurpose          # ACCOUNT_SETUP, PASSWORD_RESET
│   │   └── MfaCodeType           # TOTP, EMAIL, BACKUP
│   │
│   ├── repository/
│   │   ├── RefreshTokenRepository
│   │   ├── PasswordSetupTokenRepository
│   │   └── EmailOtpRepository
│   │
│   ├── request/
│   │   ├── LoginRequest
│   │   ├── SetupPasswordRequest
│   │   ├── MfaVerifyRequest
│   │   ├── MfaSetupVerifyRequest
│   │   └── MfaDisableRequest
│   │
│   ├── response/
│   │   ├── LoginResponse
│   │   ├── MfaChallengeResponse
│   │   ├── MfaSetupResponse
│   │   └── MfaSetupCompleteResponse
│   │
│   └── service/
│       ├── JwtService            # Token generation/validation
│       ├── PasswordSetupTokenService
│       ├── MfaService            # TOTP, backup codes, MFA tokens
│       ├── EmailOtpService       # Email OTP generation/verification
│       ├── EmailService          # Centralized email sending
│       └── CustomUserDetailsService
│
├── admin/                         # Admin Module
│   ├── controller/
│   │   └── LogController         # Log access endpoints
│   │
│   ├── response/
│   │   └── LogEntryResponse      # Log entry DTO
│   │
│   └── service/
│       └── LogService            # Log file parsing and filtering
│
├── user/                          # User Module
│   ├── controller/
│   │   ├── UserController        # User profile and admin registration
│   │   └── AdminUserController   # Admin user management
│   │
│   ├── exception/
│   │   ├── UserAlreadyExistsException
│   │   ├── UsernameAlreadyExistsException
│   │   └── InvalidRoleException
│   │
│   ├── model/
│   │   ├── User                  # User entity (with MFA fields)
│   │   ├── Role                  # Role entity
│   │   ├── RoleName              # ADMIN, LAB_MANAGER, PROFESSOR
│   │   └── Degree                # Academic degree enum
│   │
│   ├── repository/
│   │   ├── UserRepository
│   │   └── RoleRepository
│   │
│   ├── request/
│   │   └── CreateUserRequest     # Admin registration request
│   │
│   ├── response/
│   │   └── UserResponse          # User info response
│   │
│   └── service/
│       └── UserService           # User registration logic
│
├── booking/                       # Lab Booking Module
│   ├── controller/
│   │   ├── BuildingController         # Building discovery endpoints
│   │   ├── LabController              # Lab details & availability
│   │   ├── ReservationController      # Reservation CRUD & professor edits
│   │   ├── LabManagerReservationController  # Lab manager reservation management
│   │   ├── LabManagerController       # Lab manager lab operations
│   │   ├── AdminBuildingController    # Admin building CRUD
│   │   ├── AdminLabController         # Admin lab CRUD & manager assignment
│   │   ├── AdminWorkstationController # Admin workstation CRUD
│   │   ├── AdminDaysOffController     # University-wide days off
│   │   └── AdminReservationController # Admin reservation management
│   │
│   ├── exception/
│   │   ├── LabNotFoundException
│   │   ├── BuildingNotFoundException
│   │   ├── WorkstationNotFoundException
│   │   ├── ReservationNotFoundException
│   │   ├── InvalidReservationTimeException
│   │   ├── OutsideOperatingHoursException
│   │   ├── LabClosedException
│   │   ├── WorkstationNotInLabException
│   │   ├── WorkstationInactiveException
│   │   ├── NoWorkstationsSelectedException
│   │   ├── InvalidRecurringPatternException
│   │   ├── NoValidOccurrencesException
│   │   └── BookingNotAuthorizedException
│   │
│   ├── exception_handler/
│   │   └── BookingExceptionHandler  # Booking-specific error handling
│   │
│   ├── model/
│   │   ├── Building              # Building entity
│   │   ├── BuildingOperatingHours# Building per-day operating hours
│   │   ├── BuildingClosedDay     # Building-specific closure dates
│   │   ├── Lab                   # Lab entity
│   │   ├── Workstation           # Individual workstation
│   │   ├── LabManager            # User-Lab management junction
│   │   ├── LabOperatingHours     # Lab per-day operating hours
│   │   ├── LabClosedDay          # Lab-specific closure dates
│   │   ├── SpecialOperatingHours # Date-specific operating hour overrides
│   │   ├── Reservation           # Booking request
│   │   ├── ReservationWorkstation# Reservation-Workstation junction
│   │   ├── RecurringPattern      # Recurrence configuration
│   │   ├── ReservationEditProposal # Stores original/proposed values for edits
│   │   ├── ReservationStatus     # PENDING/APPROVED/REJECTED/CANCELLED/PENDING_EDIT_APPROVAL
│   │   └── RecurrenceType        # WEEKLY/BIWEEKLY/MONTHLY/CUSTOM
│   │
│   ├── repository/
│   │   ├── BuildingRepository
│   │   ├── BuildingOperatingHoursRepository
│   │   ├── BuildingClosedDayRepository
│   │   ├── LabRepository
│   │   ├── WorkstationRepository
│   │   ├── LabManagerRepository
│   │   ├── LabOperatingHoursRepository
│   │   ├── LabClosedDayRepository
│   │   ├── SpecialOperatingHoursRepository
│   │   ├── ReservationRepository
│   │   ├── RecurringPatternRepository
│   │   └── ReservationEditProposalRepository
│   │
│   ├── request/
│   │   └── CreateReservationRequest  # Reservation creation DTO
│   │
│   ├── response/
│   │   ├── LabAvailabilityResponse   # Weekly availability grid
│   │   ├── CurrentAvailabilityResponse
│   │   ├── LabWorkstationsResponse
│   │   ├── ReservationResponse
│   │   ├── RecurringReservationResponse
│   │   ├── ReservationSummaryResponse
│   │   ├── OperatingHoursResponse
│   │   ├── ClosedDayResponse
│   │   └── WorkstationResponse
│   │
│   └── service/
│       ├── BuildingService            # Building CRUD & operating hours
│       ├── LabService                 # Lab CRUD, managers & operating hours
│       ├── WorkstationService         # Workstation CRUD
│       ├── DaysOffService             # University-wide days off management
│       ├── AvailabilityService        # Availability calculations
│       ├── ReservationService         # Reservation creation & validation
│       ├── ReservationManagementService # Approve/decline reservations
│       ├── ReservationEditService     # Edit proposal workflow
│       └── LabManagerAuthorizationService # Authorization checks
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
| MFA setup initiated | INFO | Email |
| MFA enabled | INFO | Email |
| MFA verification success | INFO | Email, IP |
| MFA verification failed | WARN | Email, IP, code type |
| Invalid MFA token | WARN | IP |
| Email OTP sent | INFO | Email |
| Email OTP rate limited | DEBUG | Email |
| MFA disabled | INFO | Email |
| Backup code used | INFO | Email |

### Log Format

```
2024-01-01T12:00:00.000Z  WARN 12345 --- [http-nio-8080-exec-1] c._g.b.auth.controller.LoginController : Account user@example.com locked for 10 minutes after 3 failed attempts from IP 192.168.1.100
```

## MFA Implementation

### Overview

Multi-Factor Authentication is implemented with three verification methods:

| Method | Description | Use Case |
|--------|-------------|----------|
| **TOTP** | 6-digit code from authenticator app (Google Authenticator, Authy) | Primary MFA method |
| **Email OTP** | 6-digit code sent via email | Fallback when TOTP unavailable |
| **Backup Codes** | 10 one-time codes (e.g., "ABCD-1234") | Emergency access when other methods fail |

### Role-Based MFA Enforcement

| Role | MFA Required | Can Disable |
|------|--------------|-------------|
| ADMIN | ✅ Mandatory | ❌ No |
| LAB_MANAGER | ✅ Mandatory | ❌ No |
| PROFESSOR | ❌ Optional | ✅ Yes |

### MFA Token

A short-lived JWT (5 minutes) issued after password verification:

```json
{
  "sub": "user@example.com",
  "userId": 1,
  "mfaPending": true,
  "jti": "uuid",
  "exp": "now + 5 min",
  "iss": "booking-system-mfa"
}
```

### Backup Codes

- 10 codes generated on MFA setup
- Format: `XXXX-XXXX` (alphanumeric, excluding similar chars like 0/O, 1/I)
- Stored as BCrypt hashes
- Each code can only be used once

---

## Lab Booking System Design

### Key Design Decisions

1. **No Hard Blocking**: Neither APPROVED nor PENDING reservations block workstation selection. Users can always select any workstation - the API returns reservation data so the frontend can visually warn users about conflicts. Lab managers make the final approval decision.

2. **Recurring Reservations**: Each occurrence is a separate `Reservation` row linked by `recurring_group_id` (UUID). This allows individual occurrence management (edit/cancel one without affecting others).

3. **Lab Managers**: Many-to-many relationship via `LabManager` table with `is_primary` flag. Multiple users can manage a single lab. Admins can approve any lab as fallback.

4. **Operating Hours**: Stored per day-of-week per lab in `LabOperatingHours`. Default hours (8:00-20:00 weekdays) applied at lab creation. Sundays closed by default via `LabClosedDay`.

5. **Availability Response**: Returns standard JSON with arrays of reservations, operating hours, and closed days. Frontend handles rendering and conflict visualization.

### Booking Flow

```mermaid
sequenceDiagram
    participant User
    participant ReservationController
    participant ReservationService

    User->>ReservationController: POST /reservations<br/>{labId, startTime, endTime, ...}
    ReservationController->>ReservationService: createReservation()
    
    Note over ReservationService: Validate:<br/>- Time range valid<br/>- Within operating hrs<br/>- Lab not closed<br/>- Workstations exist<br/>- Workstations active
    Note over ReservationService: Create Reservation<br/>(status=PENDING)
    Note over ReservationService: Send emails:<br/>- Confirmation to user<br/>- Notification to lab manager(s)
    
    ReservationService-->>ReservationController: ReservationResponse
    ReservationController-->>User: 201 Created {id, status:PENDING}
```

### Recurring Reservation Flow

```mermaid
sequenceDiagram
    participant User
    participant ReservationService

    User->>ReservationService: POST /reservations<br/>{..., recurring: {patternType: WEEKLY, endDate: "2026-03-20"}}
    
    Note over ReservationService: Generate recurring_group_id (UUID)
    
    loop For each occurrence date
        Note over ReservationService: Skip if lab closed<br/>Skip if outside hours<br/>Create Reservation (linked by group_id)
    end
    
    Note over ReservationService: Send emails once for group
    
    ReservationService-->>User: 201 Created<br/>{recurringGroupId, totalOccurrences, reservations: [...]}
```

---

## Future Considerations

### Planned Enhancements

1. **Rate Limiting**
   - Redis-based rate limiting per IP/user
   - Configurable thresholds

2. **GeoIP Analysis**
   - IP geolocation for login anomaly detection
   - Suspicious location alerts

3. **Session Management**
   - Concurrent session limits
   - Session listing and remote logout

4. **Password Policies**
   - Minimum complexity requirements
   - Password history (prevent reuse)
   - Expiration policies

### Scalability Considerations

- **Horizontal Scaling**: Stateless JWT design allows multiple app instances
- **Database Scaling**: Read replicas for token validation
- **Caching**: Redis for frequently accessed user data
- **Token Storage**: Consider moving refresh tokens to Redis for faster lookups

