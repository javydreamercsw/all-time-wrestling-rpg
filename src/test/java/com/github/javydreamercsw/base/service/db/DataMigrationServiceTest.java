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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class DataMigrationServiceTest {

  @Container
  private static final MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:8.0.26");

  @Test
  void testMigrateData() throws Exception {
    // Run Flyway migration on the MySQL container
    Flyway flyway =
        Flyway.configure()
            .dataSource(
                mySQLContainer.getJdbcUrl(),
                mySQLContainer.getUsername(),
                mySQLContainer.getPassword())
            .locations("filesystem:src/main/resources/db/migration")
            .sqlMigrationSuffixes(".mysql.sql")
            .load();
    flyway.migrate();

    // H2 in-memory database setup
    String h2Url = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";
    String h2User = "sa";
    String h2Password = "";

    try (Connection h2Connection = DriverManager.getConnection(h2Url, h2User, h2Password)) {
      try (Statement statement = h2Connection.createStatement()) {
        statement.execute(
            "CREATE TABLE wrestler (ID INT PRIMARY KEY, NAME VARCHAR(255), GENDER VARCHAR(255),"
                + " EXTERNAL_ID VARCHAR(255), BUMPS INT, ACTIVE BOOLEAN, DECK_SIZE INT, IS_PLAYER"
                + " BOOLEAN, LOW_HEALTH INT, LOW_STAMINA INT, STARTING_HEALTH INT, STARTING_STAMINA"
                + " INT, FANS INT, TIER VARCHAR(255), CREATION_DATE TIMESTAMP)");
        statement.execute(
            "INSERT INTO wrestler (ID, NAME, GENDER, EXTERNAL_ID, BUMPS, ACTIVE, DECK_SIZE,"
                + " IS_PLAYER, LOW_HEALTH, LOW_STAMINA, STARTING_HEALTH, STARTING_STAMINA, FANS,"
                + " TIER, CREATION_DATE) VALUES (1, 'Hulk Hogan', 'MALE', '123', 0, TRUE, 0, FALSE,"
                + " 0, 0, 0, 0, 0, 'ROOKIE', NOW())");
      }
    }

    DatabaseManagerFactory.overrideDatabaseManager(
        "H2",
        new H2DatabaseManager() {
          @Override
          public Connection getConnection() throws java.sql.SQLException {
            return DriverManager.getConnection(h2Url, h2User, h2Password);
          }
        });

    DatabaseManagerFactory.overrideDatabaseManager(
        "MySQL",
        new MySQLDatabaseManager("jdbc:mysql://dummy", "dummy", "dummy") {
          @Override
          public Connection getConnection() throws java.sql.SQLException {
            return DriverManager.getConnection(
                mySQLContainer.getJdbcUrl(),
                mySQLContainer.getUsername(),
                mySQLContainer.getPassword());
          }
        });

    DataMigrationService service = new DataMigrationService();
    service.migrateData("H2", "MySQL");

    // Verification
    try (Connection mySqlConnection =
            DriverManager.getConnection(
                mySQLContainer.getJdbcUrl(),
                mySQLContainer.getUsername(),
                mySQLContainer.getPassword());
        Statement statement = mySqlConnection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM wrestler")) {
      resultSet.next();
      assertEquals(1, resultSet.getInt(1));
    }
  }
}
