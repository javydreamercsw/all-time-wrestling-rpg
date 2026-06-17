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
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Spring Security helper bean used in {@code @PreAuthorize} SpEL expressions to enforce
 * universe-scoped ownership checks. Register as {@code "universeAuthz"} so that expressions like
 * {@code @universeAuthz.isOwner(#universe)} resolve correctly.
 */
@Component("universeAuthz")
@RequiredArgsConstructor
public class UniverseAuthorizationService {

  private final UniverseMembershipRepository membershipRepository;
  private final UniverseInviteRepository inviteRepository;
  private final UniverseJoinRequestRepository joinRequestRepository;

  /** Returns {@code true} when the currently authenticated user is an OWNER of {@code universe}. */
  public boolean isOwner(@NonNull final Universe universe) {
    return currentAccountId()
        .map(
            id ->
                membershipRepository.existsByAccount_IdAndUniverseAndRole(
                    id, universe, UniverseMemberRole.OWNER))
        .orElse(false);
  }

  /**
   * Returns {@code true} when the current user owns the universe associated with the given invite
   * token. Returns {@code false} when the token does not exist.
   */
  public boolean isOwnerOfInvite(@NonNull final String inviteToken) {
    return inviteRepository.findById(inviteToken).map(i -> isOwner(i.getUniverse())).orElse(false);
  }

  /**
   * Returns {@code true} when the current user owns the universe associated with the given join
   * request. Returns {@code false} when the request does not exist.
   */
  public boolean isOwnerOfRequest(final long requestId) {
    return joinRequestRepository
        .findById(requestId)
        .map(r -> isOwner(r.getUniverse()))
        .orElse(false);
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
