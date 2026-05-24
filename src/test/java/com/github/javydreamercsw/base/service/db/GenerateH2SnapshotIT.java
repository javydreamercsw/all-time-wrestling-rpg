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
package com.github.javydreamercsw.base.service.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Comparator;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Generates the committed H2 reference snapshot used by {@link H2MigrationIT}.
 *
 * <p>This test is intentionally opt-in — it only runs when {@code -Dgenerate.h2.snapshot=true} is
 * passed. It applies all H2 migrations up to the version in {@code .released}, inserts
 * representative seed data that exercises the most failure-prone migration patterns (NOT NULL
 * columns, FK constraints, UNIQUE indexes on populated tables), then exports a portable SQL script
 * to {@code src/test/resources/db/h2-snapshot-<released>.sql}.
 *
 * <p>Run manually before cutting a release, or via the release workflow step "Generate H2 reference
 * snapshot":
 *
 * <pre>
 *   mvn -Pintegration-test verify -Dgenerate.h2.snapshot=true -Dtest=GenerateH2SnapshotIT
 * </pre>
 *
 * <p>Commit the resulting SQL file alongside the updated {@code .released} and {@code .checksums}.
 */
@Slf4j
@EnabledIfSystemProperty(named = "generate.h2.snapshot", matches = "true")
class GenerateH2SnapshotIT {

  private static final Path MIGRATION_DIR =
      Paths.get("src/main/resources/db/migration/h2").toAbsolutePath();
  private static final Path OUTPUT_DIR = Paths.get("src/test/resources/db").toAbsolutePath();

  @Test
  void generate_h2_reference_snapshot() throws Exception {
    String released = Files.readString(MIGRATION_DIR.resolve(".released")).strip();
    // .released stores e.g. "V74"; Flyway target() expects numeric version "74"
    String releasedVersion = released.replaceAll("[^0-9]", "");
    int releasedNum = Integer.parseInt(releasedVersion);
    Path tempDir = Paths.get("target/h2-snapshot-tmp").toAbsolutePath();
    deleteDirIfExists(tempDir);
    Files.createDirectories(tempDir);
    Path dbFile = tempDir.resolve("h2-snapshot");
    String jdbcUrl = "jdbc:h2:file:" + dbFile + ";DB_CLOSE_DELAY=-1";

    // Step 1: Apply migrations up to .released version
    log.info("Applying H2 migrations up to {} ...", released);
    Flyway flyway =
        Flyway.configure()
            .dataSource(jdbcUrl, "sa", "")
            .locations("filesystem:" + MIGRATION_DIR)
            .target(releasedVersion)
            .baselineOnMigrate(false)
            .validateOnMigrate(true)
            .load();

    MigrateResult result = flyway.migrate();
    assertThat(result.success).as("Flyway migration to %s must succeed", released).isTrue();
    log.info("Applied {} migrations", result.migrationsExecuted);

    // Step 2: Insert representative seed data
    log.info("Inserting seed data...");
    try (Connection conn = DriverManager.getConnection(jdbcUrl, "sa", "");
        Statement stmt = conn.createStatement()) {
      insertSeedData(stmt, releasedNum);
    }

    // Step 3: Export to portable SQL script
    String snapshotName = "h2-snapshot-v" + releasedVersion.toLowerCase() + ".sql";
    Path outputFile = OUTPUT_DIR.resolve(snapshotName);
    Files.createDirectories(OUTPUT_DIR);

    try (Connection conn = DriverManager.getConnection(jdbcUrl, "sa", "");
        Statement stmt = conn.createStatement()) {
      // H2 SCRIPT TO exports full DDL + data as a portable SQL file
      stmt.execute("SCRIPT TO '" + outputFile.toString().replace("\\", "/") + "'");
    }

    assertThat(outputFile).exists().isNotEmptyFile();
    long bytes = Files.size(outputFile);
    log.info("Snapshot written to {} ({} bytes)", outputFile, bytes);
    log.info("Commit this file alongside .released and .checksums before cutting the release.");
  }

