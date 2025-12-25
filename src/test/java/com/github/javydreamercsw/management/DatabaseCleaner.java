/*
* Copyright (C) 2025 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Table;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cleans the database by disabling foreign key constraints, truncating all tables, and then
 * re-enabling the constraints. This is a robust method for ensuring a clean state between tests.
 */
@Service
@Slf4j
public class DatabaseCleaner {

  @Autowired private EntityManager entityManager;

  /**
   * Disables foreign key constraints, truncates all tables managed by the entity manager, and then
   * re-enables the constraints.
   */
  @Transactional
  public void clearDatabase() {
    log.info("🧹 Starting robust database cleanup...");

    // Detach all entities to prevent any lingering state issues
    entityManager.clear();

    // Get all table names from the entity metadata
    List<String> tableNames =
        entityManager.getMetamodel().getEntities().stream()
            .map(
                entityType -> {
                  Table tableAnnotation = entityType.getJavaType().getAnnotation(Table.class);
                  return tableAnnotation != null ? tableAnnotation.name() : null;
                })
            .filter(java.util.Objects::nonNull)
            .toList();

    // Add tables that might not be directly mapped as entities but have records
    // (e.g., join tables)
    // This part might need manual adjustment if you have complex table naming schemes
    // or tables not represented by entities. For now, we assume simple cases.
    // A more robust solution might query the database schema directly.

    entityManager.flush();

    // Disable foreign key constraints
    entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
    log.debug("REFERENTIAL_INTEGRITY set to FALSE");

    // Truncate all tables
    for (String tableName : tableNames) {
      try {
        log.debug("Truncating table: {}", tableName);
        entityManager
            .createNativeQuery("TRUNCATE TABLE " + tableName + " RESTART IDENTITY")
            .executeUpdate();
      } catch (Exception e) {
        log.warn("Could not truncate table {}: {}", tableName, e.getMessage());
      }
    }

    // Re-enable foreign key constraints
    entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
    log.debug("REFERENTIAL_INTEGRITY set to TRUE");

    log.info("✨ Robust database cleanup completed.");
  }
}
