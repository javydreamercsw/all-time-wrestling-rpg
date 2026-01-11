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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DataMigrationService {

  private final DatabaseManager source;
  private final DatabaseManager target;

  public DataMigrationService() {
    this.source = null; // Will be initialized by migrateData
    this.target = null; // Will be initialized by migrateData
  }

  public DataMigrationService(DatabaseManager source, DatabaseManager target) {
    this.source = source;
    this.target = target;
  }

  public void migrateData(String sourceDbType, String targetDbType) throws SQLException {
    if (source == null || target == null) {
      // Fallback for default constructor or non-injected managers
      DatabaseManager sourceManager =
          DatabaseManagerFactory.getDatabaseManager(sourceDbType, null, null, null);
      DatabaseManager targetManager =
          DatabaseManagerFactory.getDatabaseManager(targetDbType, null, null, null);
      migrateDataInternal(sourceManager, targetManager);
    } else {
      migrateDataInternal(source, target);
    }
  }

  private void migrateDataInternal(DatabaseManager sourceManager, DatabaseManager targetManager)
      throws SQLException {

    try (Connection sourceConnection = sourceManager.getConnection();
        Connection targetConnection = targetManager.getConnection()) {
      try {
        targetConnection.setAutoCommit(false);

        // Migrate Core Lookup tables first
        migrateNpcs(sourceConnection, targetConnection);
        migrateRoles(sourceConnection, targetConnection);
        migrateFactions(sourceConnection, targetConnection);
        migrateInjuryTypes(sourceConnection, targetConnection);
        migrateCardSets(sourceConnection, targetConnection); // New

        // Dependent tables
        migrateAccounts(sourceConnection, targetConnection);
        migrateAccountRoles(sourceConnection, targetConnection);
        migrateWrestlers(sourceConnection, targetConnection);
        migrateInjuries(sourceConnection, targetConnection);
        migrateTeams(sourceConnection, targetConnection);
        migrateCards(sourceConnection, targetConnection); // New
        migrateDecks(sourceConnection, targetConnection); // New
        migrateDeckCards(sourceConnection, targetConnection); // New

        targetConnection.commit();
      } catch (SQLException e) {
        targetConnection.rollback();
        throw new SQLException("Error during data migration: " + e.getMessage(), e);
      } finally {
        targetConnection.setAutoCommit(true);
      }
    }
  }

  private void migrateRoles(Connection sourceConnection, Connection targetConnection)
      throws SQLException {
    String sql = "INSERT INTO role (ID, NAME, DESCRIPTION) VALUES (?, ?, ?)";
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery("SELECT ID, NAME, DESCRIPTION FROM role");
        PreparedStatement targetStatement = targetConnection.prepareStatement(sql)) {

      int count = 0;
      while (resultSet.next()) {
        targetStatement.setLong(1, resultSet.getLong("ID"));
        targetStatement.setString(2, resultSet.getString("NAME"));
        targetStatement.setString(3, resultSet.getString("DESCRIPTION"));
        targetStatement.addBatch();
        count++;
        if (count % 1000 == 0) {
          targetStatement.executeBatch();
        }
      }
      if (count > 0) {
        targetStatement.executeBatch();
      }
    }
  }

  private void migrateAccounts(Connection sourceConnection, Connection targetConnection)
      throws SQLException {
    String sql =
        "INSERT INTO account (ID, USERNAME, PASSWORD, EMAIL, ENABLED, "
            + "ACCOUNT_NON_EXPIRED, ACCOUNT_NON_LOCKED, CREDENTIALS_NON_EXPIRED, "
            + "FAILED_LOGIN_ATTEMPTS, LOCKED_UNTIL, LAST_LOGIN, CREATED_DATE, "
            + "UPDATED_DATE) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery(
                "SELECT ID, USERNAME, PASSWORD, EMAIL, ENABLED, ACCOUNT_NON_EXPIRED,"
                    + " ACCOUNT_NON_LOCKED, CREDENTIALS_NON_EXPIRED, FAILED_LOGIN_ATTEMPTS,"
                    + " LOCKED_UNTIL, LAST_LOGIN, CREATED_DATE, UPDATED_DATE FROM account");
        PreparedStatement targetStatement = targetConnection.prepareStatement(sql)) {

      int count = 0;
      while (resultSet.next()) {
        targetStatement.setLong(1, resultSet.getLong("ID"));
        targetStatement.setString(2, resultSet.getString("USERNAME"));
        targetStatement.setString(3, resultSet.getString("PASSWORD"));
        targetStatement.setString(4, resultSet.getString("EMAIL"));
        targetStatement.setBoolean(5, resultSet.getBoolean("ENABLED"));
        targetStatement.setBoolean(6, resultSet.getBoolean("ACCOUNT_NON_EXPIRED"));
        targetStatement.setBoolean(7, resultSet.getBoolean("ACCOUNT_NON_LOCKED"));
        targetStatement.setBoolean(8, resultSet.getBoolean("CREDENTIALS_NON_EXPIRED"));
        targetStatement.setInt(9, resultSet.getInt("FAILED_LOGIN_ATTEMPTS"));
        targetStatement.setTimestamp(10, resultSet.getTimestamp("LOCKED_UNTIL"));
        targetStatement.setTimestamp(11, resultSet.getTimestamp("LAST_LOGIN"));
        targetStatement.setTimestamp(12, resultSet.getTimestamp("CREATED_DATE"));
        targetStatement.setTimestamp(13, resultSet.getTimestamp("UPDATED_DATE"));
        targetStatement.addBatch();
        count++;
        if (count % 1000 == 0) {
          targetStatement.executeBatch();
        }
      }
      if (count > 0) {
        targetStatement.executeBatch();
      }
    }
  }

  private void migrateAccountRoles(Connection sourceConnection, Connection targetConnection)
      throws SQLException {
    String sql = "INSERT INTO account_roles (ACCOUNT_ID, ROLE_ID) VALUES (?, ?)";
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery("SELECT ACCOUNT_ID, ROLE_ID FROM account_roles");
        PreparedStatement targetStatement = targetConnection.prepareStatement(sql)) {

      int count = 0;
      while (resultSet.next()) {
        targetStatement.setLong(1, resultSet.getLong("ACCOUNT_ID"));
        targetStatement.setLong(2, resultSet.getLong("ROLE_ID"));
        targetStatement.addBatch();
        count++;
        if (count % 1000 == 0) {
          targetStatement.executeBatch();
        }
      }
      if (count > 0) {
        targetStatement.executeBatch();
      }
    }
  }

  private void migrateNpcs(Connection sourceConnection, Connection targetConnection)
      throws SQLException {
    String sql = "INSERT INTO npc (ID, NAME, NPC_TYPE, EXTERNAL_ID) VALUES (?, ?, ?, ?)";
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery("SELECT ID, NAME, NPC_TYPE, EXTERNAL_ID FROM npc");
        PreparedStatement targetStatement = targetConnection.prepareStatement(sql)) {

      int count = 0;
      while (resultSet.next()) {
        targetStatement.setLong(1, resultSet.getLong("ID"));
        targetStatement.setString(2, resultSet.getString("NAME"));
        targetStatement.setString(3, resultSet.getString("NPC_TYPE"));
        targetStatement.setString(4, resultSet.getString("EXTERNAL_ID"));
        targetStatement.addBatch();
        count++;
        if (count % 1000 == 0) {
          targetStatement.executeBatch();
        }
      }
      if (count > 0) {
        targetStatement.executeBatch();
      }
    }
  }

  private void migrateFactions(Connection sourceConnection, Connection targetConnection)
      throws SQLException {
    String sql =
        "INSERT INTO faction (FACTION_ID, NAME, DESCRIPTION, IS_ACTIVE, LEADER_ID, "
            + "FORMED_DATE, DISBANDED_DATE, CREATION_DATE, EXTERNAL_ID) "
            + "VALUES (?, ?, ?, ?, NULL, ?, ?, ?, ?)";
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery(
                "SELECT FACTION_ID, NAME, DESCRIPTION, IS_ACTIVE, LEADER_ID, FORMED_DATE,"
                    + " DISBANDED_DATE, CREATION_DATE, EXTERNAL_ID FROM faction");
        PreparedStatement targetStatement = targetConnection.prepareStatement(sql)) {

      int count = 0;
      while (resultSet.next()) {
        targetStatement.setLong(1, resultSet.getLong("FACTION_ID"));
        targetStatement.setString(2, resultSet.getString("NAME"));
        targetStatement.setString(3, resultSet.getString("DESCRIPTION"));
        targetStatement.setBoolean(4, resultSet.getBoolean("IS_ACTIVE"));
        targetStatement.setTimestamp(5, resultSet.getTimestamp("FORMED_DATE"));
        targetStatement.setTimestamp(6, resultSet.getTimestamp("DISBANDED_DATE"));
        targetStatement.setTimestamp(7, resultSet.getTimestamp("CREATION_DATE"));
        targetStatement.setString(8, resultSet.getString("EXTERNAL_ID"));
        targetStatement.addBatch();
        count++;
        if (count % 1000 == 0) {
          targetStatement.executeBatch();
        }
      }
      if (count > 0) {
        targetStatement.executeBatch();
      }
    }
  }

  private void migrateWrestlers(Connection sourceConnection, Connection targetConnection)
      throws SQLException {
    String sql =
        "INSERT INTO wrestler (wrestler_id, NAME, GENDER, EXTERNAL_ID, BUMPS, ACTIVE, "
            + "DECK_SIZE, IS_PLAYER, LOW_HEALTH, LOW_STAMINA, STARTING_HEALTH, "
            + "STARTING_STAMINA, FANS, TIER, CREATION_DATE, FACTION_ID) "
            + "VALUES (?, ?, ?, ?, 0, TRUE, 0, FALSE, 0, 0, 0, 0, 0, 'ROOKIE', NOW(), ?)";
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery(
                "SELECT ID, NAME, GENDER, EXTERNAL_ID, FACTION_ID FROM wrestler");
        PreparedStatement targetStatement = targetConnection.prepareStatement(sql)) {

      int count = 0;
      while (resultSet.next()) {
        targetStatement.setLong(1, resultSet.getLong("ID"));
        targetStatement.setString(2, resultSet.getString("NAME"));
        targetStatement.setString(3, resultSet.getString("GENDER"));
        targetStatement.setString(4, resultSet.getString("EXTERNAL_ID"));
        if (resultSet.getObject("FACTION_ID") != null) {
          targetStatement.setLong(5, resultSet.getLong("FACTION_ID"));
        } else {
          targetStatement.setObject(5, null);
        }
        targetStatement.addBatch();
        count++;
        if (count % 1000 == 0) {
          targetStatement.executeBatch();
        }
      }
      if (count > 0) {
        targetStatement.executeBatch();
      }
    }
  }

  private void migrateInjuryTypes(Connection sourceConnection, Connection targetConnection)
      throws SQLException {
    String sql =
        "INSERT INTO injury_type (INJURY_TYPE_ID, INJURY_NAME, HEALTH_EFFECT, "
            + "STAMINA_EFFECT, CARD_EFFECT, SPECIAL_EFFECTS, EXTERNAL_ID) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?)";
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery(
                "SELECT INJURY_TYPE_ID, INJURY_NAME, HEALTH_EFFECT, STAMINA_EFFECT, CARD_EFFECT,"
                    + " SPECIAL_EFFECTS, EXTERNAL_ID FROM injury_type");
        PreparedStatement targetStatement = targetConnection.prepareStatement(sql)) {

      int count = 0;
      while (resultSet.next()) {
        targetStatement.setLong(1, resultSet.getLong("INJURY_TYPE_ID"));
        targetStatement.setString(2, resultSet.getString("INJURY_NAME"));
        targetStatement.setInt(3, resultSet.getInt("HEALTH_EFFECT"));
        targetStatement.setInt(4, resultSet.getInt("STAMINA_EFFECT"));
        targetStatement.setInt(5, resultSet.getInt("CARD_EFFECT"));
        targetStatement.setString(6, resultSet.getString("SPECIAL_EFFECTS"));
        targetStatement.setString(7, resultSet.getString("EXTERNAL_ID"));
        targetStatement.addBatch();
        count++;
        if (count % 1000 == 0) {
          targetStatement.executeBatch();
        }
      }
      if (count > 0) {
        targetStatement.executeBatch();
      }
    }
  }

  private void migrateInjuries(Connection sourceConnection, Connection targetConnection)
      throws SQLException {
    String sql =
        "INSERT INTO injury (INJURY_ID, WRESTLER_ID, NAME, DESCRIPTION, SEVERITY, "
            + "HEALTH_PENALTY, IS_ACTIVE, INJURY_DATE, HEALED_DATE, HEALING_COST, "
            + "INJURY_NOTES, CREATION_DATE, EXTERNAL_ID) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery(
                "SELECT INJURY_ID, WRESTLER_ID, NAME, DESCRIPTION, SEVERITY, HEALTH_PENALTY,"
                    + " IS_ACTIVE, INJURY_DATE, HEALED_DATE, HEALING_COST, INJURY_NOTES,"
                    + " CREATION_DATE, EXTERNAL_ID FROM injury");
        PreparedStatement targetStatement = targetConnection.prepareStatement(sql)) {

      int count = 0;
      while (resultSet.next()) {
        targetStatement.setLong(1, resultSet.getLong("INJURY_ID"));
        targetStatement.setLong(2, resultSet.getLong("WRESTLER_ID"));
        targetStatement.setString(3, resultSet.getString("NAME"));
        targetStatement.setString(4, resultSet.getString("DESCRIPTION"));
        targetStatement.setString(5, resultSet.getString("SEVERITY"));
        targetStatement.setInt(6, resultSet.getInt("HEALTH_PENALTY"));
        targetStatement.setBoolean(7, resultSet.getBoolean("IS_ACTIVE"));
        targetStatement.setTimestamp(8, resultSet.getTimestamp("INJURY_DATE"));
        targetStatement.setTimestamp(9, resultSet.getTimestamp("HEALED_DATE"));
        targetStatement.setLong(10, resultSet.getLong("HEALING_COST"));
        targetStatement.setString(11, resultSet.getString("INJURY_NOTES"));
        targetStatement.setTimestamp(12, resultSet.getTimestamp("CREATION_DATE"));
        targetStatement.setString(13, resultSet.getString("EXTERNAL_ID"));
        targetStatement.addBatch();
        count++;
        if (count % 1000 == 0) {
          targetStatement.executeBatch();
        }
      }
      if (count > 0) {
        targetStatement.executeBatch();
      }
    }
  }

  private void migrateTeams(Connection sourceConnection, Connection targetConnection)
      throws SQLException {
    String sql =
        "INSERT INTO team (TEAM_ID, NAME, DESCRIPTION, WRESTLER1_ID, WRESTLER2_ID, "
            + "FACTION_ID, STATUS, FORMED_DATE, DISBANDED_DATE, EXTERNAL_ID) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery(
                "SELECT TEAM_ID, NAME, DESCRIPTION, WRESTLER1_ID, WRESTLER2_ID, FACTION_ID, STATUS,"
                    + " FORMED_DATE, DISBANDED_DATE, EXTERNAL_ID FROM team");
        PreparedStatement targetStatement = targetConnection.prepareStatement(sql)) {

      int count = 0;
      while (resultSet.next()) {
        targetStatement.setLong(1, resultSet.getLong("TEAM_ID"));
        targetStatement.setString(2, resultSet.getString("NAME"));
        targetStatement.setString(3, resultSet.getString("DESCRIPTION"));
        targetStatement.setLong(4, resultSet.getLong("WRESTLER1_ID"));
        targetStatement.setLong(5, resultSet.getLong("WRESTLER2_ID"));
        if (resultSet.getObject("FACTION_ID") != null) {
          targetStatement.setLong(6, resultSet.getLong("FACTION_ID"));
        } else {
          targetStatement.setObject(6, null);
        }
        targetStatement.setString(7, resultSet.getString("STATUS"));
        targetStatement.setTimestamp(8, resultSet.getTimestamp("FORMED_DATE"));
        targetStatement.setTimestamp(9, resultSet.getTimestamp("DISBANDED_DATE"));
        targetStatement.setString(10, resultSet.getString("EXTERNAL_ID"));
        targetStatement.addBatch();
        count++;
        if (count % 1000 == 0) {
          targetStatement.executeBatch();
        }
      }
      if (count > 0) {
        targetStatement.executeBatch();
      }
    }
  }

  private void migrateCardSets(Connection sourceConnection, Connection targetConnection)
      throws SQLException {
    String sql =
        "INSERT INTO card_set (SET_ID, SET_CODE, NAME, DESCRIPTION, RELEASE_DATE, CREATION_DATE) "
            + "VALUES (?, ?, ?, ?, ?, ?)";
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery(
                "SELECT SET_ID, SET_CODE, DESCRIPTION, RELEASE_DATE, CREATION_DATE FROM card_set");
        PreparedStatement targetStatement = targetConnection.prepareStatement(sql)) {

      int count = 0;
      while (resultSet.next()) {
        targetStatement.setLong(1, resultSet.getLong("SET_ID"));
        targetStatement.setString(2, resultSet.getString("SET_CODE"));
        targetStatement.setString(3, resultSet.getString("SET_CODE")); // Use set_code for name
        targetStatement.setString(4, resultSet.getString("DESCRIPTION"));
        targetStatement.setDate(5, resultSet.getDate("RELEASE_DATE"));
        targetStatement.setTimestamp(6, resultSet.getTimestamp("CREATION_DATE"));
        targetStatement.addBatch();
        count++;
        if (count % 1000 == 0) {
          targetStatement.executeBatch();
        }
      }
      if (count > 0) {
        targetStatement.executeBatch();
      }
    }
  }

  private void migrateCards(Connection sourceConnection, Connection targetConnection)
      throws SQLException {
    String sql =
        "INSERT INTO card (CARD_ID, NAME, TYPE, DAMAGE, STAMINA, MOMENTUM, TARGET, "
            + "NUMBER, FINISHER, SIGNATURE, PIN, TAUNT, RECOVER, CREATION_DATE, SET_ID) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery(
                "SELECT CARD_ID, NAME, TYPE, DAMAGE, STAMINA, MOMENTUM, TARGET, NUMBER,"
                    + " FINISHER, SIGNATURE, PIN, TAUNT, RECOVER, CREATION_DATE, SET_ID FROM card");
        PreparedStatement targetStatement = targetConnection.prepareStatement(sql)) {

      int count = 0;
      while (resultSet.next()) {
        targetStatement.setLong(1, resultSet.getLong("CARD_ID"));
        targetStatement.setString(2, resultSet.getString("NAME"));
        targetStatement.setString(3, resultSet.getString("TYPE"));
        targetStatement.setInt(4, resultSet.getInt("DAMAGE"));
        targetStatement.setInt(5, resultSet.getInt("STAMINA"));
        targetStatement.setInt(6, resultSet.getInt("MOMENTUM"));
        targetStatement.setInt(7, resultSet.getInt("TARGET"));
        targetStatement.setInt(8, resultSet.getInt("NUMBER"));
        targetStatement.setBoolean(9, resultSet.getBoolean("FINISHER"));
        targetStatement.setBoolean(10, resultSet.getBoolean("SIGNATURE"));
        targetStatement.setBoolean(11, resultSet.getBoolean("PIN"));
        targetStatement.setBoolean(12, resultSet.getBoolean("TAUNT"));
        targetStatement.setBoolean(13, resultSet.getBoolean("RECOVER"));
        targetStatement.setTimestamp(14, resultSet.getTimestamp("CREATION_DATE"));
        targetStatement.setLong(15, resultSet.getLong("SET_ID"));
        targetStatement.addBatch();
        count++;
        if (count % 1000 == 0) {
          targetStatement.executeBatch();
        }
      }
      if (count > 0) {
        targetStatement.executeBatch();
      }
    }
  }

  private void migrateDecks(Connection sourceConnection, Connection targetConnection)
      throws SQLException {
    String sql = "INSERT INTO deck (DECK_ID, WRESTLER_ID, CREATION_DATE) VALUES (?, ?, ?)";
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery("SELECT DECK_ID, WRESTLER_ID, CREATION_DATE FROM deck");
        PreparedStatement targetStatement = targetConnection.prepareStatement(sql)) {

      int count = 0;
      while (resultSet.next()) {
        targetStatement.setLong(1, resultSet.getLong("DECK_ID"));
        targetStatement.setLong(2, resultSet.getLong("WRESTLER_ID"));
        targetStatement.setTimestamp(3, resultSet.getTimestamp("CREATION_DATE"));
        targetStatement.addBatch();
        count++;
        if (count % 1000 == 0) {
          targetStatement.executeBatch();
        }
      }
      if (count > 0) {
        targetStatement.executeBatch();
      }
    }
  }

  private void migrateDeckCards(Connection sourceConnection, Connection targetConnection)
      throws SQLException {
    String sql =
        "INSERT INTO deck_card (ID, DECK_ID, CARD_ID, AMOUNT, CREATION_DATE, SET_ID) "
            + "VALUES (?, ?, ?, ?, ?, ?)";
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery(
                "SELECT d.ID, d.DECK_ID, d.CARD_ID, d.AMOUNT, d.CREATION_DATE, c.SET_ID FROM"
                    + " deck_card d JOIN card c ON d.CARD_ID = c.CARD_ID");
        PreparedStatement targetStatement = targetConnection.prepareStatement(sql)) {

      int count = 0;
      while (resultSet.next()) {
        targetStatement.setLong(1, resultSet.getLong("ID"));
        targetStatement.setLong(2, resultSet.getLong("DECK_ID"));
        targetStatement.setLong(3, resultSet.getLong("CARD_ID"));
        targetStatement.setInt(4, resultSet.getInt("AMOUNT"));
        targetStatement.setTimestamp(5, resultSet.getTimestamp("CREATION_DATE"));
        targetStatement.setLong(6, resultSet.getLong("SET_ID"));
        targetStatement.addBatch();
        count++;
        if (count % 1000 == 0) {
          targetStatement.executeBatch();
        }
      }
      if (count > 0) {
        targetStatement.executeBatch();
      }
    }
  }
}
