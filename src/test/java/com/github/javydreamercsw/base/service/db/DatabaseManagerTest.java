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
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class DatabaseManagerTest {

  @Container
  private static final MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:8.0.26");

  @Test
  void testGetConnection() {
    try {
      DatabaseManager dbManager = DatabaseManagerFactory.getDatabaseManager("H2");
      assertNotNull(dbManager);
      try (Connection connection = dbManager.getConnection()) {
        assertNotNull(connection);
      }
    } catch (Exception e) {
      fail("Should not have thrown an exception, but got " + e.getClass().getSimpleName(), e);
    }
  }

  @Test
  void testMySQLConnection() {
    try {
      DatabaseManager dbManager =
          new MySQLDatabaseManager(
              mySQLContainer.getJdbcUrl(),
              mySQLContainer.getUsername(),
              mySQLContainer.getPassword());
      try (Connection connection = dbManager.getConnection()) {
        assertNotNull(connection);
      }
    } catch (SQLException e) {
      fail("Should not have thrown SQLException for MySQL connection", e);
    } catch (Exception e) {
      fail("Should not have thrown an exception, but got " + e.getClass().getSimpleName(), e);
    }
  }
}
