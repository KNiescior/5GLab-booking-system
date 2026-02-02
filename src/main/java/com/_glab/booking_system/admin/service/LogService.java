package com._glab.booking_system.admin.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com._glab.booking_system.admin.response.LogEntryResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * Service to read and filter log file contents for admin log API.
 */
@Service
@Slf4j
public class LogService {

    private static final Pattern LOG_PATTERN = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}[^\\s]*)\\s+(\\w+)\\s+.*?\\s+\\[\\s*([^\\]]+)\\]\\s+([^:]+)\\s*:\\s*(.*)$",
            Pattern.DOTALL);

    @Value("${logging.file.name:logs/spring.log}")
    private String logFilePath;

    /**
     * Read log file and return entries with optional filtering and pagination.
     */
    public List<LogEntryResponse> getLogEntries(
            String level,
            LocalDate dateFrom,
            LocalDate dateTo,
            String search,
            int page,
            int size) {
        Path path = resolveLogPath();
        if (path == null || !Files.isReadable(path)) {
            return List.of();
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(path);
        } catch (IOException e) {
            log.warn("Could not read log file: {}", e.getMessage());
            return List.of();
        }
        List<LogEntryResponse> entries = new ArrayList<>();
        for (String line : lines) {
            LogEntryResponse entry = parseLine(line);
            if (entry == null) {
                entry = LogEntryResponse.builder().raw(line).build();
            }
            if (level != null && !level.isEmpty() && !level.equalsIgnoreCase(entry.getLevel())) {
                continue;
            }
            if (dateFrom != null && entry.getTimestamp() != null) {
                try {
                    LocalDate lineDate = LocalDate.parse(entry.getTimestamp().substring(0, 10));
                    if (lineDate.isBefore(dateFrom)) continue;
                } catch (Exception ignored) {}
            }
            if (dateTo != null && entry.getTimestamp() != null) {
                try {
                    LocalDate lineDate = LocalDate.parse(entry.getTimestamp().substring(0, 10));
                    if (lineDate.isAfter(dateTo)) continue;
                } catch (Exception ignored) {}
            }
            if (search != null && !search.isEmpty()) {
                String raw = entry.getRaw() != null ? entry.getRaw() : line;
                if (!raw.toLowerCase().contains(search.toLowerCase())) continue;
            }
            entries.add(entry.getRaw() == null ? entry.toBuilder().raw(line).build() : entry);
        }
        List<LogEntryResponse> reversed = new ArrayList<>(entries);
        java.util.Collections.reverse(reversed);
        int start = Math.min(page * size, reversed.size());
        int end = Math.min(start + size, reversed.size());
        return reversed.subList(start, end);
    }

    private LogEntryResponse parseLine(String line) {
        Matcher m = LOG_PATTERN.matcher(line);
        if (m.matches()) {
            return LogEntryResponse.builder()
                    .timestamp(m.group(1))
                    .level(m.group(2))
                    .logger(m.group(4).trim())
                    .message(m.group(5))
                    .raw(line)
                    .build();
        }
        return null;
    }

    private Path resolveLogPath() {
        try {
            Path p = Path.of(logFilePath);
            if (Files.exists(p)) return p;
            Path absolute = Path.of(System.getProperty("user.dir"), logFilePath);
            if (Files.exists(absolute)) return absolute;
        } catch (Exception e) {
            log.debug("Log path resolution: {}", e.getMessage());
        }
        return null;
    }
}
