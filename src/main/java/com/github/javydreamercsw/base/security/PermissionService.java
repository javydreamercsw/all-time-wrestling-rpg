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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("permissionService")
@Transactional(readOnly = true)
public class PermissionService {

  @Autowired private AccountRepository accountRepository;
  @Autowired private WrestlerRepository wrestlerRepository;

  public boolean isOwner(Object targetDomainObject) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      return false;
    }

    Object principal = authentication.getPrincipal();
    String username = authentication.getName();

    if (targetDomainObject instanceof Wrestler wrestler) {
      // Strategy 1: Check by ID if principal has wrestler info
      if (principal instanceof CustomUserDetails userDetails) {
        Wrestler userWrestler = userDetails.getWrestler();
        if (userWrestler != null
            && wrestler.getId() != null
            && userWrestler.getId().equals(wrestler.getId())) {
          return true;
        }
      }

      // Strategy 2: Check by Account Username on the passed object
      if (wrestler.getAccount() != null && username != null) {
        if (username.equals(wrestler.getAccount().getUsername())) {
          return true;
        }
      }

      // Strategy 3: Reload from DB to be sure (handles detached objects or missing account info)
      if (wrestler.getId() != null) {
        return wrestlerRepository
            .findById(wrestler.getId())
            .map(
                dbWrestler -> {
                  if (dbWrestler.getAccount() != null && username != null) {
                    return username.equals(dbWrestler.getAccount().getUsername());
                  }
                  return false;
                })
            .orElse(false);
      }

    } else if (targetDomainObject instanceof Deck deck) {
      Wrestler wrestler = deck.getWrestler();
      if (wrestler != null) {
        return isOwner(wrestler);
      }
    } else if (targetDomainObject instanceof DeckCard deckCard) {
      Deck deck = deckCard.getDeck();
      if (deck != null) {
        return isOwner(deck);
      }
    } else if (targetDomainObject instanceof InboxItem inboxItem) {
      if (principal instanceof CustomUserDetails userDetails) {
        // Check if the inbox item's target is the current user's wrestler.
        Wrestler wrestler = userDetails.getWrestler();
        if (wrestler != null) {
          return inboxItem.getTargets().stream()
              .anyMatch(target -> target.getTargetId().equals(wrestler.getId().toString()));
        }
      }
    } else if (targetDomainObject instanceof Collection<?> collection) {
      if (collection.isEmpty()) {
        return true;
      }
      // Check if all items in the collection are owned by the user
      return collection.stream().allMatch(this::isOwner);
    }
    return false;
  }
}
