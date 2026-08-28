/*
* Copyright (C) 2026 Software Consulting Dreams LLC
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

import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.event.FeudResolvedEvent;
import com.github.javydreamercsw.management.service.title.ContenderSelectionService;
import com.github.javydreamercsw.management.service.title.TitleService;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * After a feud resolves, rotates the #1 contender for every title that had a feud participant
 * registered as a challenger — the arc is over, so the rankings decide who is next.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FeudResolvedContenderListener implements ApplicationListener<FeudResolvedEvent> {

  private final TitleService titleService;
  private final ContenderSelectionService contenderSelectionService;

  @Override
  public void onApplicationEvent(@NonNull final FeudResolvedEvent event) {
    List<Wrestler> participants = event.getFeud().getActiveWrestlers();
    if (participants.isEmpty()) {
      return;
    }
    List<Title> affectedTitles =
        titleService.getActiveTitles().stream()
            .filter(title -> title.getChallengers().stream().anyMatch(participants::contains))
            .toList();
    for (Title title : affectedTitles) {
      log.info(
          "Feud '{}' resolved; auto-selecting next contender for {}",
          event.getFeud().getName(),
          title.getName());
      contenderSelectionService.autoSelectNextContender(title);
    }
  }
}
