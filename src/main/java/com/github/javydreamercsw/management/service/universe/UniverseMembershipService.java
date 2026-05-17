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

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseMembership;
import com.github.javydreamercsw.management.domain.universe.UniverseMembership.UniverseMemberRole;
import com.github.javydreamercsw.management.domain.universe.UniverseMembershipRepository;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UniverseMembershipService {

  private final UniverseMembershipRepository membershipRepository;

  @PreAuthorize("isAuthenticated()")
  public List<Universe> getUniversesForAccount(@NonNull final Account account) {
    return membershipRepository.findByAccount(account).stream()
        .map(UniverseMembership::getUniverse)
        .toList();
  }

  @PreAuthorize("isAuthenticated()")
  public List<UniverseMembership> getMembersForUniverse(@NonNull final Universe universe) {
    return membershipRepository.findByUniverse(universe);
  }

  @PreAuthorize("isAuthenticated()")
  public boolean isMember(@NonNull final Universe universe, @NonNull final Account account) {
    return membershipRepository.existsByAccountAndUniverse(account, universe);
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  public UniverseMembership addMember(
      @NonNull final Universe universe,
      @NonNull final Account account,
      @NonNull final UniverseMemberRole role) {
    if (membershipRepository.existsByAccountAndUniverse(account, universe)) {
      throw new IllegalStateException(
          account.getUsername() + " is already a member of " + universe.getName() + ".");
    }
    UniverseMembership membership = new UniverseMembership();
    membership.setUniverse(universe);
    membership.setAccount(account);
    membership.setRole(role);
    return membershipRepository.save(membership);
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  public void removeMember(@NonNull final Universe universe, @NonNull final Account account) {
    membershipRepository
        .findByAccountAndUniverse(account, universe)
        .ifPresentOrElse(
            membershipRepository::delete,
            () -> {
              throw new IllegalStateException(
                  account.getUsername() + " is not a member of " + universe.getName() + ".");
            });
  }
}
