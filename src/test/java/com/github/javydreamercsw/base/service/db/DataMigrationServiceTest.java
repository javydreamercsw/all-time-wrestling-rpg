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
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class DataMigrationServiceTest {

  @Container
  private static final MySQLContainer<?> mySQLContainer =
      new MySQLContainer<>(
          DockerImageName.parse("mysql/mysql-server:8.0.26").asCompatibleSubstituteFor("mysql"));

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

    // Clear the tables in MySQL container to avoid duplicate entries during migration
    try (Connection mySqlConnection =
            DriverManager.getConnection(
                mySQLContainer.getJdbcUrl(),
                mySQLContainer.getUsername(),
                mySQLContainer.getPassword());
        Statement statement = mySqlConnection.createStatement()) {
      statement.execute("DELETE FROM role");
      statement.execute("DELETE FROM account");
      statement.execute("DELETE FROM account_roles");
      statement.execute("DELETE FROM wrestler");
      statement.execute("DELETE FROM faction");
      statement.execute("DELETE FROM npc");
      statement.execute("DELETE FROM injury_type");
      statement.execute("DELETE FROM injury");
      statement.execute("DELETE FROM team");
      statement.execute("DELETE FROM card_set");
      statement.execute("DELETE FROM card");
      statement.execute("DELETE FROM deck");
      statement.execute("DELETE FROM deck_card");
    }

    // H2 in-memory database setup
    String h2Url = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";
    String h2User = "sa";
    String h2Password = "";

    try (Connection h2Connection = DriverManager.getConnection(h2Url, h2User, h2Password)) {
      try (Statement statement = h2Connection.createStatement()) {
        // Setup for role table in H2
        statement.execute(
            "CREATE TABLE role (ID BIGINT PRIMARY KEY AUTO_INCREMENT, NAME VARCHAR(50), DESCRIPTION"
                + " VARCHAR(500))");
        statement.execute(
            "INSERT INTO role (ID, NAME, DESCRIPTION) VALUES (1, 'ADMIN', 'Administrator role')");
        statement.execute(
            "INSERT INTO role (ID, NAME, DESCRIPTION) VALUES (2, 'USER', 'User role')");

        // Setup for account table in H2
        statement.execute(
            "CREATE TABLE account (ID BIGINT PRIMARY KEY AUTO_INCREMENT, USERNAME VARCHAR(50),"
                + " PASSWORD VARCHAR(100), EMAIL VARCHAR(100), ENABLED BOOLEAN, ACCOUNT_NON_EXPIRED"
                + " BOOLEAN, ACCOUNT_NON_LOCKED BOOLEAN, CREDENTIALS_NON_EXPIRED BOOLEAN,"
                + " FAILED_LOGIN_ATTEMPTS INT, LOCKED_UNTIL TIMESTAMP, LAST_LOGIN TIMESTAMP,"
                + " CREATED_DATE TIMESTAMP, UPDATED_DATE TIMESTAMP)");
        statement.execute(
            "INSERT INTO account (ID, USERNAME, PASSWORD, EMAIL, ENABLED, ACCOUNT_NON_EXPIRED,"
                + " ACCOUNT_NON_LOCKED, CREDENTIALS_NON_EXPIRED, FAILED_LOGIN_ATTEMPTS,"
                + " CREATED_DATE, UPDATED_DATE) VALUES (1, 'admin', 'password',"
                + " 'admin@example.com', TRUE, TRUE, TRUE, TRUE, 0, NOW(), NOW())");
        statement.execute(
            "INSERT INTO account (ID, USERNAME, PASSWORD, EMAIL, ENABLED, ACCOUNT_NON_EXPIRED,"
                + " ACCOUNT_NON_LOCKED, CREDENTIALS_NON_EXPIRED, FAILED_LOGIN_ATTEMPTS,"
                + " CREATED_DATE, UPDATED_DATE) VALUES (2, 'user', 'password', 'user@example.com',"
                + " TRUE, TRUE, TRUE, TRUE, 0, NOW(), NOW())");

        // Setup for account_roles table in H2
        statement.execute(
            "CREATE TABLE account_roles (ACCOUNT_ID BIGINT, ROLE_ID BIGINT, PRIMARY KEY"
                + " (ACCOUNT_ID, ROLE_ID))");
        statement.execute("INSERT INTO account_roles (ACCOUNT_ID, ROLE_ID) VALUES (1, 1)");
        statement.execute("INSERT INTO account_roles (ACCOUNT_ID, ROLE_ID) VALUES (2, 2)");

        // Setup for npc table in H2 (before faction and wrestler as they can depend on it)
        statement.execute(
            "CREATE TABLE npc (ID BIGINT PRIMARY KEY AUTO_INCREMENT, NAME VARCHAR(255), NPC_TYPE"
                + " VARCHAR(255), EXTERNAL_ID VARCHAR(255))");
        statement.execute(
            "INSERT INTO npc (ID, NAME, NPC_TYPE) VALUES (1, 'Manager Bob', 'MANAGER')");

        // Setup for faction table in H2 (leader_id can be null initially)
        statement.execute(
            "CREATE TABLE faction (FACTION_ID BIGINT PRIMARY KEY AUTO_INCREMENT, NAME VARCHAR(255),"
                + " DESCRIPTION LONGTEXT, IS_ACTIVE BOOLEAN, LEADER_ID BIGINT, FORMED_DATE"
                + " TIMESTAMP, DISBANDED_DATE TIMESTAMP, CREATION_DATE TIMESTAMP, EXTERNAL_ID"
                + " VARCHAR(255))");
        statement.execute(
            "INSERT INTO faction (FACTION_ID, NAME, DESCRIPTION, IS_ACTIVE, FORMED_DATE,"
                + " CREATION_DATE, EXTERNAL_ID) VALUES (1, 'The Alliance', 'A powerful alliance',"
                + " TRUE, NOW(), NOW(), 'FACTION1')");

        // Setup for wrestler table in H2
        statement.execute(
            "CREATE TABLE wrestler (ID BIGINT PRIMARY KEY, NAME VARCHAR(255), GENDER VARCHAR(255),"
                + " EXTERNAL_ID VARCHAR(255), BUMPS INT, ACTIVE BOOLEAN, DECK_SIZE INT, IS_PLAYER"
                + " BOOLEAN, LOW_HEALTH INT, LOW_STAMINA INT, STARTING_HEALTH INT, STARTING_STAMINA"
                + " INT, FANS INT, TIER VARCHAR(255), CREATION_DATE"
                + " TIMESTAMP, FACTION_ID BIGINT)"); // Added FACTION_ID
        statement.execute(
            "INSERT INTO wrestler (ID, NAME, GENDER, EXTERNAL_ID, BUMPS, ACTIVE, DECK_SIZE,"
                + " IS_PLAYER, LOW_HEALTH, LOW_STAMINA, STARTING_HEALTH, STARTING_STAMINA, FANS,"
                + " TIER, CREATION_DATE, FACTION_ID) VALUES (1, 'Hulk Hogan', 'MALE', '123', 0,"
                + " TRUE, 0, FALSE, 0, 0, 0, 0, 0, 'ROOKIE', NOW(), 1)"); // Added FACTION_ID
        statement.execute(
            "INSERT INTO wrestler (ID, NAME, GENDER, EXTERNAL_ID, BUMPS, ACTIVE, DECK_SIZE,"
                + " IS_PLAYER, LOW_HEALTH, LOW_STAMINA, STARTING_HEALTH, STARTING_STAMINA, FANS,"
                + " TIER, CREATION_DATE, FACTION_ID) VALUES (2, 'Randy Savage', 'MALE', '124', 0,"
                + " TRUE, 0, FALSE, 0, 0, 0, 0, 0, 'ROOKIE', NOW(), 1)");

        // Setup for injury_type table in H2
        statement.execute(
            "CREATE TABLE injury_type (INJURY_TYPE_ID BIGINT PRIMARY KEY AUTO_INCREMENT,"
                + " INJURY_NAME VARCHAR(100), HEALTH_EFFECT INT, STAMINA_EFFECT INT, CARD_EFFECT"
                + " INT, SPECIAL_EFFECTS LONGTEXT, EXTERNAL_ID VARCHAR(255))");
        statement.execute(
            "INSERT INTO injury_type (INJURY_TYPE_ID, INJURY_NAME, HEALTH_EFFECT, STAMINA_EFFECT,"
                + " CARD_EFFECT) VALUES (1, 'Concussion', -10, -5, -2)");

        // Setup for injury table in H2
        statement.execute(
            "CREATE TABLE injury (INJURY_ID BIGINT PRIMARY KEY AUTO_INCREMENT, WRESTLER_ID BIGINT,"
                + " NAME VARCHAR(255), DESCRIPTION LONGTEXT, SEVERITY VARCHAR(255), HEALTH_PENALTY"
                + " INT, IS_ACTIVE BOOLEAN, INJURY_DATE TIMESTAMP, HEALED_DATE TIMESTAMP,"
                + " HEALING_COST BIGINT, INJURY_NOTES LONGTEXT, CREATION_DATE TIMESTAMP,"
                + " EXTERNAL_ID VARCHAR(255))");
        statement.execute(
            "INSERT INTO injury (INJURY_ID, WRESTLER_ID, NAME, DESCRIPTION, SEVERITY,"
                + " HEALTH_PENALTY, IS_ACTIVE, INJURY_DATE, HEALING_COST, CREATION_DATE) VALUES (1,"
                + " 1, 'Minor Concussion', 'Head injury', 'MINOR', 10, TRUE, NOW(), 100, NOW())");

        // Setup for team table in H2
        statement.execute(
            "CREATE TABLE team (TEAM_ID BIGINT PRIMARY KEY AUTO_INCREMENT, NAME VARCHAR(255),"
                + " DESCRIPTION LONGTEXT, WRESTLER1_ID BIGINT NOT NULL, WRESTLER2_ID BIGINT NOT"
                + " NULL, FACTION_ID BIGINT, STATUS VARCHAR(255), FORMED_DATE TIMESTAMP,"
                + " DISBANDED_DATE TIMESTAMP, EXTERNAL_ID VARCHAR(255))");
        statement.execute(
            "INSERT INTO team (TEAM_ID, NAME, DESCRIPTION, WRESTLER1_ID, WRESTLER2_ID, FACTION_ID,"
                + " STATUS, FORMED_DATE, EXTERNAL_ID) VALUES (1, 'Mega Powers', 'Hogan and Savage',"
                + " 1, 2, 1, 'ACTIVE', NOW(), 'TEAM1')");

        // Setup for card_set table in H2
        statement.execute(
            "CREATE TABLE card_set (SET_ID BIGINT PRIMARY KEY AUTO_INCREMENT, SET_CODE"
                + " VARCHAR(255), DESCRIPTION LONGTEXT, RELEASE_DATE DATE, CREATION_DATE"
                + " TIMESTAMP)");
        statement.execute(
            "INSERT INTO card_set (SET_ID, SET_CODE, DESCRIPTION, RELEASE_DATE, CREATION_DATE)"
                + " VALUES (1, 'BASE', 'Base set', '2026-01-01', NOW())");

        // Setup for card table in H2
        statement.execute(
            "CREATE TABLE card (CARD_ID BIGINT PRIMARY KEY AUTO_INCREMENT, NAME VARCHAR(255), TYPE"
                + " VARCHAR(100), DAMAGE INT, STAMINA INT, MOMENTUM INT, TARGET INT, NUMBER INT,"
                + " FINISHER BOOLEAN, SIGNATURE BOOLEAN, PIN BOOLEAN, TAUNT BOOLEAN, RECOVER"
                + " BOOLEAN, CREATION_DATE TIMESTAMP, SET_ID BIGINT)");
        statement.execute(
            "INSERT INTO card (CARD_ID, NAME, TYPE, DAMAGE, STAMINA, MOMENTUM, TARGET, NUMBER,"
                + " FINISHER, SIGNATURE, PIN, TAUNT, RECOVER, CREATION_DATE, SET_ID) VALUES (1,"
                + " 'Clothesline', 'STRIKE', 5, 0, 1, 1, 1, FALSE, FALSE, FALSE, FALSE, FALSE,"
                + " NOW(), 1)");

        // Setup for deck table in H2
        statement.execute(
            "CREATE TABLE deck (DECK_ID BIGINT PRIMARY KEY AUTO_INCREMENT, WRESTLER_ID BIGINT,"
                + " CREATION_DATE TIMESTAMP)");
        statement.execute(
            "INSERT INTO deck (DECK_ID, WRESTLER_ID, CREATION_DATE) VALUES (1, 1, NOW())");

        // Setup for deck_card table in H2
        statement.execute(
            "CREATE TABLE deck_card (ID BIGINT PRIMARY KEY AUTO_INCREMENT, DECK_ID BIGINT, CARD_ID"
                + " BIGINT, AMOUNT INT, CREATION_DATE TIMESTAMP, SET_ID BIGINT)");
        statement.execute(
            "INSERT INTO deck_card (ID, DECK_ID, CARD_ID, AMOUNT, CREATION_DATE, SET_ID) VALUES (1,"
                + " 1, 1, 2, NOW(), 1)");
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

    // Verification for role table in MySQL
    try (Connection mySqlConnection =
            DriverManager.getConnection(
                mySQLContainer.getJdbcUrl(),
                mySQLContainer.getUsername(),
                mySQLContainer.getPassword());
        Statement statement = mySqlConnection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM role")) {
      resultSet.next();
      assertEquals(2, resultSet.getInt(1)); // 2 roles inserted
    }

    // Verification for account table in MySQL
    try (Connection mySqlConnection =
            DriverManager.getConnection(
                mySQLContainer.getJdbcUrl(),
                mySQLContainer.getUsername(),
                mySQLContainer.getPassword());
        Statement statement = mySqlConnection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM account")) {
      resultSet.next();
      assertEquals(2, resultSet.getInt(1)); // 2 accounts inserted
    }

    // Verification for account_roles table in MySQL
    try (Connection mySqlConnection =
            DriverManager.getConnection(
                mySQLContainer.getJdbcUrl(),
                mySQLContainer.getUsername(),
                mySQLContainer.getPassword());
        Statement statement = mySqlConnection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM account_roles")) {
      resultSet.next();
      assertEquals(2, resultSet.getInt(1)); // 2 account_roles inserted
    }

    // Verification for npc table in MySQL
    try (Connection mySqlConnection =
            DriverManager.getConnection(
                mySQLContainer.getJdbcUrl(),
                mySQLContainer.getUsername(),
                mySQLContainer.getPassword());
        Statement statement = mySqlConnection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM npc")) {
      resultSet.next();
      assertEquals(1, resultSet.getInt(1)); // 1 npc inserted
    }

    // Verification for faction table in MySQL
    try (Connection mySqlConnection =
            DriverManager.getConnection(
                mySQLContainer.getJdbcUrl(),
                mySQLContainer.getUsername(),
                mySQLContainer.getPassword());
        Statement statement = mySqlConnection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM faction")) {
      resultSet.next();
      assertEquals(1, resultSet.getInt(1)); // 1 faction inserted
    }

    // Verification for wrestler table in MySQL
    try (Connection mySqlConnection =
            DriverManager.getConnection(
                mySQLContainer.getJdbcUrl(),
                mySQLContainer.getUsername(),
                mySQLContainer.getPassword());
        Statement statement = mySqlConnection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM wrestler")) {
      resultSet.next();
      assertEquals(2, resultSet.getInt(1)); // 2 wrestlers inserted
    }

    // Verification for injury_type table in MySQL
    try (Connection mySqlConnection =
            DriverManager.getConnection(
                mySQLContainer.getJdbcUrl(),
                mySQLContainer.getUsername(),
                mySQLContainer.getPassword());
        Statement statement = mySqlConnection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM injury_type")) {
      resultSet.next();
      assertEquals(1, resultSet.getInt(1)); // 1 injury_type inserted
    }

    // Verification for injury table in MySQL
    try (Connection mySqlConnection =
            DriverManager.getConnection(
                mySQLContainer.getJdbcUrl(),
                mySQLContainer.getUsername(),
                mySQLContainer.getPassword());
        Statement statement = mySqlConnection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM injury")) {
      resultSet.next();
      assertEquals(1, resultSet.getInt(1)); // 1 injury inserted
    }

    // Verification for team table in MySQL
    try (Connection mySqlConnection =
            DriverManager.getConnection(
                mySQLContainer.getJdbcUrl(),
                mySQLContainer.getUsername(),
                mySQLContainer.getPassword());
        Statement statement = mySqlConnection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM team")) {
      resultSet.next();
      assertEquals(1, resultSet.getInt(1)); // 1 team inserted
    }

    // Verification for card_set table in MySQL
    try (Connection mySqlConnection =
            DriverManager.getConnection(
                mySQLContainer.getJdbcUrl(),
                mySQLContainer.getUsername(),
                mySQLContainer.getPassword());
        Statement statement = mySqlConnection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM card_set")) {
      resultSet.next();
      assertEquals(1, resultSet.getInt(1)); // 1 card_set inserted
    }

    // Verification for card table in MySQL
    try (Connection mySqlConnection =
            DriverManager.getConnection(
                mySQLContainer.getJdbcUrl(),
                mySQLContainer.getUsername(),
                mySQLContainer.getPassword());
        Statement statement = mySqlConnection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM card")) {
      resultSet.next();
      assertEquals(1, resultSet.getInt(1)); // 1 card inserted
    }

    // Verification for deck table in MySQL
    try (Connection mySqlConnection =
            DriverManager.getConnection(
                mySQLContainer.getJdbcUrl(),
                mySQLContainer.getUsername(),
                mySQLContainer.getPassword());
        Statement statement = mySqlConnection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM deck")) {
      resultSet.next();
      assertEquals(1, resultSet.getInt(1)); // 1 deck inserted
    }

    // Verification for deck_card table in MySQL
    try (Connection mySqlConnection =
            DriverManager.getConnection(
                mySQLContainer.getJdbcUrl(),
                mySQLContainer.getUsername(),
                mySQLContainer.getPassword());
        Statement statement = mySqlConnection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM deck_card")) {
      resultSet.next();
      assertEquals(1, resultSet.getInt(1)); // 1 deck_card inserted
    }
  }
}
