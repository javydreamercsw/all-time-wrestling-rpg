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
import com.github.javydreamercsw.management.service.sync.EntityDependencyAnalyzer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.JoinTable;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Automatically discovers and clears all JPA repositories in the correct dependency order. Uses
 * EntityDependencyAnalyzer to determine the safe deletion order based on entity relationships.
 */
@Service
@Slf4j
public class DatabaseCleaner implements DatabaseCleanup {

  @Autowired private ApplicationContext applicationContext;
  @Autowired private EntityDependencyAnalyzer dependencyAnalyzer;
  @Autowired private EntityManager entityManager;
  @Autowired private AccountInitializer accountInitializer;
  @Autowired private TransactionTemplate transactionTemplate;
  @Autowired private Environment environment;

  private Map<String, JpaRepository<?, ?>> cachedRepositories;
  private List<String> cachedSyncOrder;
  private Set<Class<?>> cachedEntityClasses;

  /**
   * Clears all repositories in the correct dependency order. Automatically discovers all
   * JpaRepository beans and deletes their data in reverse dependency order (children before
   * parents).
   */
  @Override
  public void clearRepositories() {
    log.debug("🧹 Starting database cleanup...");

    // Detach all managed entities to prevent dirty state from interfering with cleanup.
    entityManager.clear();

    // Break circular dependencies first
    breakCircularDependencies();

    // Clear join tables
    clearJoinTables();

    // Discover all repositories (with caching)
    if (cachedRepositories == null) {
      cachedRepositories = discoverRepositories();
      log.debug("📦 Discovered and cached {} repositories", cachedRepositories.size());
    }

    // Get entity classes and determine deletion order (with caching)
    if (cachedSyncOrder == null) {
      cachedEntityClasses = getEntityClasses();
      cachedSyncOrder = dependencyAnalyzer.determineSyncOrder(cachedEntityClasses);
      // Reverse the order for deletion (delete children before parents)
      List<String> reverseOrder = new ArrayList<>(cachedSyncOrder);
      Collections.reverse(reverseOrder);
      cachedSyncOrder = reverseOrder;
      log.debug("🔄 Calculated and cached deletion order: {}", cachedSyncOrder);
    }

    // Entities that should NOT be cleared as they contain static configuration data
    // loaded by DataInitializer and needed by many views.
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
                "statuscard"));

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
            deletedCount++;
          }
        } catch (Exception e) {
          log.warn("Failed to clear repository for {}: {}", entityName, e.getMessage());
        }
      }
    }

    // Clean up any remaining repositories not in the entity list
    for (Map.Entry<String, JpaRepository<?, ?>> entry : cachedRepositories.entrySet()) {
      if (!cachedSyncOrder.contains(entry.getKey())
          && !protectedEntities.contains(entry.getKey())) {
        try {
          if (entry.getValue().count() > 0) {
            entry.getValue().deleteAllInBatch();
            deletedCount++;
          }
        } catch (Exception e) {
          log.warn("Failed to clear remaining repository {}: {}", entry.getKey(), e.getMessage());
        }
      }
    }

    accountInitializer.init();
    log.debug("✨ Database cleanup completed. Cleared {} repositories", deletedCount);
  }

  protected int executeNativeUpdate(String sql) {
    try {
      return transactionTemplate.execute(
          status -> {
            try {
              return entityManager.createNativeQuery(sql).executeUpdate();
            } catch (Exception e) {
              log.warn("Native update execution failed: {} - {}", sql, e.getMessage());
              status.setRollbackOnly();
              return -1;
            }
          });
    } catch (Exception e) {
      log.warn("Transaction failed for native update: {} - {}", sql, e.getMessage());
      return -1;
    }
  }

  /**
   * Manually breaks known circular dependencies by setting foreign keys to NULL. This is necessary
   * before batch deletion to avoid constraint violations.
   */
  private void breakCircularDependencies() {
    log.debug("💔 Breaking circular dependencies...");
    executeNativeUpdate("DELETE FROM heat_event");
    executeNativeUpdate("UPDATE wrestler SET faction_id = NULL");
    executeNativeUpdate("UPDATE faction SET leader_id = NULL");
    executeNativeUpdate("UPDATE wrestler SET account_id = NULL");
    executeNativeUpdate("UPDATE campaign_storyline SET current_milestone_id = NULL");
    executeNativeUpdate(
        "UPDATE storyline_milestone SET next_on_success_id = NULL, next_on_failure_id = NULL");
    executeNativeUpdate("UPDATE campaign_state SET active_storyline_id = NULL");
    executeNativeUpdate("UPDATE account SET failed_login_attempts = 0, locked_until = NULL");

    // Clear storyline and milestone explicitly as they often cause issues
    executeNativeUpdate("DELETE FROM storyline_milestone");
    executeNativeUpdate("DELETE FROM campaign_storyline");
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
        executeNativeUpdate("DELETE FROM " + tableName.toUpperCase(Locale.ROOT));
      }
    }
  }

  /**
   * Discovers all JpaRepository beans in the application context. Returns a map of entity name
   * (lowercase) to repository instance.
   */
  private Map<String, JpaRepository<?, ?>> discoverRepositories() {
    Map<String, JpaRepository<?, ?>> repositories = new HashMap<>();
    String[] beanNames = applicationContext.getBeanNamesForType(JpaRepository.class);

    for (String beanName : beanNames) {
      // Skip account and role repositories to avoid deleting test users
      if (beanName.equals("accountRepository") || beanName.equals("roleRepository")) {
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

    // Strategy 2: Try to find the entity class from repository methods
    try {
      Method findAllMethod = repository.getClass().getMethod("findAll");
      Class<?> returnType = findAllMethod.getReturnType();
      if (returnType != null) {
        // This is a fallback, might not always work perfectly
        return returnType.getSimpleName().toLowerCase();
      }
    } catch (Exception e) {
      // Ignore
    }

    return null;
  }

  /** Gets all entity classes registered with the EntityManager. */
  private Set<Class<?>> getEntityClasses() {
    Set<Class<?>> entityClasses = new HashSet<>();
    Set<EntityType<?>> entities = entityManager.getMetamodel().getEntities();

    for (EntityType<?> entity : entities) {
      entityClasses.add(entity.getJavaType());
    }

    return entityClasses;
  }
}
