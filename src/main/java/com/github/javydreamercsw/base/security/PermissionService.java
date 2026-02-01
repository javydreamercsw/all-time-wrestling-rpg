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

import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.management.domain.deck.Deck;
import com.github.javydreamercsw.management.domain.deck.DeckCard;
import com.github.javydreamercsw.management.domain.inbox.InboxItem;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("permissionService")
@Transactional(readOnly = true)
public class PermissionService {
  private static final Logger log = LoggerFactory.getLogger(PermissionService.class);

  private final WrestlerRepository wrestlerRepository;
  private final AccountRepository accountRepository;

  public PermissionService(
      WrestlerRepository wrestlerRepository, AccountRepository accountRepository) {
    this.wrestlerRepository = wrestlerRepository;
    this.accountRepository = accountRepository;
  }

  public boolean isOwner(Object targetDomainObject) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      log.warn("isOwner: Authentication is null");
      return false;
    }

    Object principal = authentication.getPrincipal();
    if (!(principal instanceof UserDetails userDetails)) {
      log.warn(
          "isOwner: Principal is not UserDetails: {}",
          principal != null ? principal.getClass().getName() : "null");
      return false; // Or handle anonymous user differently
    }

    log.info("isOwner: Checking ownership for user: {}", userDetails.getUsername());

    // Fetch all wrestlers associated with the account
    java.util.List<Wrestler> userWrestlers =
        accountRepository
            .findByUsername(userDetails.getUsername())
            .map(wrestlerRepository::findByAccount)
            .orElse(java.util.Collections.emptyList());

    if (userWrestlers.isEmpty()) {
      log.warn("isOwner: No wrestlers found for user: {}", userDetails.getUsername());
      return false; // User does not have any wrestlers assigned
    }

    java.util.Set<Long> ownedWrestlerIds =
        userWrestlers.stream().map(Wrestler::getId).collect(java.util.stream.Collectors.toSet());

    if (targetDomainObject instanceof Wrestler targetWrestler) {
      return ownedWrestlerIds.contains(targetWrestler.getId());
    } else if (targetDomainObject instanceof Deck deck) {
      Wrestler deckWrestler = deck.getWrestler();
      return deckWrestler != null && ownedWrestlerIds.contains(deckWrestler.getId());
    } else if (targetDomainObject instanceof DeckCard deckCard) {
      Deck deck = deckCard.getDeck();
      if (deck != null) {
        Wrestler deckWrestler = deck.getWrestler();
        return deckWrestler != null && ownedWrestlerIds.contains(deckWrestler.getId());
      }
    } else if (targetDomainObject instanceof InboxItem inboxItem) {
      return inboxItem.getTargets().stream()
          .anyMatch(target -> ownedWrestlerIds.contains(Long.valueOf(target.getTargetId())));
    } else if (targetDomainObject instanceof Collection<?> collection) {
      if (collection.isEmpty()) {
        return true;
      }
      // Create a copy to avoid ConcurrentModificationException if the collection is being modified
      // elsewhere
      java.util.List<?> copy = new java.util.ArrayList<>(collection);
      // Check if all items in the collection are owned by the user
      return copy.stream().allMatch(this::isOwner);
    }

    log.warn(
        "isOwner: Unsupported target domain object: {}",
        targetDomainObject != null ? targetDomainObject.getClass().getName() : "null");
    return false;
  }

  public boolean isOwner(Long targetId, String targetType) {
    if (targetId == null || targetType == null) {
      return false;
    }

    if (targetType.equals("Wrestler")) {
      return wrestlerRepository.findById(targetId).map(this::isOwner).orElse(false);
    }

    return false;
  }
}
