# Test Summary

This document provides an overview of the test suite for the 5G Lab Booking System.

## Overview

| Metric | Count |
|--------|-------|
| Total test files | 23 |
| Total test methods | ~322 |
| Integration tests | 4 files (73 methods) |
| Unit tests | 19 files (~249 methods) |

### Testing Frameworks

- **JUnit 5** - Test framework
- **Mockito** - Mocking for unit tests
- **Testcontainers** - PostgreSQL containers for integration tests
- **Spring Boot Test** - Application context and MockMvc

---

## Auth Domain

**Total: 66 tests across 5 files**

| File | Type | Tests | Coverage |
|------|------|-------|----------|
| `AuthIntegrationTest.java` | Integration | 21 | Login, refresh, logout, MFA flows |
| `LoginControllerTest.java` | Unit | 11 | Login validation, lockout policy |
| `MfaServiceTest.java` | Unit | 24 | TOTP, backup codes, MFA tokens |
| `PasswordSetupTokenServiceTest.java` | Unit | 6 | Token creation/validation |
| `EmailServiceTest.java` | Unit | 4 | Email sending |

### AuthIntegrationTest (21 tests)

End-to-end authentication flow tests:
- Login endpoint (valid/invalid credentials, disabled accounts)
- Account lockout after 3 failures
- Refresh token endpoint (refresh, missing token, reuse detection)
- Logout endpoint
- Password setup with auto-login
- MFA flows (Admin setup required, Admin challenge, Professor direct login)
- MFA verify endpoint (invalid token/code)
- Email OTP sending

### LoginControllerTest (11 tests)

Controller unit tests:
- Successful login and lockout counter reset
- Failed login scenarios (unknown email, wrong password, disabled account)
- Lockout policy (10-min after 3 failures, 30-min after 6 failures)
- Lockout expiration and tier reset

### MfaServiceTest (24 tests)

MFA service logic:
- Role enforcement (Admin/Lab Manager require MFA)
- TOTP secret and QR code generation
- OTP verification
- Backup codes (generation, verification, case-insensitive)
- MFA token generation and parsing

### PasswordSetupTokenServiceTest (6 tests)

Token management:
- Token creation and existing token invalidation
- Valid token consumption
- Unknown/used/expired token rejection
- Expired token cleanup

### EmailServiceTest (4 tests)

Email functionality:
- Account setup email content
- OTP email content
- Generic email sending
- Error handling

---

## Booking Domain

**Total: 213 tests across 11 files**

| File | Type | Tests | Coverage |
|------|------|-------|----------|
| `BookingIntegrationTest.java` | Integration | 15 | Building/lab discovery, reservations |
| `LabManagerReservationIntegrationTest.java` | Integration | 25 | Manager approval workflows |
| `ProfessorReservationEditIntegrationTest.java` | Integration | 12 | Professor edit flows |
| `ReservationServiceTest.java` | Unit | 20 | Reservation creation |
| `ReservationEditServiceTest.java` | Unit | 20 | Edit proposals |
| `ReservationManagementServiceTest.java` | Unit | 20 | Approve/decline |
| `AvailabilityServiceTest.java` | Unit | 13 | Availability calculation |
| `LabManagerAuthorizationServiceTest.java` | Unit | 38 | Authorization checks |
| `BuildingServiceTest.java` | Unit | 23 | Building CRUD |
| `WorkstationServiceTest.java` | Unit | 15 | Workstation CRUD |
| `DaysOffServiceTest.java` | Unit | 12 | Days off management |

### BookingIntegrationTest (15 tests)

End-to-end booking flow tests:
- Building and lab discovery (public endpoints)
- Lab details and workstation retrieval
- Weekly and current availability queries
- Single and whole-lab reservation creation
- Recurring reservations
- Validation (time ranges, operating hours, authentication)

### LabManagerReservationIntegrationTest (25 tests)

Lab manager workflow tests:
- Viewing pending reservations (manager and admin)
- Approving/declining single reservations
- Edit proposal creation and management
- Recurring group operations (approve/decline all or single)
- Authorization scope (manager per lab, admin global)

### ProfessorReservationEditIntegrationTest (12 tests)

Professor edit workflow tests:
- Direct edits for PENDING reservations
- Edit proposals for APPROVED reservations
- Approving/rejecting lab manager edit proposals
- Recurring group edits with mixed statuses
- Authorization (own reservations only)

### ReservationServiceTest (20 tests)

