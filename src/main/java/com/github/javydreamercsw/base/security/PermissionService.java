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
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("permissionService")
@Transactional(readOnly = true)
@Slf4j
public class PermissionService {

  private final WrestlerRepository wrestlerRepository;
  private final AccountRepository accountRepository;

  public PermissionService(
      @NonNull WrestlerRepository wrestlerRepository,
      @NonNull AccountRepository accountRepository) {
    this.wrestlerRepository = wrestlerRepository;
    this.accountRepository = accountRepository;
  }

  public boolean isOwner(@NonNull Object targetDomainObject) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      log.warn("isOwner: Authentication is null");
      return false;
    }

    Object principal = authentication.getPrincipal();
    log.debug(
        "isOwner: Principal class: {}, Target object class: {}",
        principal.getClass().getName(),
        targetDomainObject.getClass().getName());

    if (!(principal instanceof UserDetails userDetails)) {
      log.warn(
          "isOwner: Principal is not UserDetails: {}",
          principal != null ? principal.getClass().getName() : "null");
      return false;
    }

    log.debug("isOwner: Checking ownership for user: {}", userDetails.getUsername());

    java.util.Set<Long> ownedWrestlerIds = new java.util.HashSet<>();

    if (principal instanceof CustomUserDetails customUserDetails
        && customUserDetails.getWrestler() != null) {
      log.debug(
          "isOwner: Found wrestler in CustomUserDetails: {}",
          customUserDetails.getWrestler().getId());
      ownedWrestlerIds.add(customUserDetails.getWrestler().getId());
    }

    // Always fetch from repo as well to handle integration tests with mock users
    // where the transient ID in principal might not match the persistent ID in DB.
    log.debug(
        "isOwner: Fetching wrestlers from repository for user: {}", userDetails.getUsername());
    accountRepository
        .findByUsername(userDetails.getUsername())
        .ifPresentOrElse(
            account -> {
              log.debug(
                  "isOwner: Found persistent account ID: {} for username: {}",
                  account.getId(),
                  account.getUsername());
              java.util.List<Wrestler> wrestlers = wrestlerRepository.findByAccount(account);
              log.debug(
                  "isOwner: Found {} wrestlers in DB for account ID: {}",
                  wrestlers.size(),
                  account.getId());
              wrestlers.forEach(
                  w -> {
                    log.debug("isOwner: User owns Wrestler ID: {} ({})", w.getId(), w.getName());
                    ownedWrestlerIds.add(w.getId());
                  });
            },
            () ->
                log.warn(
                    "isOwner: Account not found in DB for username: {}",
                    userDetails.getUsername()));

    if (ownedWrestlerIds.isEmpty()) {
      log.warn("isOwner: No wrestlers found for user: {}", userDetails.getUsername());
      return false; // User does not have any wrestlers assigned
    }

    log.debug("isOwner: Total owned wrestler IDs: {}", ownedWrestlerIds);

    if (targetDomainObject instanceof Wrestler targetWrestler) {
      Long targetId = targetWrestler.getId();
      log.debug("isOwner: Checking Wrestler ID: {} against owned: {}", targetId, ownedWrestlerIds);
      if (targetId != null && ownedWrestlerIds.contains(targetId)) {
        return true;
      }

      // If ID check fails, try a name check if both have names (for detached/mock entities)
      if (principal instanceof CustomUserDetails customUserDetails
          && customUserDetails.getWrestler() != null) {
        Wrestler userWrestler = customUserDetails.getWrestler();
        if (userWrestler.getName() != null
            && userWrestler.getName().equals(targetWrestler.getName())) {
          log.debug("isOwner: Match by name: {}", targetWrestler.getName());
          return true;
        }
      }

      // Try checking the account username directly
      if (targetWrestler.getAccount() != null
          && targetWrestler.getAccount().getUsername() != null
          && targetWrestler.getAccount().getUsername().equals(userDetails.getUsername())) {
        log.debug("isOwner: Match by account username: {}", userDetails.getUsername());
        return true;
      }

      return false;
    }

    if (targetDomainObject instanceof Deck deck) {
      Wrestler deckWrestler = deck.getWrestler();
      if (deckWrestler == null) {
        log.warn("isOwner: Deck {} has no wrestler", deck.getId());
        return false;
      }
      Long wrestlerId = deckWrestler.getId();
      log.debug(
          "isOwner: Checking Deck {} (Wrestler ID: {}) against owned: {}",
          deck.getId(),
          wrestlerId,
          ownedWrestlerIds);
      return wrestlerId != null && ownedWrestlerIds.contains(wrestlerId);
    }

    if (targetDomainObject instanceof DeckCard deckCard) {
      Deck deck = deckCard.getDeck();
      if (deck != null && deck.getWrestler() != null) {
        Long wrestlerId = deck.getWrestler().getId();
        log.debug("isOwner: Checking DeckCard associated with Wrestler ID: {}", wrestlerId);
        return ownedWrestlerIds.contains(wrestlerId);
      }
    }

    if (targetDomainObject instanceof InboxItem inboxItem) {
      log.debug("isOwner: Checking InboxItem: {}", inboxItem.getId());
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
        return true;
      }
      log.debug("isOwner: Checking collection of size: {}", collection.size());
      java.util.List<?> copy = new java.util.ArrayList<>(collection);
      return copy.stream().allMatch(this::isOwner);
    }

    log.warn(
        "isOwner: Unsupported target domain object: {} (Class: {})",
        targetDomainObject,
        targetDomainObject.getClass().getName());
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
