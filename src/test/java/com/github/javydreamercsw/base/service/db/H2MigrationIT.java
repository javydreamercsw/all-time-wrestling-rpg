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
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Comparator;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;

/**
 * Verifies that the committed H2 reference snapshot ({@code h2-snapshot-v*.sql}) can be upgraded to
 * the current migration tip without errors.
 *
 * <p>This test simulates the upgrade path experienced by portable/desktop installer users who run
 * an H2 file database. It:
 *
 * <ol>
 *   <li>Loads the committed snapshot (pre-populated with seed data up to {@code .released})
 *   <li>Applies all migrations above {@code .released} (the "new" migrations)
 *   <li>Asserts that migration succeeded and key tables are still accessible
 * </ol>
 *
 * <p>Run via the integration-test profile:
 *
 * <pre>
 *   mvn -Pintegration-test verify -Dtest=H2MigrationIT
 * </pre>
 *
 * <p>If this test fails after adding a new migration, ensure:
 *
 * <ul>
 *   <li>The migration is idempotent and handles existing data (NOT NULL columns get defaults, etc.)
 *   <li>The snapshot is current — regenerate via {@link GenerateH2SnapshotIT} if {@code .released}
 *       was bumped.
 * </ul>
 */
@Slf4j
class H2MigrationIT {

  private static final Path MIGRATION_DIR =
      Paths.get("src/main/resources/db/migration/h2").toAbsolutePath();

  @Test
  void h2_snapshot_can_be_upgraded_to_current_migration_tip() throws Exception {
    String released = Files.readString(MIGRATION_DIR.resolve(".released")).strip();
    String releasedVersion = released.replaceAll("[^0-9]", "");
    String snapshotName = "h2-snapshot-v" + releasedVersion.toLowerCase() + ".sql";

    URL snapshotUrl = H2MigrationIT.class.getResource("/db/" + snapshotName);
    assertThat(snapshotUrl)
        .as(
            "Snapshot %s not found in test resources. Regenerate via GenerateH2SnapshotIT with"
                + " -Dgenerate.h2.snapshot=true",
            snapshotName)
        .isNotNull();

    Path workDir = Paths.get("target/h2-migration-test-tmp").toAbsolutePath();
    deleteDirIfExists(workDir);
    Files.createDirectories(workDir);
    Path dbFile = workDir.resolve("h2-migrated");
    String jdbcUrl = "jdbc:h2:file:" + dbFile + ";DB_CLOSE_DELAY=-1";

    // Step 1: Load snapshot into fresh H2 file DB
    log.info("Loading snapshot {} into H2 file DB...", snapshotName);
    try (Connection conn = DriverManager.getConnection(jdbcUrl, "sa", "");
        Statement stmt = conn.createStatement()) {
      String snapshotSql = snapshotUrl.toURI().getPath();
      stmt.execute("RUNSCRIPT FROM '" + snapshotSql.replace("\\", "/") + "'");
    }
    log.info("Snapshot loaded.");

    // Step 2: Apply all migrations above .released (baseline at .released so Flyway
    // skips already-applied scripts and only runs the new ones)
    log.info("Applying new migrations above {} ...", released);
    Flyway flyway =
        Flyway.configure()
            .dataSource(jdbcUrl, "sa", "")
            .locations("filesystem:" + MIGRATION_DIR)
            .baselineVersion(releasedVersion)
            .baselineOnMigrate(true)
            .validateOnMigrate(false)
            .load();

    MigrateResult result = flyway.migrate();
    assertThat(result.success)
        .as("Migration from %s to current tip must succeed", released)
        .isTrue();
    log.info("Applied {} new migrations", result.migrationsExecuted);

    // Step 3: Verify key tables are accessible and seed data survived
    try (Connection conn = DriverManager.getConnection(jdbcUrl, "sa", "");
        Statement stmt = conn.createStatement()) {
      assertTableHasRows(stmt, "wrestler", "SELECT COUNT(*) FROM wrestler");
      assertTableHasRows(stmt, "faction", "SELECT COUNT(*) FROM faction");
      assertTableHasRows(stmt, "injury_type", "SELECT COUNT(*) FROM injury_type");
      assertTableHasRows(stmt, "show_type", "SELECT COUNT(*) FROM show_type");
      assertTableHasRows(stmt, "segment_type", "SELECT COUNT(*) FROM segment_type");
    }
    log.info("Post-migration verification passed.");
  }

  private static void assertTableHasRows(Statement stmt, String tableName, String countSql)
      throws Exception {
    ResultSet rs = stmt.executeQuery(countSql);
    rs.next();
    int count = rs.getInt(1);
    assertThat(count)
        .as("Table %s should have at least 1 row after migration", tableName)
        .isGreaterThan(0);
  }

  private static void deleteDirIfExists(Path dir) throws IOException {
    if (!Files.exists(dir)) return;
    try (var walk = Files.walk(dir)) {
      walk.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
    }
  }
}
