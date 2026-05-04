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
import jakarta.persistence.JoinTable;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

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
  private final TransactionTemplate transactionTemplate;
  private final Environment environment;

  @PersistenceContext private EntityManager entityManager;

  private Map<String, JpaRepository<?, ?>> cachedRepositories;
  private List<String> cachedSyncOrder;
  private Set<Class<?>> cachedEntityClasses;

  @Override
  public void clearRepositories() {
    log.info("🧹 Starting database cleanup...");

    // Ensure we have admin privileges for this manual execution
    GeneralSecurityUtils.runAsAdmin(
        () -> {
          // 1. First break known circular dependencies and clear tables with many-to-many
          // relationships
          breakCircularDependencies();

          // 2. Clear join tables (defined via @JoinTable)
          clearJoinTables();

          // 3. Discover all JPA repositories (with caching)
          if (cachedRepositories == null) {
            cachedRepositories = discoverRepositories();
            log.debug("📦 Discovered and cached {} repositories", cachedRepositories.size());
          }

          // 4. Get the correct synchronization/deletion order (with caching)
          if (cachedSyncOrder == null) {
            cachedEntityClasses = getEntityClasses();
            cachedSyncOrder = dependencyAnalyzer.determineSyncOrder(cachedEntityClasses);
            // Reverse the order for deletion (delete children before parents)
            List<String> reverseOrder = new ArrayList<>(cachedSyncOrder);
            Collections.reverse(reverseOrder);
            cachedSyncOrder = reverseOrder;
            log.debug("🔄 Calculated and cached deletion order: {}", cachedSyncOrder);
          }

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
                      "injurytype",
                      "location",
                      "arena",
                      "npc",
                      "faction",
                      "title",
                      "achievement",
                      "ringsideaction",
                      "ringsideactiontype",
                      "commentator",
                      "commentaryteam",
                      "role"));

          // Determine if we should use surgical cleanup (optimized for E2E)
          // or full cleanup (safer for Integration Tests)
          boolean isE2E = Arrays.asList(environment.getActiveProfiles()).contains("e2e");
          log.debug("🧹 Cleanup mode: {}", isE2E ? "SURGICAL (E2E)" : "FULL (Integration)");

          // Delete data in the correct order
          int deletedCount = 0;
          for (String entityName : cachedSyncOrder) {
            if (protectedEntities.contains(entityName.toLowerCase())) {
              log.debug("🛡️ Skipping protected entity: {}", entityName);
              continue;
            }

            // Special handling for Entities that might have both master data and test data:
            // we want to keep the master data (with externalId) but remove test data (without
            // externalId).
            // ONLY apply this optimization in E2E tests to avoid state leakage in ITs.
            if (isE2E
                && ("wrestler".equalsIgnoreCase(entityName)
                    || "team".equalsIgnoreCase(entityName)
                    || "faction".equalsIgnoreCase(entityName)
                    || "npc".equalsIgnoreCase(entityName)
                    || "arena".equalsIgnoreCase(entityName)
                    || "location".equalsIgnoreCase(entityName))) {
              log.debug("🧹 Surgical cleanup for {}...", entityName);
              int deleted =
                  executeNativeUpdate(
                      "DELETE FROM "
                          + entityName.toLowerCase()
                          + " WHERE external_id IS NULL OR external_id = ''");
              if (deleted >= 0) {
                log.debug("✅ Deleted {} test {}", deleted, entityName);
                deletedCount++;
                continue; // Skip the full repository deletion below
              }
            }

            JpaRepository<?, ?> repository = cachedRepositories.get(entityName.toLowerCase());
            if (repository != null) {
              try {
                if (repository.count() > 0) {
                  repository.deleteAllInBatch();
                  resetSequence(entityName);
                  deletedCount++;
                }
              } catch (Exception e) {
                log.warn("Could not clear repository {}: {}", entityName, e.getMessage());
                // Fallback to manual DELETE for critical tables if repository fails
                try {
                  jdbcTemplate.execute("DELETE FROM " + entityName.toUpperCase(Locale.ROOT));
                  resetSequence(entityName);
                  deletedCount++;
                } catch (Exception ex) {
                  log.trace("Manual delete also failed for {}: {}", entityName, ex.getMessage());
                }
              }
            }
          }

          // Clean up any remaining repositories not in the entity list
          for (Map.Entry<String, JpaRepository<?, ?>> entry : cachedRepositories.entrySet()) {
            if (!cachedSyncOrder.contains(entry.getKey())
                && !protectedEntities.contains(entry.getKey().toLowerCase())) {
              try {
                if (entry.getValue().count() > 0) {
                  entry.getValue().deleteAllInBatch();
                  resetSequence(entry.getKey());
                  deletedCount++;
                }
              } catch (Exception e) {
                log.trace(
                    "Could not clear remaining repository {}: {}", entry.getKey(), e.getMessage());
              }
            }
          }

          // Re-initialize accounts to ensure admin, booker, etc. are available
          accountInitializer.init();
          entityManager.flush();
          entityManager.clear();
          log.info("✨ Database cleanup completed. Cleared {} repositories", deletedCount);
        });
  }

  protected int executeNativeUpdate(String sql) {
    try {
      return transactionTemplate.execute(
          status -> {
            try {
              return entityManager.createNativeQuery(sql).executeUpdate();
            } catch (Exception e) {
              log.warn("Native update failed: {} - {}", sql, e.getMessage());
              return -1;
            }
          });
    } catch (Exception e) {
      log.warn("Transaction for native update failed: {} - {}", sql, e.getMessage());
      return -1;
    }
  }

  /**
   * Manually breaks known circular dependencies by setting foreign keys to NULL. This is necessary
   * before batch deletion to avoid constraint violations.
   */
  private void breakCircularDependencies() {
    log.debug("💔 Breaking circular dependencies...");
    // Order matters here to handle foreign key constraints correctly
    String[] manualTables = {
      "campaign_state",
      "storyline_milestone",
      "campaign_storyline",
      "wrestler_relationship",
      "wrestler_state",
      "league_roster",
      "league_membership",
      "team_members",
      "team",
      "faction_members",
      "faction",
      "account_roles",
      "account"
    };

    for (String table : manualTables) {
      try {
        if (table.equals("campaign_state")) {
          jdbcTemplate.execute(
              "UPDATE campaign_state SET active_storyline_id = NULL, current_match_id = NULL");
        } else if (table.equals("campaign_storyline")) {
          jdbcTemplate.execute("UPDATE campaign_storyline SET current_milestone_id = NULL");
        } else if (table.equals("storyline_milestone")) {
          jdbcTemplate.execute(
              "UPDATE storyline_milestone SET next_on_success_id = NULL, next_on_failure_id = NULL,"
                  + " storyline_id = NULL");
        } else if (table.equals("faction")) {
          jdbcTemplate.execute("UPDATE faction SET leader_id = NULL, manager_id = NULL");
        }

        jdbcTemplate.execute("DELETE FROM " + table);
        log.debug("✅ Manually cleared table: {}", table);
      } catch (Exception e) {
        log.trace("Could not manually clear table {}: {}", table, e.getMessage());
      }
    }

    try {
      entityManager
          .createNativeQuery("UPDATE account SET failed_login_attempts = 0, locked_until = NULL")
          .executeUpdate();
      log.debug("✅ Reset account lockout states");
    } catch (Exception e) {
      log.warn("Could not reset account lockout states: {}", e.getMessage());
    }

    try {
      entityManager.createNativeQuery("DELETE FROM storyline_milestone").executeUpdate();
      log.debug("✅ Explicitly cleared storyline_milestone");
    } catch (Exception e) {
      log.warn("Could not explicitly clear storyline_milestone: {}", e.getMessage());
    }
    try {
      entityManager.createNativeQuery("DELETE FROM campaign_storyline").executeUpdate();
      log.debug("✅ Explicitly cleared campaign_storyline");
    } catch (Exception e) {
      log.warn("Could not explicitly clear campaign_storyline: {}", e.getMessage());
    }
  }

  /**
   * Discovers and clears all join tables defined with @JoinTable annotation. This is necessary for
   * Many-to-Many relationships that don't have a dedicated repository.
   */
  private void clearJoinTables() {
    Set<String> joinTableNames = new HashSet<>();
    joinTableNames.add("heat_event");
    Set<EntityType<?>> entities = entityManager.getMetamodel().getEntities();

    for (EntityType<?> entity : entities) {
      for (Attribute<?, ?> attribute : entity.getAttributes()) {
        if (attribute.isCollection()) {
          Member member = attribute.getJavaMember();
          if (member instanceof AnnotatedElement annotatedElement) {
            JoinTable joinTable = annotatedElement.getAnnotation(JoinTable.class);
            if (joinTable != null) {
              // Skip account_roles to prevent deleting user roles
              if (!"account_roles".equalsIgnoreCase(joinTable.name())) {
                joinTableNames.add(joinTable.name());
              }
            }
          }
        }
      }
    }

    if (!joinTableNames.isEmpty()) {
      log.debug("🧹 Clearing {} join tables...", joinTableNames.size());
      for (String tableName : joinTableNames) {
        try {
          log.debug("Deleting all records from join table {}", tableName);
          // Use uppercase to match database schema and no quotes to avoid case-sensitivity issues
          entityManager
              .createNativeQuery("DELETE FROM " + tableName.toUpperCase(Locale.ROOT))
              .executeUpdate();
          log.debug("✅ Deleted all records from join table {}", tableName);
        } catch (Exception e) {
          log.error("❌ Failed to delete from join table {}: {}", tableName, e.getMessage());
        }
      }
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

  /**
   * Discovers all JpaRepository beans in the application context. Returns a map of entity name
   * (lowercase) to repository instance.
   */
  private Map<String, JpaRepository<?, ?>> discoverRepositories() {
    Map<String, JpaRepository<?, ?>> repositories = new HashMap<>();
    String[] beanNames = applicationContext.getBeanNamesForType(JpaRepository.class);

    for (String beanName : beanNames) {
      // Skip role repository to avoid deleting core roles
      if (beanName.equals("roleRepository")) {
        continue;
      }
      try {
        JpaRepository<?, ?> repository = applicationContext.getBean(beanName, JpaRepository.class);

        // Extract entity name from repository
        String entityName = extractEntityName(repository, beanName);
        if (entityName != null) {
          repositories.put(entityName.toLowerCase(), repository);
          log.debug("📍 Mapped repository {} to entity {}", beanName, entityName);
        }
      } catch (Exception e) {
        log.debug("⚠️ Skipping bean {}: {}", beanName, e.getMessage());
      }
    }

    return repositories;
  }

  /**
   * Extracts the entity name from a repository bean. Tries multiple strategies: bean name pattern,
   * repository interface generics.
   */
  private String extractEntityName(JpaRepository<?, ?> repository, String beanName) {
    // Strategy 1: Bean name pattern (e.g., "cardRepository" -> "card")
    if (beanName.endsWith("Repository")) {
      return beanName.substring(0, beanName.length() - "Repository".length());
    }

    // Strategy 2: Fallback to manual replacement if strategy 1 fails for some reason
    return beanName.replace("Repository", "").toLowerCase();
  }
}
