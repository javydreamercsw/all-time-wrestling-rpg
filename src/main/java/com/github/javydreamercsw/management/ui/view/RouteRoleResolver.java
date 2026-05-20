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
package com.github.javydreamercsw.management.ui.view;

import com.github.javydreamercsw.base.domain.account.RoleName;
import com.vaadin.flow.router.Route;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Service;

/**
 * Scans all Spring-managed {@code @Route} views at startup and builds a map of route path →
 * required {@link RoleName}s derived from security annotations ({@code @RolesAllowed},
 * {@code @PermitAll}, {@code @DenyAll}).
 *
 * <p>This allows {@link MenuService} to automatically enforce menu visibility based on the view's
 * own annotations, eliminating the need to maintain role lists in two places.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RouteRoleResolver {

  private final ApplicationContext applicationContext;

  /**
   * Paths with {@code @PermitAll} or no security annotation map to an empty set (visible to all).
   * Paths with {@code @RolesAllowed} map to the set of required roles. Paths with {@code @DenyAll}
   * are tracked in {@link #deniedPaths}.
   */
  private Map<String, Set<RoleName>> routeRoleCache = Map.of();

  /** Paths whose views are annotated {@code @DenyAll} — always hidden in the menu. */
  private Set<String> deniedPaths = Set.of();

  @PostConstruct
  void buildCache() {
    Map<String, Set<RoleName>> roleMap = new HashMap<>();
    Set<String> denied = new HashSet<>();

    String[] beanNames = applicationContext.getBeanNamesForAnnotation(Route.class);
    for (String beanName : beanNames) {
      Class<?> beanType = applicationContext.getType(beanName);
      if (beanType == null) {
        continue;
      }

      Route route = AnnotationUtils.findAnnotation(beanType, Route.class);
      if (route == null) {
        continue;
      }

      String path = route.value();

      if (AnnotationUtils.findAnnotation(beanType, DenyAll.class) != null) {
        denied.add(path);
        log.debug("Route '{}' → @DenyAll (always hidden)", path);

      } else if (AnnotationUtils.findAnnotation(beanType, PermitAll.class) != null) {
        roleMap.put(path, Set.of());
        log.debug("Route '{}' → @PermitAll (visible to all)", path);

      } else {
        RolesAllowed rolesAllowed = AnnotationUtils.findAnnotation(beanType, RolesAllowed.class);
        if (rolesAllowed != null) {
          Set<RoleName> roles =
              Arrays.stream(rolesAllowed.value())
                  .map(
                      r -> {
                        try {
                          return RoleName.valueOf(r);
                        } catch (IllegalArgumentException e) {
                          log.warn("Unknown role '{}' on route '{}', skipping", r, path);
                          return null;
                        }
                      })
                  .filter(Objects::nonNull)
                  .collect(Collectors.toUnmodifiableSet());
          roleMap.put(path, roles);
          log.debug("Route '{}' → @RolesAllowed({})", path, roles);
        }
        // No annotation: not added to the map — MenuService falls back to MenuItem explicit roles
      }
    }

    this.routeRoleCache = Map.copyOf(roleMap);
    this.deniedPaths = Set.copyOf(denied);
    log.info(
        "RouteRoleResolver: scanned {} @Route beans; {} role-restricted, {} denied",
        beanNames.length,
        roleMap.size(),
        denied.size());
  }

  /**
   * Returns the roles resolved from the view's security annotation for the given route path.
   *
   * <ul>
   *   <li>{@code Optional.empty()} — no annotation found; caller should fall back to any explicit
   *       roles configured on the {@link MenuItem}.
   *   <li>{@code Optional.of(emptySet)} — view is {@code @PermitAll}; visible to all.
   *   <li>{@code Optional.of(nonEmptySet)} — view is {@code @RolesAllowed}; user must hold at least
   *       one of the returned roles.
   * </ul>
   *
   * <p>Callers should also check {@link #isDeniedAll(String)} first.
   *
   * @param path the Vaadin route path (i.e. the {@code @Route} value)
   * @return resolved access rules
   */
  public Optional<Set<RoleName>> resolveRoles(final String path) {
    if (path == null) {
      return Optional.empty();
    }
    // containsKey distinguishes "in map with empty set" from "not in map"
    if (routeRoleCache.containsKey(path)) {
      return Optional.of(routeRoleCache.get(path));
    }
    return Optional.empty();
  }

  /**
   * Returns {@code true} if the view for this route path is annotated {@code @DenyAll}, meaning it
   * must never appear in the menu.
   */
  public boolean isDeniedAll(final String path) {
    return path != null && deniedPaths.contains(path);
  }
}
