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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Generates the committed MySQL reference snapshot used by {@link FlywayMigrationIT}.
 *
 * <p>This test is intentionally opt-in — it only runs when {@code -Dgenerate.mysql.snapshot=true}
 * is passed. It applies all MySQL migrations up to the version in {@code .released}, inserts
 * representative seed data that exercises the most failure-prone migration patterns (NOT NULL
 * columns, FK constraints, UNIQUE indexes on populated tables), then exports a portable SQL script
 * to {@code src/test/resources/db/mysql-snapshot-v&lt;released&gt;.sql} using {@code mysqldump}.
 *
 * <p>The dump includes the {@code flyway_schema_history} table so that {@link
 * FlywayMigrationIT#prodDumpMigratesSuccessfully} can identify which migrations are already applied
 * and only run the newer ones on top.
 *
 * <p>Run manually before cutting a release, or via the release workflow step "Generate MySQL
 * reference snapshot":
 *
 * <pre>
 *   mvn -Pintegration-test verify \
 *     -Dgenerate.mysql.snapshot=true \
 *     -Dit.test=GenerateMysqlSnapshotIT \
 *     -Dsurefire.skip=true \
 *     -DfailIfNoTests=false \
 *     -Dspotless.check.skip=true
 * </pre>
 *
 * <p>Commit the resulting SQL file alongside the updated {@code .released} and {@code .checksums}.
 */
@Testcontainers
@Slf4j
@EnabledIfSystemProperty(named = "generate.mysql.snapshot", matches = "true")
class GenerateMysqlSnapshotIT {

  static final String DB_NAME = "atwrpg";
  static final String DB_USER = "atwrpg";
  static final String DB_PASS = "test";

  @Container
  static final MySQLContainer<?> MYSQL =
      new MySQLContainer<>("mysql:8.0")
          .withDatabaseName(DB_NAME)
          .withUsername(DB_USER)
          .withPassword(DB_PASS);

  private static final Path MYSQL_MIGRATION_DIR =
      Paths.get("src/main/resources/db/migration/mysql").toAbsolutePath();
  private static final Path OUTPUT_DIR = Paths.get("src/test/resources/db").toAbsolutePath();

  @Test
  void generate_mysql_reference_snapshot() throws Exception {
    String released = Files.readString(MYSQL_MIGRATION_DIR.resolve(".released")).strip();
    // .released stores e.g. "V53"; Flyway target() expects numeric version "53"
    String releasedVersion = released.replaceAll("[^0-9]", "");
    int releasedNum = Integer.parseInt(releasedVersion);

    // Step 1: Apply all MySQL migrations up to .released
    log.info("Applying MySQL migrations up to {} ...", released);
    Flyway flyway =
        Flyway.configure()
            .dataSource(MYSQL.getJdbcUrl(), DB_USER, DB_PASS)
            .locations("filesystem:" + MYSQL_MIGRATION_DIR)
            .target(releasedVersion)
            .cleanDisabled(true)
            .baselineOnMigrate(false)
            .load();

    MigrateResult result = flyway.migrate();
    assertThat(result.success).as("Flyway migration to %s must succeed", released).isTrue();
    log.info("Applied {} migrations", result.migrationsExecuted);

    // Step 2: Insert representative seed data
    log.info("Inserting seed data for V{} schema ...", releasedNum);
    try (Connection conn = DriverManager.getConnection(MYSQL.getJdbcUrl(), DB_USER, DB_PASS);
        Statement stmt = conn.createStatement()) {
      insertSeedData(stmt, releasedNum);
    }

    // Step 3: Export via mysqldump — includes flyway_schema_history so FlywayMigrationIT
    // knows which migrations have already been applied when it loads the dump.
    String snapshotName = "mysql-snapshot-v" + releasedVersion + ".sql";
    Path outputFile = OUTPUT_DIR.resolve(snapshotName);
    Files.createDirectories(OUTPUT_DIR);

    log.info("Running mysqldump ...");
    // Note: passing -p<pass> directly produces a harmless warning on stderr; we ignore it.
    var dumpResult =
        MYSQL.execInContainer(
            "mysqldump",
            "--no-tablespaces",
            "--single-transaction",
            "--routines",
            "-u" + DB_USER,
            "-p" + DB_PASS,
            DB_NAME);

    if (dumpResult.getExitCode() != 0) {
      throw new IllegalStateException(
          "mysqldump failed (exit "
              + dumpResult.getExitCode()
              + "): "
              + dumpResult.getStderr());
    }
    assertThat(dumpResult.getStdout())
        .as("mysqldump output must not be blank")
        .isNotBlank();

    Files.writeString(outputFile, dumpResult.getStdout(), StandardCharsets.UTF_8);

    long bytes = Files.size(outputFile);
    assertThat(outputFile).exists().isNotEmptyFile();
    log.info("MySQL snapshot written to {} ({} bytes)", outputFile, bytes);
    log.info("Commit this file alongside .released and .checksums before cutting the release.");
  }

  /**
   * Inserts representative rows that stress-test the migration patterns most likely to fail on
   * populated data: NOT NULL columns, FK constraints, UNIQUE indexes.
   *
   * <p>At V51+, {@code wrestler} no longer carries fans/tier/bumps/current_health — those live in
   * {@code wrestler_state}. The {@code universe} table was created in V51 with a default row
   * (id=1, 'Default Universe'), so it is already present when this method runs.
   */
  private static void insertSeedData(Statement stmt, int releasedNum) throws Exception {
    // wrestler — core entity; schema at V52+ has no fans/tier/bumps columns
    stmt.execute(
        """
        INSERT INTO wrestler
            (name, starting_stamina, low_stamina, starting_health, low_health,
             deck_size, creation_date, is_player, gender)
        VALUES ('Reference Wrestler', 12, 2, 12, 2, 12, NOW(), FALSE, 'MALE')
        """);
    stmt.execute(
        """
        INSERT INTO wrestler
            (name, starting_stamina, low_stamina, starting_health, low_health,
             deck_size, creation_date, is_player, gender)
        VALUES ('Reference Wrestler 2', 14, 3, 14, 3, 14, NOW(), FALSE, 'FEMALE')
        """);

    // wrestler_state — universe id=1 is pre-inserted by the V51 migration
    stmt.execute(
        """
        INSERT INTO wrestler_state
            (wrestler_id, universe_id, fans, tier, bumps, current_health)
        SELECT wrestler_id, 1, 1000, 'ROOKIE', 0, 12
        FROM wrestler WHERE name = 'Reference Wrestler'
        """);
    stmt.execute(
        """
        INSERT INTO wrestler_state
            (wrestler_id, universe_id, fans, tier, bumps, current_health)
        SELECT wrestler_id, 1, 500, 'VETERAN', 2, 14
        FROM wrestler WHERE name = 'Reference Wrestler 2'
        """);

    // faction — universe_id added in V51
    stmt.execute(
        """
        INSERT INTO faction (name, description, is_active, creation_date, universe_id)
        VALUES ('Reference Faction', 'Snapshot seed faction', TRUE, NOW(), 1)
        """);

    // injury_type — needed for injury FK constraints
    stmt.execute(
        """
        INSERT INTO injury_type (injury_name, health_effect, stamina_effect, card_effect)
        VALUES ('Sprain', -2, -1, 0)
        """);

    // title — universe_id added in V51
    stmt.execute(
        """
        INSERT INTO title (name, creation_date, universe_id)
        VALUES ('Reference Championship', NOW(), 1)
        """);

    // show_type → show_template → wrestling_show chain
    stmt.execute(
        """
        INSERT INTO show_type (name, creation_date)
        VALUES ('Weekly Show', NOW())
        """);
    stmt.execute(
        """
        INSERT INTO show_template (name, creation_date, show_type_id)
        SELECT 'Reference Template', NOW(), show_type_id
        FROM show_type WHERE name = 'Weekly Show'
        """);
    // wrestling_show — universe_id added in V51
    stmt.execute(
        """
        INSERT INTO wrestling_show (name, creation_date, show_type_id, universe_id)
        SELECT 'Reference Show', NOW(), show_type_id, 1
        FROM show_type WHERE name = 'Weekly Show'
        """);

    // drama_event — many NOT NULL columns; universe_id added in V51
    stmt.execute(
        """
        INSERT INTO drama_event
            (title, description, event_type, severity, event_date, creation_date,
             injury_caused, rivalry_created, rivalry_ended, is_processed, universe_id)
        VALUES ('Reference Event', 'Test drama event', 'BACKSTAGE_INCIDENT', 'NEUTRAL',
                NOW(), NOW(), FALSE, FALSE, FALSE, FALSE, 1)
        """);

    // campaign — universe_id added in V51
    stmt.execute(
        """
        INSERT INTO campaign (wrestler_id, status, started_at, universe_id)
        SELECT wrestler_id, 'ACTIVE', NOW(), 1
        FROM wrestler WHERE name = 'Reference Wrestler'
        """);

    // segment_type — needed for show/match segments
    stmt.execute(
        """
        INSERT IGNORE INTO segment_type (name, creation_date)
        VALUES ('Match', NOW())
        """);

    log.info("Seed data inserted (released V{}).", releasedNum);
  }
}
