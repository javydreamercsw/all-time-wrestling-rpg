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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for checking object ownership and providing fine-grained access control. Used primarily
 * in SpEL expressions within @PreAuthorize annotations.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PermissionService {

  private final WrestlerRepository wrestlerRepository;
  private final AccountRepository accountRepository;

  /**
   * Checks if the currently authenticated user owns the target domain object.
   *
   * @param targetDomainObject The object to check ownership for.
   * @return True if the user is the owner, false otherwise.
   */
  public boolean isOwner(@NonNull Object targetDomainObject) {
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
      return false;
    }

    log.debug("isOwner: Checking ownership for user: {}", userDetails.getUsername());

    Set<Long> ownedWrestlerIds = new HashSet<>();

    // Always fetch from repo to handle integration tests with mock users
    // where the transient ID in principal might not match the persistent ID in DB.
    // We prioritize the database as the source of truth for ownership.
    accountRepository
        .findByUsername(userDetails.getUsername())
        .ifPresent(
            account -> {
              List<Wrestler> wrestlers = wrestlerRepository.findByAccount(account);
              wrestlers.forEach(w -> ownedWrestlerIds.add(w.getId()));
            });

    if (ownedWrestlerIds.isEmpty()) {
      log.debug("isOwner: No wrestlers found in DB for user: {}", userDetails.getUsername());
      return false; // User does not have any wrestlers assigned in the database
    }

    if (targetDomainObject instanceof Wrestler targetWrestler) {
      Long targetId = targetWrestler.getId();
      return targetId != null && ownedWrestlerIds.contains(targetId);
    }

    if (targetDomainObject instanceof Deck deck) {
      Wrestler deckWrestler = deck.getWrestler();
      if (deckWrestler == null) {
        return false;
      }
      Long wrestlerId = deckWrestler.getId();
      return wrestlerId != null && ownedWrestlerIds.contains(wrestlerId);
    }

    if (targetDomainObject instanceof DeckCard deckCard) {
      Deck deck = deckCard.getDeck();
      if (deck != null && deck.getWrestler() != null) {
        Long wrestlerId = deck.getWrestler().getId();
        return ownedWrestlerIds.contains(wrestlerId);
      }
    }

    if (targetDomainObject instanceof InboxItem inboxItem) {
      return inboxItem.getTargets().stream()
          .anyMatch(
              target -> {
                try {
                  return ownedWrestlerIds.contains(Long.valueOf(target.getTargetId()));
                } catch (NumberFormatException e) {
                  return false;
                }
              });
    }

    if (targetDomainObject instanceof Collection<?> collection) {
      if (collection.isEmpty()) {
        return false;
      }
      List<?> copy = new java.util.ArrayList<>(collection);
      return copy.stream().allMatch(this::isOwner);
    }

    log.warn(
        "isOwner: Unsupported target domain object: {} (Class: {})",
        targetDomainObject,
        targetDomainObject.getClass().getName());
    return false;
  }

  /**
   * Checks if the currently authenticated user owns the object with the specified ID and type.
   *
   * @param targetId The ID of the object.
   * @param targetType The type of the object (e.g., "Wrestler", "Deck").
   * @return True if the user is the owner, false otherwise.
   */
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
