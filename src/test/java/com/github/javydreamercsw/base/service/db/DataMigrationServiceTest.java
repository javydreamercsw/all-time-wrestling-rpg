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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
@Slf4j
class DataMigrationServiceTest {

  @Container
  private static final MySQLContainer MYSQL_CONTAINER =
      new MySQLContainer("mysql:8.0.26")
          .withDatabaseName("test")
          .withUsername("test")
          .withPassword("test");

  private static final String H2_URL =
      "jdbc:h2:./target/db/sample_test_migration_" + System.currentTimeMillis();
  private static final String H2_PROTECTED_URL =
      "jdbc:h2:./target/db/sample_protected_migration_" + System.currentTimeMillis();
  private static final String H2_USER = "sa";
  private static final String H2_PASSWORD = "";

  @BeforeAll
  @SneakyThrows
  public static void setDatabases() {
    Path targetDir = Paths.get("target/db");
    if (!Files.exists(targetDir)) {
      Files.createDirectories(targetDir);
    }

    // Configure and run Flyway for H2 database
    Flyway h2Flyway =
        Flyway.configure()
            .dataSource(H2_URL, H2_USER, H2_PASSWORD)
            .locations("filesystem:src/main/resources/db/migration/h2")
            .cleanDisabled(false)
            .load();
    h2Flyway.migrate(); // Migrate H2 schema

    // Add sample data to H2 source
    try (Connection conn = DriverManager.getConnection(H2_URL, H2_USER, H2_PASSWORD);
        Statement stmt = conn.createStatement()) {
      stmt.execute(
          "MERGE INTO universe (id, name, type, creation_date) KEY(id) VALUES (10, 'Migration"
              + " Test Universe', 'GLOBAL', CURRENT_TIMESTAMP())");
      stmt.execute(
          "MERGE INTO injury_type (INJURY_TYPE_ID, INJURY_NAME, HEALTH_EFFECT, special_effects)"
              + " KEY(INJURY_TYPE_ID) VALUES (10, 'Migration Knee', 2, 'Migration Description')");
      stmt.execute(
          "MERGE INTO faction (faction_id, name, universe_id, creation_date) KEY(faction_id)"
              + " VALUES (10, 'Migration Test Faction', 10, CURRENT_TIMESTAMP())");
      stmt.execute(
          "MERGE INTO npc (id, name, npc_type, external_id) KEY(id) VALUES (10,"
              + " 'Migration Manager', 'MANAGER', 'ext-npc-migration-1')");

      stmt.execute(
          "MERGE INTO wrestler (wrestler_id, NAME, STARTING_STAMINA, LOW_STAMINA, STARTING_HEALTH,"
              + " LOW_HEALTH, DECK_SIZE, CREATION_DATE, EXTERNAL_ID, IS_PLAYER, GENDER, ACTIVE,"
              + " UPDATED_AT) KEY(wrestler_id) VALUES (10, 'Migration Test Wrestler', 15, 2, 15, 4,"
              + " 15, CURRENT_TIMESTAMP(), 'ext-migration-1', false, 'MALE', true,"
              + " CURRENT_TIMESTAMP())");

      stmt.execute(
          "MERGE INTO wrestler_state (id, wrestler_id, universe_id, fans, tier, bumps,"
              + " current_health, physical_condition, morale, management_stamina, faction_id,"
              + " manager_id, updated_at) KEY(id) VALUES (10, 10, 10, 1000, 'ROOKIE', 0, 15, 100,"
              + " 100, 100, 10, 10, CURRENT_TIMESTAMP())");

      stmt.execute(
          "MERGE INTO injury (injury_id, wrestler_id, universe_id, name, description, severity,"
              + " health_penalty, healing_cost, is_active, injury_date, creation_date, updated_at)"
              + " KEY(injury_id) VALUES (10, 10, 10, 'Migration Torn ACL', 'Migration Torn ACL',"
              + " 'SEVERE', 2, 100, true, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(),"
              + " CURRENT_TIMESTAMP())");
    }

    // Configure and run Flyway for H2 protected database
    Flyway h2ProtectedFlyway =
        Flyway.configure()
            .dataSource(H2_PROTECTED_URL, H2_USER, H2_PASSWORD)
            .locations("filesystem:src/main/resources/db/migration/h2")
            .cleanDisabled(false)
            .load();
    h2ProtectedFlyway.migrate(); // Migrate H2 schema

    // Set password on the protected DB after migration
    try (Connection conn = DriverManager.getConnection(H2_PROTECTED_URL, H2_USER, H2_PASSWORD);
        Statement stmt = conn.createStatement()) {
      stmt.execute("ALTER USER " + H2_USER + " SET PASSWORD 'secret'");
    }
  }