  /**
   * Inserts representative rows that stress-test the migration patterns most likely to fail on
   * populated data: NOT NULL columns added to existing rows, FK constraints, UNIQUE indexes.
   */
  private static void insertSeedData(Statement stmt, int releasedNum) throws Exception {
    // Core entities present from V1
    stmt.execute(
        """
        INSERT INTO faction (name, description, is_active, creation_date)
        VALUES ('Test Faction', 'Reference snapshot faction', TRUE, CURRENT_TIMESTAMP)
        """);

    if (releasedNum >= 82) {
      // V82 dropped fans/tier/bumps/current_health from wrestler; those now live in wrestler_state
      stmt.execute(
          """
          INSERT INTO wrestler (name, starting_stamina, low_stamina, starting_health, low_health,
            deck_size, creation_date, is_player, gender)
          VALUES ('Reference Wrestler', 12, 2, 12, 2, 12, CURRENT_TIMESTAMP, FALSE, 'MALE')
          """);
      stmt.execute(
          """
          INSERT INTO wrestler (name, starting_stamina, low_stamina, starting_health, low_health,
            deck_size, creation_date, is_player, gender)
          VALUES ('Reference Wrestler 2', 14, 3, 14, 3, 14, CURRENT_TIMESTAMP, FALSE, 'FEMALE')
          """);
      // universe row 1 exists after V81; insert state for each seed wrestler
      stmt.execute(
          """
          INSERT INTO wrestler_state (wrestler_id, universe_id, fans, tier, bumps, current_health)
          SELECT wrestler_id, 1, 1000, 'ROOKIE', 0, 12 FROM wrestler
          WHERE name = 'Reference Wrestler'
          """);
      stmt.execute(
          """
          INSERT INTO wrestler_state (wrestler_id, universe_id, fans, tier, bumps, current_health)
          SELECT wrestler_id, 1, 500, 'VETERAN', 2, 14 FROM wrestler
          WHERE name = 'Reference Wrestler 2'
          """);
    } else {
      stmt.execute(
          """
          INSERT INTO wrestler (name, starting_stamina, low_stamina, starting_health, low_health,
            deck_size, creation_date, fans, tier, bumps, current_health, is_player, gender)
          VALUES ('Reference Wrestler', 12, 2, 12, 2, 12, CURRENT_TIMESTAMP, 1000,
            'ROOKIE', 0, 12, FALSE, 'MALE')
          """);
      stmt.execute(
          """
          INSERT INTO wrestler (name, starting_stamina, low_stamina, starting_health, low_health,
            deck_size, creation_date, fans, tier, bumps, current_health, is_player, gender)
          VALUES ('Reference Wrestler 2', 14, 3, 14, 3, 14, CURRENT_TIMESTAMP, 500,
            'VETERAN', 2, 14, FALSE, 'FEMALE')
          """);
    }

    // injury_type — needed for injuries
    stmt.execute(
        """
        INSERT INTO injury_type (injury_name, health_effect, stamina_effect, card_effect)
        VALUES ('Sprain', -2, -1, 0)
        """);

    // title — present from V1
    stmt.execute(
        """
        INSERT INTO title (name, creation_date)
        VALUES ('Reference Championship', CURRENT_TIMESTAMP)
        """);

    // show_type and show_template — present from early migrations
    stmt.execute(
        """
        INSERT INTO show_type (name, creation_date)
        VALUES ('Weekly Show', CURRENT_TIMESTAMP)
        """);

    stmt.execute(
        """
        INSERT INTO show_template (name, creation_date, show_type_id)
        SELECT 'Reference Template', CURRENT_TIMESTAMP, show_type_id FROM show_type
        WHERE name = 'Weekly Show'
        """);

    // V35 renamed "show" to "wrestling_show"
    stmt.execute(
        """
        INSERT INTO wrestling_show (name, creation_date, show_type_id)
        SELECT 'Reference Show', CURRENT_TIMESTAMP, show_type_id FROM show_type
        WHERE name = 'Weekly Show'
        """);

    // drama_event has many NOT NULL columns; event_type/severity must match enum values
    stmt.execute(
        """
        INSERT INTO drama_event
          (title, description, event_type, severity, event_date, creation_date,
           injury_caused, rivalry_created, rivalry_ended, is_processed)
        VALUES ('Reference Event', 'Test drama event', 'BACKSTAGE_INCIDENT', 'NEUTRAL',
          CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE, FALSE, FALSE, FALSE)
        """);

    // campaign references wrestler_id; use the first seed wrestler
    if (releasedNum >= 37) {
      stmt.execute(
          """
          INSERT INTO campaign (wrestler_id, status, started_at)
          SELECT wrestler_id, 'ACTIVE', CURRENT_TIMESTAMP FROM wrestler
          WHERE name = 'Reference Wrestler'
          """);
    }

    // segment_type — present since V1; verify team_number column exists (added in V74)
    stmt.execute(
        """
        INSERT INTO segment_type (name, creation_date)
        SELECT 'Match', CURRENT_TIMESTAMP
        WHERE NOT EXISTS (SELECT 1 FROM segment_type WHERE name = 'Match')
        """);

    log.info("Seed data inserted.");
  }

  private static void deleteDirIfExists(Path dir) throws IOException {
    if (!Files.exists(dir)) {
      return;
    }
    try (var walk = Files.walk(dir)) {
      walk.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
    }
  }
}
