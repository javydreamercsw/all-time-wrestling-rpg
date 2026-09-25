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

import java.util.Optional;

/**
 * One-time, run-once-per-install data repair that cannot be expressed in SQL.
 *
 * <p>Contract: a {@code DataMigration} runs at most once per install — {@link DataMigrationRunner}
 * executes every registered implementation exactly once, records the execution in {@code
 * data_migration_history}, and never re-runs it on a later boot. Use this for repairs that need
 * Java services (e.g. rescoring historical rows with domain logic); use Flyway for schema changes
 * and {@link com.github.javydreamercsw.management.sync.DataSyncContributor} for idempotent
 * seed/sync work that runs on every startup.
 *
 * <p>Migrations run after all seed syncs, so they see fully seeded data.
 */
public interface DataMigration {

  /**
   * Stable, kebab-case identifier recorded in {@code data_migration_history}. Ids gate execution
   * (id present in history = already ran), so an id must never change or be reused once shipped.
   */
  String id();

  /** Performs the repair. Invoked at most once per install, after all seed syncs. */
  void migrate();

  /**
   * Global {@code game_setting} key that marks this repair as already applied by earlier ad-hoc
   * means. When that setting exists, the runner records a {@code LEGACY_SEEDED} history row instead
   * of running the migration, grandfathering pre-existing installs. Absent key = normal one-time
   * execution.
   */
  default Optional<String> legacyGameSettingKey() {
    return Optional.empty();
  }
}
