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

import com.github.javydreamercsw.management.domain.deck.Deck;
import com.github.javydreamercsw.management.domain.deck.DeckCard;
import com.github.javydreamercsw.management.domain.inbox.InboxItem;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.util.Collection;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service("permissionService")
public class PermissionService {

  public boolean isOwner(Object targetDomainObject) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      return false;
    }
    if (targetDomainObject instanceof Wrestler wrestler) {
      if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
        // Check if the wrestler is a player wrestler and if the account matches.
        if (wrestler.getIsPlayer()) {
          return wrestler.getAccount().getUsername().equals(userDetails.getUsername());
        }
      }
    } else if (targetDomainObject instanceof Deck deck) {
      if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
        // Check if the deck's wrestler is a player wrestler and if the account matches.
        Wrestler wrestler = deck.getWrestler();
        if (wrestler != null && wrestler.getIsPlayer()) {
          return wrestler.getAccount().getUsername().equals(userDetails.getUsername());
        }
      }
    } else if (targetDomainObject instanceof DeckCard deckCard) {
      if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
        // Check if the deck card's deck's wrestler is a player wrestler and if the account
        // matches.
        Deck deck = deckCard.getDeck();
        if (deck != null) {
          Wrestler wrestler = deck.getWrestler();
          if (wrestler != null && wrestler.getIsPlayer()) {
            return wrestler.getAccount().getUsername().equals(userDetails.getUsername());
          }
        }
      }
    } else if (targetDomainObject instanceof InboxItem inboxItem) {
      if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
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
