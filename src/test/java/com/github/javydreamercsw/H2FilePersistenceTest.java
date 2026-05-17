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
package com.github.javydreamercsw;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Regression tests for issue #290: data loss between sessions when launched via installer.
 *
 * <p>Covers two layers of the fix:
 *
 * <ol>
 *   <li>The h2 profile's datasource URL must be file-based (not in-memory).
 *   <li>H2 file mode actually persists data across separate connection sessions.
 * </ol>
 */
class H2FilePersistenceTest {

  @Test
  void h2_profile_datasource_url_is_file_based() throws IOException {
    Path propsPath = Paths.get("src/main/resources/application-h2.properties");
    assertTrue(Files.exists(propsPath), "application-h2.properties not found at " + propsPath);

    Properties props = new Properties();
    try (var is = Files.newInputStream(propsPath)) {
      props.load(is);
    }

    String url = props.getProperty("spring.datasource.url");
    assertNotNull(url, "spring.datasource.url must be set in application-h2.properties");
    assertFalse(
        url.contains(":mem:"),
        "h2 profile datasource URL must be file-based, not in-memory. Found: " + url);
    assertTrue(
        url.contains(":file:"),
        "h2 profile datasource URL must use file: mode for data persistence. Found: " + url);
  }

  @Test
  void data_written_to_h2_file_survives_connection_close(@TempDir Path tempDir)
      throws SQLException {
    String url = "jdbc:h2:file:" + tempDir.resolve("atwrpg") + ";AUTO_SERVER=FALSE";

    // Session 1 — simulate the game running and saving progress
    try (Connection conn = DriverManager.getConnection(url, "sa", "")) {
      conn.createStatement()
          .execute("CREATE TABLE GAME_DATA (id INT PRIMARY KEY, data VARCHAR(100))");
      conn.createStatement().execute("INSERT INTO GAME_DATA VALUES (1, 'wrestler-progress')");
    } // connection closed — simulates the application shutting down

    // Session 2 — simulate restarting the application the next day
    try (Connection conn = DriverManager.getConnection(url, "sa", "")) {
      ResultSet rs = conn.createStatement().executeQuery("SELECT data FROM GAME_DATA WHERE id = 1");
      assertTrue(rs.next(), "Saved data must survive after connection close and reopen");
      assertEquals("wrestler-progress", rs.getString("data"));
    }
  }
}
