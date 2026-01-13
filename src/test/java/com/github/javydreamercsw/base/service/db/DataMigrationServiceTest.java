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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@Slf4j
class DataMigrationServiceTest {

  @Container
  private static final MySQLContainer<?> MYSQL_CONTAINER =
      new MySQLContainer<>("mysql:8.0.26")
          .withDatabaseName("test")
          .withUsername("test")
          .withPassword("test");

  private static final String H2_URL = "jdbc:h2:./src/test/resources/db/sample";
  private static final String H2_USER = "sa";
  private static final String H2_PASSWORD = "";

  @BeforeEach
  void setUp() throws SQLException {
    // Configure and run Flyway for H2 in-memory database
    Flyway h2Flyway =
        Flyway.configure()
            .dataSource(H2_URL, H2_USER, H2_PASSWORD)
            .locations("filesystem:src/main/resources/db/migration/h2")
            .cleanDisabled(false)
            .load();
    h2Flyway.migrate(); // Migrate H2 schema
  }

  @AfterEach
  void tearDown() {
    // No specific cleanup for in-memory H2 needed, it's destroyed with the connection.
    // MySQL container is handled by Testcontainers.
  }

  @Test
  void testMigrateData() throws SQLException {
    DataMigrationService migrationService = new DataMigrationService();
    migrationService.migrateData(
        "H2_FILE",
        "MySQL",
        MYSQL_CONTAINER.getHost(),
        MYSQL_CONTAINER.getFirstMappedPort(),
        MYSQL_CONTAINER.getUsername(),
        MYSQL_CONTAINER.getPassword());

    H2FileDatabaseManager h2Manager = new H2FileDatabaseManager(H2_URL, H2_USER, H2_PASSWORD);
    // Verify the data in the target MySQL database
    try (Connection mySqlConnection =
            DriverManager.getConnection(
                MYSQL_CONTAINER.getJdbcUrl(),
                MYSQL_CONTAINER.getUsername(),
                MYSQL_CONTAINER.getPassword());
        Statement stmt = mySqlConnection.createStatement();
        Connection sourceConnection = h2Manager.getConnection();
        Statement sourceStatement = sourceConnection.createStatement()) {

      Assertions.assertAll(
          () -> {
            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM role");
            rs.next();
            int count = rs.getInt(1);
            Assertions.assertTrue(count > 0, "No roles were migrated!");
          },
          () -> {
            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM wrestler");
            rs.next();
            int count = rs.getInt(1);
            Assertions.assertTrue(count > 0, "No wrestlers were migrated!");
          },
          () -> {
            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM faction");
            rs.next();
            int count = rs.getInt(1);
            Assertions.assertTrue(count > 0, "No factions were migrated!");
          },
          () -> {
            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM npc");
            rs.next();
            int count = rs.getInt(1);
            Assertions.assertTrue(count > 0, "No NPCs were migrated!");
          },
          () -> {
            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM season");
            rs.next();
            int count = rs.getInt(1);
            Assertions.assertTrue(count > 0, "No seasons were migrated!");
          },
          () -> {
            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM show_type");
            rs.next();
            int count = rs.getInt(1);
            Assertions.assertTrue(count > 0, "No show_types were migrated!");
          },
          () -> {
            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM show_template");
            rs.next();
            int count = rs.getInt(1);
            Assertions.assertTrue(count > 0, "No show_templates were migrated!");
          },
          () -> {
            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM `show`");
            rs.next();
            int count = rs.getInt(1);
            Assertions.assertTrue(count > 0, "No shows were migrated!");
          },
          () -> {
            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM segment_type");
            rs.next();
            int count = rs.getInt(1);
            Assertions.assertTrue(count > 0, "No segment_types were migrated!");
          },
          () -> {
            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM segment_rule");
            rs.next();
            int count = rs.getInt(1);
            Assertions.assertTrue(count > 0, "No segment_rules were migrated!");
          },
          () -> {
            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM segment");
            rs.next();
            int count = rs.getInt(1);
            Assertions.assertTrue(count > 0, "No segments were migrated!");
          },
          () -> {
            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM segment_participant");
            rs.next();
            int count = rs.getInt(1);
            Assertions.assertTrue(count > 0, "No segment_participants were migrated!");
          },
          () -> {
            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM segment_segment_rule");
            rs.next();
            int count = rs.getInt(1);
            Assertions.assertTrue(count > 0, "No segment_segment_rules were migrated!");
          },
          () -> {
            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM title");
            rs.next();
            int count = rs.getInt(1);
            Assertions.assertTrue(count > 0, "No titles were migrated!");
          },
          () -> {
            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM segment_title");
            rs.next();
            int count = rs.getInt(1);
            Assertions.assertTrue(count > 0, "No segment_titles were migrated!");
          },
          () -> {
            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM title_champion");
            rs.next();
            int count = rs.getInt(1);
            Assertions.assertTrue(count > 0, "No title_champions were migrated!");
          },
          () -> {
            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM title_contender");
            rs.next();
            int count = rs.getInt(1);
            if (count == 0) {
              System.out.println("Warning: No title_contenders were migrated!");
            }
          },
          () -> {
            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM title_reign");
            rs.next();
            int count = rs.getInt(1);
            Assertions.assertTrue(count > 0, "No title_reigns were migrated!");
          },
          () -> {
            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM title_reign_champion");
            rs.next();
            int count = rs.getInt(1);
            Assertions.assertTrue(count > 0, "No title_reign_champions were migrated!");
          },
          () -> {
            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM rivalry");
            rs.next();
            int count = rs.getInt(1);
            Assertions.assertTrue(count > 0, "No rivalries were migrated!");
          },
          () -> {
            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM heat_event");
            rs.next();
            int count = rs.getInt(1);
            Assertions.assertTrue(count > 0, "No heat_events were migrated!");
          },
          () -> {
            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM faction_rivalry");
            rs.next();
            int count = rs.getInt(1);
            Assertions.assertTrue(count > 0, "No faction_rivalries were migrated!");
          },
          () -> {
            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM faction_heat_event");
            rs.next();
            int count = rs.getInt(1);
            Assertions.assertTrue(count > 0, "No faction_heat_events were migrated!");
          },
          () -> {
            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM drama_event");
            rs.next();
            int count = rs.getInt(1);
            if (count == 0) {
              log.warn("Warning: No drama_events were migrated!");
            }
          },
          () -> {
            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM multi_wrestler_feud");
            rs.next();
            int count = rs.getInt(1);
            if (count == 0) {
              log.warn("Warning: No multi_wrestler_feuds were migrated!");
            }
          },
          () -> {
            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM feud_participant");
            rs.next();
            int count = rs.getInt(1);
            if (count == 0) {
              log.warn("Warning: No feud_participants were migrated!");
            }
          },
          () -> {
            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM feud_heat_event");
            rs.next();
            int count = rs.getInt(1);
            if (count == 0) {
              log.warn("Warning: No feud_heat_events were migrated!");
            }
          });

      ResultSet tables = sourceStatement.executeQuery("SHOW TABLES");
      Assertions.assertAll(
          "Verify row counts for all tables",
          () -> {
            while (tables.next()) {
              String tableName = tables.getString(1);
              if (tableName.equalsIgnoreCase("flyway_schema_history")) {
                continue;
              }
              try (Statement innerSourceStatement = sourceConnection.createStatement();
                  Statement innerTargetStatement = mySqlConnection.createStatement()) {
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
              }
            }
          });
    }
  }
}
