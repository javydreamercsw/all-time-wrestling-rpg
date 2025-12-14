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
package com.github.javydreamercsw.management.event;

import com.github.javydreamercsw.management.domain.inbox.InboxEventType;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import lombok.NonNull;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class HeatChangeInboxListener implements ApplicationListener<HeatChangeEvent> {

  private final InboxService inboxService;
  private final InboxEventType rivalryHeatChange;

  public HeatChangeInboxListener(
      @NonNull InboxService inboxService, @NonNull InboxEventType rivalryHeatChange) {
    this.inboxService = inboxService;
    this.rivalryHeatChange = rivalryHeatChange;
  }

  @Override
  public void onApplicationEvent(@NonNull HeatChangeEvent event) {
    String message =
        String.format(
            "Rivalry between %s and %s %s %d heat. New total: %d. Reason: %s",
            event.getWrestlers().get(0).getName(),
            event.getWrestlers().get(1).getName(),
            (event.getNewHeat() - event.getOldHeat()) > 0 ? "gained" : "lost",
            Math.abs(event.getNewHeat() - event.getOldHeat()),
            event.getNewHeat(),
            event.getReason());

    // Assuming the rivalry ID is the relevant reference for the inbox item
    inboxService.createInboxItem(rivalryHeatChange, message, event.getRivalryId().toString());
  }
}
