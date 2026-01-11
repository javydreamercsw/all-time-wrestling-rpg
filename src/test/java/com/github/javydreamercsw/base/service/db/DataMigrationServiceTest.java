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
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class DataMigrationServiceTest  {

  @Container
  private static final MySQLContainer<?> MYSQL_CONTAINER =
      new MySQLContainer<>("mysql:8.0.26")
          .withDatabaseName("test")
          .withUsername("test")
          .withPassword("test");

  private static final String H2_URL = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";
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
    
    // Clean up roles and accounts from H2 to avoid conflicts
    try (Connection h2Connection = DriverManager.getConnection(H2_URL, H2_USER, H2_PASSWORD);
        Statement stmt = h2Connection.createStatement()) {
        stmt.execute("DELETE FROM account_roles");
        stmt.execute("DELETE FROM role");
    }

    // Configure and run Flyway for MySQL Testcontainer
    Flyway mysqlFlyway =
        Flyway.configure()
            .dataSource(
                MYSQL_CONTAINER.getJdbcUrl(),
                MYSQL_CONTAINER.getUsername(),
                MYSQL_CONTAINER.getPassword())
            .locations("filesystem:src/main/resources/db/migration/mysql")
            .cleanDisabled(false)
            .load();
    mysqlFlyway.clean(); // Clean MySQL before migration
  }

  @AfterEach
  void tearDown() {
    // No specific cleanup for in-memory H2 needed, it's destroyed with the connection.
    // MySQL container is handled by Testcontainers.
  }

  @Test
  void testMigrateData() throws SQLException {
    H2FileDatabaseManager h2Manager = new H2FileDatabaseManager(H2_URL);
    MySQLDatabaseManager mySQLManager =
        new MySQLDatabaseManager(
            MYSQL_CONTAINER.getJdbcUrl(),
            MYSQL_CONTAINER.getUsername(),
            MYSQL_CONTAINER.getPassword());

    DataMigrationService migrationService = new DataMigrationService(h2Manager, mySQLManager);
    migrationService.migrateData("H2_FILE", "MySQL");

    // Verify the data in the target MySQL database
    try (Connection mySqlConnection =
            DriverManager.getConnection(
                MYSQL_CONTAINER.getJdbcUrl(),
                MYSQL_CONTAINER.getUsername(),
                MYSQL_CONTAINER.getPassword());
        Statement stmt = mySqlConnection.createStatement()) {

      ResultSet rs = stmt.executeQuery("SELECT count(*) FROM role");
      rs.next();
      int count = rs.getInt(1);
      Assertions.assertTrue(count > 0, "No roles were migrated!");

      rs = stmt.executeQuery("SELECT count(*) FROM wrestler");
      rs.next();
      count = rs.getInt(1);
      Assertions.assertTrue(count > 0, "No wrestlers were migrated!");

      rs = stmt.executeQuery("SELECT count(*) FROM faction");
      rs.next();
      count = rs.getInt(1);
      Assertions.assertTrue(count > 0, "No factions were migrated!");

      rs = stmt.executeQuery("SELECT count(*) FROM npc");
      rs.next();
      count = rs.getInt(1);
      Assertions.assertTrue(count > 0, "No NPCs were migrated!");
    }
  }
}
