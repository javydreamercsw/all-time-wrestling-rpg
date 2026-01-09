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

import java.util.HashMap;
import java.util.Map;

public class DatabaseManagerFactory {

  private static final Map<String, DatabaseManager> overrides = new HashMap<>();

  private DatabaseManagerFactory() {
    // Private constructor to prevent instantiation
  }

  public static void overrideDatabaseManager(String dbType, DatabaseManager manager) {
    overrides.put(dbType.toUpperCase(), manager);
  }

  public static void reset() {
    overrides.clear();
  }

  public static DatabaseManager getDatabaseManager(String dbType) {
    if (overrides.containsKey(dbType.toUpperCase())) {
      return overrides.get(dbType.toUpperCase());
    }
    if ("H2".equalsIgnoreCase(dbType)) {
      return new H2DatabaseManager();
    } else if ("MySQL".equalsIgnoreCase(dbType)) {
      // Placeholder values - these will be replaced with proper configuration
      String url = "jdbc:mysql://localhost:3306/alltimewrestlingrpg";
      String user = "user";
      String password = "password";
      return new MySQLDatabaseManager(url, user, password);
    } else {
      throw new IllegalArgumentException("Unsupported database type: " + dbType);
    }
  }
}