Reservation creation logic:
- Single reservation (workstations and whole-lab)
- Recurring reservation patterns (weekly)
- Time range validation
- Operating hours and lab closure validation
- Workstation validation (existence, lab membership, active)
- Retrieval by ID, user, and status

### ReservationEditServiceTest (20 tests)

Edit workflow logic:
- Manager-initiated edit proposals
- Professor-initiated edits (direct vs. proposals)
- Edit approval/rejection workflows
- Recurring group edits (manager and professor)
- Authorization and validation

### ReservationManagementServiceTest (20 tests)

Management operations:
- Retrieving pending reservations
- Approving/declining single reservations
- Approving/declining recurring groups
- Authorization checks (lab manager vs. admin)
- Status validation and error handling

### AvailabilityServiceTest (13 tests)

Availability calculation:
- Weekly availability (operating hours, closed days, reservations)
- Current availability (open status, active reservations)
- Lab workstation listing
- Week normalization and date handling

### LabManagerAuthorizationServiceTest (38 tests)

Authorization logic:
- Admin role detection
- Lab manager assignment checks
- Reservation management authorization
- Reservation ownership verification
- Managed labs retrieval
- Pending reservations filtering by role

### BuildingServiceTest (23 tests)

Building management:
- Building CRUD (create, read, update, archive, delete)
- Operating hours management (set, update, delete)
- Days off management (specific dates, recurring days)
- Archived building restrictions

### WorkstationServiceTest (15 tests)

Workstation management:
- Workstation CRUD operations
- Retrieval (by ID, lab ID, active status)
- Identifier uniqueness within labs
- Archiving workstations

### DaysOffServiceTest (12 tests)

Days off management:
- CRUD for university-wide days off
- Specific date and recurring closures
- University-wide vs. lab-specific distinction
- Error handling for invalid requests

---

## User Domain

**Total: 43 tests across 5 files**

| File | Type | Tests | Coverage |
|------|------|-------|----------|
| `UserServiceTest.java` | Unit | 30 | User CRUD, profile, roles |
| `LogServiceTest.java` | Unit | 10 | Log parsing/filtering |
| `EmailValidationServiceTests.java` | Unit | 1 (13 cases) | Email format validation |
| `UserRepositoryTests.java` | Integration | 1 | Repository queries |
| `RoleRepositoryTests.java` | Integration | 1 | Repository queries |

### UserServiceTest (30 tests)

User management logic:
- **Registration (5)**: Successful registration, duplicate email/username, invalid role
- **Availability checks (4)**: Username and email availability
- **Get user (2)**: By ID (found/not found)
- **Update own profile (4)**: Full/partial updates, duplicate email handling
- **Admin update user (4)**: Profile and role updates, validation
- **Change role (3)**: Role changes and validation
- **Deactivate user (3)**: Soft delete, cannot deactivate anonymous
- **Hard delete user (3)**: GDPR-compliant deletion with reassignment
- **Anonymous user (2)**: Retrieval and exception handling

### LogServiceTest (10 tests)

Log service functionality:
- Empty log file handling
- Parsing valid log entries (INFO, DEBUG, ERROR)
- Filtering by log level
- Filtering by date range
- Filtering by search term (case-insensitive)
- Pagination
- Non-parseable log line handling
- Combined filters

### EmailValidationServiceTests (1 parameterized test, 13 cases)

Email validation:
- Valid emails (various domains, special characters)
- Invalid emails (missing @, empty, double @@, etc.)

### Repository Tests (2 tests)

- `UserRepositoryTests`: findByEmail, findByUsername
- `RoleRepositoryTests`: findByName

---

## Coverage Areas

### Authentication & Security
- Login/logout flows
- JWT token management (access, refresh, rotation)
- MFA (TOTP, email OTP, backup codes)
- Account lockout policy
- Token reuse detection
- Password setup and reset

### Reservation Lifecycle
- Creation (single, whole-lab, recurring)
- Editing (direct, proposals)
- Approval/rejection workflows
- Cancellation

### Authorization
- Role-based access (Admin, Lab Manager, Professor)
- Resource-based access (lab manager scope)
- Ownership verification

### CRUD Operations
- Buildings (with operating hours)
- Labs (with managers, operating hours)
- Workstations
- Users (with soft/hard delete)
- Days off (university, building, lab)

### Validation
- Time range validation
- Operating hours enforcement
- Lab closure checks
- Email format validation
- Workstation membership

---

## Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "AuthIntegrationTest"

# Run tests with coverage report
./gradlew test jacocoTestReport
```

**Note**: Integration tests require Docker for Testcontainers (PostgreSQL).
