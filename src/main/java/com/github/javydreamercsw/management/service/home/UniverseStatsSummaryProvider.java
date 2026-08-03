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
package com.github.javydreamercsw.management.service.home;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UniverseStatsSummaryProvider implements LandingPageSummaryProvider {

  private final WrestlerService wrestlerService;
  private final TitleService titleService;
  private final ShowService showService;

  @Override
  public String getTitle() {
    return "Universe at a Glance";
  }

  @Override
  public int getOrder() {
    return 20;
  }

  @Override
  public Set<RoleName> applicableRoles() {
    return Set.of(RoleName.ADMIN, RoleName.BOOKER);
  }

  @Override
  public Component buildSummaryCard(final LocalDateTime since, final Account account) {
    int activeWrestlers = wrestlerService.findAllActiveWrestlers().size();
    int activeTitles = titleService.getActiveTitles().size();
    List<?> upcomingShows = showService.getUpcomingShows(5);

    VerticalLayout card = new VerticalLayout();
    card.setPadding(false);
    card.setSpacing(false);

    card.add(statRow("Active Wrestlers", activeWrestlers));
    card.add(statRow("Active Titles", activeTitles));
    card.add(statRow("Upcoming Shows", upcomingShows.size()));

    return card;
  }

  private Span statRow(final String label, final int value) {
    Span span = new Span(label + ": " + value);
    span.getStyle().set("font-size", "small");
    return span;
  }
}
