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
package com.github.javydreamercsw.management.ui.view.season;

import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.season.SeasonAward;
import com.github.javydreamercsw.management.service.show.ShowContextFacade;
import com.github.javydreamercsw.management.ui.ViewContext;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Route("season-detail/:seasonId")
@PageTitle("Season Detail")
@PermitAll
public class SeasonDetailView extends Main implements BeforeEnterObserver {

  private static final DateTimeFormatter DATE_FMT =
      DateTimeFormatter.ofPattern("MMM dd, yyyy").withZone(ZoneId.systemDefault());

  private final ShowContextFacade showContextFacade;

  private Season season;

  public SeasonDetailView(
      final ShowContextFacade showContextFacade, final ViewContext viewContext) {
    this.showContextFacade = showContextFacade;
    setSizeFull();
  }

  @Override
  public void beforeEnter(final BeforeEnterEvent event) {
    Optional<String> idParam = event.getRouteParameters().get("seasonId");
    if (idParam.isEmpty()) {
      event.forwardTo(SeasonListView.class);
      return;
    }
    try {
      long seasonId = Long.parseLong(idParam.get());
      Optional<Season> opt = showContextFacade.getSeasonService().getSeasonByIdWithShows(seasonId);
      if (opt.isEmpty()) {
        event.forwardTo(SeasonListView.class);
        return;
      }
      season = opt.get();
      buildView();
    } catch (NumberFormatException e) {
      event.forwardTo(SeasonListView.class);
    }
  }

  private void buildView() {
    removeAll();

    VerticalLayout content = new VerticalLayout();
    content.setPadding(true);
    content.setSpacing(true);
    content.setWidthFull();

    content.add(new ViewToolbar(season.getName() + " — Season Detail"));
    content.add(buildBackLink());
    content.add(buildSeasonStats());
    content.add(buildAwardsCeremony());

    add(content);
  }

  private Component buildBackLink() {
    RouterLink back = new RouterLink("← Back to Seasons", SeasonListView.class);
    back.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
    return back;
  }

  private Component buildSeasonStats() {
    VerticalLayout section = new VerticalLayout();
    section.setPadding(false);
    section.setSpacing(true);
    section.add(new H3("Season Overview"));

    HorizontalLayout cards = new HorizontalLayout();
    cards.setSpacing(true);

    String status = Boolean.TRUE.equals(season.getIsActive()) ? "Active" : "Completed";
    String statusColor =
        Boolean.TRUE.equals(season.getIsActive())
            ? "var(--lumo-success-color)"
            : "var(--lumo-secondary-text-color)";

    cards.add(statCard("Status", status, statusColor));
    cards.add(
        statCard(
            "Total Shows", String.valueOf(season.getTotalShows()), "var(--lumo-primary-color)"));
    cards.add(
        statCard(
            "Start Date",
            season.getStartDate() != null ? DATE_FMT.format(season.getStartDate()) : "—",
            "var(--lumo-header-text-color)"));
    cards.add(
        statCard(
            "End Date",
            season.getEndDate() != null ? DATE_FMT.format(season.getEndDate()) : "Ongoing",
            "var(--lumo-warning-color)"));
    cards.add(
        statCard("Duration", season.getDurationDays() + " days", "var(--lumo-primary-text-color)"));

    section.add(cards);
    return section;
  }

  private Component buildAwardsCeremony() {
    VerticalLayout section = new VerticalLayout();
    section.setPadding(false);
    section.setSpacing(true);
    section.add(new H3("Awards Ceremony"));

    List<SeasonAward> awards = showContextFacade.getSeasonAwardsService().getAwards(season.getId());

    if (awards.isEmpty()) {
      section.add(
          new Paragraph(
              Boolean.TRUE.equals(season.getIsActive())
                  ? "Awards will be presented when the season ends."
                  : "No awards were recorded for this season."));
      return section;
    }

    Grid<SeasonAward> grid = new Grid<>();
    grid.addColumn(a -> a.getAwardType().getDisplayName())
        .setHeader("Award")
        .setAutoWidth(true)
        .setSortable(true);
    grid.addColumn(a -> a.getWrestler().getName())
        .setHeader("Winner")
        .setAutoWidth(true)
        .setSortable(true);
    grid.addColumn(SeasonAward::getAwardValue).setHeader("Achievement").setAutoWidth(true);
    grid.addColumn(a -> DATE_FMT.format(a.getAwardedAt())).setHeader("Date").setAutoWidth(true);
    grid.setItems(awards);
    grid.setAllRowsVisible(true);
    grid.setWidthFull();

    section.add(grid);
    return section;
  }

  private Div statCard(final String label, final String value, final String color) {
    Div card = new Div();
    card.addClassNames(
        LumoUtility.BorderRadius.MEDIUM, LumoUtility.Padding.MEDIUM, LumoUtility.BoxShadow.SMALL);
    card.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
    card.getStyle().set("min-width", "120px");

    Span val = new Span(value);
    val.addClassNames(LumoUtility.FontSize.XLARGE, LumoUtility.FontWeight.BOLD);
    val.getStyle().set("color", color);

    Span lbl = new Span(label);
    lbl.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);

    Div inner = new Div(val, new Div(lbl));
    inner.addClassName(LumoUtility.Display.FLEX);
    inner.getStyle().set("flex-direction", "column");
    card.add(inner);
    return card;
  }
}
