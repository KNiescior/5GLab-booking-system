# Future Features & TODOs

This document tracks planned features and improvements for the 5GLab Booking System.

## High Priority (Core Functionality)

### Lab Manager Features
- [ ] **Approve/Reject reservations** - Endpoints for lab managers to review and approve/reject pending requests
- [ ] **Edit reservations** - Allow lab managers to modify reservation details (time, workstations)
- [ ] **View pending requests** - Dashboard/endpoint to see all pending reservations for managed labs
- [ ] **Bulk actions for recurring** - Approve/reject entire recurring series at once

### Status Change Notifications
- [ ] **Email on approval** - Notify user when reservation is approved
- [ ] **Email on rejection** - Notify user when reservation is rejected (with reason)

## Medium Priority (Enhancements)

### Reminder System
- [ ] **8am reminder emails** - Send reminder email at 8am on the day of user's reservation
  - Requires: Spring `@Scheduled` task
  - Query: APPROVED reservations starting today
  - Email: `sendReservationReminderEmail()` method in EmailService

### Calendar Integration
- [ ] **iCal export** - Allow users to export reservations to calendar apps
- [ ] **Calendar view API** - Endpoint optimized for calendar UI rendering

## Low Priority (Nice to Have)

### Admin Features
- [ ] **Usage statistics** - Reports on lab usage, popular times, etc.
- [ ] **Audit log** - Track all reservation changes with timestamps

### User Features
- [ ] **Favorite labs** - Quick access to frequently used labs
- [ ] **Reservation templates** - Save common booking patterns

## Technical Debt

- [ ] **Production CORS** - Configure proper CORS origins before deployment
- [ ] **CSRF** - Re-enable CSRF protection for browser clients
- [ ] **Rate limiting** - Add rate limiting to public endpoints
- [ ] **Delete DataInitializer.java** - Remove before production deployment
- [ ] **Delete test users** - Remove admin@5glab.com and professor@5glab.com from production DB

## Completed ✅

### Authentication & User Management
- [x] JWT Authentication with refresh tokens
- [x] MFA (TOTP, Email OTP, Backup codes)
- [x] User registration (admin-only)
- [x] Account lockout policy (tiered: 3 attempts → 10 min, 6+ → 30 min)
- [x] Token reuse detection security

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
