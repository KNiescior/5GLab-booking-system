package com._glab.booking_system.booking.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com._glab.booking_system.user.model.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reservation_edit_proposal", indexes = {
    @Index(name = "idx_edit_proposal_reservation", columnList = "reservation_id"),
    @Index(name = "idx_edit_proposal_resolution", columnList = "resolution")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class ReservationEditProposal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "edited_by", nullable = false)
    private User editedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "original_status", nullable = false, length = 20)
    private ReservationStatus originalStatus;

    @Column(name = "original_start_time", nullable = false)
    private OffsetDateTime originalStartTime;

    @Column(name = "original_end_time", nullable = false)
    private OffsetDateTime originalEndTime;

    @Column(name = "original_description", columnDefinition = "TEXT")
    private String originalDescription;

    @Column(name = "original_whole_lab", nullable = false)
    private Boolean originalWholeLab = false;

    @Convert(converter = WorkstationIdsConverter.class)
    @Column(name = "original_workstation_ids", columnDefinition = "TEXT")
    private List<Integer> originalWorkstationIds;

    @Column(name = "proposed_start_time", nullable = false)
    private OffsetDateTime proposedStartTime;

    @Column(name = "proposed_end_time", nullable = false)
    private OffsetDateTime proposedEndTime;

    @Column(name = "proposed_description", columnDefinition = "TEXT")
    private String proposedDescription;

    @Column(name = "proposed_whole_lab", nullable = false)
    private Boolean proposedWholeLab = false;

    @Convert(converter = WorkstationIdsConverter.class)
    @Column(name = "proposed_workstation_ids", columnDefinition = "TEXT")
    private List<Integer> proposedWorkstationIds;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution", nullable = false, length = 20)
    private ResolutionStatus resolution = ResolutionStatus.PENDING;

    /**
     * Converter for storing List<Integer> as JSON string in the database.
     */
    @Converter
    public static class WorkstationIdsConverter implements AttributeConverter<List<Integer>, String> {
        private static final ObjectMapper objectMapper = new ObjectMapper();

        @Override
        public String convertToDatabaseColumn(List<Integer> attribute) {
            if (attribute == null || attribute.isEmpty()) {
                return null;
            }
            try {
                return objectMapper.writeValueAsString(attribute);
            } catch (Exception e) {
                throw new RuntimeException("Error converting workstation IDs to JSON", e);
            }
        }

        @Override
        public List<Integer> convertToEntityAttribute(String dbData) {
            if (dbData == null || dbData.isEmpty()) {
                return List.of();
            }
            try {
                return objectMapper.readValue(dbData, new TypeReference<List<Integer>>() {});
            } catch (Exception e) {
                throw new RuntimeException("Error converting JSON to workstation IDs", e);
            }
        }
    }
}