  @Test
  void testMigrateData() throws SQLException {
    DataMigrationService migrationService = new DataMigrationService(null, null);
    migrationService.migrateData(
        "H2_FILE",
        H2_URL,
        H2_USER,
        H2_PASSWORD,
        "MySQL",
        MYSQL_CONTAINER.getHost(),
        MYSQL_CONTAINER.getFirstMappedPort(),
        MYSQL_CONTAINER.getDatabaseName(),
        MYSQL_CONTAINER.getUsername(),
        MYSQL_CONTAINER.getPassword());

    verifyDataMigration(H2_URL, H2_USER, H2_PASSWORD);
  }

  @Test
  void testMigrateDataWithPassword() throws SQLException {
    // Run migration with password
    DataMigrationService migrationService = new DataMigrationService(null, null);
    migrationService.migrateData(
        "H2_FILE",
        H2_PROTECTED_URL,
        H2_USER,
        "secret",
        "MySQL",
        MYSQL_CONTAINER.getHost(),
        MYSQL_CONTAINER.getFirstMappedPort(),
        MYSQL_CONTAINER.getDatabaseName(),
        MYSQL_CONTAINER.getUsername(),
        MYSQL_CONTAINER.getPassword());

    // 4. Verify
    verifyDataMigration(H2_PROTECTED_URL, H2_USER, "secret");
  }

  private void verifyDataMigration(String h2Url, String h2User, String h2Password)
      throws SQLException {
    DatabaseManager h2Manager =
        DatabaseManagerFactory.getDatabaseManager("H2_FILE", h2Url, h2User, h2Password);
    // Verify the data in the target MySQL database
    try (Connection mySqlConnection =
            DriverManager.getConnection(
                MYSQL_CONTAINER.getJdbcUrl(),
                MYSQL_CONTAINER.getUsername(),
                MYSQL_CONTAINER.getPassword());
        Statement stmt = mySqlConnection.createStatement();
        Connection sourceConnection = h2Manager.getConnection();
        Statement sourceStatement = sourceConnection.createStatement()) {

      // Verify core lookup tables and entities
      Assertions.assertAll(
          () -> {
            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM universe");
            rs.next();
            Assertions.assertTrue(rs.getInt(1) > 0, "No universes were migrated!");
          },
          () -> {
            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM wrestler");
            rs.next();
            Assertions.assertTrue(rs.getInt(1) > 0, "No wrestlers were migrated!");
          },
          () -> {
            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM faction");
            rs.next();
            Assertions.assertTrue(rs.getInt(1) > 0, "No factions were migrated!");
          },
          () -> {
            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM npc");
            rs.next();
            Assertions.assertTrue(rs.getInt(1) > 0, "No NPCs were migrated!");
          },
          () -> {
            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM wrestler_state");
            rs.next();
            Assertions.assertTrue(rs.getInt(1) > 0, "No wrestler states were migrated!");
          },
          () -> {
            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM injury");
            rs.next();
            Assertions.assertTrue(rs.getInt(1) > 0, "No injuries were migrated!");
          });

      // Also verify counts match between source and target for all tables
      ResultSet tables = sourceStatement.executeQuery("SHOW TABLES");
      java.util.List<org.junit.jupiter.api.function.Executable> assertions =
          new java.util.ArrayList<>();

      while (tables.next()) {
        String tableName = tables.getString(1);
        if (tableName.equalsIgnoreCase("flyway_schema_history")) {
          continue;
        }
        assertions.add(
            () -> {
              try (Statement innerSourceStatement = sourceConnection.createStatement();
                  Statement innerTargetStatement = mySqlConnection.createStatement()) {
                // Verify Row Count
                final ResultSet sourceRs =
                    innerSourceStatement.executeQuery("SELECT count(*) FROM " + tableName);
                sourceRs.next();
                final int sourceCount = sourceRs.getInt(1);

                final ResultSet targetRs =
                    innerTargetStatement.executeQuery(
                        "SELECT count(*) FROM `" + tableName.toLowerCase() + "`");
                targetRs.next();
                final int targetCount = targetRs.getInt(1);

                Assertions.assertEquals(
                    sourceCount,
                    targetCount,
                    "Row count for table " + tableName + " should match!");

                // Verify Column Non-Null Counts
                ResultSet columns =
                    sourceConnection.getMetaData().getColumns(null, null, tableName, null);
                while (columns.next()) {
                  String columnName = columns.getString("COLUMN_NAME");
                  final ResultSet sourceColRs =
                      innerSourceStatement.executeQuery(
                          "SELECT count(" + columnName + ") FROM " + tableName);
                  sourceColRs.next();
                  final int sourceColCount = sourceColRs.getInt(1);

                  final ResultSet targetColRs =
                      innerTargetStatement.executeQuery(
                          "SELECT count("
                              + columnName
                              + ") FROM `"
                              + tableName.toLowerCase()
                              + "`");
                  targetColRs.next();
                  final int targetColCount = targetColRs.getInt(1);

                  Assertions.assertEquals(
                      sourceColCount,
                      targetColCount,
                      "Non-null count for column "
                          + columnName
                          + " in table "
                          + tableName
                          + " should match!");
                }
              }
            });
      }
      Assertions.assertAll(
          "Verify row counts and non-null column counts for all tables", assertions);
    }
  }
}
