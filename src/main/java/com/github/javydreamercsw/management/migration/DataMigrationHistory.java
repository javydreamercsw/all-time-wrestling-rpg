/*
* Copyright (C) 2026 Software Consulting Dreams LLC
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <www.gnu.org>.
*/
package com.github.javydreamercsw.management.migration;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/**
 * Auditable record of a {@link DataMigration} execution on this install — the Java-side analogue of
 * {@code flyway_schema_history}. One row per migration id, ever.
 *
 * <p>Deliberately global: data repairs are database-wide, so there is no {@code universe_id} column
 * and rows must never be resolved through a session-scoped universe context.
 */
@Entity
@Table(
    name = "data_migration_history",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_data_migration_history_migration_id",
            columnNames = "migration_id"))
@Getter
@Setter
public class DataMigrationHistory {

  /** Execution status recorded by {@link DataMigrationRunner}. */
  public static final String STATUS_SUCCESS = "SUCCESS";

  /** Execution status recorded by {@link DataMigrationRunner}. */
  public static final String STATUS_FAILED = "FAILED";

  /** Execution status recorded by {@link DataMigrationRunner}. */
  public static final String STATUS_LEGACY_SEEDED = "LEGACY_SEEDED";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  /** {@link DataMigration#id() migration id} — unique; presence in this table gates execution. */
  @Column(name = "migration_id", nullable = false)
  private String migrationId;

  /** App version that executed the migration (audit only; "unknown" when unavailable). */
  @Column(name = "app_version", nullable = false)
  private String appVersion;

  @Column(name = "executed_at", nullable = false)
  private Instant executedAt;

  @Column(name = "duration_ms", nullable = false)
  private long durationMs;

  /** One of the {@code STATUS_*} constants. */
  @Column(name = "status", nullable = false)
  private String status;
}
