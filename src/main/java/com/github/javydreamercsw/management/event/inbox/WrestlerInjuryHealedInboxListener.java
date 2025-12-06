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
package com.github.javydreamercsw.management.event.inbox;

import com.github.javydreamercsw.management.domain.inbox.InboxEventType;
import com.github.javydreamercsw.management.event.dto.WrestlerInjuryHealedEvent;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class WrestlerInjuryHealedInboxListener {

  private final InboxService inboxService;

  @EventListener
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handleWrestlerInjuryHealedEvent(WrestlerInjuryHealedEvent event) {
    String message =
        String.format(
            "%s's injury (%s) has been healed!",
            event.getWrestler().getName(), event.getInjury().getName());
    inboxService.createInboxItem(
        InboxEventType.WRESTLER_INJURY_HEALED, message, event.getWrestler().getId().toString());
    log.info("Inbox item created for WrestlerInjuryHealedEvent: {}", message);
  }
}
