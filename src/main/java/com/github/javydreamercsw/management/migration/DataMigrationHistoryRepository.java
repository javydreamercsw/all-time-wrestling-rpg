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
package com.github.javydreamercsw.management.migration;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataMigrationHistoryRepository extends JpaRepository<DataMigrationHistory, Long> {

  Optional<DataMigrationHistory> findByMigrationId(String migrationId);

  /**
   * The most recently recorded migration row — its {@code app_version} is the last recorded app
   * version (all rows written during one boot carry that boot's version). Ordered by recorded
   * timestamp, not by a version-string comparison, which would not hold across version schemes.
   */
  Optional<DataMigrationHistory> findTopByOrderByExecutedAtDesc();
}
