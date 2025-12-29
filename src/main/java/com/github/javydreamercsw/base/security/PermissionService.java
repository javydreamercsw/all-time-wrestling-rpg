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
import org.springframework.transaction.annotation.Transactional;

@Service("permissionService")
@Transactional(readOnly = true)
public class PermissionService {

  public boolean isOwner(Object targetDomainObject) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      return false;
    }

    Object principal = authentication.getPrincipal();
    if (!(principal instanceof CustomUserDetails userDetails)) {
      return false; // Or handle anonymous user differently
    }

    Wrestler userWrestler = userDetails.getWrestler();
    if (userWrestler == null) {
      return false; // User does not have a wrestler assigned
    }

    if (targetDomainObject instanceof Wrestler targetWrestler) {
      return userWrestler.getId().equals(targetWrestler.getId());
    } else if (targetDomainObject instanceof Deck deck) {
      Wrestler deckWrestler = deck.getWrestler();
      return deckWrestler != null && userWrestler.getId().equals(deckWrestler.getId());
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
      // Check if all items in the collection are owned by the user
      return collection.stream().allMatch(this::isOwner);
    }

    return false;
  }
}
