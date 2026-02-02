package com._glab.booking_system.admin.service;

import com._glab.booking_system.admin.response.LogEntryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LogServiceTest {

    private LogService logService;

    @TempDir
    Path tempDir;

    private Path logFile;

    @BeforeEach
    void setUp() throws IOException {
        logService = new LogService();
        logFile = tempDir.resolve("test.log");
        ReflectionTestUtils.setField(logService, "logFilePath", logFile.toString());
    }

    @Nested
    @DisplayName("Get Log Entries Tests")
    class GetLogEntriesTests {

        @Test
        @DisplayName("Should return empty list when log file does not exist")
        void shouldReturnEmptyListWhenLogFileDoesNotExist() {
            List<LogEntryResponse> result = logService.getLogEntries(null, null, null, null, 0, 100);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should parse valid log entries")
        void shouldParseValidLogEntries() throws IOException {
            String logContent = """
                    2026-02-02T10:30:00.123+00:00  INFO --- [           main] c._glab.booking_system.Application       : Application started
                    2026-02-02T10:30:01.456+00:00 DEBUG --- [           main] c._glab.booking_system.service.TestSvc   : Debug message
                    2026-02-02T10:30:02.789+00:00 ERROR --- [           main] c._glab.booking_system.service.ErrorSvc  : Error occurred
                    """;
            Files.writeString(logFile, logContent);

            List<LogEntryResponse> result = logService.getLogEntries(null, null, null, null, 0, 100);

            assertThat(result).hasSize(3);
            // Results are reversed (newest first)
            assertThat(result.get(0).getLevel()).isEqualTo("ERROR");
            assertThat(result.get(1).getLevel()).isEqualTo("DEBUG");
            assertThat(result.get(2).getLevel()).isEqualTo("INFO");
        }

        @Test
        @DisplayName("Should filter by log level")
        void shouldFilterByLogLevel() throws IOException {
            String logContent = """
                    2026-02-02T10:30:00.123+00:00  INFO --- [           main] c._glab.booking_system.Application       : Info message
                    2026-02-02T10:30:01.456+00:00 DEBUG --- [           main] c._glab.booking_system.service.TestSvc   : Debug message
                    2026-02-02T10:30:02.789+00:00 ERROR --- [           main] c._glab.booking_system.service.ErrorSvc  : Error message
                    """;
            Files.writeString(logFile, logContent);

            List<LogEntryResponse> result = logService.getLogEntries("ERROR", null, null, null, 0, 100);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getLevel()).isEqualTo("ERROR");
            assertThat(result.get(0).getMessage()).isEqualTo("Error message");
        }

        @Test
        @DisplayName("Should filter by date range")
        void shouldFilterByDateRange() throws IOException {
            String logContent = """
                    2026-02-01T10:30:00.123+00:00  INFO --- [           main] c._glab.booking_system.Application       : Day 1
                    2026-02-02T10:30:01.456+00:00  INFO --- [           main] c._glab.booking_system.service.TestSvc   : Day 2
                    2026-02-03T10:30:02.789+00:00  INFO --- [           main] c._glab.booking_system.service.ErrorSvc  : Day 3
                    """;
            Files.writeString(logFile, logContent);

            List<LogEntryResponse> result = logService.getLogEntries(
                    null,
                    LocalDate.of(2026, 2, 2),
                    LocalDate.of(2026, 2, 2),
                    null,
                    0,
                    100
            );

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getMessage()).isEqualTo("Day 2");
        }

        @Test
        @DisplayName("Should filter by search term")
        void shouldFilterBySearchTerm() throws IOException {
            String logContent = """
                    2026-02-02T10:30:00.123+00:00  INFO --- [           main] c._glab.booking_system.Application       : Application started
                    2026-02-02T10:30:01.456+00:00  INFO --- [           main] c._glab.booking_system.service.TestSvc   : User logged in
                    2026-02-02T10:30:02.789+00:00 ERROR --- [           main] c._glab.booking_system.service.ErrorSvc  : Application error
                    """;
            Files.writeString(logFile, logContent);

            List<LogEntryResponse> result = logService.getLogEntries(null, null, null, "Application", 0, 100);

            assertThat(result).hasSize(2);
            // Both "Application started" and "Application error" contain "Application"
        }

        @Test
        @DisplayName("Should filter by search term case-insensitively")
        void shouldFilterBySearchTermCaseInsensitive() throws IOException {
            String logContent = """
                    2026-02-02T10:30:00.123+00:00  INFO --- [           main] c._glab.booking_system.Application       : APPLICATION STARTED
                    2026-02-02T10:30:01.456+00:00  INFO --- [           main] c._glab.booking_system.service.TestSvc   : User logged in
                    """;
            Files.writeString(logFile, logContent);

            List<LogEntryResponse> result = logService.getLogEntries(null, null, null, "application", 0, 100);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should paginate results")
        void shouldPaginateResults() throws IOException {
            String logContent = """
                    2026-02-02T10:30:00.123+00:00  INFO --- [           main] c._glab.booking_system.Application       : Line 1
                    2026-02-02T10:30:01.456+00:00  INFO --- [           main] c._glab.booking_system.service.TestSvc   : Line 2
                    2026-02-02T10:30:02.789+00:00  INFO --- [           main] c._glab.booking_system.service.ErrorSvc  : Line 3
                    2026-02-02T10:30:03.012+00:00  INFO --- [           main] c._glab.booking_system.service.FourSvc   : Line 4
                    2026-02-02T10:30:04.345+00:00  INFO --- [           main] c._glab.booking_system.service.FiveSvc   : Line 5
                    """;
            Files.writeString(logFile, logContent);

            // Page 0 with size 2 (newest first)
            List<LogEntryResponse> page0 = logService.getLogEntries(null, null, null, null, 0, 2);
            assertThat(page0).hasSize(2);
            assertThat(page0.get(0).getMessage()).isEqualTo("Line 5");
            assertThat(page0.get(1).getMessage()).isEqualTo("Line 4");

            // Page 1 with size 2
            List<LogEntryResponse> page1 = logService.getLogEntries(null, null, null, null, 1, 2);
            assertThat(page1).hasSize(2);
            assertThat(page1.get(0).getMessage()).isEqualTo("Line 3");
            assertThat(page1.get(1).getMessage()).isEqualTo("Line 2");

            // Page 2 with size 2
            List<LogEntryResponse> page2 = logService.getLogEntries(null, null, null, null, 2, 2);
            assertThat(page2).hasSize(1);
            assertThat(page2.get(0).getMessage()).isEqualTo("Line 1");
        }

        @Test
        @DisplayName("Should handle non-parseable log lines")
        void shouldHandleNonParseableLogLines() throws IOException {
            String logContent = """
                    2026-02-02T10:30:00.123+00:00  INFO --- [           main] c._glab.booking_system.Application       : Valid line
                    This is not a valid log line
                    Another invalid line
                    """;
            Files.writeString(logFile, logContent);

            List<LogEntryResponse> result = logService.getLogEntries(null, null, null, null, 0, 100);

            assertThat(result).hasSize(3);
            // Invalid lines should still be included with raw content
            assertThat(result.get(0).getRaw()).contains("Another invalid line");
            assertThat(result.get(1).getRaw()).contains("This is not a valid log line");
            assertThat(result.get(2).getMessage()).isEqualTo("Valid line");
        }

        @Test
        @DisplayName("Should combine filters")
        void shouldCombineFilters() throws IOException {
            String logContent = """
                    2026-02-01T10:30:00.123+00:00 ERROR --- [           main] c._glab.booking_system.Application       : Error day 1
                    2026-02-02T10:30:01.456+00:00 ERROR --- [           main] c._glab.booking_system.service.TestSvc   : Error day 2 with keyword
                    2026-02-02T10:30:02.789+00:00  INFO --- [           main] c._glab.booking_system.service.InfoSvc   : Info day 2 with keyword
                    2026-02-03T10:30:03.012+00:00 ERROR --- [           main] c._glab.booking_system.service.ErrorSvc  : Error day 3 with keyword
                    """;
            Files.writeString(logFile, logContent);

            // Filter by ERROR level, date Feb 2, and search "keyword"
            List<LogEntryResponse> result = logService.getLogEntries(
                    "ERROR",
                    LocalDate.of(2026, 2, 2),
                    LocalDate.of(2026, 2, 2),
                    "keyword",
                    0,
                    100
            );

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getMessage()).isEqualTo("Error day 2 with keyword");
        }

        @Test
        @DisplayName("Should return empty list for page beyond results")
        void shouldReturnEmptyListForPageBeyondResults() throws IOException {
            String logContent = """
                    2026-02-02T10:30:00.123+00:00  INFO --- [           main] c._glab.booking_system.Application       : Line 1
                    """;
            Files.writeString(logFile, logContent);

            List<LogEntryResponse> result = logService.getLogEntries(null, null, null, null, 10, 100);

            assertThat(result).isEmpty();
        }
    }
}
