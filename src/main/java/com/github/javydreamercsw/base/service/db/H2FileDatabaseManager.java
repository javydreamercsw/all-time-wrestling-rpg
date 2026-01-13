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
import java.sql.SQLException;
import lombok.Getter;
import lombok.NonNull;

@Getter
public class H2FileDatabaseManager implements DatabaseManager {

  private final String dbFilePath;
  @NonNull private final String user;
  @NonNull private final String password;

  public H2FileDatabaseManager(
      @NonNull String dbFilePath, @NonNull String user, @NonNull String password) {
    this.dbFilePath = dbFilePath;
    this.user = user;
    this.password = password;
  }

  public String getURL() {
    return dbFilePath + ";DB_CLOSE_DELAY=-1";
  }

  @Override
  public void testConnection() throws SQLException {
    try (Connection conn = getConnection()) {
      // Connection successful, do nothing
    }
  }

  @Override
  public Connection getConnection() throws SQLException {
    return getConnection(password);
  }

  @Override
  public Connection getConnection(String password) throws SQLException {
    return DriverManager.getConnection(getURL(), user, password);
  }
}
