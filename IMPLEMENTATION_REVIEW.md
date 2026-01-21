# Lab Manager Reservation Management - Implementation Review

## Executive Summary

The service layer implementation is **mostly complete** with comprehensive business logic, but several critical components are **missing or incomplete**:

- ✅ **Service Layer**: Well-implemented with all required methods
- ❌ **Controller Layer**: Missing `LabManagerReservationController` entirely
- ❌ **Email Service**: All email methods have TODO placeholders (not implemented)
- ⚠️ **Exception Handling**: Using generic exceptions instead of custom ones
- ⚠️ **Logic Issues**: Status handling bug in `applyEditProposal`

---

## ✅ What's Done Properly

### 1. Data Model & Repository
- ✅ `PENDING_EDIT_APPROVAL` status added to `ReservationStatus` enum
- ✅ `ReservationEditProposal` entity created with all required fields
- ✅ `ReservationEditProposalRepository` with all required query methods
- ✅ `ReservationRepository` has `findByStatus()` method
- ⚠️ Missing: `findByRecurringGroupIdAndStatus()` and `findByLabIdAndStatus()` (mentioned in plan but may not be needed)

### 2. Authorization Service
- ✅ `LabManagerAuthorizationService` fully implemented
- ✅ All required methods: `isAdmin()`, `isLabManagerForLab()`, `canManageReservation()`, `isReservationOwner()`, `getPendingReservationsForUser()`
- ✅ Proper admin bypass logic

### 3. Service Layer - ReservationManagementService
- ✅ `getPendingReservationsForManager()` - correctly filters by lab manager or admin
- ✅ `approveReservation()` - proper authorization and status checks
- ✅ `declineReservation()` - proper authorization and status checks
- ✅ `approveRecurringGroup()` - handles all occurrences in group
- ✅ `declineRecurringGroup()` - handles all occurrences in group
- ✅ Email notifications sent via `sendStatusChangeEmail()`

### 4. Service Layer - ReservationEditService
- ✅ **Lab Manager Methods** (all implemented):
  - `editReservationByManager()` - creates edit proposal, sets status to PENDING_EDIT_APPROVAL
  - `approveEditByManager()` - approves professor's edit
  - `rejectEditByManager()` - rejects professor's edit, restores original
  - `editRecurringGroupOccurrenceByManager()` - edits single occurrence
  - `editRecurringGroupByManager()` - edits all occurrences
  - `approveRecurringGroupEditByManager()` - approves professor's group edit
  - `rejectRecurringGroupEditByManager()` - rejects professor's group edit

- ✅ **Professor Methods** (all implemented):
  - `editReservationByProfessor()` - handles PENDING (direct edit) and APPROVED (edit proposal)
  - `approveEditByProfessor()` - approves lab manager's edit
  - `rejectEditByProfessor()` - rejects lab manager's edit
  - `editRecurringGroupByProfessor()` - handles mixed PENDING/APPROVED occurrences
  - `approveRecurringGroupEditByProfessor()` - approves lab manager's group edit
  - `rejectRecurringGroupEditByProfessor()` - rejects lab manager's group edit

- ✅ **Helper Methods**:
  - `createEditProposal()` - properly stores original and proposed values
  - `applyEditProposal()` - applies edits (but has logic bug - see issues)
  - `restoreOriginalValues()` - restores original values correctly
  - `applyEditDirectly()` - for PENDING reservations edited by professor
  - `validateEditRequest()` - comprehensive validation
  - `validateOperatingHours()` - reuses logic from ReservationService
  - `validateLabNotClosed()` - proper validation

---

### 2. Email Service - CRITICAL
- ❌ **All email methods have TODO placeholders** - None are actually implemented:
  - `sendReservationEditProposalEmailToProfessor()` - TODO only
  - `sendReservationEditProposalEmailToManager()` - TODO only
  - `sendReservationUpdatedEmailToManager()` - TODO only
  - `sendEditApprovedByManagerEmail()` - TODO only
  - `sendEditRejectedByManagerEmail()` - TODO only
  - `sendEditApprovedByProfessorEmail()` - TODO only
  - `sendEditRejectedByProfessorEmail()` - TODO only

- ⚠️ **Existing email methods** need updates:
  - `sendNewReservationRequestEmail()` - link should include reservation ID
  - `sendReservationSubmittedEmail()` - link should include reservation ID

### 3. DTOs
- ⚠️ **`ApproveEditRequest`** - Missing `reason` field (plan says it should have reason for rejection)
  - Current: Only has `approve` (Boolean)
  - Should have: `approve` (Boolean) and `reason` (String, optional)

- ❓ **Missing DTOs** (may not be needed if using existing):
  - `ApproveReservationRequest` - Currently using String reason parameter
  - `DeclineReservationRequest` - Currently using String reason parameter
  - `ReservationEditProposalResponse` - May be needed for GET endpoints
  - `PendingReservationsResponse` - May be needed for GET /pending endpoint

---

## ⚠️ Issues & Bugs

### 1. Logic Bug in `applyEditProposal()` - CRITICAL
**Location**: `ReservationEditService.java` lines 669-676

**Problem**: Both branches of the if-else set status to `APPROVED`, making the condition meaningless.

```java
// Set status based on original status
// If original was APPROVED and professor edited, it goes back to APPROVED
// If original was PENDING and lab manager edited, it becomes APPROVED
if (proposal.getOriginalStatus() == ReservationStatus.APPROVED) {
    reservation.setStatus(ReservationStatus.APPROVED);
} else {
    reservation.setStatus(ReservationStatus.APPROVED);  // BUG: Same as if branch!
}
```

