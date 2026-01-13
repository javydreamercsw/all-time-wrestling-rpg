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
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DataMigrationService {

  public void migrateData(
      String sourceDbType,
      String targetDbType,
      String host,
      Integer port,
      String user,
      String password)
      throws SQLException {
    DatabaseManager sourceManager =
        DatabaseManagerFactory.getDatabaseManager(sourceDbType, null, null, null, null);
    DatabaseManager targetManager =
        DatabaseManagerFactory.getDatabaseManager(targetDbType, host, port, user, password);
    migrateDataInternal(sourceManager, targetManager, password);
  }

  public void setTargetFlywayMigration(
      @NonNull String url,
      @NonNull String user,
      @NonNull String password,
      @NonNull String migrationLocation) {
    log.debug("Starting Flyway migration for target database at {}", url);
    Flyway flyway =
        Flyway.configure()
            .dataSource(url, user, password)
            .locations(migrationLocation)
            .cleanDisabled(false)
            .load();
    flyway.clean(); // Clean target before migration
    flyway.migrate(); // Migrate target schema
    log.debug("Finished Flyway migration for target database.");
  }

  private void migrateDataInternal(
      @NonNull DatabaseManager sourceManager,
      @NonNull DatabaseManager targetManager,
      @NonNull String password)
      throws SQLException {

    try (Connection sourceConnection = sourceManager.getConnection();
        Connection targetConnection = targetManager.getConnection(password)) {
      try {
        // Perform Flyway migration for the target database
        setTargetFlywayMigration(
            targetManager.getURL(),
            targetManager.getUser(),
            password,
            "filesystem:src/main/resources/db/migration/mysql");

        targetConnection.setAutoCommit(false);

        // Truncate all tables in the target database before migration
        truncateAllTables(targetConnection);

        // Migrate Core Lookup tables first
        migrateNpcs(sourceConnection, targetConnection);
        migrateRoles(sourceConnection, targetConnection);
        migrateFactions(sourceConnection, targetConnection);
        migrateInjuryTypes(sourceConnection, targetConnection);
        migrateCardSets(sourceConnection, targetConnection);

        // Dependent tables
        migrateAccounts(sourceConnection, targetConnection);
        migrateAccountRoles(sourceConnection, targetConnection);
        migrateWrestlers(sourceConnection, targetConnection);
        migrateInjuries(sourceConnection, targetConnection);
        migrateTeams(sourceConnection, targetConnection);
        migrateCards(sourceConnection, targetConnection);
        migrateDecks(sourceConnection, targetConnection);
        migrateDeckCards(sourceConnection, targetConnection);
        migrateSeasons(sourceConnection, targetConnection);
        migrateShowTypes(sourceConnection, targetConnection);
        migrateShowTemplates(sourceConnection, targetConnection);
        migrateShows(sourceConnection, targetConnection);
        migrateSegmentTypes(sourceConnection, targetConnection);
        migrateSegmentRules(sourceConnection, targetConnection);
        migrateSegments(sourceConnection, targetConnection);
        migrateSegmentParticipants(sourceConnection, targetConnection);
        migrateSegmentSegmentRules(sourceConnection, targetConnection);
        migrateTitles(sourceConnection, targetConnection);
        migrateSegmentTitles(sourceConnection, targetConnection);
        migrateTitleChampions(sourceConnection, targetConnection);
        migrateTitleContenders(sourceConnection, targetConnection);
        migrateTitleReigns(sourceConnection, targetConnection);
        migrateTitleReignChampions(sourceConnection, targetConnection);
        migrateRivalries(sourceConnection, targetConnection);
        migrateHeatEvents(sourceConnection, targetConnection);
        migrateFactionRivalries(sourceConnection, targetConnection);
        migrateFactionHeatEvents(sourceConnection, targetConnection);
        migrateDramaEvents(sourceConnection, targetConnection);
        migrateMultiWrestlerFeuds(sourceConnection, targetConnection);
        migrateFeudParticipants(sourceConnection, targetConnection);
        migrateFeudHeatEvents(sourceConnection, targetConnection);
        migrateInboxItems(sourceConnection, targetConnection);
        migrateTierBoundaries(sourceConnection, targetConnection);
        migrateInboxItemTargets(sourceConnection, targetConnection);
        migrateGameSettings(sourceConnection, targetConnection);
        migratePasswordResetTokens(sourceConnection, targetConnection);
        migrateHolidays(sourceConnection, targetConnection);

        targetConnection.commit();
      } catch (SQLException e) {
        targetConnection.rollback();
        throw new SQLException("Error during data migration: " + e.getMessage(), e);
      } finally {
        targetConnection.setAutoCommit(true);
      }
    }
  }

  private void migrateHolidays(
      @NonNull Connection sourceConnection, @NonNull Connection targetConnection)
      throws SQLException {
    String sql =
        "INSERT INTO holiday (id, description, theme, decorations, day_of_month, "
            + "holiday_month, day_of_week, week_of_month, type, creation_date, external_id) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery(
                "SELECT id, description, theme, decorations, day_of_month, holiday_month,"
                    + " day_of_week, week_of_month, type, creation_date, external_id FROM holiday");
        PreparedStatement targetStatement = targetConnection.prepareStatement(sql)) {

      int count = 0;
      while (resultSet.next()) {
        targetStatement.setLong(1, resultSet.getLong("id"));
        targetStatement.setString(2, resultSet.getString("description"));
        targetStatement.setString(3, resultSet.getString("theme"));
        targetStatement.setString(4, resultSet.getString("decorations"));
        targetStatement.setInt(5, resultSet.getInt("day_of_month"));
        targetStatement.setString(6, resultSet.getString("holiday_month"));
        targetStatement.setString(7, resultSet.getString("day_of_week"));
        targetStatement.setInt(8, resultSet.getInt("week_of_month"));
        targetStatement.setString(9, resultSet.getString("type"));
        targetStatement.setTimestamp(10, resultSet.getTimestamp("creation_date"));
        targetStatement.setString(11, resultSet.getString("external_id"));
        targetStatement.addBatch();
        count++;
        if (count % 1000 == 0) {
          targetStatement.executeBatch();
        }
      }
      if (count > 0) {
        targetStatement.executeBatch();
        log.debug("Migrated {} Holidays", count);
      }
    }
  }

  private void migratePasswordResetTokens(
      @NonNull Connection sourceConnection, @NonNull Connection targetConnection)
      throws SQLException {
    String sql =
        "INSERT INTO password_reset_token (id, token, account_id, expiry_date) "
            + "VALUES (?, ?, ?, ?)";
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery(
                "SELECT id, token, account_id, expiry_date FROM password_reset_token");
        PreparedStatement targetStatement = targetConnection.prepareStatement(sql)) {

      int count = 0;
      while (resultSet.next()) {
        targetStatement.setLong(1, resultSet.getLong("id"));
        targetStatement.setString(2, resultSet.getString("token"));
        targetStatement.setLong(3, resultSet.getLong("account_id"));
        targetStatement.setTimestamp(4, resultSet.getTimestamp("expiry_date"));
        targetStatement.addBatch();
        count++;
        if (count % 1000 == 0) {
          targetStatement.executeBatch();
        }
      }
      if (count > 0) {
        targetStatement.executeBatch();
        log.debug("Migrated {} Password Reset Tokens", count);
      }
    }
  }

  private void migrateGameSettings(
      @NonNull Connection sourceConnection, @NonNull Connection targetConnection)
      throws SQLException {
    String sql = "INSERT INTO game_setting (setting_key, setting_value) VALUES (?, ?)";
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery("SELECT setting_key, setting_value FROM game_setting");
        PreparedStatement targetStatement = targetConnection.prepareStatement(sql)) {

      int count = 0;
      while (resultSet.next()) {
        targetStatement.setString(1, resultSet.getString("setting_key"));
        targetStatement.setString(2, resultSet.getString("setting_value"));
        targetStatement.addBatch();
        count++;
        if (count % 1000 == 0) {
          targetStatement.executeBatch();
        }
      }
      if (count > 0) {
        targetStatement.executeBatch();
        log.debug("Migrated {} Game Settings", count);
      }
    }
  }

  private void migrateInboxItemTargets(
      @NonNull Connection sourceConnection, @NonNull Connection targetConnection)
      throws SQLException {
    String sql =
        "INSERT INTO inbox_item_target (inbox_item_target_id, inbox_item_id, "
            + "target_id, external_id) VALUES (?, ?, ?, ?)";
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery(
                "SELECT inbox_item_target_id, inbox_item_id, target_id, "
                    + "external_id FROM inbox_item_target");
        PreparedStatement targetStatement = targetConnection.prepareStatement(sql)) {

      int count = 0;
      while (resultSet.next()) {
        targetStatement.setLong(1, resultSet.getLong("inbox_item_target_id"));
        targetStatement.setLong(2, resultSet.getLong("inbox_item_id"));
        targetStatement.setString(3, resultSet.getString("target_id"));
        targetStatement.setString(4, resultSet.getString("external_id"));
        targetStatement.addBatch();
        count++;
        if (count % 1000 == 0) {
          targetStatement.executeBatch();
        }
      }
      if (count > 0) {
        targetStatement.executeBatch();
        log.debug("Migrated {} Inbox Item Targets", count);
      }
    }
  }

  private void migrateTierBoundaries(
      @NonNull Connection sourceConnection, @NonNull Connection targetConnection)
      throws SQLException {
    String sql =
        "INSERT INTO tier_boundary (id, tier, gender, min_fans, max_fans, "
            + "challenge_cost, contender_entry_fee) VALUES (?, ?, ?, ?, ?, ?, ?)";
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery(
                "SELECT id, tier, gender, min_fans, max_fans, challenge_cost, "
                    + "contender_entry_fee FROM tier_boundary");
        PreparedStatement targetStatement = targetConnection.prepareStatement(sql)) {

      int count = 0;
      while (resultSet.next()) {
        targetStatement.setLong(1, resultSet.getLong("id"));
        targetStatement.setString(2, resultSet.getString("tier"));
        targetStatement.setString(3, resultSet.getString("gender"));
        targetStatement.setLong(4, resultSet.getLong("min_fans"));
        targetStatement.setLong(5, resultSet.getLong("max_fans"));
        targetStatement.setLong(6, resultSet.getLong("challenge_cost"));
        targetStatement.setLong(7, resultSet.getLong("contender_entry_fee"));
        targetStatement.addBatch();
        count++;
        if (count % 1000 == 0) {
          targetStatement.executeBatch();
        }
      }
      if (count > 0) {
        targetStatement.executeBatch();
        log.debug("Migrated {} Tier Boundaries", count);
      }
    }
  }

  private void migrateInboxItems(
      @NonNull Connection sourceConnection, @NonNull Connection targetConnection)
      throws SQLException {
    String sql =
        "INSERT INTO inbox_item (inbox_item_id, event_type, description, "
            + "event_timestamp, is_read, external_id) VALUES (?, ?, ?, ?, ?, ?)";
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery(
                "SELECT inbox_item_id, event_type, description, event_timestamp, "
                    + "is_read, external_id FROM inbox_item");
        PreparedStatement targetStatement = targetConnection.prepareStatement(sql)) {

      int count = 0;
      while (resultSet.next()) {
        targetStatement.setLong(1, resultSet.getLong("inbox_item_id"));
        targetStatement.setString(2, resultSet.getString("event_type"));
        targetStatement.setString(3, resultSet.getString("description"));
        targetStatement.setTimestamp(4, resultSet.getTimestamp("event_timestamp"));
        targetStatement.setBoolean(5, resultSet.getBoolean("is_read"));
        targetStatement.setString(6, resultSet.getString("external_id"));
        targetStatement.addBatch();
        count++;
        if (count % 1000 == 0) {
          targetStatement.executeBatch();
        }
      }
      if (count > 0) {
        targetStatement.executeBatch();
        log.debug("Migrated {} Inbox Items", count);
      }
    }
  }

  private void migrateFeudHeatEvents(
      @NonNull Connection sourceConnection, @NonNull Connection targetConnection)
      throws SQLException {
    String sql =
        "INSERT INTO feud_heat_event (feud_heat_event_id, feud_id, "
            + "heat_change, heat_after_event, reason, event_date, creation_date) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?)";
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery(
                "SELECT feud_heat_event_id, feud_id, heat_change, "
                    + "heat_after_event, reason, event_date, creation_date FROM feud_heat_event");
        PreparedStatement targetStatement = targetConnection.prepareStatement(sql)) {

      int count = 0;
      while (resultSet.next()) {
        targetStatement.setLong(1, resultSet.getLong("feud_heat_event_id"));
        targetStatement.setLong(2, resultSet.getLong("feud_id"));
        targetStatement.setInt(3, resultSet.getInt("heat_change"));
        targetStatement.setInt(4, resultSet.getInt("heat_after_event"));
        targetStatement.setString(5, resultSet.getString("reason"));
        targetStatement.setTimestamp(6, resultSet.getTimestamp("event_date"));
        targetStatement.setTimestamp(7, resultSet.getTimestamp("creation_date"));
        targetStatement.addBatch();
        count++;
        if (count % 1000 == 0) {
          targetStatement.executeBatch();
        }
      }
      if (count > 0) {
        targetStatement.executeBatch();
        log.debug("Migrated {} Feud Heat Events", count);
      }
    }
  }

  private void migrateFeudParticipants(
      @NonNull Connection sourceConnection, @NonNull Connection targetConnection)
      throws SQLException {
    String sql =
        "INSERT INTO feud_participant (feud_participant_id, feud_id, wrestler_id, "
            + "role, is_active, joined_date, left_date, left_reason, creation_date) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery(
                "SELECT feud_participant_id, feud_id, wrestler_id, role, is_active, "
                    + "joined_date, left_date, left_reason, creation_date FROM feud_participant");
        PreparedStatement targetStatement = targetConnection.prepareStatement(sql)) {

      int count = 0;
      while (resultSet.next()) {
        targetStatement.setLong(1, resultSet.getLong("feud_participant_id"));
        targetStatement.setLong(2, resultSet.getLong("feud_id"));
        targetStatement.setLong(3, resultSet.getLong("wrestler_id"));
        targetStatement.setString(4, resultSet.getString("role"));
        targetStatement.setBoolean(5, resultSet.getBoolean("is_active"));
        targetStatement.setTimestamp(6, resultSet.getTimestamp("joined_date"));
        targetStatement.setTimestamp(7, resultSet.getTimestamp("left_date"));
        targetStatement.setString(8, resultSet.getString("left_reason"));
        targetStatement.setTimestamp(9, resultSet.getTimestamp("creation_date"));
        targetStatement.addBatch();
        count++;
        if (count % 1000 == 0) {
          targetStatement.executeBatch();
        }
      }
      if (count > 0) {
        targetStatement.executeBatch();
        log.debug("Migrated {} Feud Participants", count);
      }
    }
  }

  private void migrateMultiWrestlerFeuds(
      @NonNull Connection sourceConnection, @NonNull Connection targetConnection)
      throws SQLException {
    String sql =
        "INSERT INTO multi_wrestler_feud (multi_wrestler_feud_id, name, description, "
            + "heat, is_active, started_date, ended_date, storyline_notes, creation_date) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery(
                "SELECT multi_wrestler_feud_id, name, description, heat, is_active, started_date,"
                    + " ended_date, storyline_notes, creation_date FROM multi_wrestler_feud");
        PreparedStatement targetStatement = targetConnection.prepareStatement(sql)) {

      int count = 0;
      while (resultSet.next()) {
        targetStatement.setLong(1, resultSet.getLong("multi_wrestler_feud_id"));
        targetStatement.setString(2, resultSet.getString("name"));
        targetStatement.setString(3, resultSet.getString("description"));
        targetStatement.setInt(4, resultSet.getInt("heat"));
        targetStatement.setBoolean(5, resultSet.getBoolean("is_active"));
        targetStatement.setTimestamp(6, resultSet.getTimestamp("started_date"));
        targetStatement.setTimestamp(7, resultSet.getTimestamp("ended_date"));
        targetStatement.setString(8, resultSet.getString("storyline_notes"));
        targetStatement.setTimestamp(9, resultSet.getTimestamp("creation_date"));
        targetStatement.addBatch();
        count++;
        if (count % 1000 == 0) {
          targetStatement.executeBatch();
        }
      }
      if (count > 0) {
        targetStatement.executeBatch();
        log.debug("Migrated {} Multi-Wrestler Feuds", count);
      }
    }
  }

  private void migrateDramaEvents(
      @NonNull Connection sourceConnection, @NonNull Connection targetConnection)
      throws SQLException {
    String sql =
        "INSERT INTO drama_event (drama_event_id, title, description, event_type, "
            + "severity, event_date, creation_date, heat_impact, fan_impact, "
            + "injury_caused, rivalry_created, rivalry_ended, is_processed, "
            + "processed_date, processing_notes, primary_wrestler_id, secondary_wrestler_id) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery(
                "SELECT drama_event_id, title, description, event_type, severity, event_date,"
                    + " creation_date, heat_impact, fan_impact, injury_caused, rivalry_created,"
                    + " rivalry_ended, is_processed, processed_date, processing_notes,"
                    + " primary_wrestler_id, secondary_wrestler_id FROM drama_event");
        PreparedStatement targetStatement = targetConnection.prepareStatement(sql)) {

      int count = 0;
      while (resultSet.next()) {
        targetStatement.setLong(1, resultSet.getLong("drama_event_id"));
        targetStatement.setString(2, resultSet.getString("title"));
        targetStatement.setString(3, resultSet.getString("description"));
        targetStatement.setString(4, resultSet.getString("event_type"));
        targetStatement.setString(5, resultSet.getString("severity"));
        targetStatement.setTimestamp(6, resultSet.getTimestamp("event_date"));
        targetStatement.setTimestamp(7, resultSet.getTimestamp("creation_date"));
        targetStatement.setInt(8, resultSet.getInt("heat_impact"));
        targetStatement.setInt(9, resultSet.getInt("fan_impact"));
        targetStatement.setBoolean(10, resultSet.getBoolean("injury_caused"));
        targetStatement.setBoolean(11, resultSet.getBoolean("rivalry_created"));
        targetStatement.setBoolean(12, resultSet.getBoolean("rivalry_ended"));
        targetStatement.setBoolean(13, resultSet.getBoolean("is_processed"));
        targetStatement.setTimestamp(14, resultSet.getTimestamp("processed_date"));
        targetStatement.setString(15, resultSet.getString("processing_notes"));
        targetStatement.setLong(16, resultSet.getLong("primary_wrestler_id"));
        targetStatement.setLong(17, resultSet.getLong("secondary_wrestler_id"));
        targetStatement.addBatch();
        count++;
        if (count % 1000 == 0) {
          targetStatement.executeBatch();
        }
      }
      if (count > 0) {
        targetStatement.executeBatch();
        log.debug("Migrated {} Drama Events", count);
      }
    }
  }

  private void migrateFactionHeatEvents(
      @NonNull Connection sourceConnection, @NonNull Connection targetConnection)
      throws SQLException {
    String sql =
        "INSERT INTO faction_heat_event (faction_heat_event_id, faction_rivalry_id, "
            + "heat_change, heat_after_event, reason, event_date, creation_date) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?)";
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery(
                "SELECT faction_heat_event_id, faction_rivalry_id, heat_change, heat_after_event,"
                    + " reason, event_date, creation_date FROM faction_heat_event");
        PreparedStatement targetStatement = targetConnection.prepareStatement(sql)) {

      int count = 0;
      while (resultSet.next()) {
        targetStatement.setLong(1, resultSet.getLong("faction_heat_event_id"));
        targetStatement.setLong(2, resultSet.getLong("faction_rivalry_id"));
        targetStatement.setInt(3, resultSet.getInt("heat_change"));
        targetStatement.setInt(4, resultSet.getInt("heat_after_event"));
        targetStatement.setString(5, resultSet.getString("reason"));
        targetStatement.setTimestamp(6, resultSet.getTimestamp("event_date"));
        targetStatement.setTimestamp(7, resultSet.getTimestamp("creation_date"));
        targetStatement.addBatch();
        count++;
        if (count % 1000 == 0) {
          targetStatement.executeBatch();
        }
      }
      if (count > 0) {
        targetStatement.executeBatch();
        log.debug("Migrated {} Faction Heat Events", count);
      }
    }
  }

  private void migrateFactionRivalries(
      @NonNull Connection sourceConnection, @NonNull Connection targetConnection)
      throws SQLException {
    String sql =
        "INSERT INTO faction_rivalry (faction_rivalry_id, faction1_id, faction2_id, "
            + "heat, is_active, started_date, ended_date, storyline_notes, creation_date) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery(
                "SELECT faction_rivalry_id, faction1_id, faction2_id, heat, is_active,"
                    + " started_date, ended_date, storyline_notes, creation_date FROM"
                    + " faction_rivalry");
        PreparedStatement targetStatement = targetConnection.prepareStatement(sql)) {

      int count = 0;
      while (resultSet.next()) {
        targetStatement.setLong(1, resultSet.getLong("faction_rivalry_id"));
        targetStatement.setLong(2, resultSet.getLong("faction1_id"));
        targetStatement.setLong(3, resultSet.getLong("faction2_id"));
        targetStatement.setInt(4, resultSet.getInt("heat"));
        targetStatement.setBoolean(5, resultSet.getBoolean("is_active"));
        targetStatement.setTimestamp(6, resultSet.getTimestamp("started_date"));
        targetStatement.setTimestamp(7, resultSet.getTimestamp("ended_date"));
        targetStatement.setString(8, resultSet.getString("storyline_notes"));
        targetStatement.setTimestamp(9, resultSet.getTimestamp("creation_date"));
        targetStatement.addBatch();
        count++;
        if (count % 1000 == 0) {
          targetStatement.executeBatch();
        }
      }
      if (count > 0) {
        targetStatement.executeBatch();
        log.debug("Migrated {} Faction Rivalries", count);
      }
    }
  }

  private void migrateHeatEvents(
      @NonNull Connection sourceConnection, @NonNull Connection targetConnection)
      throws SQLException {
    String sql =
        "INSERT INTO heat_event (heat_event_id, rivalry_id, heat_change, "
            + "heat_after_event, reason, event_date, creation_date) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?)";
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery(
                "SELECT heat_event_id, rivalry_id, heat_change, "
                    + "heat_after_event, reason, event_date, creation_date FROM heat_event");
        PreparedStatement targetStatement = targetConnection.prepareStatement(sql)) {

      int count = 0;
      while (resultSet.next()) {
        targetStatement.setLong(1, resultSet.getLong("heat_event_id"));
        targetStatement.setLong(2, resultSet.getLong("rivalry_id"));
        targetStatement.setInt(3, resultSet.getInt("heat_change"));
        targetStatement.setInt(4, resultSet.getInt("heat_after_event"));
        targetStatement.setString(5, resultSet.getString("reason"));
        targetStatement.setTimestamp(6, resultSet.getTimestamp("event_date"));
        targetStatement.setTimestamp(7, resultSet.getTimestamp("creation_date"));
        targetStatement.addBatch();
        count++;
        if (count % 1000 == 0) {
          targetStatement.executeBatch();
        }
      }
      if (count > 0) {
        targetStatement.executeBatch();
        log.debug("Migrated {} Heat Events", count);
      }
    }
  }

  private void migrateRivalries(
      @NonNull Connection sourceConnection, @NonNull Connection targetConnection)
      throws SQLException {
    String sql =
        "INSERT INTO rivalry (rivalry_id, wrestler1_id, wrestler2_id, heat, is_active, "
            + "started_date, ended_date, storyline_notes, creation_date) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery(
                "SELECT rivalry_id, wrestler1_id, wrestler2_id, heat, is_active, "
                    + "started_date, ended_date, storyline_notes, creation_date FROM rivalry");
        PreparedStatement targetStatement = targetConnection.prepareStatement(sql)) {

      int count = 0;
      while (resultSet.next()) {
        targetStatement.setLong(1, resultSet.getLong("rivalry_id"));
        targetStatement.setLong(2, resultSet.getLong("wrestler1_id"));
        targetStatement.setLong(3, resultSet.getLong("wrestler2_id"));
        targetStatement.setInt(4, resultSet.getInt("heat"));
        targetStatement.setBoolean(5, resultSet.getBoolean("is_active"));
        targetStatement.setTimestamp(6, resultSet.getTimestamp("started_date"));
        targetStatement.setTimestamp(7, resultSet.getTimestamp("ended_date"));
        targetStatement.setString(8, resultSet.getString("storyline_notes"));
        targetStatement.setTimestamp(9, resultSet.getTimestamp("creation_date"));
        targetStatement.addBatch();
        count++;
        if (count % 1000 == 0) {
          targetStatement.executeBatch();
        }
      }
      if (count > 0) {
        targetStatement.executeBatch();
        log.debug("Migrated {} Rivalries", count);
      }
    }
  }

  private void migrateTitleReignChampions(
      @NonNull Connection sourceConnection, @NonNull Connection targetConnection)
      throws SQLException {
    String sql = "INSERT INTO title_reign_champion (title_reign_id, wrestler_id) VALUES (?, ?)";
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery(
                "SELECT title_reign_id, wrestler_id FROM title_reign_champion");
        PreparedStatement targetStatement = targetConnection.prepareStatement(sql)) {

      int count = 0;
      while (resultSet.next()) {
        targetStatement.setLong(1, resultSet.getLong("title_reign_id"));
        targetStatement.setLong(2, resultSet.getLong("wrestler_id"));
        targetStatement.addBatch();
        count++;
        if (count % 1000 == 0) {
          targetStatement.executeBatch();
        }
      }
      if (count > 0) {
        targetStatement.executeBatch();
        log.debug("Migrated {} Title Reign Champions", count);
      }
    }
  }

  private void migrateTitleReigns(
      @NonNull Connection sourceConnection, @NonNull Connection targetConnection)
      throws SQLException {
    String sql =
        "INSERT INTO title_reign (title_reign_id, external_id, title_id, start_date, "
            + "end_date, reign_number, notes, creation_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery(
                "SELECT title_reign_id, external_id, title_id, start_date, "
                    + "end_date, reign_number, notes, creation_date FROM title_reign");
        PreparedStatement targetStatement = targetConnection.prepareStatement(sql)) {

      int count = 0;
      while (resultSet.next()) {
        targetStatement.setLong(1, resultSet.getLong("title_reign_id"));
        targetStatement.setString(2, resultSet.getString("external_id"));
        targetStatement.setLong(3, resultSet.getLong("title_id"));
        targetStatement.setTimestamp(4, resultSet.getTimestamp("start_date"));
        targetStatement.setTimestamp(5, resultSet.getTimestamp("end_date"));
        targetStatement.setInt(6, resultSet.getInt("reign_number"));
        targetStatement.setString(7, resultSet.getString("notes"));
        targetStatement.setTimestamp(8, resultSet.getTimestamp("creation_date"));
        targetStatement.addBatch();
        count++;
        if (count % 1000 == 0) {
          targetStatement.executeBatch();
        }
      }
      if (count > 0) {
        targetStatement.executeBatch();
        log.debug("Migrated {} Title Reigns", count);
      }
    }
  }

  private void migrateTitleContenders(
      @NonNull Connection sourceConnection, @NonNull Connection targetConnection)
      throws SQLException {
    String sql = "INSERT INTO title_contender (title_id, wrestler_id) VALUES (?, ?)";
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery("SELECT title_id, wrestler_id FROM title_contender");
        PreparedStatement targetStatement = targetConnection.prepareStatement(sql)) {

      int count = 0;
      while (resultSet.next()) {
        targetStatement.setLong(1, resultSet.getLong("title_id"));
        targetStatement.setLong(2, resultSet.getLong("wrestler_id"));
        targetStatement.addBatch();
        count++;
        if (count % 1000 == 0) {
          targetStatement.executeBatch();
        }
      }
      if (count > 0) {
        targetStatement.executeBatch();
        log.debug("Migrated {} Title Contenders", count);
      }
    }
  }

  private void migrateTitleChampions(
      @NonNull Connection sourceConnection, @NonNull Connection targetConnection)
      throws SQLException {
    String sql = "INSERT INTO title_champion (title_id, wrestler_id) VALUES (?, ?)";
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery("SELECT title_id, wrestler_id FROM title_champion");
        PreparedStatement targetStatement = targetConnection.prepareStatement(sql)) {

      int count = 0;
      while (resultSet.next()) {
        targetStatement.setLong(1, resultSet.getLong("title_id"));
        targetStatement.setLong(2, resultSet.getLong("wrestler_id"));
        targetStatement.addBatch();
        count++;
        if (count % 1000 == 0) {
          targetStatement.executeBatch();
        }
      }
      if (count > 0) {
        targetStatement.executeBatch();
        log.debug("Migrated {} Title Champions", count);
      }
    }
  }

  private void migrateSegmentTitles(
      @NonNull Connection sourceConnection, @NonNull Connection targetConnection)
      throws SQLException {
    String sql = "INSERT INTO segment_title (segment_id, title_id) VALUES (?, ?)";
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery("SELECT segment_id, title_id FROM segment_title");
        PreparedStatement targetStatement = targetConnection.prepareStatement(sql)) {

      int count = 0;
      while (resultSet.next()) {
        targetStatement.setLong(1, resultSet.getLong("segment_id"));
        targetStatement.setLong(2, resultSet.getLong("title_id"));
        targetStatement.addBatch();
        count++;
        if (count % 1000 == 0) {
          targetStatement.executeBatch();
        }
      }
      if (count > 0) {
        targetStatement.executeBatch();
        log.debug("Migrated {} Segment Titles", count);
      }
    }
  }

  private void migrateTitles(
      @NonNull Connection sourceConnection, @NonNull Connection targetConnection)
      throws SQLException {
    String sql =
        "INSERT INTO title (title_id, name, description, tier, gender, is_active, "
            + "creation_date, external_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery(
                "SELECT title_id, name, description, tier, gender, is_active, "
                    + "creation_date, external_id FROM title");
        PreparedStatement targetStatement = targetConnection.prepareStatement(sql)) {

      int count = 0;
      while (resultSet.next()) {
        targetStatement.setLong(1, resultSet.getLong("title_id"));
        targetStatement.setString(2, resultSet.getString("name"));
        targetStatement.setString(3, resultSet.getString("description"));
        targetStatement.setString(4, resultSet.getString("tier"));
        targetStatement.setString(5, resultSet.getString("gender"));
        targetStatement.setBoolean(6, resultSet.getBoolean("is_active"));
        targetStatement.setTimestamp(7, resultSet.getTimestamp("creation_date"));
        targetStatement.setString(8, resultSet.getString("external_id"));
        targetStatement.addBatch();
        count++;
        if (count % 1000 == 0) {
          targetStatement.executeBatch();
        }
      }
      if (count > 0) {
        targetStatement.executeBatch();
        log.debug("Migrated {} Titles", count);
      }
    }
  }

  private void migrateSegmentSegmentRules(
      @NonNull Connection sourceConnection, @NonNull Connection targetConnection)
      throws SQLException {
    String sql = "INSERT INTO segment_segment_rule (segment_id, segment_rule_id) VALUES (?, ?)";
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery(
                "SELECT segment_id, segment_rule_id FROM segment_segment_rule");
        PreparedStatement targetStatement = targetConnection.prepareStatement(sql)) {

      int count = 0;
      while (resultSet.next()) {
        targetStatement.setLong(1, resultSet.getLong("segment_id"));
        targetStatement.setLong(2, resultSet.getLong("segment_rule_id"));
        targetStatement.addBatch();
        count++;
        if (count % 1000 == 0) {
          targetStatement.executeBatch();
        }
      }
      if (count > 0) {
        targetStatement.executeBatch();
        log.debug("Migrated {} Segment's Segment Rules", count);
      }
    }
  }

  private void migrateSegmentParticipants(
      @NonNull Connection sourceConnection, @NonNull Connection targetConnection)
      throws SQLException {
    String sql =
        "INSERT INTO segment_participant (segment_participant_id, segment_id, "
            + "wrestler_id, is_winner) VALUES (?, ?, ?, ?)";
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery(
                "SELECT segment_participant_id, segment_id, wrestler_id, "
                    + "is_winner FROM segment_participant");
        PreparedStatement targetStatement = targetConnection.prepareStatement(sql)) {

      int count = 0;
      while (resultSet.next()) {
        targetStatement.setLong(1, resultSet.getLong("segment_participant_id"));
        targetStatement.setLong(2, resultSet.getLong("segment_id"));
        targetStatement.setLong(3, resultSet.getLong("wrestler_id"));
        targetStatement.setBoolean(4, resultSet.getBoolean("is_winner"));
        targetStatement.addBatch();
        count++;
        if (count % 1000 == 0) {
          targetStatement.executeBatch();
        }
      }
      if (count > 0) {
        targetStatement.executeBatch();
        log.debug("Migrated {} Segment Participants", count);
      }
    }
  }

  private void migrateSegments(
      @NonNull Connection sourceConnection, @NonNull Connection targetConnection)
      throws SQLException {
    String sql =
        "INSERT INTO segment (segment_id, show_id, segment_type_id, winner_id, "
            + "segment_date, duration_minutes, segment_rating, status, narration, "
            + "summary, is_title_segment, is_npc_generated, external_id) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery(
                "SELECT segment_id, show_id, segment_type_id, winner_id, "
                    + "segment_date, duration_minutes, segment_rating, status, narration, "
                    + "summary, is_title_segment, is_npc_generated, external_id FROM segment");
        PreparedStatement targetStatement = targetConnection.prepareStatement(sql)) {

      int count = 0;
      while (resultSet.next()) {
        targetStatement.setLong(1, resultSet.getLong("segment_id"));
        targetStatement.setLong(2, resultSet.getLong("show_id"));
        targetStatement.setLong(3, resultSet.getLong("segment_type_id"));
        if (resultSet.getObject("winner_id") != null) {
          targetStatement.setLong(4, resultSet.getLong("winner_id"));
        } else {
          targetStatement.setObject(4, null);
        }
        targetStatement.setTimestamp(5, resultSet.getTimestamp("segment_date"));
        targetStatement.setInt(6, resultSet.getInt("duration_minutes"));
        targetStatement.setInt(7, resultSet.getInt("segment_rating"));
        targetStatement.setString(8, resultSet.getString("status"));
        targetStatement.setString(9, resultSet.getString("narration"));
        targetStatement.setString(10, resultSet.getString("summary"));
        targetStatement.setBoolean(11, resultSet.getBoolean("is_title_segment"));
        targetStatement.setBoolean(12, resultSet.getBoolean("is_npc_generated"));
        targetStatement.setString(13, resultSet.getString("external_id"));
        targetStatement.addBatch();
        count++;
        if (count % 1000 == 0) {
          targetStatement.executeBatch();
        }
      }
      if (count > 0) {
        targetStatement.executeBatch();
        log.debug("Migrated {} Segments", count);
      }
    }
  }

  private void migrateSegmentRules(
      @NonNull Connection sourceConnection, @NonNull Connection targetConnection)
      throws SQLException {
    String sql =
        "INSERT INTO segment_rule (segment_rule_id, name, description, "
            + "requires_high_heat, creation_date) VALUES (?, ?, ?, ?, ?)";
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery(
                "SELECT segment_rule_id, name, description, "
                    + "requires_high_heat, creation_date FROM segment_rule");
        PreparedStatement targetStatement = targetConnection.prepareStatement(sql)) {

      int count = 0;
      while (resultSet.next()) {
        targetStatement.setLong(1, resultSet.getLong("segment_rule_id"));
        targetStatement.setString(2, resultSet.getString("name"));
        targetStatement.setString(3, resultSet.getString("description"));
        targetStatement.setBoolean(4, resultSet.getBoolean("requires_high_heat"));
        targetStatement.setTimestamp(5, resultSet.getTimestamp("creation_date"));
        targetStatement.addBatch();
        count++;
        if (count % 1000 == 0) {
          targetStatement.executeBatch();
        }
      }
      if (count > 0) {
        targetStatement.executeBatch();
        log.debug("Migrated {} Segment Rules", count);
      }
    }
  }

  private void migrateSegmentTypes(
      @NonNull Connection sourceConnection, @NonNull Connection targetConnection)
      throws SQLException {
    String sql =
        "INSERT INTO segment_type (segment_type_id, name, description, creation_date) "
            + "VALUES (?, ?, ?, ?)";
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery(
                "SELECT segment_type_id, name, description, creation_date FROM segment_type");
        PreparedStatement targetStatement = targetConnection.prepareStatement(sql)) {

      int count = 0;
      while (resultSet.next()) {
        targetStatement.setLong(1, resultSet.getLong("segment_type_id"));
        targetStatement.setString(2, resultSet.getString("name"));
        targetStatement.setString(3, resultSet.getString("description"));
        targetStatement.setTimestamp(4, resultSet.getTimestamp("creation_date"));
        targetStatement.addBatch();
        count++;
        if (count % 1000 == 0) {
          targetStatement.executeBatch();
        }
      }
      if (count > 0) {
        targetStatement.executeBatch();
        log.debug("Migrated {} Segment Types", count);
      }
    }
  }

  private void migrateShows(
      @NonNull Connection sourceConnection, @NonNull Connection targetConnection)
      throws SQLException {
    String sql =
        "INSERT INTO `show` (show_id, name, description, show_date, show_type_id, "
            + "season_id, template_id, external_id, creation_date) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery(
                "SELECT show_id, name, description, show_date, show_type_id, "
                    + "season_id, template_id, external_id, creation_date FROM `show`");
        PreparedStatement targetStatement = targetConnection.prepareStatement(sql)) {

      int count = 0;
      while (resultSet.next()) {
        targetStatement.setLong(1, resultSet.getLong("show_id"));
        targetStatement.setString(2, resultSet.getString("name"));
        targetStatement.setString(3, resultSet.getString("description"));
        targetStatement.setTimestamp(4, resultSet.getTimestamp("show_date"));
        targetStatement.setLong(5, resultSet.getLong("show_type_id"));
        targetStatement.setLong(6, resultSet.getLong("season_id"));
        targetStatement.setLong(7, resultSet.getLong("template_id"));
        targetStatement.setString(8, resultSet.getString("external_id"));
        targetStatement.setTimestamp(9, resultSet.getTimestamp("creation_date"));
        targetStatement.addBatch();
        count++;
        if (count % 1000 == 0) {
          targetStatement.executeBatch();
        }
      }
      if (count > 0) {
        targetStatement.executeBatch();
        log.debug("Migrated {} Shows", count);
      }
    }
  }

  private void migrateShowTemplates(
      @NonNull Connection sourceConnection, @NonNull Connection targetConnection)
      throws SQLException {
    String sql =
        "INSERT INTO show_template (template_id, name, description, show_type_id, "
            + "notion_url, external_id, creation_date) VALUES (?, ?, ?, ?, ?, ?, ?)";
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery(
                "SELECT template_id, name, description, show_type_id, "
                    + "notion_url, external_id, creation_date FROM show_template");
        PreparedStatement targetStatement = targetConnection.prepareStatement(sql)) {

      int count = 0;
      while (resultSet.next()) {
        targetStatement.setLong(1, resultSet.getLong("template_id"));
        targetStatement.setString(2, resultSet.getString("name"));
        targetStatement.setString(3, resultSet.getString("description"));
        targetStatement.setLong(4, resultSet.getLong("show_type_id"));
        targetStatement.setString(5, resultSet.getString("notion_url"));
        targetStatement.setString(6, resultSet.getString("external_id"));
        targetStatement.setTimestamp(7, resultSet.getTimestamp("creation_date"));
        targetStatement.addBatch();
        count++;
        if (count % 1000 == 0) {
          targetStatement.executeBatch();
        }
      }
      if (count > 0) {
        targetStatement.executeBatch();
        log.debug("Migrated {} Show Templates", count);
      }
    }
  }

  private void migrateShowTypes(
      @NonNull Connection sourceConnection, @NonNull Connection targetConnection)
      throws SQLException {
    String sql =
        "INSERT INTO show_type (show_type_id, name, description, is_ppv, creation_date) "
            + "VALUES (?, ?, ?, ?, ?)";
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery(
                "SELECT show_type_id, name, description, is_ppv, creation_date FROM show_type");
        PreparedStatement targetStatement = targetConnection.prepareStatement(sql)) {

      int count = 0;
      while (resultSet.next()) {
        targetStatement.setLong(1, resultSet.getLong("show_type_id"));
        targetStatement.setString(2, resultSet.getString("name"));
        targetStatement.setString(3, resultSet.getString("description"));
        targetStatement.setBoolean(4, resultSet.getBoolean("is_ppv"));
        targetStatement.setTimestamp(5, resultSet.getTimestamp("creation_date"));
        targetStatement.addBatch();
        count++;
        if (count % 1000 == 0) {
          targetStatement.executeBatch();
        }
      }
      if (count > 0) {
        targetStatement.executeBatch();
        log.debug("Migrated {} Show Types", count);
      }
    }
  }

  private void migrateSeasons(
      @NonNull Connection sourceConnection, @NonNull Connection targetConnection)
      throws SQLException {
    String sql =
        "INSERT INTO season (season_id, name, description, start_date, end_date, is_active, "
            + "creation_date, notion_id, shows_per_ppv) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery(
                "SELECT season_id, name, description, start_date, end_date, is_active, "
                    + "creation_date, notion_id, shows_per_ppv FROM season");
        PreparedStatement targetStatement = targetConnection.prepareStatement(sql)) {

      int count = 0;
      while (resultSet.next()) {
        targetStatement.setLong(1, resultSet.getLong("season_id"));
        targetStatement.setString(2, resultSet.getString("name"));
        targetStatement.setString(3, resultSet.getString("description"));
        targetStatement.setDate(4, resultSet.getDate("start_date"));
        targetStatement.setDate(5, resultSet.getDate("end_date"));
        targetStatement.setBoolean(6, resultSet.getBoolean("is_active"));
        targetStatement.setTimestamp(7, resultSet.getTimestamp("creation_date"));
        targetStatement.setString(8, resultSet.getString("notion_id"));
        targetStatement.setInt(9, resultSet.getInt("shows_per_ppv"));
        targetStatement.addBatch();
        count++;
        if (count % 1000 == 0) {
          targetStatement.executeBatch();
        }
      }
      if (count > 0) {
        targetStatement.executeBatch();
        log.debug("Migrated {} Seasons", count);
      }
    }
  }

  private void truncateAllTables(@NonNull Connection connection) throws SQLException {
    try (Statement showTablesStatement = connection.createStatement();
        Statement truncateStatement = connection.createStatement()) {
      // For MySQL, disable foreign key checks
      truncateStatement.execute("SET FOREIGN_KEY_CHECKS = 0");

      ResultSet resultSet = showTablesStatement.executeQuery("SHOW TABLES");
      while (resultSet.next()) {
        String tableName = resultSet.getString(1);
        truncateStatement.executeUpdate("TRUNCATE TABLE `" + tableName + "`");
      }
      resultSet.close();

      // For MySQL, re-enable foreign key checks
      truncateStatement.execute("SET FOREIGN_KEY_CHECKS = 1");
    }
  }

  private void migrateRoles(
      @NonNull Connection sourceConnection, @NonNull Connection targetConnection)
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
        log.debug("Migrated {} Roles", count);
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
        log.debug("Migrated {} Accounts", count);
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
        log.debug("Migrated {} Account Roles", count);
      }
    }
  }

  private void migrateNpcs(
      @NonNull Connection sourceConnection, @NonNull Connection targetConnection)
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
        log.debug("Migrated {} NPCs", count);
      }
    }
  }

  private void migrateFactions(
      @NonNull Connection sourceConnection, @NonNull Connection targetConnection)
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
        log.debug("Migrated {} Factions", count);
      }
    }
  }

  private void migrateWrestlers(
      @NonNull Connection sourceConnection, @NonNull Connection targetConnection)
      throws SQLException {
    String sql =
        "INSERT INTO wrestler (wrestler_id, NAME, STARTING_STAMINA, LOW_STAMINA, "
            + "STARTING_HEALTH, LOW_HEALTH, DECK_SIZE, CREATION_DATE, EXTERNAL_ID, "
            + "FANS, TIER, BUMPS, CURRENT_HEALTH, IS_PLAYER, GENDER, DESCRIPTION, FACTION_ID) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    try (Statement sourceStatement = sourceConnection.createStatement();
        ResultSet resultSet =
            sourceStatement.executeQuery(
                "SELECT wrestler_id, NAME, STARTING_STAMINA, LOW_STAMINA, STARTING_HEALTH,"
                    + " LOW_HEALTH, DECK_SIZE, CREATION_DATE, EXTERNAL_ID, FANS, TIER, BUMPS,"
                    + " CURRENT_HEALTH, IS_PLAYER, GENDER, DESCRIPTION, FACTION_ID FROM wrestler");
        PreparedStatement targetStatement = targetConnection.prepareStatement(sql)) {

      int count = 0;
      while (resultSet.next()) {
        targetStatement.setLong(1, resultSet.getLong("wrestler_id"));
        targetStatement.setString(2, resultSet.getString("NAME"));
        targetStatement.setInt(3, resultSet.getInt("STARTING_STAMINA"));
        targetStatement.setInt(4, resultSet.getInt("LOW_STAMINA"));
        targetStatement.setInt(5, resultSet.getInt("STARTING_HEALTH"));
        targetStatement.setInt(6, resultSet.getInt("LOW_HEALTH"));
        targetStatement.setInt(7, resultSet.getInt("DECK_SIZE"));
        targetStatement.setTimestamp(8, resultSet.getTimestamp("CREATION_DATE"));
        targetStatement.setString(9, resultSet.getString("EXTERNAL_ID"));
        targetStatement.setLong(10, resultSet.getLong("FANS"));
        targetStatement.setString(11, resultSet.getString("TIER"));
        targetStatement.setInt(12, resultSet.getInt("BUMPS"));
        targetStatement.setInt(13, resultSet.getInt("CURRENT_HEALTH"));
        targetStatement.setBoolean(14, resultSet.getBoolean("IS_PLAYER"));
        targetStatement.setString(15, resultSet.getString("GENDER"));
        targetStatement.setString(16, resultSet.getString("DESCRIPTION"));
        if (resultSet.getObject("FACTION_ID") != null) {
          targetStatement.setLong(17, resultSet.getLong("FACTION_ID"));
        } else {
          targetStatement.setObject(17, null);
        }
        targetStatement.addBatch();
        count++;
        if (count % 1000 == 0) {
          targetStatement.executeBatch();
        }
      }
      if (count > 0) {
        targetStatement.executeBatch();
        log.debug("Migrated {} Wrestlers", count);
      }
    }
  }

  private void migrateInjuryTypes(
      @NonNull Connection sourceConnection, @NonNull Connection targetConnection)
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
        log.debug("Migrated {} Injury Types", count);
      }
    }
  }

  private void migrateInjuries(
      @NonNull Connection sourceConnection, @NonNull Connection targetConnection)
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
        log.debug("Migrated {} Injuries", count);
      }
    }
  }

  private void migrateTeams(
      @NonNull Connection sourceConnection, @NonNull Connection targetConnection)
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
        log.debug("Migrated {} Teams", count);
      }
    }
  }

  private void migrateCardSets(
      @NonNull Connection sourceConnection, @NonNull Connection targetConnection)
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
        log.debug("Migrated {} Card Sets", count);
      }
    }
  }

  private void migrateCards(
      @NonNull Connection sourceConnection, @NonNull Connection targetConnection)
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
        log.debug("Migrated {} Cards", count);
      }
    }
  }

  private void migrateDecks(
      @NonNull Connection sourceConnection, @NonNull Connection targetConnection)
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
        log.debug("Migrated {} Decks", count);
      }
    }
  }

  private void migrateDeckCards(
      @NonNull Connection sourceConnection, @NonNull Connection targetConnection)
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
        log.debug("Migrated {} Deck Cards", count);
      }
    }
  }
}
