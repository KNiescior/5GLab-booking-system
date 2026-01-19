package com._glab.booking_system.booking.model;

import java.util.UUID;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reservation_workstation", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"reservation_id", "workstation_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class ReservationWorkstation {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workstation_id", nullable = false)
    private Workstation workstation;

    public ReservationWorkstation(Reservation reservation, Workstation workstation) {
        this.reservation = reservation;
        this.workstation = workstation;
    }
}
