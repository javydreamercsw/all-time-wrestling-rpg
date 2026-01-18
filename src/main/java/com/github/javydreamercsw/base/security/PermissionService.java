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
import java.util.Optional;
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

    // Fetch the wrestler directly using the username from the security context
    Optional<Wrestler> userWrestlerOpt =
        accountRepository
            .findByUsername(userDetails.getUsername())
            .flatMap(wrestlerRepository::findByAccount);

    if (userWrestlerOpt.isEmpty()) {
      log.warn("isOwner: No wrestler found for user: {}", userDetails.getUsername());
      return false; // User does not have a wrestler assigned
    }
    Wrestler userWrestler = userWrestlerOpt.get();
    log.info(
        "isOwner: User {} has wrestler: {} (ID: {})",
        userDetails.getUsername(),
        userWrestler.getName(),
        userWrestler.getId());

    if (targetDomainObject instanceof Wrestler targetWrestler) {
      boolean match = userWrestler.getId().equals(targetWrestler.getId());
      if (!match)
        log.warn(
            "isOwner: Wrestler mismatch. User wrestler ID: {}, Target wrestler ID: {}",
            userWrestler.getId(),
            targetWrestler.getId());
      return match;
    } else if (targetDomainObject instanceof Deck deck) {
      Wrestler deckWrestler = deck.getWrestler();
      boolean match = deckWrestler != null && userWrestler.getId().equals(deckWrestler.getId());
      if (!match)
        log.warn(
            "isOwner: Deck wrestler mismatch. User wrestler ID: {}, Deck wrestler: {}",
            userWrestler.getId(),
            deckWrestler != null ? deckWrestler.getId() : "null");
      return match;
    } else if (targetDomainObject instanceof DeckCard deckCard) {
      Deck deck = deckCard.getDeck();
      if (deck != null) {
        Wrestler deckWrestler = deck.getWrestler();
        return deckWrestler != null && userWrestler.getId().equals(deckWrestler.getId());
      }
    } else if (targetDomainObject instanceof InboxItem inboxItem) {
      return inboxItem.getTargets().stream()
          .anyMatch(target -> target.getTargetId().equals(userWrestler.getId().toString()));
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
