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
package com.github.javydreamercsw.base.security;

import com.github.javydreamercsw.management.domain.account.RoleName;
import com.vaadin.flow.spring.security.AuthenticationContext;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Utility class for security-related operations. */
@Component
@RequiredArgsConstructor
public class SecurityUtils {

  private final AuthenticationContext authenticationContext;

  /**
   * Check if the current user has a specific role.
   *
   * @param roleName the role name to check
   * @return true if the user has the role
   */
  public boolean hasRole(RoleName roleName) {
    return hasRole("ROLE_" + roleName.name());
  }

  /**
   * Check if the current user has a specific role (with ROLE_ prefix).
   *
   * @param role the role with ROLE_ prefix
   * @return true if the user has the role
   */
  public boolean hasRole(String role) {
    return authenticationContext
        .getAuthenticatedUser(CustomUserDetails.class)
        .map(
            user ->
                user.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(authority -> authority.equals(role)))
        .orElse(false);
  }

  /**
   * Check if the current user has any of the specified roles.
   *
   * @param roleNames the role names to check
   * @return true if the user has any of the roles
   */
  public boolean hasAnyRole(RoleName... roleNames) {
    for (RoleName roleName : roleNames) {
      if (hasRole(roleName)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Check if the current user is an admin.
   *
   * @return true if the user is an admin
   */
  public boolean isAdmin() {
    return hasRole(RoleName.ADMIN);
  }

  /**
   * Check if the current user is a booker.
   *
   * @return true if the user is a booker
   */
  public boolean isBooker() {
    return hasRole(RoleName.BOOKER);
  }

  /**
   * Check if the current user is a player.
   *
   * @return true if the user is a player
   */
  public boolean isPlayer() {
    return hasRole(RoleName.PLAYER);
  }

  /**
   * Check if the current user is a viewer.
   *
   * @return true if the user is a viewer
   */
  public boolean isViewer() {
    return hasRole(RoleName.VIEWER);
  }

  /**
   * Check if the current user can create content.
   *
   * @return true if the user can create
   */
  public boolean canCreate() {
    return hasAnyRole(RoleName.ADMIN, RoleName.BOOKER, RoleName.PLAYER);
  }

  /**
   * Check if the current user can edit content.
   *
   * @return true if the user can edit
   */
  public boolean canEdit() {
    return hasAnyRole(RoleName.ADMIN, RoleName.BOOKER, RoleName.PLAYER);
  }

  /**
   * Check if the current user can delete content.
   *
   * @return true if the user can delete
   */
  public boolean canDelete() {
    return hasAnyRole(RoleName.ADMIN, RoleName.BOOKER);
  }

  /**
   * Get the currently authenticated user.
   *
   * @return the authenticated user, or empty if not authenticated
   */
  public Optional<CustomUserDetails> getAuthenticatedUser() {
    return authenticationContext.getAuthenticatedUser(CustomUserDetails.class);
  }

  /**
   * Get the username of the currently authenticated user.
   *
   * @return the username, or "anonymous" if not authenticated
   */
  public String getCurrentUsername() {
    return getAuthenticatedUser().map(CustomUserDetails::getUsername).orElse("anonymous");
  }

  /**
   * Get the account ID of the currently authenticated user.
   *
   * @return the account ID, or empty if not authenticated
   */
  public Optional<Long> getCurrentAccountId() {
    return getAuthenticatedUser().map(CustomUserDetails::getId);
  }

  /**
   * Check if the current user is authenticated.
   *
   * @return true if authenticated
   */
  public boolean isAuthenticated() {
    return authenticationContext.isAuthenticated();
  }

  /**
   * Get the current authentication object (for use outside Vaadin context).
   *
   * @return the authentication
   */
  public static Optional<Authentication> getAuthentication() {
    return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication());
  }
}
