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
package com.github.javydreamercsw.management.service.sync;

import jakarta.persistence.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

/**
 * Analyzes JPA entity relationships to automatically determine sync order. Uses reflection to
 * examine entity annotations and build a dependency graph.
 */
@Component
@Slf4j
public class EntityDependencyAnalyzer {

  @Autowired private ApplicationContext applicationContext;

  private List<SyncEntityType> syncOrderCache;

  /**
   * Automatically determines sync order based on JPA entity relationships. Caches the result to
   * avoid re-calculating on every call.
   *
   * @return List of SyncEntityType in dependency order
   */
  public List<SyncEntityType> getAutomaticSyncOrder() {
    if (syncOrderCache == null) {
      Set<Class<?>> entityClasses = discoverEntityClasses();
      List<String> entityNames = determineSyncOrder(entityClasses);
      syncOrderCache = convertToSyncEntityTypes(entityNames);
    }
    return syncOrderCache;
  }

  /**
   * Converts a list of entity names to SyncEntityType list. Logs a warning for any entity names
   * that don't match a SyncEntityType.
   *
   * @param entityNames List of entity names
   * @return List of SyncEntityType objects
   */
  private List<SyncEntityType> convertToSyncEntityTypes(List<String> entityNames) {
    return entityNames.stream()
        .map(
            name -> {
              Optional<SyncEntityType> entityType = SyncEntityType.fromEntityClassName(name);
              if (entityType.isEmpty()) {
                log.warn(
                    "Entity '{}' does not have a corresponding SyncEntityType enum value", name);
              }
              return entityType;
            })
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(java.util.stream.Collectors.toList());
  }

  /** Discovers all JPA entity classes in the application. */
  private Set<Class<?>> discoverEntityClasses() {
    Set<Class<?>> entityClasses = new HashSet<>();

    // Get all beans and check if they're entities
    String[] beanNames = applicationContext.getBeanDefinitionNames();
    for (String beanName : beanNames) {
      try {
        Class<?> beanClass = applicationContext.getType(beanName);
        if (beanClass != null && beanClass.isAnnotationPresent(Entity.class)) {
          entityClasses.add(beanClass);
        }
      } catch (Exception e) {
        // Skip beans that can't be analyzed
        log.debug("Skipping bean {} during entity discovery: {}", beanName, e.getMessage());
      }
    }

    // Also scan domain packages directly
    entityClasses.addAll(scanDomainPackages());

    log.info(
        "🔍 Discovered {} entity classes: {}",
        entityClasses.size(),
        entityClasses.stream().map(Class::getSimpleName).collect(Collectors.toList()));

    return entityClasses;
  }

