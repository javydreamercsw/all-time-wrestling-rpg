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
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

  /**
   * Clears all repositories in the correct dependency order. Automatically discovers all
   * JpaRepository beans and deletes their data in reverse dependency order (children before
   * parents).
   */
  @Transactional
  @Override
  public void clearRepositories() {
    log.info("🧹 Starting database cleanup...");

    // Detach all managed entities to prevent dirty state from interfering with cleanup.
    entityManager.clear();

    // Manually break known circular dependencies before doing anything else.
    breakCircularDependencies();

    // Clear join tables first to handle Many-to-Many relationships without dedicated repositories
    clearJoinTables();

    // Discover all repositories
    Map<String, JpaRepository<?, ?>> repositories = discoverRepositories();
    log.info("📦 Discovered {} repositories", repositories.size());

    // Get entity classes and determine deletion order
    Set<Class<?>> entityClasses = getEntityClasses();
    List<String> syncOrder = dependencyAnalyzer.determineSyncOrder(entityClasses);

    // Reverse the order for deletion (delete children before parents)
    Collections.reverse(syncOrder);

    log.info("🔄 Deletion order: {}", syncOrder);

    // Delete data in the correct order
    int deletedCount = 0;
    for (String entityName : syncOrder) {
      JpaRepository<?, ?> repository = repositories.get(entityName.toLowerCase());
      if (repository != null) {
        if (repository.count() > 0) {
          repository.deleteAllInBatch();
          deletedCount++;
        }
      }
    }

    // Clean up any remaining repositories not in the entity list
    for (Map.Entry<String, JpaRepository<?, ?>> entry : repositories.entrySet()) {
      if (!syncOrder.contains(entry.getKey())) {
        if (entry.getValue().count() > 0) {
          entry.getValue().deleteAllInBatch();
          deletedCount++;
        }
      }
    }

    accountInitializer.init();
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
      entityManager.createNativeQuery("UPDATE wrestler SET faction_id = NULL").executeUpdate();
      log.debug("✅ Nullified wrestler.faction_id");
    } catch (Exception e) {
      // This might fail if the table doesn't exist yet on first run, which is fine.
      log.warn("Could not nullify wrestler.faction_id: {}", e.getMessage());
    }
    try {
      entityManager.createNativeQuery("UPDATE faction SET leader_id = NULL").executeUpdate();
      log.debug("✅ Nullified faction.leader_id");
    } catch (Exception e) {
      // This might fail if the table doesn't exist yet on first run, which is fine.
      log.warn("Could not nullify faction.leader_id: {}", e.getMessage());
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
          if (member instanceof AnnotatedElement) {
            AnnotatedElement annotatedElement = (AnnotatedElement) member;
            JoinTable joinTable = annotatedElement.getAnnotation(JoinTable.class);
            if (joinTable != null) {
              joinTableNames.add(joinTable.name());
            }
          }
        }
      }
    }

    if (!joinTableNames.isEmpty()) {
      log.info("🧹 Clearing {} join tables...", joinTableNames.size());
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
