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

import com.github.javydreamercsw.base.AccountInitializer;
import com.github.javydreamercsw.base.security.GeneralSecurityUtils;
import com.github.javydreamercsw.management.service.sync.EntityDependencyAnalyzer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of DatabaseCleanup using Spring's ApplicationContext to find all repositories and
 * clear them in the correct order.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DatabaseCleaner implements DatabaseCleanup {

  private final ApplicationContext applicationContext;
  private final AccountInitializer accountInitializer;
  private final EntityDependencyAnalyzer dependencyAnalyzer;
  private final JdbcTemplate jdbcTemplate;

  @PersistenceContext private EntityManager entityManager;

  @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
  @Transactional
  @Override
  public void clearRepositories() {
    log.info("🧹 Starting database cleanup...");

    // Detach all managed entities to prevent dirty state from interfering with cleanup.
    entityManager.clear();

    // 1. First break known circular dependencies and clear tables with many-to-many relationships
    breakCircularDependencies();

    // 2. Discover all JPA repositories
    Map<String, JpaRepository<?, ?>> repositories = discoverRepositories();

    // 3. Get the correct synchronization/deletion order
    List<String> syncOrder = dependencyAnalyzer.determineSyncOrder(getEntityClasses());
    log.info("🔄 Deletion order: {}", syncOrder);

    // Entities that should not be cleared (static config, core roles, etc.)
    Set<String> protectedEntities =
        new HashSet<>(
            Arrays.asList(
                "showtype",
                "segmenttype",
                "segmentrule",
                "cardset",
                "card",
                "campaignabilitycard",
                "campaignupgrade",
                "holiday",
                "injurytype"));

    // Delete data in the correct order (reverse of sync order)
    int deletedCount = 0;
    for (int i = syncOrder.size() - 1; i >= 0; i--) {
      String entityName = syncOrder.get(i);
      if (protectedEntities.contains(entityName.toLowerCase())) {
        log.debug("🛡️ Skipping protected entity: {}", entityName);
        continue;
      }

      JpaRepository<?, ?> repository = repositories.get(entityName.toLowerCase());
      if (repository != null) {
        if (repository.count() > 0) {
          repository.deleteAllInBatch();
          resetSequence(entityName);
          deletedCount++;
        }
      }
    }

    // Clean up any remaining repositories not in the entity list
    for (Map.Entry<String, JpaRepository<?, ?>> entry : repositories.entrySet()) {
      if (!syncOrder.contains(entry.getKey())
          && !protectedEntities.contains(entry.getKey().toLowerCase())) {
        if (entry.getValue().count() > 0) {
          entry.getValue().deleteAllInBatch();
          resetSequence(entry.getKey());
          deletedCount++;
        }
      }
    }

    // Re-initialize accounts to ensure admin, booker, etc. are available
    GeneralSecurityUtils.runAsAdmin(accountInitializer::init);

    entityManager.flush();
    entityManager.clear();
    log.info("✨ Database cleanup completed. Cleared {} repositories", deletedCount);
  }

  /**
   * Manually breaks known circular dependencies by setting foreign keys to NULL. This is necessary
   * before batch deletion to avoid constraint violations.
   */
  private void breakCircularDependencies() {
    log.info("💔 Breaking circular dependencies...");
    try {
      entityManager.createNativeQuery("DELETE FROM heat_event").executeUpdate();
      entityManager.createNativeQuery("DELETE FROM wrestler_state").executeUpdate();
      entityManager.createNativeQuery("DELETE FROM team").executeUpdate();
      entityManager.createNativeQuery("DELETE FROM wrestler_relationship").executeUpdate();
      log.debug("✅ Cleared heat_event, wrestler_state, team and wrestler_relationship tables");
    } catch (Exception e) {
      log.warn("Could not clear relationship tables: {}", e.getMessage());
    }
    try {
      entityManager.createNativeQuery("UPDATE faction SET leader_id = NULL").executeUpdate();
      log.debug("✅ Nullified faction.leader_id");
    } catch (Exception e) {
      log.warn("Could not nullify faction.leader_id: {}", e.getMessage());
    }
    try {
      entityManager.createNativeQuery("DELETE FROM league_roster").executeUpdate();
      entityManager.createNativeQuery("DELETE FROM league_membership").executeUpdate();
      log.debug("✅ Cleared league_roster and league_membership");
    } catch (Exception e) {
      log.warn("Could not clear league tables: {}", e.getMessage());
    }
  }

  private void resetSequence(String tableName) {
    try {
      String sql = "ALTER TABLE " + tableName + " ALTER COLUMN id RESTART WITH 1";
      if (tableName.equalsIgnoreCase("wrestler")) {
        sql = "ALTER TABLE wrestler ALTER COLUMN wrestler_id RESTART WITH 1";
      } else if (tableName.equalsIgnoreCase("show")) {
        sql = "ALTER TABLE show ALTER COLUMN show_id RESTART WITH 1";
      }

      jdbcTemplate.execute(sql);
      log.trace("✅ Reset sequence for table: {}", tableName);
    } catch (Exception e) {
      log.trace("Could not reset sequence for table {}: {}", tableName, e.getMessage());
    }
  }

  private Set<Class<?>> getEntityClasses() {
    Set<Class<?>> entityClasses = new HashSet<>();
    Set<jakarta.persistence.metamodel.EntityType<?>> entities =
        entityManager.getMetamodel().getEntities();

    for (jakarta.persistence.metamodel.EntityType<?> entity : entities) {
      entityClasses.add(entity.getJavaType());
    }

    return entityClasses;
  }

  /** Uses ApplicationContext to find all JpaRepository beans. */
  private Map<String, JpaRepository<?, ?>> discoverRepositories() {
    Map<String, JpaRepository<?, ?>> repositories = new HashMap<>();
    String[] beanNames = applicationContext.getBeanNamesForType(JpaRepository.class);

    for (String beanName : beanNames) {
      // Repository names are typically entityNameRepository
      String entityName = beanName.replace("Repository", "").toLowerCase();
      repositories.put(entityName, (JpaRepository<?, ?>) applicationContext.getBean(beanName));
    }

    return repositories;
  }
}
