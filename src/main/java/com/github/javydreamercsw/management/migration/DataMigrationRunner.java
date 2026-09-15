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

import com.github.javydreamercsw.management.service.GameSettingService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

/**
 * Executes every registered {@link DataMigration} exactly once per install, after all seed syncs.
 *
 * <p>Per-migration gate: a row in {@code data_migration_history} (any status) means the migration
 * already ran or was grandfathered and is skipped. A migration whose {@link
 * DataMigration#legacyGameSettingKey() legacy key} exists as a global game setting is recorded as
 * {@code LEGACY_SEEDED} without running — pre-existing installs already applied that repair ad-hoc.
 * Otherwise the migration runs once: success records {@code SUCCESS}; any thrown {@link Throwable}
 * is logged, recorded as {@code FAILED}, and never propagates — one failing migration must not
 * block the remaining migrations or application boot.
 */
@Slf4j
@Component
public class DataMigrationRunner {

  static final String UNKNOWN_VERSION = "unknown";

  private final List<DataMigration> migrations;
  private final DataMigrationHistoryRepository historyRepository;
  private final GameSettingService gameSettingService;
  private final Optional<BuildProperties> buildProperties;

  public DataMigrationRunner(
      final List<DataMigration> migrations,
      final DataMigrationHistoryRepository historyRepository,
      final GameSettingService gameSettingService,
      final Optional<BuildProperties> buildProperties) {
    this.migrations = List.copyOf(migrations);
    this.historyRepository = historyRepository;
    this.gameSettingService = gameSettingService;
    this.buildProperties = buildProperties;
    validateUniqueIds(this.migrations);
  }

  public void runMigrations() {
    List<DataMigration> ordered = new ArrayList<>(migrations);
    AnnotationAwareOrderComparator.sort(ordered);
    String appVersion = resolveAppVersion();
    log.info("Running data migrations ({} registered, app version {})", ordered.size(), appVersion);

    for (DataMigration migration : ordered) {
      if (historyRepository.findByMigrationId(migration.id()).isPresent()) {
        log.debug("Data migration '{}' already recorded — skipping.", migration.id());
        continue;
      }

      Optional<String> legacyKey = migration.legacyGameSettingKey();
      if (legacyKey.isPresent()
          && gameSettingService.findByKeyForUniverse(legacyKey.get(), null).isPresent()) {
        log.info(
            "Data migration '{}' already applied via legacy game_setting key '{}' — seeding"
                + " history.",
            migration.id(),
            legacyKey.get());
        record(migration.id(), appVersion, 0L, DataMigrationHistory.STATUS_LEGACY_SEEDED);
        continue;
      }

      long start = System.currentTimeMillis();
      try {
        log.info("Executing data migration '{}'.", migration.id());
        migration.migrate();
        long durationMs = System.currentTimeMillis() - start;
        record(migration.id(), appVersion, durationMs, DataMigrationHistory.STATUS_SUCCESS);
        log.info("Data migration '{}' succeeded in {} ms.", migration.id(), durationMs);
      } catch (Throwable t) {
        long durationMs = System.currentTimeMillis() - start;
        log.error("Data migration '{}' failed after {} ms.", migration.id(), durationMs, t);
        record(migration.id(), appVersion, durationMs, DataMigrationHistory.STATUS_FAILED);
      }
    }
    log.info("Data migrations complete.");
  }

  private void record(
      final String migrationId,
      final String appVersion,
      final long durationMs,
      final String status) {
    DataMigrationHistory history = new DataMigrationHistory();
    history.setMigrationId(migrationId);
    history.setAppVersion(appVersion);
    history.setExecutedAt(Instant.now());
    history.setDurationMs(durationMs);
    history.setStatus(status);
    historyRepository.save(history);
  }

  private String resolveAppVersion() {
    return buildProperties.map(BuildProperties::getVersion).orElse(UNKNOWN_VERSION);
  }

  private static void validateUniqueIds(final List<DataMigration> migrations) {
    List<String> ids = migrations.stream().map(DataMigration::id).toList();
    List<String> duplicates =
        ids.stream().distinct().filter(id -> ids.stream().filter(id::equals).count() > 1).toList();
    if (!duplicates.isEmpty()) {
      throw new IllegalStateException(
          "Duplicate DataMigration ids: " + duplicates + ". Migration ids must be unique.");
    }
  }
}
