package com._glab.booking_system.admin.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com._glab.booking_system.admin.response.LogEntryResponse;
import com._glab.booking_system.admin.service.LogService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Admin-only controller for viewing application logs (paginated, filtered).
 */
@RestController
@RequestMapping("/api/v1/admin/logs")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class LogController {

    private final LogService logService;

    @GetMapping
    public ResponseEntity<List<LogEntryResponse>> getLogs(
            @RequestParam(required = false) String level,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        int safeSize = Math.min(Math.max(1, size), 500);
        List<LogEntryResponse> entries = logService.getLogEntries(level, dateFrom, dateTo, search, page, safeSize);
        return ResponseEntity.ok(entries);
    }
}
