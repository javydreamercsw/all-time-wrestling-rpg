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
package com.github.javydreamercsw.management.domain.campaign;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.management.test.AbstractIntegrationTest;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CampaignMigrationTest extends AbstractIntegrationTest {

  @Autowired private DataSource dataSource;

  @Test
  void testCampaignTablesExist() throws SQLException {
    try (Connection connection = dataSource.getConnection()) {
      ResultSet tables =
          connection.getMetaData().getTables(null, "PUBLIC", null, new String[] {"TABLE"});
      Set<String> tableNames = new HashSet<>();
      while (tables.next()) {
        tableNames.add(tables.getString("TABLE_NAME").toUpperCase());
      }

      assertThat(tableNames)
          .contains("CAMPAIGN", "CAMPAIGN_STATE", "WRESTLER_ALIGNMENT", "BACKSTAGE_ACTION_HISTORY");
    }
  }
}
