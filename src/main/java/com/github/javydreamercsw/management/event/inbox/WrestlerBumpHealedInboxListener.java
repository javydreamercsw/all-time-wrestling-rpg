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
import com.github.javydreamercsw.management.event.dto.WrestlerBumpHealedEvent;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class WrestlerBumpHealedInboxListener
    implements ApplicationListener<WrestlerBumpHealedEvent> {

  private final InboxService inboxService;
  private final InboxEventType wrestlerBumpHealed;

  public WrestlerBumpHealedInboxListener(
      @NonNull InboxService inboxService, @NonNull InboxEventType wrestlerBumpHealed) {
    this.inboxService = inboxService;
    this.wrestlerBumpHealed = wrestlerBumpHealed;
  }

  @Override
  public void onApplicationEvent(@NonNull WrestlerBumpHealedEvent event) {
    log.info("Received WrestlerBumpHealedEvent for wrestler: {}", event.getWrestler().getName());
    inboxService.createInboxItem(
        wrestlerBumpHealed,
        String.format(
            "Wrestler %s healed a bump. Total bumps: %d",
            event.getWrestler().getName(), event.getWrestler().getBumps()),
        event.getWrestler().getId().toString());
  }
}
