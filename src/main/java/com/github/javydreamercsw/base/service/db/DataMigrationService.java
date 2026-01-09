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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DataMigrationService {

  public void migrateData(String sourceDbType, String targetDbType) throws SQLException {
    DatabaseManager sourceManager = DatabaseManagerFactory.getDatabaseManager(sourceDbType);
    DatabaseManager targetManager = DatabaseManagerFactory.getDatabaseManager(targetDbType);

    try (Connection sourceConnection = sourceManager.getConnection();
        Connection targetConnection = targetManager.getConnection()) {

      // Migrate Core Lookup tables first
      migrateNpcs(sourceConnection, targetConnection);
      migrateRoles(sourceConnection, targetConnection);
      migrateFactions(sourceConnection, targetConnection);
      migrateInjuryTypes(sourceConnection, targetConnection);

      // Dependent tables
      migrateAccounts(sourceConnection, targetConnection);
      migrateAccountRoles(sourceConnection, targetConnection);
      migrateWrestlers(sourceConnection, targetConnection);
      migrateInjuries(sourceConnection, targetConnection);
      migrateTeams(sourceConnection, targetConnection);

    } catch (SQLException e) {
      throw new SQLException("Error during data migration: " + e.getMessage(), e);
    }
  }

  private void migrateRoles(Connection sourceConnection, Connection targetConnection)
      throws SQLException {
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery("SELECT ID, NAME, DESCRIPTION FROM role");
        Statement targetStatement = targetConnection.createStatement()) {

      while (resultSet.next()) {
        long id = resultSet.getLong("ID");
        String name = resultSet.getString("NAME");
        String description = resultSet.getString("DESCRIPTION");

        String insertSql =
            String.format(
                "INSERT INTO role (ID, NAME, DESCRIPTION) VALUES (%d, '%s', '%s')",
                id, name.replace("'", "''"), description.replace("'", "''"));
        targetStatement.executeUpdate(insertSql);
      }
    }
  }

  private void migrateAccounts(Connection sourceConnection, Connection targetConnection)
      throws SQLException {
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery(
                "SELECT ID, USERNAME, PASSWORD, EMAIL, ENABLED, ACCOUNT_NON_EXPIRED,"
                    + " ACCOUNT_NON_LOCKED, CREDENTIALS_NON_EXPIRED, FAILED_LOGIN_ATTEMPTS,"
                    + " LOCKED_UNTIL, LAST_LOGIN, CREATED_DATE, UPDATED_DATE FROM account");
        Statement targetStatement = targetConnection.createStatement()) {

      while (resultSet.next()) {
        long id = resultSet.getLong("ID");
        String username = resultSet.getString("USERNAME");
        String password = resultSet.getString("PASSWORD");
        String email = resultSet.getString("EMAIL");
        boolean enabled = resultSet.getBoolean("ENABLED");
        boolean accountNonExpired = resultSet.getBoolean("ACCOUNT_NON_EXPIRED");
        boolean accountNonLocked = resultSet.getBoolean("ACCOUNT_NON_LOCKED");
        boolean credentialsNonExpired = resultSet.getBoolean("CREDENTIALS_NON_EXPIRED");
        int failedLoginAttempts = resultSet.getInt("FAILED_LOGIN_ATTEMPTS");
        String lockedUntil =
            resultSet.getTimestamp("LOCKED_UNTIL") != null
                ? "'" + resultSet.getTimestamp("LOCKED_UNTIL").toString() + "'"
                : "NULL";
        String lastLogin =
            resultSet.getTimestamp("LAST_LOGIN") != null
                ? "'" + resultSet.getTimestamp("LAST_LOGIN").toString() + "'"
                : "NULL";
        String createdDate = resultSet.getTimestamp("CREATED_DATE").toString();
        String updatedDate = resultSet.getTimestamp("UPDATED_DATE").toString();

        String insertSql =
            String.format(
                "INSERT INTO account (ID, USERNAME, PASSWORD, EMAIL, ENABLED, ACCOUNT_NON_EXPIRED,"
                    + " ACCOUNT_NON_LOCKED, CREDENTIALS_NON_EXPIRED, FAILED_LOGIN_ATTEMPTS,"
                    + " LOCKED_UNTIL, LAST_LOGIN, CREATED_DATE, UPDATED_DATE) VALUES (%d, '%s',"
                    + " '%s', '%s', %b, %b, %b, %b, %d, %s, %s, '%s', '%s')",
                id,
                username.replace("'", "''"),
                password.replace("'", "''"),
                email.replace("'", "''"),
                enabled,
                accountNonExpired,
                accountNonLocked,
                credentialsNonExpired,
                failedLoginAttempts,
                lockedUntil,
                lastLogin,
                createdDate,
                updatedDate);
        targetStatement.executeUpdate(insertSql);
      }
    }
  }

  private void migrateAccountRoles(Connection sourceConnection, Connection targetConnection)
      throws SQLException {
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery("SELECT ACCOUNT_ID, ROLE_ID FROM account_roles");
        Statement targetStatement = targetConnection.createStatement()) {

      while (resultSet.next()) {
        long accountId = resultSet.getLong("ACCOUNT_ID");
        long roleId = resultSet.getLong("ROLE_ID");

        String insertSql =
            String.format(
                "INSERT INTO account_roles (ACCOUNT_ID, ROLE_ID) VALUES (%d, %d)",
                accountId, roleId);
        targetStatement.executeUpdate(insertSql);
      }
    }
  }

  private void migrateNpcs(Connection sourceConnection, Connection targetConnection)
      throws SQLException {
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery("SELECT ID, NAME, NPC_TYPE, EXTERNAL_ID FROM npc");
        Statement targetStatement = targetConnection.createStatement()) {

      while (resultSet.next()) {
        long id = resultSet.getLong("ID");
        String name = resultSet.getString("NAME");
        String npcType = resultSet.getString("NPC_TYPE");
        String externalId = resultSet.getString("EXTERNAL_ID");

        String insertSql =
            String.format(
                "INSERT INTO npc (ID, NAME, NPC_TYPE, EXTERNAL_ID) VALUES (%d, '%s', '%s', %s)",
                id,
                name.replace("'", "''"),
                npcType.replace("'", "''"),
                externalId != null ? "'" + externalId.replace("'", "''") + "'" : "NULL");
        targetStatement.executeUpdate(insertSql);
      }
    }
  }

  private void migrateFactions(Connection sourceConnection, Connection targetConnection)
      throws SQLException {
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery(
                "SELECT FACTION_ID, NAME, DESCRIPTION, IS_ACTIVE, LEADER_ID, FORMED_DATE,"
                    + " DISBANDED_DATE, CREATION_DATE, EXTERNAL_ID FROM faction");
        Statement targetStatement = targetConnection.createStatement()) {

      while (resultSet.next()) {
        long factionId = resultSet.getLong("FACTION_ID");
        String name = resultSet.getString("NAME");
        String description = resultSet.getString("DESCRIPTION");
        boolean isActive = resultSet.getBoolean("IS_ACTIVE");
        // LEADER_ID will be handled in a separate update step due to circular dependency
        String formedDate =
            resultSet.getTimestamp("FORMED_DATE") != null
                ? "'" + resultSet.getTimestamp("FORMED_DATE").toString() + "'"
                : "NULL";
        String disbandedDate =
            resultSet.getTimestamp("DISBANDED_DATE") != null
                ? "'" + resultSet.getTimestamp("DISBANDED_DATE").toString() + "'"
                : "NULL";
        String creationDate = resultSet.getTimestamp("CREATION_DATE").toString();
        String externalId = resultSet.getString("EXTERNAL_ID");

        String insertSql =
            String.format(
                "INSERT INTO faction (FACTION_ID, NAME, DESCRIPTION, IS_ACTIVE, FORMED_DATE,"
                    + " DISBANDED_DATE, CREATION_DATE, EXTERNAL_ID) VALUES (%d, '%s', %s, %b, %s,"
                    + " %s, '%s', %s)",
                factionId,
                name.replace("'", "''"),
                description != null ? "'" + description.replace("'", "''") + "'" : "NULL",
                isActive,
                formedDate,
                disbandedDate,
                creationDate,
                externalId != null ? "'" + externalId.replace("'", "''") + "'" : "NULL");
        targetStatement.executeUpdate(insertSql);
      }
    }
  }

  private void migrateWrestlers(Connection sourceConnection, Connection targetConnection)
      throws SQLException {
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery(
                "SELECT ID, NAME, GENDER, EXTERNAL_ID, FACTION_ID FROM wrestler");
        Statement targetStatement = targetConnection.createStatement()) {

      while (resultSet.next()) {
        long id = resultSet.getLong("ID");
        String name = resultSet.getString("NAME");
        String gender = resultSet.getString("GENDER");
        String externalId = resultSet.getString("EXTERNAL_ID");
        long factionId = resultSet.getLong("FACTION_ID");
        String factionIdStr = resultSet.wasNull() ? "NULL" : String.valueOf(factionId);

        String insertSql =
            String.format(
                "INSERT INTO wrestler (WRESTLER_ID, NAME, GENDER, EXTERNAL_ID, BUMPS, ACTIVE,"
                    + " DECK_SIZE, IS_PLAYER, LOW_HEALTH, LOW_STAMINA, STARTING_HEALTH,"
                    + " STARTING_STAMINA, FANS, TIER, CREATION_DATE, FACTION_ID) VALUES (%d, '%s',"
                    + " '%s', %s, 0, TRUE, 0, FALSE, 0, 0, 0, 0, 0, 'ROOKIE', NOW(), %s)",
                id,
                name.replace("'", "''"),
                gender.replace("'", "''"),
                externalId != null ? "'" + externalId.replace("'", "''") + "'" : "NULL",
                factionIdStr);
        targetStatement.executeUpdate(insertSql);
      }
    }
  }

  private void migrateInjuryTypes(Connection sourceConnection, Connection targetConnection)
      throws SQLException {
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery(
                "SELECT INJURY_TYPE_ID, INJURY_NAME, HEALTH_EFFECT, STAMINA_EFFECT, CARD_EFFECT,"
                    + " SPECIAL_EFFECTS, EXTERNAL_ID FROM injury_type");
        Statement targetStatement = targetConnection.createStatement()) {

      while (resultSet.next()) {
        long injuryTypeId = resultSet.getLong("INJURY_TYPE_ID");
        String injuryName = resultSet.getString("INJURY_NAME");
        int healthEffect = resultSet.getInt("HEALTH_EFFECT");
        int staminaEffect = resultSet.getInt("STAMINA_EFFECT");
        int cardEffect = resultSet.getInt("CARD_EFFECT");
        String specialEffects = resultSet.getString("SPECIAL_EFFECTS");
        String externalId = resultSet.getString("EXTERNAL_ID");

        String insertSql =
            String.format(
                "INSERT INTO injury_type (INJURY_TYPE_ID, INJURY_NAME, HEALTH_EFFECT,"
                    + " STAMINA_EFFECT, CARD_EFFECT, SPECIAL_EFFECTS, EXTERNAL_ID) VALUES (%d,"
                    + " '%s', %d, %d, %d, %s, %s)",
                injuryTypeId,
                injuryName.replace("'", "''"),
                healthEffect,
                staminaEffect,
                cardEffect,
                specialEffects != null ? "'" + specialEffects.replace("'", "''") + "'" : "NULL",
                externalId != null ? "'" + externalId.replace("'", "''") + "'" : "NULL");
        targetStatement.executeUpdate(insertSql);
      }
    }
  }

  private void migrateInjuries(Connection sourceConnection, Connection targetConnection)
      throws SQLException {
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery(
                "SELECT INJURY_ID, WRESTLER_ID, NAME, DESCRIPTION, SEVERITY, HEALTH_PENALTY,"
                    + " IS_ACTIVE, INJURY_DATE, HEALED_DATE, HEALING_COST, INJURY_NOTES,"
                    + " CREATION_DATE, EXTERNAL_ID FROM injury");
        Statement targetStatement = targetConnection.createStatement()) {

      while (resultSet.next()) {
        long injuryId = resultSet.getLong("INJURY_ID");
        long wrestlerId = resultSet.getLong("WRESTLER_ID");
        String name = resultSet.getString("NAME");
        String description = resultSet.getString("DESCRIPTION");
        String severity = resultSet.getString("SEVERITY");
        int healthPenalty = resultSet.getInt("HEALTH_PENALTY");
        boolean isActive = resultSet.getBoolean("IS_ACTIVE");
        String injuryDate = resultSet.getTimestamp("INJURY_DATE").toString();
        String healedDate =
            resultSet.getTimestamp("HEALED_DATE") != null
                ? "'" + resultSet.getTimestamp("HEALED_DATE").toString() + "'"
                : "NULL";
        long healingCost = resultSet.getLong("HEALING_COST");
        String injuryNotes = resultSet.getString("INJURY_NOTES");
        String creationDate = resultSet.getTimestamp("CREATION_DATE").toString();
        String externalId = resultSet.getString("EXTERNAL_ID");

        String insertSql =
            String.format(
                "INSERT INTO injury (INJURY_ID, WRESTLER_ID, NAME, DESCRIPTION, SEVERITY,"
                    + " HEALTH_PENALTY, IS_ACTIVE, INJURY_DATE, HEALED_DATE, HEALING_COST,"
                    + " INJURY_NOTES, CREATION_DATE, EXTERNAL_ID) VALUES (%d, %d, '%s', %s, '%s',"
                    + " %d, %b, '%s', %s, %d, %s, '%s', %s)",
                injuryId,
                wrestlerId,
                name.replace("'", "''"),
                description != null ? "'" + description.replace("'", "''") + "'" : "NULL",
                severity.replace("'", "''"),
                healthPenalty,
                isActive,
                injuryDate,
                healedDate,
                healingCost,
                injuryNotes != null ? "'" + injuryNotes.replace("'", "''") + "'" : "NULL",
                creationDate,
                externalId != null ? "'" + externalId.replace("'", "''") + "'" : "NULL");
        targetStatement.executeUpdate(insertSql);
      }
    }
  }

  private void migrateTeams(Connection sourceConnection, Connection targetConnection)
      throws SQLException {
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery(
                "SELECT TEAM_ID, NAME, DESCRIPTION, WRESTLER1_ID, WRESTLER2_ID, FACTION_ID, STATUS,"
                    + " FORMED_DATE, DISBANDED_DATE, EXTERNAL_ID FROM team");
        Statement targetStatement = targetConnection.createStatement()) {

      while (resultSet.next()) {
        long teamId = resultSet.getLong("TEAM_ID");
        String name = resultSet.getString("NAME");
        String description = resultSet.getString("DESCRIPTION");
        long wrestler1Id = resultSet.getLong("WRESTLER1_ID");
        long wrestler2Id = resultSet.getLong("WRESTLER2_ID");
        long factionId = resultSet.getLong("FACTION_ID");
        String factionIdStr = resultSet.wasNull() ? "NULL" : String.valueOf(factionId);
        String status = resultSet.getString("STATUS");
        String formedDate = resultSet.getTimestamp("FORMED_DATE").toString();
        String disbandedDate =
            resultSet.getTimestamp("DISBANDED_DATE") != null
                ? "'" + resultSet.getTimestamp("DISBANDED_DATE").toString() + "'"
                : "NULL";
        String externalId = resultSet.getString("EXTERNAL_ID");

        String insertSql =
            String.format(
                "INSERT INTO team (TEAM_ID, NAME, DESCRIPTION, WRESTLER1_ID, WRESTLER2_ID,"
                    + " FACTION_ID, STATUS, FORMED_DATE, DISBANDED_DATE, EXTERNAL_ID) VALUES (%d,"
                    + " '%s', %s, %d, %d, %s, '%s', '%s', %s, %s)",
                teamId,
                name.replace("'", "''"),
                description != null ? "'" + description.replace("'", "''") + "'" : "NULL",
                wrestler1Id,
                wrestler2Id,
                factionIdStr,
                status.replace("'", "''"),
                formedDate,
                disbandedDate,
                externalId != null ? "'" + externalId.replace("'", "''") + "'" : "NULL");
        targetStatement.executeUpdate(insertSql);
      }
    }
  }
}
