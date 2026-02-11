# Future Features & TODOs

This document tracks planned features and improvements for the 5GLab Booking System.

## Medium Priority (Enhancements)

### Authentication
- [ ] **Change password endpoint** - Allow authenticated users to change their password (old → new)
  - Endpoint: `POST /api/v1/auth/change-password`
  - Requires: current password verification, new password validation
  - Update `passwordChangedAt` timestamp

- [ ] **Email-only MFA setup option** - Allow users to set up MFA with email OTP instead of authenticator app
  - Alternative to TOTP for users who don't want to use authenticator apps
  - New endpoint: `POST /api/v1/auth/mfa/setup/email` - Setup MFA with email as primary
  - Sends verification email, user confirms with code to enable MFA
  - Consider: less secure than TOTP (email can be compromised/delayed)
  - Consider: should this be allowed for Admins/Lab Managers (mandatory MFA roles)?

### Notifications
- [ ] **Notify admins of every new reservation request** - Send the same "new reservation request" email to all admins when a reservation is submitted (like lab managers). Requires: `UserRepository.findByRole_Name(RoleName.ADMIN)` (or equivalent), then in `ReservationService.sendReservationEmails()` loop over admins and call `EmailService.sendNewReservationRequestEmail()` (or an admin-specific variant with `/admin/reservations` links).

### Reminder System
- [ ] **8am reminder emails** - Send reminder email at 8am on the day of user's reservation
  - Requires: Spring `@Scheduled` task
  - Query: APPROVED reservations starting today
  - Email: `sendReservationReminderEmail()` method in EmailService

### Calendar Integration
- [ ] **Calendar view API** - Endpoint optimized for calendar UI rendering

## Low Priority (Nice to Have)

### Admin Features
- [ ] **Usage statistics** - Reports on lab usage, popular times, etc.
- [ ] **Audit log** - Track all reservation changes with timestamps

### User Features
- [ ] **Favorite labs** - Quick access to frequently used labs
- [ ] **Reservation templates** - Save common booking patterns

## Technical Debt

- [x] **Production CORS** - Configure proper CORS origins before deployment (via `CORS_ALLOWED_ORIGINS` env var)
- [x] **CSRF** - Re-enable CSRF protection for browser clients (via `CSRF_ENABLED` env var)
- [x] **Rate limiting** - Add rate limiting to public endpoints (via Bucket4j, configurable via `RATE_LIMIT_*` env vars)
- [ ] **Delete DataInitializer.java** - Remove before production deployment
- [ ] **Delete test users** - Remove admin@5glab.com and professor@5glab.com from production DB
- [ ] **LabService tests** - Add unit tests for `LabService` (CRUD, operating hours, manager assignment)

## Completed ✅

### Authentication & User Management
- [x] JWT Authentication with refresh tokens
- [x] MFA (TOTP, Email OTP, Backup codes)
- [x] User registration (admin-only)
- [x] Account lockout policy (tiered: 3 attempts → 10 min, 6+ → 30 min)
- [x] Token reuse detection security
- [x] User self-profile editing (`GET/PUT /users/me`)
- [x] Admin user profile editing and role management (`AdminUserController`)
- [x] Account deactivation (soft delete)
- [x] Account hard delete with GDPR compliance (anonymous user placeholder)

### Lab Booking System (from lab_booking_form plan)
- [x] Database entities: Building, Lab, Workstation, LabManager, LabOperatingHours, LabClosedDay, Reservation, ReservationWorkstation, RecurringPattern
- [x] JPA repositories with custom queries for availability lookups
- [x] Building & Lab discovery API (`GET /buildings`, `GET /buildings/{id}/labs`, `GET /labs/{id}`)
- [x] Availability service with weekly availability and current status
- [x] Availability endpoints (`GET /labs/{id}/availability`, `/availability/current`, `/workstations`)
- [x] Reservation service with validation (time range, operating hours, lab closures, workstation validation)
- [x] Reservation creation endpoint (`POST /reservations`)
- [x] Get reservation endpoints (`GET /reservations/{id}`, `GET /reservations/me`)
- [x] Recurring reservation pattern support (WEEKLY, BIWEEKLY, MONTHLY, CUSTOM)
- [x] Email notifications for reservation submission (user confirmation + lab manager notification)

### Lab Manager Features (from lab_manager_reservation_management plan)
- [x] `PENDING_EDIT_APPROVAL` status for edit workflow
- [x] Lab manager authorization service (permission checks)
- [x] View pending reservations (`GET /manager/reservations/pending`)
- [x] Approve/decline single reservations (`POST /manager/reservations/{id}/approve`, `/decline`)
- [x] Edit reservations with professor approval workflow (`POST /manager/reservations/{id}/edit`)
- [x] Approve/reject professor edits (`POST /manager/reservations/{id}/edit/approve`, `/reject`)
- [x] Recurring group management (approve/decline/edit entire groups)
- [x] Individual occurrence management (approve/decline/edit single occurrences)
- [x] Professor self-editing with automatic or approval-required flow
- [x] Professor edit approval/rejection endpoints
- [x] `ReservationEditProposal` entity for storing original/proposed values
- [x] Email notifications for edit proposals, approvals, and rejections
- [x] Email on reservation approval
- [x] Email on reservation rejection (with reason)

### Admin Features (from admin-options-implementation plan)
- [x] Building CRUD with soft delete (`AdminBuildingController`)
- [x] Lab CRUD with soft delete and manager assignment (`AdminLabController`)
- [x] Workstation CRUD (`AdminWorkstationController`)
- [x] Building operating hours management
- [x] Lab operating hours with building inheritance
- [x] Special day operating hours override
- [x] University-wide days off management (`AdminDaysOffController`)
- [x] Building days off management
- [x] Lab days off management (with lab manager access)
- [x] Admin reservation management (`AdminReservationController`)
- [x] Log rotation configuration (logback-spring.xml)
- [x] Log access API with filtering and pagination (`GET /admin/logs`)