  /** Scans domain packages for entity classes. */
  private Set<Class<?>> scanDomainPackages() {
    Set<Class<?>> entities = new HashSet<>();
    ClassPathScanningCandidateComponentProvider scanner =
        new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));

    // List of known domain packages to scan
    String[] domainPackages = {"com.github.javydreamercsw.management.domain"};

    for (String packageName : domainPackages) {
      for (BeanDefinition bd : scanner.findCandidateComponents(packageName)) {
        try {
          entities.add(Class.forName(bd.getBeanClassName()));
        } catch (ClassNotFoundException e) {
          log.debug("Error loading class {}: {}", bd.getBeanClassName(), e.getMessage());
        }
      }
    }

    return entities;
  }

  /**
   * Analyzes entity classes and returns them in dependency order (dependencies first). Uses
   * topological sorting based on JPA relationship annotations.
   *
   * @param entityClasses Set of entity classes to analyze
   * @return List of entity names in sync order (dependencies first)
   */
  public List<String> determineSyncOrder(Set<Class<?>> entityClasses) {
    log.info("🔍 Analyzing {} entity classes for dependency relationships", entityClasses.size());

    // Build dependency graph
    Map<String, Set<String>> dependencies = buildDependencyGraph(entityClasses);

    // Perform topological sort
    List<String> syncOrder = topologicalSort(dependencies);

    log.info("📋 Determined sync order: {}", syncOrder);
    return syncOrder;
  }

  /**
   * Builds a dependency graph by analyzing JPA annotations. Key = entity name, Value = set of
   * entities this entity depends on
   */
  private Map<String, Set<String>> buildDependencyGraph(Set<Class<?>> entityClasses) {
    Map<String, Set<String>> dependencies = new HashMap<>();
    Map<String, Class<?>> entityNameToClass =
        entityClasses.stream().collect(Collectors.toMap(this::getEntityName, clazz -> clazz));

    for (Class<?> entityClass : entityClasses) {
      String entityName = getEntityName(entityClass);
      Set<String> entityDependencies = new HashSet<>();

      // Analyze all fields for relationship annotations
      for (Field field : getAllFields(entityClass)) {
        analyzeForeignKeyDependencies(field, entityDependencies, entityNameToClass);
      }

      dependencies.put(entityName, entityDependencies);
      log.debug("Entity '{}' depends on: {}", entityName, entityDependencies);
    }

    return dependencies;
  }

  /** Analyzes a field for foreign key relationships that create dependencies. */
  private void analyzeForeignKeyDependencies(
      Field field, Set<String> dependencies, Map<String, Class<?>> entityNameToClass) {
    if (field.getDeclaringClass().getSimpleName().equals("Faction")
        && field.getName().equals("leader")) {
      return;
    }
    // @ManyToOne - this entity depends on the referenced entity
    if (field.isAnnotationPresent(ManyToOne.class)) {
      String referencedEntity = getEntityNameFromFieldType(field, entityNameToClass);
      if (referencedEntity != null) {
        dependencies.add(referencedEntity);
        log.debug("Found @ManyToOne dependency: {} -> {}", field.getName(), referencedEntity);
      }
    }

    // @JoinColumn - indicates a foreign key dependency
    if (field.isAnnotationPresent(JoinColumn.class)) {
      String referencedEntity = getEntityNameFromFieldType(field, entityNameToClass);
      if (referencedEntity != null) {
        dependencies.add(referencedEntity);
        log.debug("Found @JoinColumn dependency: {} -> {}", field.getName(), referencedEntity);
      }
    }

    // @OneToOne with @JoinColumn - this entity owns the relationship
    if (field.isAnnotationPresent(OneToOne.class) && field.isAnnotationPresent(JoinColumn.class)) {
      String referencedEntity = getEntityNameFromFieldType(field, entityNameToClass);
      if (referencedEntity != null) {
        dependencies.add(referencedEntity);
        log.debug("Found @OneToOne dependency: {} -> {}", field.getName(), referencedEntity);
      }
    }
  }

  /** Gets the entity name from a field's type. */
  private String getEntityNameFromFieldType(Field field, Map<String, Class<?>> entityNameToClass) {
    Class<?> fieldType = field.getType();

    // Handle Collection types (OneToMany, ManyToMany)
    if (Collection.class.isAssignableFrom(fieldType)) {
      // For collections, we don't create dependencies (they're usually bidirectional)
      return null;
    }

    // Check if the field type is one of our entities
    for (Map.Entry<String, Class<?>> entry : entityNameToClass.entrySet()) {
      if (entry.getValue().equals(fieldType)) {
        return entry.getKey();
      }
    }

    return null;
  }

  /** Gets all fields from a class including inherited fields. */
  private List<Field> getAllFields(Class<?> clazz) {
    List<Field> fields = new ArrayList<>();
    Class<?> currentClass = clazz;

    while (currentClass != null && currentClass != Object.class) {
      fields.addAll(Arrays.asList(currentClass.getDeclaredFields()));
      currentClass = currentClass.getSuperclass();
    }

    return fields;
  }

  /** Gets the entity name from a class (uses @Entity name or class simple name). */
  private String getEntityName(Class<?> entityClass) {
    Entity entityAnnotation = entityClass.getAnnotation(Entity.class);
    if (entityAnnotation != null && !entityAnnotation.name().isEmpty()) {
      return entityAnnotation.name().toLowerCase();
    }
    return entityClass.getSimpleName().toLowerCase();
  }

  /**
   * Performs topological sort on the dependency graph. Returns entities in dependency order
   * (dependencies first).
   */
  private List<String> topologicalSort(Map<String, Set<String>> dependencies) {
    List<String> result = new ArrayList<>();
    Set<String> visited = new HashSet<>();
    Set<String> visiting = new HashSet<>();

    // Sort keys to ensure deterministic order for independent nodes
    List<String> sortedKeys = new ArrayList<>(dependencies.keySet());
    Collections.sort(sortedKeys);

    for (String entity : sortedKeys) {
      if (!visited.contains(entity)) {
        topologicalSortVisit(entity, dependencies, visited, visiting, result);
      }
    }

    return result;
  }

  /** Recursive helper for topological sort with cycle detection. */
  private void topologicalSortVisit(
      String entity,
      Map<String, Set<String>> dependencies,
      Set<String> visited,
      Set<String> visiting,
      List<String> result) {
    if (visiting.contains(entity)) {
      // Circular dependency detected.
      // We log it but don't throw exception to allow best-effort sorting.
      // In a real cycle, one must be broken manually or via nullable fields.
      log.warn("Circular dependency detected involving entity: {}", entity);
      return;
    }

    if (visited.contains(entity)) {
      return;
    }

    visiting.add(entity);

    // Visit all dependencies first
    Set<String> entityDeps = dependencies.getOrDefault(entity, Collections.emptySet());
    // Sort dependencies for deterministic order
    List<String> sortedDeps = new ArrayList<>(entityDeps);
    Collections.sort(sortedDeps);

    for (String dependency : sortedDeps) {
      if (dependencies.containsKey(dependency)) {
        topologicalSortVisit(dependency, dependencies, visited, visiting, result);
      }
    }

    visiting.remove(entity);
    visited.add(entity);
    result.add(entity);
  }
}
