package com._glab.booking_system.booking.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com._glab.booking_system.booking.exception.LabNotFoundException;
import com._glab.booking_system.booking.exception.WorkstationNotFoundException;
import com._glab.booking_system.booking.model.Lab;
import com._glab.booking_system.booking.model.Workstation;
import com._glab.booking_system.booking.repository.LabRepository;
import com._glab.booking_system.booking.repository.WorkstationRepository;
import com._glab.booking_system.booking.request.CreateWorkstationRequest;
import com._glab.booking_system.booking.request.UpdateWorkstationRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkstationService {

    private final WorkstationRepository workstationRepository;
    private final LabRepository labRepository;

    public Workstation getWorkstationById(Integer id) {
        return workstationRepository.findById(id)
                .orElseThrow(() -> new WorkstationNotFoundException(id));
    }

    public List<Workstation> getWorkstationsByLabId(Integer labId) {
        return workstationRepository.findByLabId(labId);
    }

    public List<Workstation> getActiveWorkstationsByLabId(Integer labId) {
        return workstationRepository.findByLabIdAndActiveTrue(labId);
    }

    public List<Workstation> getAllWorkstations() {
        return workstationRepository.findAll();
    }

    @Transactional
    public Workstation createWorkstation(CreateWorkstationRequest request) {
        Lab lab = labRepository.findById(request.getLabId())
                .orElseThrow(() -> new LabNotFoundException(request.getLabId()));
        if (workstationRepository.findByLabAndIdentifier(lab, request.getIdentifier()).isPresent()) {
            throw new IllegalStateException("Workstation with identifier " + request.getIdentifier() + " already exists in this lab");
        }
        Workstation ws = new Workstation();
        ws.setLab(lab);
        ws.setIdentifier(request.getIdentifier());
        ws.setDescription(request.getDescription());
        ws.setActive(true);
        return workstationRepository.save(ws);
    }

    @Transactional
    public Workstation updateWorkstation(Integer id, UpdateWorkstationRequest request) {
        Workstation ws = getWorkstationById(id);
        if (request.getIdentifier() != null) {
            if (workstationRepository.findByLabAndIdentifier(ws.getLab(), request.getIdentifier())
                    .filter(other -> !other.getId().equals(id))
                    .isPresent()) {
                throw new IllegalStateException("Workstation with identifier " + request.getIdentifier() + " already exists in this lab");
            }
            ws.setIdentifier(request.getIdentifier());
        }
        if (request.getDescription() != null) ws.setDescription(request.getDescription());
        if (request.getActive() != null) ws.setActive(request.getActive());
        return workstationRepository.save(ws);
    }

    @Transactional
    public Workstation archiveWorkstation(Integer id) {
        Workstation ws = getWorkstationById(id);
        ws.setActive(false);
        return workstationRepository.save(ws);
    }
}
