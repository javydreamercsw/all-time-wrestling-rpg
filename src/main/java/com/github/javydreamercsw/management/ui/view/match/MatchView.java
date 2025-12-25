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
package com.github.javydreamercsw.management.ui.view.match;

import com.github.javydreamercsw.base.domain.wrestler.WrestlerStats;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.ui.view.MainLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "match/:matchId", layout = MainLayout.class)
@PageTitle("Match | ATW RPG")
@PermitAll
@Slf4j
public class MatchView extends VerticalLayout implements BeforeEnterObserver {

  private final SegmentService segmentService;
  private final WrestlerService wrestlerService;
  private final SecurityUtils securityUtils;
  private Segment segment;

  @Autowired
  public MatchView(
      SegmentService segmentService, WrestlerService wrestlerService, SecurityUtils securityUtils) {
    this.segmentService = segmentService;
    this.wrestlerService = wrestlerService;
    this.securityUtils = securityUtils;
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    try {
      removeAll();
      add(new H2("Match Details"));
      String matchId = event.getRouteParameters().get("matchId").orElse(null);
      if (matchId != null) {
        Optional<Segment> foundSegment = segmentService.findByIdWithShow(Long.valueOf(matchId));
        if (foundSegment.isPresent()) {
          segment = foundSegment.get();
          buildView();
        } else {
          add(new H2("Match not found."));
        }
      }
    } catch (Exception e) {
      log.error("Error in MatchView.beforeEnter", e);
      add(new H2("Error displaying match details."));
    }
  }

  private void buildView() {
    setId("match-view-" + segment.getId());
    Wrestler playerWrestler = securityUtils.getAuthenticatedUser().get().getWrestler();

    H3 showName = new H3("Show: " + segment.getShow().getName());
    showName.setId("show-name");
    add(showName);

    Paragraph matchType = new Paragraph("Match Type: " + segment.getSegmentType().getName());
    matchType.setId("match-type");
    add(matchType);

    add(new H3("Participants"));
    List<Wrestler> opponents =
        segment.getWrestlers().stream()
            .filter(w -> !w.equals(playerWrestler))
            .collect(Collectors.toList());

    add(new Paragraph("Your Wrestler: " + playerWrestler.getName()));
    add(
        new Paragraph(
            "Opponents: "
                + opponents.stream().map(Wrestler::getName).collect(Collectors.joining(", "))));

    opponents.forEach(
        opponent -> {
          add(createOpponentSummary(opponent));
        });

    add(new H3("Match Rules"));
    if (segment.getSegmentRules().isEmpty()) {
      add(new Paragraph("No special rules."));
    } else {
      segment.getSegmentRules().forEach(rule -> add(new Paragraph("- " + rule.getName())));
    }

    add(new H3("Titles on the Line"));
    if (segment.getTitles().isEmpty()) {
      add(new Paragraph("No titles on the line."));
    } else {
      segment.getTitles().forEach(title -> add(new Paragraph("- " + title.getName())));
    }
  }

  private VerticalLayout createOpponentSummary(Wrestler opponent) {
    VerticalLayout summary = new VerticalLayout();
    summary.add(new H3("Opponent: " + opponent.getName()));

    Optional<WrestlerStats> stats = wrestlerService.getWrestlerStats(opponent.getId());
    if (stats.isPresent()) {
      WrestlerStats wrestlerStats = stats.get();
      summary.add(new Paragraph("Wins: " + wrestlerStats.getWins()));
      summary.add(new Paragraph("Losses: " + wrestlerStats.getLosses()));
    }

    Wrestler opponentWithInjuries = wrestlerService.findByIdWithInjuries(opponent.getId()).get();

    summary.add(new Paragraph("Bumps: " + opponentWithInjuries.getBumps()));
    if (opponentWithInjuries.getInjuries().isEmpty()) {
      summary.add(new Paragraph("No current injuries."));
    } else {
      opponentWithInjuries
          .getInjuries()
          .forEach(
              injury -> {
                summary.add(new Paragraph("- " + injury.getDisplayString()));
              });
    }

    return summary;
  }
}