**Expected Behavior** (from plan):
- If original was `APPROVED` and professor edited → `APPROVED` ✅
- If original was `APPROVED` and lab manager edited → `APPROVED` ✅
- If original was `PENDING` and lab manager edited → Should become `APPROVED` (after approval) ✅
- If original was `PENDING` and professor edited → Should stay `PENDING` (but this shouldn't reach here - professor edits PENDING directly)

**Fix**: The logic seems correct (both should be APPROVED after approval), but the comment is misleading. However, we need to verify the actual business logic:
- When a lab manager edits a PENDING reservation and professor approves → Should it become APPROVED? (Yes, per plan)
- When a professor edits an APPROVED reservation and lab manager approves → Should it stay APPROVED? (Yes, per plan)

**Recommendation**: Simplify to just `reservation.setStatus(ReservationStatus.APPROVED);` and update comment, OR verify the business logic is correct.

### 2. Exception Handling - Using Generic Exceptions
**Location**: Multiple places in `ReservationEditService.java`

**Problem**: Using `IllegalStateException` instead of custom exceptions:
- Line 78: `throw new IllegalStateException("Reservation already has a pending edit proposal");`
  - Should use: `EditAlreadyResolvedException` or `InvalidEditException`
- Line 115: `throw new IllegalStateException("This edit proposal was not created by the reservation owner");`
  - Should use: `InvalidEditException`
- Line 147: `throw new IllegalStateException("This edit proposal was not created by the reservation owner");`
  - Should use: `InvalidEditException`
- Line 176: `throw new IllegalStateException("This reservation is not part of a recurring group...");`
  - Should use: `InvalidEditException`
- Line 193: `throw new IllegalStateException("This occurrence already has a pending edit proposal");`
  - Should use: `EditAlreadyResolvedException`
- Line 375: `throw new IllegalStateException("Reservation already has a pending edit proposal");`
  - Should use: `EditAlreadyResolvedException`
- Line 390: `throw new IllegalStateException("Only PENDING or APPROVED reservations can be edited");`
  - Should use: `InvalidEditException`
- Line 414: `throw new IllegalStateException("This edit proposal was not created by a lab manager");`
  - Should use: `InvalidEditException`
- Line 446: `throw new IllegalStateException("This edit proposal was not created by a lab manager");`
  - Should use: `InvalidEditException`

**Note**: Custom exceptions exist but are not being used:
- `EditAlreadyResolvedException` - created but unused
- `InvalidEditException` - created but unused
- `EditProposalNotFoundException` - correctly used ✅

### 3. Missing Validation in `applyEditProposal()`
**Location**: `ReservationEditService.java` line 648

**Problem**: Method doesn't check if proposal is already resolved before applying.

**Fix**: Add check at start:
```java
if (proposal.getResolution() != ResolutionStatus.PENDING) {
    throw new EditAlreadyResolvedException(proposal.getId());
}
```

### 4. Missing Validation in `restoreOriginalValues()`
**Location**: `ReservationEditService.java` line 693

**Problem**: Method doesn't check if proposal is already resolved before restoring.

**Fix**: Add check at start (same as above).

### 5. Status Check in ReservationManagementService
**Location**: `ReservationManagementService.java` lines 68-70, 103-105

**Problem**: Should also check for `PENDING_EDIT_APPROVAL` status? According to plan, reservations in `PENDING_EDIT_APPROVAL` should not be approved/declined through normal flow.

**Current**: Only checks for `PENDING` status.

**Question**: Should `PENDING_EDIT_APPROVAL` reservations be excluded from approve/decline operations? Plan doesn't explicitly say, but it makes sense - they need edit approval first.

---

## 📋 Summary Checklist

### Completed ✅
- [x] Add PENDING_EDIT_APPROVAL status
- [x] Create ReservationEditProposal entity
- [x] Create ReservationEditProposalRepository
- [x] Create authorization service
- [x] Implement all service methods (lab manager + professor)
- [x] Create EditReservationRequest DTO
- [x] Create exception classes

### Missing ❌
- [ ] Create LabManagerReservationController (CRITICAL)
- [ ] Add professor edit endpoints to ReservationController (CRITICAL)
- [ ] Implement all email service methods (CRITICAL)
- [ ] Update existing email methods with reservation ID links
- [ ] Add reason field to ApproveEditRequest
- [ ] Create missing DTOs (if needed)

### Needs Fix ⚠️
- [ ] Fix logic bug in applyEditProposal() (status handling)
- [ ] Replace IllegalStateException with custom exceptions
- [ ] Add validation checks in applyEditProposal() and restoreOriginalValues()
- [ ] Consider PENDING_EDIT_APPROVAL status in approve/decline methods

---

## 🎯 Priority Actions

### High Priority (Blocking)
1. **Create LabManagerReservationController** - Without this, the feature is unusable
2. **Add professor edit endpoints to ReservationController** - Required for professor functionality
3. **Implement email service methods** - Currently all TODOs, emails won't be sent
4. **Fix status handling bug in applyEditProposal()** - Logic issue

### Medium Priority
5. Replace generic exceptions with custom ones
6. Add validation checks for already-resolved proposals
7. Add reason field to ApproveEditRequest

### Low Priority
8. Update existing email methods with reservation ID links
9. Create missing response DTOs if needed for frontend

---

## 📝 Notes

- The service layer implementation is **excellent** and comprehensive
- Authorization logic is properly implemented with admin bypass
- Edit proposal workflow is correctly implemented
- The main blockers are the missing controllers and email implementations
- Exception handling could be improved for better error messages
