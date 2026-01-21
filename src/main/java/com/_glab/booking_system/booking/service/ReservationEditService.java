package com._glab.booking_system.booking.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com._glab.booking_system.booking.exception.BookingNotAuthorizedException;
import com._glab.booking_system.booking.exception.EditProposalNotFoundException;
import com._glab.booking_system.booking.exception.ReservationNotFoundException;
import com._glab.booking_system.booking.model.Lab;
import com._glab.booking_system.booking.model.Reservation;
import com._glab.booking_system.booking.model.ReservationEditProposal;
import com._glab.booking_system.booking.model.ReservationStatus;
import com._glab.booking_system.booking.model.ReservationWorkstation;
import com._glab.booking_system.booking.model.ResolutionStatus;
import com._glab.booking_system.booking.model.Workstation;
import com._glab.booking_system.booking.model.LabOperatingHours;
import com._glab.booking_system.booking.repository.LabClosedDayRepository;
import com._glab.booking_system.booking.repository.LabOperatingHoursRepository;
import com._glab.booking_system.booking.repository.ReservationEditProposalRepository;
import com._glab.booking_system.booking.repository.ReservationRepository;
import com._glab.booking_system.booking.repository.ReservationWorkstationRepository;
import com._glab.booking_system.booking.repository.WorkstationRepository;
import com._glab.booking_system.booking.request.EditReservationRequest;
import com._glab.booking_system.user.model.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationEditService {

    private final ReservationRepository reservationRepository;
    private final ReservationEditProposalRepository editProposalRepository;
    private final ReservationWorkstationRepository reservationWorkstationRepository;
    private final WorkstationRepository workstationRepository;
    private final LabOperatingHoursRepository labOperatingHoursRepository;
    private final LabClosedDayRepository labClosedDayRepository;
    private final LabManagerAuthorizationService authorizationService;
    private final com._glab.booking_system.auth.service.EmailService emailService;
    private final com._glab.booking_system.booking.repository.LabManagerRepository labManagerRepository;

    // ==================== Lab Manager/Admin Edit Operations ====================

    /**
     * Lab manager/admin edits a reservation.
     * Always creates an edit proposal requiring professor approval.
     */
    @Transactional
    public void editReservationByManager(UUID reservationId, EditReservationRequest request, User manager) {
        log.info("Manager {} attempting to edit reservation {}", manager.getEmail(), reservationId);

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> {
                    log.warn("Reservation not found: {}", reservationId);
                    return new ReservationNotFoundException(reservationId);
                });

        // Check authorization
        if (!authorizationService.canManageReservation(manager, reservation)) {
            log.warn("User {} is not authorized to edit reservation {}", manager.getEmail(), reservationId);
            throw new BookingNotAuthorizedException("You are not authorized to edit this reservation");
        }

        // Validate edit request (reuse validation from ReservationService)
        validateEditRequest(reservation.getLab(), request);

        // Check if there's already an active edit proposal
        Optional<ReservationEditProposal> existingProposal = editProposalRepository
                .findByReservationIdAndResolution(reservationId, ResolutionStatus.PENDING);
        if (existingProposal.isPresent()) {
            log.warn("Reservation {} already has a pending edit proposal", reservationId);
            throw new IllegalStateException("Reservation already has a pending edit proposal");
        }

        // Create edit proposal
        ReservationEditProposal proposal = createEditProposal(reservation, request, manager, reservation.getStatus());
        editProposalRepository.save(proposal);

        // Set reservation status to PENDING_EDIT_APPROVAL
        reservation.setStatus(ReservationStatus.PENDING_EDIT_APPROVAL);
        reservationRepository.save(reservation);

        log.info("Edit proposal created for reservation {} by manager {}", reservationId, manager.getEmail());

        // Send email to professor
        sendEditProposalEmailToProfessor(reservation, proposal);
    }

    /**
     * Lab manager/admin approves professor's edit of a reservation.
     */
    @Transactional
    public void approveEditByManager(UUID reservationId, User manager) {
        log.info("Manager {} attempting to approve edit for reservation {}", manager.getEmail(), reservationId);

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));

        // Check authorization
        if (!authorizationService.canManageReservation(manager, reservation)) {
            throw new BookingNotAuthorizedException("You are not authorized to approve this edit");
        }

        ReservationEditProposal proposal = getActiveEditProposal(reservationId);
        
        // Verify the edit was made by professor (not by another manager)
        if (!authorizationService.isReservationOwner(proposal.getEditedBy(), reservation)) {
            log.warn("Edit proposal {} was not created by the reservation owner", proposal.getId());
            throw new IllegalStateException("This edit proposal was not created by the reservation owner");
        }

        // Apply the edit
        applyEditProposal(proposal, manager);

        log.info("Edit approved for reservation {} by manager {}", reservationId, manager.getEmail());

        // Send email to professor
        sendEditApprovedByManagerEmail(reservation, proposal);
    }

    /**
     * Lab manager/admin rejects professor's edit of a reservation.
     */
    @Transactional
    public void rejectEditByManager(UUID reservationId, User manager, String reason) {
        log.info("Manager {} attempting to reject edit for reservation {}", manager.getEmail(), reservationId);

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));

        // Check authorization
        if (!authorizationService.canManageReservation(manager, reservation)) {
            throw new BookingNotAuthorizedException("You are not authorized to reject this edit");
        }

        ReservationEditProposal proposal = getActiveEditProposal(reservationId);
        
        // Verify the edit was made by professor
        if (!authorizationService.isReservationOwner(proposal.getEditedBy(), reservation)) {
            log.warn("Edit proposal {} was not created by the reservation owner", proposal.getId());
            throw new IllegalStateException("This edit proposal was not created by the reservation owner");
        }

        // Restore original values
        restoreOriginalValues(proposal, manager, reason);

        log.info("Edit rejected for reservation {} by manager {}", reservationId, manager.getEmail());

        // Send email to professor
        sendEditRejectedByManagerEmail(reservation, proposal, reason);
    }

    /**
     * Lab manager/admin edits a single occurrence within a recurring group.
     * This allows editing just one occurrence without affecting the rest of the group.
     */
    @Transactional
    public void editRecurringGroupOccurrenceByManager(UUID occurrenceId, EditReservationRequest request, User manager) {
        log.info("Manager {} attempting to edit single occurrence {} from recurring group", manager.getEmail(), occurrenceId);

        Reservation occurrence = reservationRepository.findById(occurrenceId)
                .orElseThrow(() -> {
                    log.warn("Reservation not found: {}", occurrenceId);
                    return new ReservationNotFoundException(occurrenceId);
                });

        // Verify this is part of a recurring group
        if (occurrence.getRecurringGroupId() == null) {
            log.warn("Reservation {} is not part of a recurring group", occurrenceId);
            throw new IllegalStateException("This reservation is not part of a recurring group. Use editReservationByManager instead.");
        }

        // Check authorization
        if (!authorizationService.canManageReservation(manager, occurrence)) {
            log.warn("User {} is not authorized to edit occurrence {}", manager.getEmail(), occurrenceId);
            throw new BookingNotAuthorizedException("You are not authorized to edit this occurrence");
        }

        // Validate edit request
        validateEditRequest(occurrence.getLab(), request);

        // Check if there's already an active edit proposal
        Optional<ReservationEditProposal> existingProposal = editProposalRepository
                .findByReservationIdAndResolution(occurrenceId, ResolutionStatus.PENDING);
        if (existingProposal.isPresent()) {
            log.warn("Occurrence {} already has a pending edit proposal", occurrenceId);
            throw new IllegalStateException("This occurrence already has a pending edit proposal");
        }

        // Create edit proposal for this single occurrence
        ReservationEditProposal proposal = createEditProposal(occurrence, request, manager, occurrence.getStatus());
        editProposalRepository.save(proposal);

        // Set reservation status to PENDING_EDIT_APPROVAL
        occurrence.setStatus(ReservationStatus.PENDING_EDIT_APPROVAL);
        reservationRepository.save(occurrence);

        log.info("Edit proposal created for occurrence {} (from recurring group {}) by manager {}", 
                occurrenceId, occurrence.getRecurringGroupId(), manager.getEmail());

        // Send email to professor
        sendEditProposalEmailToProfessor(occurrence, proposal);
    }

    /**
     * Lab manager/admin edits all occurrences in a recurring group.
     */
    @Transactional
    public void editRecurringGroupByManager(UUID recurringGroupId, EditReservationRequest request, User manager) {
        log.info("Manager {} attempting to edit recurring group {}", manager.getEmail(), recurringGroupId);

        List<Reservation> reservations = reservationRepository.findByRecurringGroupId(recurringGroupId);
        
        if (reservations.isEmpty()) {
            throw new ReservationNotFoundException("No reservations found for recurring group: " + recurringGroupId);
        }

        // Check authorization (all should be in same lab)
        Reservation firstReservation = reservations.get(0);
        if (!authorizationService.canManageReservation(manager, firstReservation)) {
            throw new BookingNotAuthorizedException("You are not authorized to edit this recurring group");
        }

        // Validate edit request
        validateEditRequest(firstReservation.getLab(), request);

        // Create edit proposals for all reservations in the group
        for (Reservation reservation : reservations) {
            // Check if there's already an active edit proposal
            Optional<ReservationEditProposal> existingProposal = editProposalRepository
                    .findByReservationIdAndResolution(reservation.getId(), ResolutionStatus.PENDING);
            if (existingProposal.isPresent()) {
                log.warn("Reservation {} already has a pending edit proposal, skipping", reservation.getId());
                continue;
            }

            ReservationEditProposal proposal = createEditProposal(reservation, request, manager, reservation.getStatus());
            editProposalRepository.save(proposal);

            reservation.setStatus(ReservationStatus.PENDING_EDIT_APPROVAL);
            reservationRepository.save(reservation);
        }

        log.info("Edit proposals created for {} reservations in recurring group {} by manager {}", 
                reservations.size(), recurringGroupId, manager.getEmail());

        // Send email to professor (using first reservation for details)
        sendEditProposalEmailToProfessor(firstReservation, 
                editProposalRepository.findByReservationIdAndResolution(
                        firstReservation.getId(), ResolutionStatus.PENDING).orElse(null));
    }

    /**
     * Lab manager/admin approves professor's edit of a recurring group.
     */
    @Transactional
    public void approveRecurringGroupEditByManager(UUID recurringGroupId, User manager) {
        log.info("Manager {} attempting to approve edit for recurring group {}", manager.getEmail(), recurringGroupId);

        List<Reservation> reservations = reservationRepository.findByRecurringGroupId(recurringGroupId);
        
        if (reservations.isEmpty()) {
            throw new ReservationNotFoundException("No reservations found for recurring group: " + recurringGroupId);
        }

        // Check authorization
        Reservation firstReservation = reservations.get(0);
        if (!authorizationService.canManageReservation(manager, firstReservation)) {
            throw new BookingNotAuthorizedException("You are not authorized to approve this edit");
        }

        // Get all active edit proposals for the group
        List<ReservationEditProposal> proposals = editProposalRepository
                .findByRecurringGroupIdAndResolution(recurringGroupId, ResolutionStatus.PENDING);

        if (proposals.isEmpty()) {
            throw new EditProposalNotFoundException("No active edit proposals found for recurring group: " + recurringGroupId);
        }

        // Approve all proposals
        for (ReservationEditProposal proposal : proposals) {
            applyEditProposal(proposal, manager);
        }

        log.info("Approved {} edit proposals in recurring group {} by manager {}", 
                proposals.size(), recurringGroupId, manager.getEmail());

        // Send email to professor
        sendEditApprovedByManagerEmail(firstReservation, proposals.get(0));
    }

    /**
     * Lab manager/admin rejects professor's edit of a recurring group.
     */
    @Transactional
    public void rejectRecurringGroupEditByManager(UUID recurringGroupId, User manager, String reason) {
        log.info("Manager {} attempting to reject edit for recurring group {}", manager.getEmail(), recurringGroupId);

        List<Reservation> reservations = reservationRepository.findByRecurringGroupId(recurringGroupId);
        
        if (reservations.isEmpty()) {
            throw new ReservationNotFoundException("No reservations found for recurring group: " + recurringGroupId);
        }

        // Check authorization
        Reservation firstReservation = reservations.get(0);
        if (!authorizationService.canManageReservation(manager, firstReservation)) {
            throw new BookingNotAuthorizedException("You are not authorized to reject this edit");
        }

        // Get all active edit proposals for the group
        List<ReservationEditProposal> proposals = editProposalRepository
                .findByRecurringGroupIdAndResolution(recurringGroupId, ResolutionStatus.PENDING);

        if (proposals.isEmpty()) {
            throw new EditProposalNotFoundException("No active edit proposals found for recurring group: " + recurringGroupId);
        }

        // Reject all proposals
        for (ReservationEditProposal proposal : proposals) {
            restoreOriginalValues(proposal, manager, reason);
        }

        log.info("Rejected {} edit proposals in recurring group {} by manager {}", 
                proposals.size(), recurringGroupId, manager.getEmail());

        // Send email to professor
        sendEditRejectedByManagerEmail(firstReservation, proposals.get(0), reason);
    }

    // ==================== Professor Edit Operations ====================

    /**
     * Professor edits their own reservation.
     * If PENDING: changes applied automatically, email sent to lab manager.
     * If APPROVED: creates edit proposal requiring lab manager re-approval.
     */
    @Transactional
    public void editReservationByProfessor(UUID reservationId, EditReservationRequest request, User professor) {
        log.info("Professor {} attempting to edit reservation {}", professor.getEmail(), reservationId);

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));

        // Check authorization - must be the owner
        if (!authorizationService.isReservationOwner(professor, reservation)) {
            log.warn("User {} is not the owner of reservation {}", professor.getEmail(), reservationId);
            throw new BookingNotAuthorizedException("You can only edit your own reservations");
        }

        // Validate edit request
        validateEditRequest(reservation.getLab(), request);

        if (reservation.getStatus() == ReservationStatus.PENDING) {
            // PENDING: Apply changes directly
            applyEditDirectly(reservation, request);
            reservationRepository.save(reservation);

            log.info("Reservation {} edited directly by professor (PENDING status)", reservationId);

            // Send notification email to lab manager
            sendReservationUpdatedEmailToManager(reservation);
        } else if (reservation.getStatus() == ReservationStatus.APPROVED) {
            // APPROVED: Create edit proposal requiring re-approval
            Optional<ReservationEditProposal> existingProposal = editProposalRepository
                    .findByReservationIdAndResolution(reservationId, ResolutionStatus.PENDING);
            if (existingProposal.isPresent()) {
                log.warn("Reservation {} already has a pending edit proposal", reservationId);
                throw new IllegalStateException("Reservation already has a pending edit proposal");
            }

            ReservationEditProposal proposal = createEditProposal(reservation, request, professor, ReservationStatus.APPROVED);
            editProposalRepository.save(proposal);

            reservation.setStatus(ReservationStatus.PENDING_EDIT_APPROVAL);
            reservationRepository.save(reservation);

            log.info("Edit proposal created for reservation {} by professor (APPROVED -> PENDING_EDIT_APPROVAL)", reservationId);

            // Send email to lab manager
            sendEditProposalEmailToManager(reservation, proposal);
        } else {
            log.warn("Cannot edit reservation {} with status {}", reservationId, reservation.getStatus());
            throw new IllegalStateException("Only PENDING or APPROVED reservations can be edited");
        }
    }

    /**
     * Professor approves lab manager's edit of their reservation.
     */
    @Transactional
    public void approveEditByProfessor(UUID reservationId, User professor) {
        log.info("Professor {} attempting to approve edit for reservation {}", professor.getEmail(), reservationId);

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));

        // Check authorization - must be the owner
        if (!authorizationService.isReservationOwner(professor, reservation)) {
            throw new BookingNotAuthorizedException("You can only approve edits to your own reservations");
        }

        ReservationEditProposal proposal = getActiveEditProposal(reservationId);
        
        // Verify the edit was made by a lab manager (not by the professor)
        if (authorizationService.isReservationOwner(proposal.getEditedBy(), reservation)) {
            log.warn("Edit proposal {} was created by the reservation owner, not a lab manager", proposal.getId());
            throw new IllegalStateException("This edit proposal was not created by a lab manager");
        }

        // Apply the edit
        applyEditProposal(proposal, professor);

        log.info("Edit approved for reservation {} by professor {}", reservationId, professor.getEmail());

        // Send email to lab manager
        sendEditApprovedByProfessorEmail(reservation, proposal);
    }

    /**
     * Professor rejects lab manager's edit of their reservation.
     */
    @Transactional
    public void rejectEditByProfessor(UUID reservationId, User professor, String reason) {
        log.info("Professor {} attempting to reject edit for reservation {}", professor.getEmail(), reservationId);

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));

        // Check authorization - must be the owner
        if (!authorizationService.isReservationOwner(professor, reservation)) {
            throw new BookingNotAuthorizedException("You can only reject edits to your own reservations");
        }

        ReservationEditProposal proposal = getActiveEditProposal(reservationId);
        
        // Verify the edit was made by a lab manager
        if (authorizationService.isReservationOwner(proposal.getEditedBy(), reservation)) {
            log.warn("Edit proposal {} was created by the reservation owner, not a lab manager", proposal.getId());
            throw new IllegalStateException("This edit proposal was not created by a lab manager");
        }

        // Restore original values
        restoreOriginalValues(proposal, professor, reason);

        log.info("Edit rejected for reservation {} by professor {}", reservationId, professor.getEmail());

        // Send email to lab manager
        sendEditRejectedByProfessorEmail(reservation, proposal, reason);
    }

    /**
     * Professor edits their own recurring group.
     * If all occurrences are PENDING: changes applied automatically.
     * If any occurrence is APPROVED: those need re-approval.
     */
    @Transactional
    public void editRecurringGroupByProfessor(UUID recurringGroupId, EditReservationRequest request, User professor) {
        log.info("Professor {} attempting to edit recurring group {}", professor.getEmail(), recurringGroupId);

        List<Reservation> reservations = reservationRepository.findByRecurringGroupId(recurringGroupId);
        
        if (reservations.isEmpty()) {
            throw new ReservationNotFoundException("No reservations found for recurring group: " + recurringGroupId);
        }

        // Check authorization - must be the owner of all reservations
        for (Reservation reservation : reservations) {
            if (!authorizationService.isReservationOwner(professor, reservation)) {
                throw new BookingNotAuthorizedException("You can only edit your own recurring groups");
            }
        }

        // Validate edit request
        validateEditRequest(reservations.get(0).getLab(), request);

        // Separate PENDING and APPROVED reservations
        List<Reservation> pendingReservations = reservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.PENDING)
                .collect(Collectors.toList());
        
        List<Reservation> approvedReservations = reservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.APPROVED)
                .collect(Collectors.toList());

        // Apply changes directly to PENDING reservations
        for (Reservation reservation : pendingReservations) {
            applyEditDirectly(reservation, request);
            reservationRepository.save(reservation);
        }

        // Create edit proposals for APPROVED reservations
        for (Reservation reservation : approvedReservations) {
            Optional<ReservationEditProposal> existingProposal = editProposalRepository
                    .findByReservationIdAndResolution(reservation.getId(), ResolutionStatus.PENDING);
            if (existingProposal.isPresent()) {
                log.warn("Reservation {} already has a pending edit proposal, skipping", reservation.getId());
                continue;
            }

            ReservationEditProposal proposal = createEditProposal(reservation, request, professor, ReservationStatus.APPROVED);
            editProposalRepository.save(proposal);

            reservation.setStatus(ReservationStatus.PENDING_EDIT_APPROVAL);
            reservationRepository.save(reservation);
        }

        log.info("Edited recurring group {}: {} PENDING (direct), {} APPROVED (needs re-approval)", 
                recurringGroupId, pendingReservations.size(), approvedReservations.size());

        // Send email to lab manager
        if (!pendingReservations.isEmpty()) {
            sendReservationUpdatedEmailToManager(pendingReservations.get(0));
        }
        if (!approvedReservations.isEmpty()) {
            ReservationEditProposal firstProposal = editProposalRepository
                    .findByReservationIdAndResolution(approvedReservations.get(0).getId(), ResolutionStatus.PENDING)
                    .orElse(null);
            sendEditProposalEmailToManager(approvedReservations.get(0), firstProposal);
        }
    }

    /**
     * Professor approves lab manager's edit of their recurring group.
     */
    @Transactional
    public void approveRecurringGroupEditByProfessor(UUID recurringGroupId, User professor) {
        log.info("Professor {} attempting to approve edit for recurring group {}", professor.getEmail(), recurringGroupId);

        List<Reservation> reservations = reservationRepository.findByRecurringGroupId(recurringGroupId);
        
        if (reservations.isEmpty()) {
            throw new ReservationNotFoundException("No reservations found for recurring group: " + recurringGroupId);
        }

        // Check authorization
        for (Reservation reservation : reservations) {
            if (!authorizationService.isReservationOwner(professor, reservation)) {
                throw new BookingNotAuthorizedException("You can only approve edits to your own recurring groups");
            }
        }

        // Get all active edit proposals for the group
        List<ReservationEditProposal> proposals = editProposalRepository
                .findByRecurringGroupIdAndResolution(recurringGroupId, ResolutionStatus.PENDING);

        if (proposals.isEmpty()) {
            throw new EditProposalNotFoundException("No active edit proposals found for recurring group: " + recurringGroupId);
        }

        // Approve all proposals
        for (ReservationEditProposal proposal : proposals) {
            applyEditProposal(proposal, professor);
        }

        log.info("Approved {} edit proposals in recurring group {} by professor {}", 
                proposals.size(), recurringGroupId, professor.getEmail());

        // Send email to lab manager
        sendEditApprovedByProfessorEmail(reservations.get(0), proposals.get(0));
    }

    /**
     * Professor rejects lab manager's edit of their recurring group.
     */
    @Transactional
    public void rejectRecurringGroupEditByProfessor(UUID recurringGroupId, User professor, String reason) {
        log.info("Professor {} attempting to reject edit for recurring group {}", professor.getEmail(), recurringGroupId);

        List<Reservation> reservations = reservationRepository.findByRecurringGroupId(recurringGroupId);
        
        if (reservations.isEmpty()) {
            throw new ReservationNotFoundException("No reservations found for recurring group: " + recurringGroupId);
        }

        // Check authorization
        for (Reservation reservation : reservations) {
            if (!authorizationService.isReservationOwner(professor, reservation)) {
                throw new BookingNotAuthorizedException("You can only reject edits to your own recurring groups");
            }
        }

        // Get all active edit proposals for the group
        List<ReservationEditProposal> proposals = editProposalRepository
                .findByRecurringGroupIdAndResolution(recurringGroupId, ResolutionStatus.PENDING);

        if (proposals.isEmpty()) {
            throw new EditProposalNotFoundException("No active edit proposals found for recurring group: " + recurringGroupId);
        }

        // Reject all proposals
        for (ReservationEditProposal proposal : proposals) {
            restoreOriginalValues(proposal, professor, reason);
        }

        log.info("Rejected {} edit proposals in recurring group {} by professor {}", 
                proposals.size(), recurringGroupId, professor.getEmail());

        // Send email to lab manager
        sendEditRejectedByProfessorEmail(reservations.get(0), proposals.get(0), reason);
    }

    // ==================== Helper Methods ====================

    /**
     * Create an edit proposal from a reservation and edit request.
     */
    private ReservationEditProposal createEditProposal(Reservation reservation, EditReservationRequest request, 
                                                       User editor, ReservationStatus originalStatus) {
        // Get current workstation IDs
        List<Integer> currentWorkstationIds = reservationWorkstationRepository
                .findWorkstationIdsByReservationId(reservation.getId());

        ReservationEditProposal proposal = new ReservationEditProposal();
        proposal.setReservation(reservation);
        proposal.setEditedBy(editor);
        proposal.setOriginalStatus(originalStatus);
        
        // Store original values (ensure non-null for nullable=false columns)
        proposal.setOriginalStartTime(reservation.getStartTime());
        proposal.setOriginalEndTime(reservation.getEndTime());
        proposal.setOriginalDescription(reservation.getDescription());
        proposal.setOriginalWholeLab(reservation.getWholeLab() != null ? reservation.getWholeLab() : false);
        proposal.setOriginalWorkstationIds(currentWorkstationIds != null ? currentWorkstationIds : List.of());
        
        // Store proposed values (ensure non-null for nullable=false columns)
        proposal.setProposedStartTime(request.getStartTime());
        proposal.setProposedEndTime(request.getEndTime());
        proposal.setProposedDescription(request.getDescription());
        proposal.setProposedWholeLab(request.getWholeLab() != null ? request.getWholeLab() : false);
        proposal.setProposedWorkstationIds(request.getWorkstationIds() != null ? request.getWorkstationIds() : List.of());
        
        proposal.setResolution(ResolutionStatus.PENDING);

        return proposal;
    }

    /**
     * Apply an approved edit proposal to the reservation.
     */
    @Transactional
    private void applyEditProposal(ReservationEditProposal proposal, User approver) {
        Reservation reservation = proposal.getReservation();

        // Update reservation with proposed values
        reservation.setStartTime(proposal.getProposedStartTime());
        reservation.setEndTime(proposal.getProposedEndTime());
        reservation.setDescription(proposal.getProposedDescription());
        Boolean proposedWholeLab = proposal.getProposedWholeLab() != null ? proposal.getProposedWholeLab() : false;
        reservation.setWholeLab(proposedWholeLab);

        // Update workstation assignments
        reservationWorkstationRepository.deleteByReservationId(reservation.getId());
        if (!proposedWholeLab && proposal.getProposedWorkstationIds() != null 
                && !proposal.getProposedWorkstationIds().isEmpty()) {
            for (Integer workstationId : proposal.getProposedWorkstationIds()) {
                Workstation workstation = workstationRepository.findById(workstationId)
                        .orElseThrow(() -> new RuntimeException("Workstation not found: " + workstationId));
                ReservationWorkstation rw = new ReservationWorkstation(reservation, workstation);
                reservationWorkstationRepository.save(rw);
            }
        }

        // Set status based on original status
        // If original was APPROVED and professor edited, it goes back to APPROVED
        // If original was PENDING and lab manager edited, it becomes APPROVED
        if (proposal.getOriginalStatus() == ReservationStatus.APPROVED) {
            reservation.setStatus(ReservationStatus.APPROVED);
        } else {
            reservation.setStatus(ReservationStatus.APPROVED);
        }

        reservationRepository.save(reservation);

        // Mark proposal as approved
        proposal.setResolution(ResolutionStatus.APPROVED);
        proposal.setResolvedAt(OffsetDateTime.now());
        proposal.setResolvedBy(approver);
        editProposalRepository.save(proposal);

        log.debug("Applied edit proposal {} to reservation {}", proposal.getId(), reservation.getId());
    }

    /**
     * Restore original values from an edit proposal when edit is rejected.
     */
    @Transactional
    private void restoreOriginalValues(ReservationEditProposal proposal, User rejector, String reason) {
        Reservation reservation = proposal.getReservation();

        // Restore original values
        reservation.setStartTime(proposal.getOriginalStartTime());
        reservation.setEndTime(proposal.getOriginalEndTime());
        reservation.setDescription(proposal.getOriginalDescription());
        Boolean originalWholeLab = proposal.getOriginalWholeLab() != null ? proposal.getOriginalWholeLab() : false;
        reservation.setWholeLab(originalWholeLab);

        // Restore workstation assignments
        reservationWorkstationRepository.deleteByReservationId(reservation.getId());
        if (!originalWholeLab && proposal.getOriginalWorkstationIds() != null 
                && !proposal.getOriginalWorkstationIds().isEmpty()) {
            for (Integer workstationId : proposal.getOriginalWorkstationIds()) {
                Workstation workstation = workstationRepository.findById(workstationId)
                        .orElseThrow(() -> new RuntimeException("Workstation not found: " + workstationId));
                ReservationWorkstation rw = new ReservationWorkstation(reservation, workstation);
                reservationWorkstationRepository.save(rw);
            }
        }

        // Restore original status
        reservation.setStatus(proposal.getOriginalStatus());
        reservationRepository.save(reservation);

        // Mark proposal as rejected
        proposal.setResolution(ResolutionStatus.REJECTED);
        proposal.setResolvedAt(OffsetDateTime.now());
        proposal.setResolvedBy(rejector);
        editProposalRepository.save(proposal);

        log.debug("Restored original values for reservation {} from edit proposal {}", 
                reservation.getId(), proposal.getId());
    }

    /**
     * Apply edit directly to a reservation (for PENDING reservations edited by professor).
     */
    private void applyEditDirectly(Reservation reservation, EditReservationRequest request) {
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());
        reservation.setDescription(request.getDescription());
        Boolean wholeLab = request.getWholeLab() != null ? request.getWholeLab() : false;
        reservation.setWholeLab(wholeLab);

        // Update workstation assignments
        reservationWorkstationRepository.deleteByReservationId(reservation.getId());
        if (!wholeLab && request.getWorkstationIds() != null 
                && !request.getWorkstationIds().isEmpty()) {
            for (Integer workstationId : request.getWorkstationIds()) {
                Workstation workstation = workstationRepository.findById(workstationId)
                        .orElseThrow(() -> new RuntimeException("Workstation not found: " + workstationId));
                ReservationWorkstation rw = new ReservationWorkstation(reservation, workstation);
                reservationWorkstationRepository.save(rw);
            }
        }
    }

    /**
     * Get active (PENDING) edit proposal for a reservation.
     */
    private ReservationEditProposal getActiveEditProposal(UUID reservationId) {
        return editProposalRepository.findByReservationIdAndResolution(reservationId, ResolutionStatus.PENDING)
                .orElseThrow(() -> {
                    log.warn("No active edit proposal found for reservation {}", reservationId);
                    return new EditProposalNotFoundException("No active edit proposal found for reservation: " + reservationId);
                });
    }

    /**
     * Validate operating hours for a lab.
     */
    private void validateOperatingHours(Lab lab, OffsetDateTime startTime, OffsetDateTime endTime) {
        // Convert Java DayOfWeek (1=Mon to 7=Sun) to our format (0=Sun to 6=Sat)
        int javaDayValue = startTime.getDayOfWeek().getValue();
        int dayOfWeek = (javaDayValue == 7) ? 0 : javaDayValue;
        
        log.debug("Validating operating hours for lab {} on day {} ({})", 
                lab.getId(), dayOfWeek, startTime.getDayOfWeek());

        Optional<LabOperatingHours> operatingHoursOpt = labOperatingHoursRepository
                .findByLabIdAndDayOfWeek(lab.getId(), dayOfWeek);

        if (operatingHoursOpt.isPresent()) {
            LabOperatingHours operatingHours = operatingHoursOpt.get();
            log.debug("Found specific operating hours: {} - {}, closed: {}", 
                    operatingHours.getOpenTime(), operatingHours.getCloseTime(), operatingHours.getIsClosed());

            if (operatingHours.getIsClosed()) {
                log.warn("Lab {} is closed on day {}", lab.getId(), dayOfWeek);
                throw new com._glab.booking_system.booking.exception.LabClosedException("Lab is closed on this day");
            }

            java.time.LocalTime startLocalTime = startTime.toLocalTime();
            java.time.LocalTime endLocalTime = endTime.toLocalTime();

            if (startLocalTime.isBefore(operatingHours.getOpenTime()) || 
                endLocalTime.isAfter(operatingHours.getCloseTime())) {
                log.warn("Reservation {} - {} is outside operating hours {} - {}", 
                        startLocalTime, endLocalTime, operatingHours.getOpenTime(), operatingHours.getCloseTime());
                throw new com._glab.booking_system.booking.exception.OutsideOperatingHoursException("Reservation time must be within operating hours (" 
                        + operatingHours.getOpenTime() + " - " + operatingHours.getCloseTime() + ")");
            }
        } else {
            log.debug("No specific operating hours found, using lab defaults");
            // No specific hours defined - use lab defaults if available
            if (lab.getDefaultOpenTime() != null && lab.getDefaultCloseTime() != null) {
                java.time.LocalTime startLocalTime = startTime.toLocalTime();
                java.time.LocalTime endLocalTime = endTime.toLocalTime();

                if (startLocalTime.isBefore(lab.getDefaultOpenTime()) || 
                    endLocalTime.isAfter(lab.getDefaultCloseTime())) {
                    log.warn("Reservation {} - {} is outside default operating hours {} - {}", 
                            startLocalTime, endLocalTime, lab.getDefaultOpenTime(), lab.getDefaultCloseTime());
                    throw new com._glab.booking_system.booking.exception.OutsideOperatingHoursException("Reservation time must be within operating hours (" 
                            + lab.getDefaultOpenTime() + " - " + lab.getDefaultCloseTime() + ")");
                }
            }
            
            // Check if it's Sunday (default closed)
            if (dayOfWeek == 0) {
                log.warn("Lab {} is closed on Sundays by default", lab.getId());
                throw new com._glab.booking_system.booking.exception.LabClosedException("Lab is closed on Sundays by default");
            }
        }
        
        log.debug("Operating hours validation passed");
    }

    /**
     * Validate that lab is not closed on the specified date.
     */
    private void validateLabNotClosed(Integer labId, java.time.LocalDate date) {
        int javaDayValue = date.getDayOfWeek().getValue();
        int dayOfWeek = (javaDayValue == 7) ? 0 : javaDayValue;
        
        log.debug("Checking if lab {} is closed on {} (day {})", labId, date, dayOfWeek);

        if (labClosedDayRepository.isLabClosedOnDate(labId, date, dayOfWeek)) {
            log.warn("Lab {} is closed on {}", labId, date);
            throw new com._glab.booking_system.booking.exception.LabClosedException("Lab is closed on " + date);
        }
        
        log.debug("Lab {} is open on {}", labId, date);
    }

    /**
     * Validate edit request.
     */
    private void validateEditRequest(Lab lab, EditReservationRequest request) {
        // Validate times
        if (request.getStartTime().isAfter(request.getEndTime())) {
            throw new com._glab.booking_system.booking.exception.InvalidReservationTimeException("Start time must be before end time");
        }

        if (request.getStartTime().isBefore(OffsetDateTime.now())) {
            throw new com._glab.booking_system.booking.exception.InvalidReservationTimeException("Start time must be in the future");
        }

        long durationMinutes = java.time.Duration.between(request.getStartTime(), request.getEndTime()).toMinutes();
        if (durationMinutes < 15) {
            throw new com._glab.booking_system.booking.exception.InvalidReservationTimeException("Reservation duration must be at least 15 minutes");
        }

        // Validate operating hours
        validateOperatingHours(lab, request.getStartTime(), request.getEndTime());
        
        // Validate lab is not closed on this date
        validateLabNotClosed(lab.getId(), request.getStartTime().toLocalDate());

        // Validate workstations if not whole lab
        if (!request.getWholeLab() && (request.getWorkstationIds() == null || request.getWorkstationIds().isEmpty())) {
            throw new com._glab.booking_system.booking.exception.NoWorkstationsSelectedException();
        }

        if (!request.getWholeLab() && request.getWorkstationIds() != null) {
            for (Integer workstationId : request.getWorkstationIds()) {
                Workstation workstation = workstationRepository.findById(workstationId)
                        .orElseThrow(() -> new com._glab.booking_system.booking.exception.WorkstationNotFoundException(workstationId));
                
                if (!workstation.getLab().getId().equals(lab.getId())) {
                    throw new com._glab.booking_system.booking.exception.WorkstationNotInLabException(workstationId, lab.getId());
                }
                
                if (!workstation.getActive()) {
                    throw new com._glab.booking_system.booking.exception.WorkstationInactiveException(workstation.getIdentifier());
                }
            }
        }
    }

    // ==================== Email Notifications ====================

    private void sendEditProposalEmailToProfessor(Reservation reservation, ReservationEditProposal proposal) {
        try {
            User professor = reservation.getUser();
            User manager = proposal.getEditedBy();
            String professorName = professor.getFirstName() + " " + professor.getLastName();
            String managerName = manager.getFirstName() + " " + manager.getLastName();
            String labName = reservation.getLab().getName();
            
            emailService.sendReservationEditProposalEmailToProfessor(
                    professor.getEmail(),
                    professorName,
                    managerName,
                    labName,
                    reservation.getId()
            );
        } catch (Exception e) {
            log.error("Failed to send edit proposal email to professor: {}", e.getMessage());
        }
    }

    private void sendEditProposalEmailToManager(Reservation reservation, ReservationEditProposal proposal) {
        try {
            User professor = reservation.getUser();
            String professorName = professor.getFirstName() + " " + professor.getLastName();
            String labName = reservation.getLab().getName();
            
            // Get lab managers for this lab
            List<com._glab.booking_system.booking.model.LabManager> managers = 
                    labManagerRepository.findByLab(reservation.getLab());
            
            for (com._glab.booking_system.booking.model.LabManager labManager : managers) {
                User managerUser = labManager.getUser();
                String managerName = managerUser.getFirstName() + " " + managerUser.getLastName();
                
                emailService.sendReservationEditProposalEmailToManager(
                        managerUser.getEmail(),
                        managerName,
                        professorName,
                        labName,
                        reservation.getId()
                );
            }
        } catch (Exception e) {
            log.error("Failed to send edit proposal email to manager: {}", e.getMessage());
        }
    }

    private void sendReservationUpdatedEmailToManager(Reservation reservation) {
        try {
            User professor = reservation.getUser();
            String professorName = professor.getFirstName() + " " + professor.getLastName();
            String labName = reservation.getLab().getName();
            
            // Get lab managers for this lab
            List<com._glab.booking_system.booking.model.LabManager> managers = 
                    labManagerRepository.findByLab(reservation.getLab());
            
            for (com._glab.booking_system.booking.model.LabManager labManager : managers) {
                User managerUser = labManager.getUser();
                String managerName = managerUser.getFirstName() + " " + managerUser.getLastName();
                
                emailService.sendReservationUpdatedEmailToManager(
                        managerUser.getEmail(),
                        managerName,
                        professorName,
                        labName,
                        reservation.getId()
                );
            }
        } catch (Exception e) {
            log.error("Failed to send reservation updated email to manager: {}", e.getMessage());
        }
    }

    private void sendEditApprovedByManagerEmail(Reservation reservation, ReservationEditProposal proposal) {
        try {
            User professor = reservation.getUser();
            String professorName = professor.getFirstName() + " " + professor.getLastName();
            String managerName = proposal.getResolvedBy() != null 
                    ? proposal.getResolvedBy().getFirstName() + " " + proposal.getResolvedBy().getLastName()
                    : "Lab Manager";
            String labName = reservation.getLab().getName();
            
            emailService.sendEditApprovedByManagerEmail(
                    professor.getEmail(),
                    professorName,
                    managerName,
                    labName,
                    reservation.getId()
            );
        } catch (Exception e) {
            log.error("Failed to send edit approved email: {}", e.getMessage());
        }
    }

    private void sendEditRejectedByManagerEmail(Reservation reservation, ReservationEditProposal proposal, String reason) {
        try {
            User professor = reservation.getUser();
            String professorName = professor.getFirstName() + " " + professor.getLastName();
            String managerName = proposal.getResolvedBy() != null 
                    ? proposal.getResolvedBy().getFirstName() + " " + proposal.getResolvedBy().getLastName()
                    : "Lab Manager";
            String labName = reservation.getLab().getName();
            
            emailService.sendEditRejectedByManagerEmail(
                    professor.getEmail(),
                    professorName,
                    managerName,
                    labName,
                    reservation.getId(),
                    reason
            );
        } catch (Exception e) {
            log.error("Failed to send edit rejected email: {}", e.getMessage());
        }
    }

    private void sendEditApprovedByProfessorEmail(Reservation reservation, ReservationEditProposal proposal) {
        try {
            User manager = proposal.getEditedBy();
            String managerName = manager.getFirstName() + " " + manager.getLastName();
            String professorName = reservation.getUser().getFirstName() + " " + reservation.getUser().getLastName();
            String labName = reservation.getLab().getName();
            
            emailService.sendEditApprovedByProfessorEmail(
                    manager.getEmail(),
                    managerName,
                    professorName,
                    labName,
                    reservation.getId()
            );
        } catch (Exception e) {
            log.error("Failed to send edit approved email: {}", e.getMessage());
        }
    }

    private void sendEditRejectedByProfessorEmail(Reservation reservation, ReservationEditProposal proposal, String reason) {
        try {
            User manager = proposal.getEditedBy();
            String managerName = manager.getFirstName() + " " + manager.getLastName();
            String professorName = reservation.getUser().getFirstName() + " " + reservation.getUser().getLastName();
            String labName = reservation.getLab().getName();
            
            emailService.sendEditRejectedByProfessorEmail(
                    manager.getEmail(),
                    managerName,
                    professorName,
                    labName,
                    reservation.getId(),
                    reason
            );
        } catch (Exception e) {
            log.error("Failed to send edit rejected email: {}", e.getMessage());
        }
    }
}
