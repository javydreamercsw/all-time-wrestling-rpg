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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

/**
 * Validates that all MySQL Flyway migrations apply without errors.
 *
 * <p>Two modes:
 *
 * <ol>
 *   <li><b>Fresh</b> (always runs) — starts from an empty schema; confirms every migration in
 *       {@code db/migration/mysql/} applies cleanly.
 *   <li><b>Prod dump</b> (opt-in) — restores a production SQL dump first, then applies any pending
 *       migrations on top. Pass {@code -Dprod.dump.file=/absolute/path/to/dump.sql} to enable.
 * </ol>
 *
 * <p>Run with: {@code mvn -Pintegration-test verify -Dtest=FlywayMigrationIT
 * [-Dprod.dump.file=...]}
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
class FlywayMigrationIT {

  static final String DB_NAME = "atwrpg";
  static final String DB_USER = "atwrpg";
  static final String DB_PASS = "test";

  @Container
  static final MySQLContainer<?> MYSQL =
      new MySQLContainer<>("mysql:8.0")
          .withDatabaseName(DB_NAME)
          .withUsername(DB_USER)
          .withPassword(DB_PASS);

  // -----------------------------------------------------------------------
  // Test 1: fresh schema — every migration must apply from scratch
  // -----------------------------------------------------------------------

  @Test
  @Order(1)
  void freshSchemaMigratesSuccessfully() {
    Flyway flyway = buildFlyway();

    MigrateResult result = flyway.migrate();

    assertThat(result.success).as("Flyway migration should succeed on empty schema").isTrue();
    assertThat(result.migrationsExecuted)
        .as("At least one migration should have been applied")
        .isGreaterThan(0);

    List<String> failed = failedMigrations(flyway);
    assertThat(failed).as("No migrations should be in FAILED state: %s", failed).isEmpty();

    log.info("Fresh migration OK: {} migrations applied", result.migrationsExecuted);
  }

  // -----------------------------------------------------------------------
  // Test 2: prod dump — skipped when -Dprod.dump.file is not set
  // -----------------------------------------------------------------------

  @Test
  @Order(2)
  void prodDumpMigratesSuccessfully() throws IOException, InterruptedException, SQLException {
    String dumpPath = System.getProperty("prod.dump.file");
    assumeTrue(
        dumpPath != null && !dumpPath.isBlank(),
        "Skipped: pass -Dprod.dump.file=/path/to/dump.sql to run this test");

    log.info("Loading production dump from: {}", dumpPath);
    loadDump(dumpPath);

    Flyway flyway = buildFlyway();

    int pending = pendingCount(flyway);
    log.info("Pending migrations after dump restore: {}", pending);

    MigrateResult result = flyway.migrate();

    assertThat(result.success).as("Flyway migration should succeed on top of prod dump").isTrue();

    List<String> failed = failedMigrations(flyway);
    assertThat(failed).as("No migrations should be in FAILED state: %s", failed).isEmpty();

    // Sanity-check that core tables exist and have rows
    assertCoreTablesPopulated();

    log.info(
        "Prod dump migration OK: {} new migrations applied over existing data",
        result.migrationsExecuted);
  }

  // -----------------------------------------------------------------------
  // Helpers
  // -----------------------------------------------------------------------

  private static Flyway buildFlyway() {
    return Flyway.configure()
        .dataSource(MYSQL.getJdbcUrl(), DB_USER, DB_PASS)
        .locations("filesystem:src/main/resources/db/migration/mysql")
        .cleanDisabled(true) // never wipe a DB we restored prod data into
        .baselineOnMigrate(false)
        .load();
  }

  private static void loadDump(String hostPath) throws IOException, InterruptedException {
    if (hostPath.startsWith("~")) {
      hostPath = System.getProperty("user.home") + hostPath.substring(1);
    }
    MYSQL.copyFileToContainer(MountableFile.forHostPath(hostPath), "/tmp/prod_dump.sql");

    var exec =
        MYSQL.execInContainer(
            "bash",
            "-c",
            String.format("mysql -u%s -p%s %s < /tmp/prod_dump.sql", DB_USER, DB_PASS, DB_NAME));

    if (exec.getExitCode() != 0) {
      throw new IllegalStateException(
          "Failed to load prod dump (exit " + exec.getExitCode() + "): " + exec.getStderr());
    }
  }

  private static List<String> failedMigrations(Flyway flyway) {
    List<String> failed = new ArrayList<>();
    for (MigrationInfo info : flyway.info().all()) {
      if (info.getState() == MigrationState.FAILED) {
        failed.add(info.getVersion() + " - " + info.getDescription());
      }
    }
    return failed;
  }

  private static int pendingCount(Flyway flyway) {
    return flyway.info().pending().length;
  }

  private void assertCoreTablesPopulated() throws SQLException {
    try (Connection conn = DriverManager.getConnection(MYSQL.getJdbcUrl(), DB_USER, DB_PASS);
        Statement stmt = conn.createStatement()) {
      for (String table : List.of("wrestler", "universe", "faction")) {
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + table);
        rs.next();
        int count = rs.getInt(1);
        log.info("Table '{}' row count after migration: {}", table, count);
        assertThat(count)
            .as("Table '%s' should have rows after migrating prod dump", table)
            .isGreaterThan(0);
      }
    }
  }
}
