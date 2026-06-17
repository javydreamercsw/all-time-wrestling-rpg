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
package com.github.javydreamercsw.management.service.universe;

import com.github.javydreamercsw.base.security.CustomUserDetails;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseInviteRepository;
import com.github.javydreamercsw.management.domain.universe.UniverseJoinRequestRepository;
import com.github.javydreamercsw.management.domain.universe.UniverseMembership.UniverseMemberRole;
import com.github.javydreamercsw.management.domain.universe.UniverseMembershipRepository;
import java.util.Map;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Spring Security helper bean used in {@code @PreAuthorize} SpEL expressions to enforce
 * universe-scoped role checks. Register as {@code "universeAuthz"} so that expressions like
 * {@code @universeAuthz.hasRole(#universe, 'BOOKER')} resolve correctly.
 *
 * <p>Role hierarchy (highest to lowest): OWNER > ADMIN > BOOKER > PLAYER > MEMBER.
 */
@Component("universeAuthz")
@RequiredArgsConstructor
public class UniverseAuthorizationService {

  private static final Map<UniverseMemberRole, Integer> ROLE_RANK =
      Map.of(
          UniverseMemberRole.OWNER, 4,
          UniverseMemberRole.ADMIN, 3,
          UniverseMemberRole.BOOKER, 2,
          UniverseMemberRole.PLAYER, 1,
          UniverseMemberRole.MEMBER, 0);

  private final UniverseMembershipRepository membershipRepository;
  private final UniverseInviteRepository inviteRepository;
  private final UniverseJoinRequestRepository joinRequestRepository;
  private final UniverseContextService universeContextService;

  /**
   * Returns {@code true} when the current user's universe role is at least {@code roleName}
   * (respects the OWNER > ADMIN > BOOKER > PLAYER > MEMBER hierarchy).
   *
   * <p>Use in {@code @PreAuthorize} as {@code @universeAuthz.hasRole(#universe, 'BOOKER')}.
   */
  public boolean hasRole(@NonNull final Universe universe, @NonNull final String roleName) {
    try {
      return hasRole(universe, UniverseMemberRole.valueOf(roleName));
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  /** Programmatic variant of {@link #hasRole(Universe, String)}. */
  public boolean hasRole(
      @NonNull final Universe universe, @NonNull final UniverseMemberRole required) {
    int requiredRank = ROLE_RANK.getOrDefault(required, Integer.MAX_VALUE);
    return currentAccountId()
        .flatMap(id -> membershipRepository.findByAccount_IdAndUniverse(id, universe))
        .map(m -> ROLE_RANK.getOrDefault(m.getRole(), -1) >= requiredRank)
        .orElse(false);
  }

  /** Returns {@code true} when the current user is an OWNER of {@code universe}. */
  public boolean isOwner(@NonNull final Universe universe) {
    return hasRole(universe, UniverseMemberRole.OWNER);
  }

  /**
   * Returns {@code true} when the current user has at least {@code roleName} in the universe
   * associated with {@code inviteToken}. Returns {@code false} when the token does not exist.
   *
   * <p>Use as {@code @universeAuthz.hasRoleForInvite(#inviteToken, 'ADMIN')}.
   */
  public boolean hasRoleForInvite(
      @NonNull final String inviteToken, @NonNull final String roleName) {
    return inviteRepository
        .findById(inviteToken)
        .map(i -> hasRole(i.getUniverse(), roleName))
        .orElse(false);
  }

  /** Convenience alias — {@code true} when the current user is OWNER of the invite's universe. */
  public boolean isOwnerOfInvite(@NonNull final String inviteToken) {
    return hasRoleForInvite(inviteToken, "OWNER");
  }

  /**
   * Returns {@code true} when the current user has at least {@code roleName} in the universe
   * associated with {@code requestId}. Returns {@code false} when the request does not exist.
   *
   * <p>Use as {@code @universeAuthz.hasRoleForRequest(#requestId, 'ADMIN')}.
   */
  public boolean hasRoleForRequest(final long requestId, @NonNull final String roleName) {
    return joinRequestRepository
        .findById(requestId)
        .map(r -> hasRole(r.getUniverse(), roleName))
        .orElse(false);
  }

  /** Convenience alias — {@code true} when the current user is OWNER of the request's universe. */
  public boolean isOwnerOfRequest(final long requestId) {
    return hasRoleForRequest(requestId, "OWNER");
  }

  /**
   * Returns {@code true} when the current user has at least {@code roleName} in the user's
   * currently active universe (resolved via {@link UniverseContextService}).
   *
   * <p>Use in services that have no universe parameter but operate on the active universe:
   * {@code @universeAuthz.hasRoleInCurrentUniverse('BOOKER')}.
   */
  public boolean hasRoleInCurrentUniverse(@NonNull final String roleName) {
    return universeContextService.getCurrentUniverse().map(u -> hasRole(u, roleName)).orElse(false);
  }

  private Optional<Long> currentAccountId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) {
      return Optional.empty();
    }
    if (auth.getPrincipal() instanceof CustomUserDetails details) {
      return Optional.ofNullable(details.getId());
    }
    return Optional.empty();
  }
}
