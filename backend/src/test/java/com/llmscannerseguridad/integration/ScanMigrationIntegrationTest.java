package com.llmscannerseguridad.integration;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnabledIfEnvironmentVariable(named = "SCAN_TEST_DB_URL", matches = "jdbc:postgresql:.+")
class ScanMigrationIntegrationTest {
    private final Instant now = Instant.parse("2026-10-10T08:00:00.123456Z");

    @Test
    void v1AndV2UpgradeWithoutChangingCompatibleHistoryScoresForeignKeysOrIndexes() throws Exception {
        try (var database = new ScanTestDatabase()) {
            database.migrate("2");
            for (String status : List.of("PENDING", "RUNNING", "COMPLETED")) {
                UUID project = project(database);
                database.jdbc().update("""
                        INSERT INTO scans(id, project_id, status, created_at, started_at, finished_at, global_score)
                        VALUES (?, ?, ?, ?, ?, ?, 52.75)
                        """, UUID.randomUUID(), project, status, Timestamp.from(now),
                        status.equals("PENDING") ? null : Timestamp.from(now),
                        status.equals("COMPLETED") ? Timestamp.from(now) : null);
            }
            var before = database.jdbc().queryForList("SELECT id, project_id, status, created_at, started_at, finished_at, global_score FROM scans ORDER BY id");
            database.migrate(null);
            assertThat(database.jdbc().queryForList("SELECT id, project_id, status, created_at, started_at, finished_at, global_score FROM scans ORDER BY id"))
                    .isEqualTo(before);
            assertThat(database.jdbc().queryForObject("SELECT count(*) FROM scans WHERE error_reason IS NOT NULL", Long.class)).isZero();
            assertThat(database.jdbc().queryForList("SELECT global_score FROM scans", BigDecimal.class))
                    .allSatisfy(score -> assertThat(score).isEqualByComparingTo("52.75"));
            assertThat(database.jdbc().queryForObject("""
                    SELECT count(*) FROM information_schema.columns
                    WHERE table_schema = ? AND table_name = 'scans'
                        AND column_name IN ('created_at', 'started_at', 'finished_at') AND datetime_precision = 6
                    """, Long.class, database.schema)).isEqualTo(3L);
            assertThat(database.jdbc().queryForObject("""
                    SELECT count(*) FROM information_schema.table_constraints
                    WHERE table_schema = ? AND table_name = 'scans' AND constraint_type = 'FOREIGN KEY'
                    """, Long.class, database.schema)).isEqualTo(1L);
            String index = database.jdbc().queryForObject("""
                    SELECT indexdef FROM pg_indexes WHERE schemaname = ? AND indexname = 'uq_scans_active_project'
                    """, String.class, database.schema);
            assertThat(index).contains("UNIQUE INDEX", "project_id", "WHERE", "PENDING", "RUNNING");
            assertThat(database.jdbc().queryForObject("""
                    SELECT count(*) FROM pg_indexes WHERE schemaname = ? AND indexname = 'idx_scans_project_id'
                    """, Long.class, database.schema)).isEqualTo(1L);
        }
    }

    @Test
    void incompatibleLegacyStatusDatesFailedReasonAndDuplicateActivesRejectWholeV3WithoutNormalizing() throws Exception {
        for (String incompatible : List.of("UNKNOWN", "MISSING_START", "REVERSED_DATES", "FAILED_WITHOUT_REASON", "DUPLICATE_ACTIVE")) {
            try (var database = new ScanTestDatabase()) {
                database.migrate("2");
                UUID project = project(database);
                String status = switch (incompatible) {
                    case "UNKNOWN" -> "UNKNOWN";
                    case "FAILED_WITHOUT_REASON" -> "FAILED";
                    case "DUPLICATE_ACTIVE" -> "PENDING";
                    default -> "COMPLETED";
                };
                database.jdbc().update("""
                        INSERT INTO scans(id, project_id, status, created_at, started_at, finished_at, global_score)
                        VALUES (?, ?, ?, ?, ?, ?, 91.50)
                        """, UUID.randomUUID(), project, status, Timestamp.from(now),
                        incompatible.equals("REVERSED_DATES") ? Timestamp.from(now.plusSeconds(1)) : null,
                        status.equals("COMPLETED") || status.equals("FAILED") ? Timestamp.from(now) : null);
                if (incompatible.equals("DUPLICATE_ACTIVE")) {
                    database.jdbc().update("INSERT INTO scans(id, project_id, status, created_at) VALUES (?, ?, 'RUNNING', ?)",
                            UUID.randomUUID(), project, Timestamp.from(now));
                    // Las fechas son compatibles para que el fallo sea específicamente el índice parcial.
                    database.jdbc().update("UPDATE scans SET started_at = created_at WHERE status = 'RUNNING'");
                }
                var before = database.jdbc().queryForList("SELECT * FROM scans ORDER BY id");
                assertThatThrownBy(() -> database.migrate(null)).isInstanceOf(FlywayException.class);
                assertThat(database.jdbc().queryForList("SELECT * FROM scans ORDER BY id")).isEqualTo(before);
                assertThat(database.jdbc().queryForObject("""
                        SELECT count(*) FROM information_schema.columns WHERE table_schema = ?
                        AND table_name = 'scans' AND column_name = 'error_reason'
                        """, Long.class, database.schema)).isZero();
                assertThat(database.jdbc().queryForObject("SELECT count(*) FROM flyway_schema_history WHERE version = '3' AND success", Long.class)).isZero();
            }
        }
    }

    private UUID project(ScanTestDatabase database) {
        UUID id = UUID.randomUUID();
        database.jdbc().update("INSERT INTO projects(id, name, created_at, updated_at) VALUES (?, 'Migración Scan', ?, ?)",
                id, Timestamp.from(now), Timestamp.from(now));
        return id;
    }
}
