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
package com.github.javydreamercsw.management.ui.component;

import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleOpportunity;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.title.TitleOpportunityService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

/**
 * The Money in the Bank-style briefcase section (ATW-8p72, shared by WrestlerCareerView and the
 * wrestler profile per ATW-312z): the held case with its Cash In action, plus the CASHED_IN/EXPIRED
 * history. Cash-in books a title match on a chosen show against a chosen championship's reigning
 * champion; {@code onBooked} lets the embedding view refresh.
 */
@Slf4j
public class BriefcaseSection extends VerticalLayout {

  private static final DateTimeFormatter DATE_FMT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());

  public BriefcaseSection(
      long wrestlerId,
      TitleOpportunityService titleOpportunityService,
      TitleService titleService,
      ShowService showService,
      @Nullable Runnable onBooked) {
    setPadding(false);
    setSpacing(true);
    add(new H3("Briefcase"));

    List<TitleOpportunity> opportunities = titleOpportunityService.findByWrestler(wrestlerId);

    if (opportunities.isEmpty()) {
      add(new Paragraph("No briefcase opportunities on record."));
      return;
    }

    Optional<TitleOpportunity> held =
        opportunities.stream().filter(TitleOpportunity::isHeld).findFirst();
    if (held.isPresent()) {
      add(heldRow(held.get(), titleOpportunityService, titleService, showService, onBooked));
    }

    add(historyGrid(opportunities, titleService));
  }

  private Component heldRow(
      TitleOpportunity current,
      TitleOpportunityService titleOpportunityService,
      TitleService titleService,
      ShowService showService,
      @Nullable Runnable onBooked) {
    String division = current.getGender() != null ? " — " + current.getGender() : "";
    Span badge = new Span("💼 " + current.getName() + division + " — HELD");
    badge
        .getElement()
        .setAttribute(
            "style",
            "background:var(--lumo-primary-color-10pct);border-radius:var(--lumo-border-radius-m);padding:4px"
                + " 10px;font-weight:600");
    HorizontalLayout heldRow = new HorizontalLayout(badge);
    heldRow.setAlignItems(FlexComponent.Alignment.CENTER);
    // Uploaded artwork (ATW-jpki) replaces the emoji prefix visually — show it at the row start.
    if (current.getImageUrl() != null && !current.getImageUrl().isBlank()) {
      Image artwork = new Image(current.getImageUrl(), current.getName() + " briefcase");
      artwork.setHeight("48px");
      artwork.setWidth("48px");
      artwork.addClassNames("border-radius-m");
      heldRow.addComponentAsFirst(artwork);
    }
    Span expiry =
        new Span(
            "Earned "
                + current.getEarnedAt()
                + (current.getExpiryDate() != null
                    ? " — cashable until " + current.getExpiryDate()
                    : ""));
    expiry.getStyle().set("color", "var(--lumo-secondary-text-color)");
    heldRow.add(expiry);
    Button cashInBtn =
        new Button(
            "Cash In",
            e ->
                new BriefcaseCashInDialog(
                        current, titleOpportunityService, titleService, showService, onBooked)
                    .open());
    cashInBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    cashInBtn.setTooltipText(
        "Book a title match on the spot against the reigning champion of any active"
            + " championship. The briefcase is spent whether the match is won or lost.");
    heldRow.add(cashInBtn);
    return heldRow;
  }

  private Grid<TitleOpportunity> historyGrid(
      List<TitleOpportunity> opportunities, TitleService titleService) {
    Grid<TitleOpportunity> grid = new Grid<>();
    grid.addColumn(TitleOpportunity::getName).setHeader("Opportunity").setAutoWidth(true);
    grid.addColumn(o -> DATE_FMT.format(o.getEarnedAt())).setHeader("Earned").setAutoWidth(true);
    grid.addColumn(o -> o.getStatus().name()).setHeader("Status").setAutoWidth(true);
    // Same eager-resolution discipline as the title-reign grid: the cashed-against title is a
    // lazy proxy and grid value providers run outside a transaction.
    Map<Long, String> cashedAgainst = new HashMap<>();
    opportunities.stream()
        .filter(o -> o.getCashedAgainstTitle() != null)
        .forEach(
            o ->
                cashedAgainst.put(
                    o.getId(),
                    titleService
                        .getTitleById(o.getCashedAgainstTitle().getId())
                        .map(Title::getName)
                        .orElse("?")));
    grid.addColumn(o -> cashedAgainst.getOrDefault(o.getId(), "—"))
        .setHeader("Cashed Against")
        .setAutoWidth(true);
    grid.setItems(opportunities);
    grid.setAllRowsVisible(true);
    grid.setWidthFull();
    return grid;
  }
}
