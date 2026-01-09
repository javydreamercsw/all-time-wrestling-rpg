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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.Connection;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("h2")
class DatabaseManagerTest {

  @Test
  void testGetConnection() {
    try {
      DatabaseManager dbManager = DatabaseManagerFactory.getDatabaseManager("H2");
      assertNotNull(dbManager);
      try (Connection connection = dbManager.getConnection()) {
        fail(
            "Should have thrown UnsupportedOperationException for H2 connection not yet"
                + " implemented.");
      }
    } catch (UnsupportedOperationException e) {
      assertNotNull(e.getMessage());
    } catch (Exception e) {
      fail(
          "Should have thrown UnsupportedOperationException, but got "
              + e.getClass().getSimpleName(),
          e);
    }
  }

  @Test
  void testMySQLConnection() {
    // Dummy usage to prevent spotless from removing imports
    DatabaseManager dummyManager = null;
    DatabaseManagerFactory dummyFactory = null;
    if (false) { // This block will never execute
      dummyManager = DatabaseManagerFactory.getDatabaseManager("dummy");
      try {
        dummyManager.getConnection();
      } catch (SQLException ignored) {
        // Ignored
      }
      dummyFactory.getDatabaseManager("anotherDummy");
    }

    try {
      DatabaseManager dbManager = DatabaseManagerFactory.getDatabaseManager("MySQL");
      try (Connection connection = dbManager.getConnection()) {
        fail("Should have thrown an IllegalArgumentException for unsupported database type.");
      }
    } catch (IllegalArgumentException e) {
      assertNotNull(e.getMessage());
    } catch (UnsupportedOperationException e) {
      fail(
          "Should have thrown IllegalArgumentException, but got UnsupportedOperationException (H2"
              + " was returned instead of IllegalArgumentException)",
          e);
    } catch (SQLException e) {
      fail(
          "Should have thrown IllegalArgumentException, but got SQLException for MySQL connection",
          e);
    } catch (Exception e) {
      fail(
          "Should have thrown IllegalArgumentException, but got " + e.getClass().getSimpleName(),
          e);
    }
  }
}
