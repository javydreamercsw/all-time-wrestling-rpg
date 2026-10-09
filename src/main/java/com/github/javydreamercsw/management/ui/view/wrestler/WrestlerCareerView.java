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
package com.github.javydreamercsw.management.ui.view.wrestler;

import com.github.appreciated.apexcharts.ApexCharts;
import com.github.appreciated.apexcharts.ApexChartsBuilder;
import com.github.appreciated.apexcharts.config.builder.ChartBuilder;
import com.github.appreciated.apexcharts.config.builder.DataLabelsBuilder;
import com.github.appreciated.apexcharts.config.builder.StrokeBuilder;
import com.github.appreciated.apexcharts.config.builder.XAxisBuilder;
import com.github.appreciated.apexcharts.config.chart.Type;
import com.github.appreciated.apexcharts.config.stroke.Curve;
import com.github.appreciated.apexcharts.helper.Series;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerStats;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleOpportunity;
import com.github.javydreamercsw.management.domain.title.TitleReign;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateHistory;
import com.github.javydreamercsw.management.service.show.ShowFacade;
import com.github.javydreamercsw.management.service.title.TitleOpportunityService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerFacade;
import com.github.javydreamercsw.management.ui.ViewContext;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Route("wrestler-career/:wrestlerId")
@PageTitle("Wrestler Career")
@PermitAll
@Slf4j
public class WrestlerCareerView extends Main implements BeforeEnterObserver {

  private static final DateTimeFormatter DATE_FMT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());

  private final WrestlerFacade wrestlerFacade;
  private final ShowFacade showFacade;
  private final ViewContext viewContext;

  private Wrestler wrestler;
  private Long universeId;

  public WrestlerCareerView(
      final WrestlerFacade wrestlerFacade,
      final ShowFacade showFacade,
      final ViewContext viewContext) {
    this.wrestlerFacade = wrestlerFacade;
    this.showFacade = showFacade;
    this.viewContext = viewContext;
    setSizeFull();
  }

  @Override
  public void beforeEnter(final BeforeEnterEvent event) {
    Optional<String> idParam = event.getRouteParameters().get("wrestlerId");
    if (idParam.isEmpty()) {
      event.forwardTo(WrestlerListView.class);
      return;
    }

    try {
      long wrestlerId = Long.parseLong(idParam.get());
      Optional<Wrestler> opt = wrestlerFacade.getWrestlerService().findById(wrestlerId);
      if (opt.isEmpty()) {
        event.forwardTo(WrestlerListView.class);
        return;
      }
      wrestler = opt.get();
      universeId = viewContext.getUniverseContextService().getCurrentUniverseId();
      buildView();
    } catch (NumberFormatException e) {
      event.forwardTo(WrestlerListView.class);
    }
  }

  private void buildView() {
    removeAll();

    VerticalLayout content = new VerticalLayout();
    content.setPadding(true);
    content.setSpacing(true);
    content.setWidthFull();

    ViewToolbar toolbar = new ViewToolbar(wrestler.getName() + " — Career");
    content.add(toolbar);

    content.add(buildBackLink());
    content.add(buildCareerRecord());

    List<WrestlerStateHistory> history =
        wrestlerFacade.getWrestlerStateHistoryService().getHistory(wrestler.getId(), universeId);

    content.add(buildFanGrowthSection(history));
    content.add(buildTierProgressionSection(history));
    content.add(buildTitleReignSection());
    content.add(buildBriefcaseSection());
    content.add(buildInjuryLogSection());

    add(content);
  }

  private Component buildBackLink() {
    RouterLink back =
        new RouterLink(
            "← Back to Profile",
            WrestlerProfileView.class,
            new RouteParameters("wrestlerId", wrestler.getId().toString()));
    back.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
    return back;
  }

  private Component buildCareerRecord() {
    VerticalLayout section = new VerticalLayout();
    section.setPadding(false);
    section.setSpacing(true);
    section.add(new H3("Career Record"));

    Optional<WrestlerStats> statsOpt =
        universeId != null
            ? wrestlerFacade
                .getWrestlerStatsService()
                .getWrestlerStats(wrestler.getId(), universeId)
            : Optional.empty();

    WrestlerStats stats = statsOpt.orElse(new WrestlerStats(0L, 0L, 0L));

    HorizontalLayout cards = new HorizontalLayout();
    cards.setSpacing(true);
    cards.add(
        statCard("Wins", String.valueOf(stats.getWins()), "var(--lumo-success-color)"),
        statCard("Losses", String.valueOf(stats.getLosses()), "var(--lumo-error-color)"),
        statCard(
            "Win Rate", String.format("%.1f%%", stats.getWinRate()), "var(--lumo-primary-color)"),
        statCard(
            "Titles Held", String.valueOf(stats.getTitlesHeld()), "var(--lumo-warning-color)"));

    section.add(cards);
    return section;
  }

  private Div statCard(final String label, final String value, final String color) {
    Div card = new Div();
    card.addClassNames(
        LumoUtility.BorderRadius.MEDIUM, LumoUtility.Padding.MEDIUM, LumoUtility.BoxShadow.SMALL);
    card.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
    card.getStyle().set("min-width", "120px");

    Span val = new Span(value);
    val.addClassNames(LumoUtility.FontSize.XXLARGE, LumoUtility.FontWeight.BOLD);
    val.getStyle().set("color", color);

    Span lbl = new Span(label);
    lbl.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);

    Div inner = new Div(val, new Div(lbl));
    inner.addClassName(LumoUtility.Display.FLEX);
    inner.getStyle().set("flex-direction", "column");
    card.add(inner);
    return card;
  }

  private Component buildFanGrowthSection(final List<WrestlerStateHistory> history) {
    VerticalLayout section = new VerticalLayout();
    section.setPadding(false);
    section.setSpacing(true);
    section.add(new H3("Fan Growth"));

    if (history.isEmpty()) {
      section.add(
          new Paragraph("No history yet — fan growth will appear after shows are adjudicated."));
      return section;
    }

    Double[] fans = history.stream().map(h -> (double) h.getFans()).toArray(Double[]::new);
    String[] labels =
        history.stream().map(h -> DATE_FMT.format(h.getRecordedAt())).toArray(String[]::new);

    ApexCharts chart =
        ApexChartsBuilder.get()
            .withChart(
                ChartBuilder.get()
                    .withType(Type.AREA)
                    .withHeight("280px")
                    .withWidth("100%")
                    .build())
            .withStroke(StrokeBuilder.get().withCurve(Curve.SMOOTH).build())
            .withDataLabels(DataLabelsBuilder.get().withEnabled(false).build())
            .withSeries(new Series<>("Fans", fans))
            .withColors("#7c3aed")
            .withXaxis(XAxisBuilder.get().withCategories(labels).build())
            .build();
    chart.getElement().getStyle().set("width", "100%");

    section.add(chart);
    return section;
  }

  private Component buildTierProgressionSection(final List<WrestlerStateHistory> history) {
    VerticalLayout section = new VerticalLayout();
    section.setPadding(false);
    section.setSpacing(true);
    section.add(new H3("Tier Progression"));

    List<WrestlerStateHistory> tierChanges = extractTierChanges(history);

    if (tierChanges.isEmpty()) {
      section.add(new Paragraph("No tier changes recorded yet."));
      return section;
    }

    Grid<WrestlerStateHistory> grid = new Grid<>();
    grid.addColumn(h -> DATE_FMT.format(h.getRecordedAt())).setHeader("Date").setAutoWidth(true);
    grid.addColumn(h -> h.getTier().name()).setHeader("Tier").setAutoWidth(true);
    grid.addColumn(h -> String.format("%,d", h.getFans()))
        .setHeader("Fans at Time")
        .setAutoWidth(true);
    grid.setItems(tierChanges);
    grid.setAllRowsVisible(true);
    grid.setWidthFull();

    section.add(grid);
    return section;
  }

  private List<WrestlerStateHistory> extractTierChanges(final List<WrestlerStateHistory> history) {
    List<WrestlerStateHistory> changes = new ArrayList<>();
    for (int i = 0; i < history.size(); i++) {
      if (i == 0 || history.get(i).getTier() != history.get(i - 1).getTier()) {
        changes.add(history.get(i));
      }
    }
    return changes;
  }

  private Component buildTitleReignSection() {
    VerticalLayout section = new VerticalLayout();
    section.setPadding(false);
    section.setSpacing(true);
    section.add(new H3("Title Reign History"));

    List<TitleReign> reigns = wrestlerFacade.getTitleService().findReignsByChampion(wrestler);

    if (reigns.isEmpty()) {
      section.add(new Paragraph("No title reigns on record."));
      return section;
    }

    // Resolve titles eagerly while the session is open: the grid's value providers run outside
    // any transaction and the reign's lazy title proxy would throw LazyInitializationException
    // (seen on prod-shaped data, ATW-8p72 sandbox verification).
    Map<Long, String> titleNames = new HashMap<>();
    reigns.forEach(
        r ->
            titleNames.put(
                r.getId(),
                wrestlerFacade
                    .getTitleService()
                    .getTitleById(r.getTitle().getId())
                    .map(Title::getName)
                    .orElse("?")));

    Grid<TitleReign> grid = new Grid<>();
    grid.addColumn(r -> titleNames.getOrDefault(r.getId(), "?"))
        .setHeader("Title")
        .setAutoWidth(true);
    grid.addColumn(r -> "Reign #" + r.getReignNumber())
        .setHeader("Reign")
        .setAutoWidth(true)
        .setFlexGrow(0);
    grid.addColumn(r -> DATE_FMT.format(r.getStartDate())).setHeader("Won").setAutoWidth(true);
    grid.addColumn(r -> r.getEndDate() != null ? DATE_FMT.format(r.getEndDate()) : "Current")
        .setHeader("Lost")
        .setAutoWidth(true);
    grid.addColumn(r -> r.getReignLengthDisplay(Instant.now()))
        .setHeader("Duration")
        .setAutoWidth(true);
    grid.setItems(
        reigns.stream().sorted(Comparator.comparing(TitleReign::getStartDate).reversed()).toList());
    grid.setAllRowsVisible(true);
    grid.setWidthFull();

    section.add(grid);
    return section;
  }

  /**
   * The Money in the Bank-style briefcase section (ATW-8p72): held cases with a Cash In action,
   * plus the CASHED_IN/EXPIRED history. Cash-in books a title match on a chosen show against a
   * chosen championship's reigning champion.
   */
  private Component buildBriefcaseSection() {
    VerticalLayout section = new VerticalLayout();
    section.setPadding(false);
    section.setSpacing(true);
    section.add(new H3("Briefcase"));

    List<TitleOpportunity> opportunities =
        wrestlerFacade.getTitleOpportunityService().findByWrestler(wrestler.getId());

    if (opportunities.isEmpty()) {
      section.add(new Paragraph("No briefcase opportunities on record."));
      return section;
    }

    Optional<TitleOpportunity> held =
        opportunities.stream().filter(TitleOpportunity::isHeld).findFirst();
    if (held.isPresent()) {
      TitleOpportunity current = held.get();
      Span badge = new Span("💼 " + current.getName() + " — HELD");
      badge
          .getElement()
          .setAttribute(
              "style",
              "background:var(--lumo-primary-color-10pct);border-radius:var(--lumo-border-radius-m);padding:4px"
                  + " 10px;font-weight:600");
      HorizontalLayout heldRow = new HorizontalLayout(badge);
      heldRow.setAlignItems(FlexComponent.Alignment.CENTER);
      Span expiry =
          new Span(
              "Earned "
                  + current.getEarnedAt()
                  + (current.getExpiryDate() != null
                      ? " — cashable until " + current.getExpiryDate()
                      : ""));
      expiry.getStyle().set("color", "var(--lumo-secondary-text-color)");
      heldRow.add(expiry);
      Button cashInBtn = new Button("Cash In", e -> openCashInDialog(current));
      cashInBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
      cashInBtn.setTooltipText(
          "Book a title match on the spot against the reigning champion of any active"
              + " championship. The briefcase is spent whether the match is won or lost.");
      heldRow.add(cashInBtn);
      section.add(heldRow);
    }

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
                    wrestlerFacade
                        .getTitleService()
                        .getTitleById(o.getCashedAgainstTitle().getId())
                        .map(Title::getName)
                        .orElse("?")));
    grid.addColumn(o -> cashedAgainst.getOrDefault(o.getId(), "—"))
        .setHeader("Cashed Against")
        .setAutoWidth(true);
    grid.setItems(opportunities);
    grid.setAllRowsVisible(true);
    grid.setWidthFull();
    section.add(grid);
    return section;
  }

  /**
   * The cash-in dialog: pick the championship to challenge (active, same universe, reigning
   * champion) and the show to book on. Validation errors surface as notifications.
   */
  private void openCashInDialog(TitleOpportunity opportunity) {
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle("Cash In: " + opportunity.getName());

    TitleOpportunityService titleOpportunityService = wrestlerFacade.getTitleOpportunityService();

    // Cashable championships: active titles that currently have a reigning champion. The title's
    // denormalized champion list is fine here — the combo renders before any cash-in call, and the
    // service re-validates from the reign table inside the transaction.
    ComboBox<Title> titleCombo = new ComboBox<>("Championship");
    List<Title> cashable =
        wrestlerFacade.getTitleService().findAll().stream()
            .filter(t -> Boolean.TRUE.equals(t.getIsActive()) && !t.getCurrentChampions().isEmpty())
            .toList();
    titleCombo.setItems(cashable);
    titleCombo.setItemLabelGenerator(Title::getName);
    titleCombo.setWidthFull();
    titleCombo.setAllowCustomValue(false);

    ComboBox<Show> showCombo = new ComboBox<>("Show");
    showCombo.setItems(showFacade.getShowService().getUpcomingShows(50));
    showCombo.setItemLabelGenerator(
        s -> s.getName() + (s.getShowDate() != null ? " — " + s.getShowDate() : ""));
    showCombo.setWidthFull();
    showCombo.setAllowCustomValue(false);

    Span warning =
        new Span(
            "The briefcase is spent when the match is booked — win or lose. The winner takes the"
                + " championship.");
    warning.getStyle().set("color", "var(--lumo-error-text-color)");

    Button cancel = new Button("Cancel", e -> dialog.close());
    Button confirm =
        new Button(
            "Cash In",
            e -> {
              if (titleCombo.getValue() == null || showCombo.getValue() == null) {
                Notification.show(
                    "Select a championship and a show.", 3000, Notification.Position.MIDDLE);
                return;
              }
              try {
                titleOpportunityService.cashIn(
                    opportunity.getId(),
                    titleCombo.getValue().getId(),
                    showCombo.getValue().getId());
                dialog.close();
                Notification.show(
                        "Cash-in booked on " + showCombo.getValue().getName() + "!",
                        3000,
                        Notification.Position.BOTTOM_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                buildView();
              } catch (Exception ex) {
                log.error("Error cashing in briefcase", ex);
                Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
              }
            });
    confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    confirm.setEnabled(false);
    titleCombo.addValueChangeListener(
        e -> confirm.setEnabled(e.getValue() != null && showCombo.getValue() != null));
    showCombo.addValueChangeListener(
        e -> confirm.setEnabled(e.getValue() != null && titleCombo.getValue() != null));

    dialog.add(new VerticalLayout(titleCombo, showCombo, warning));
    dialog.getFooter().add(cancel, confirm);
    dialog.open();
  }

  private Component buildInjuryLogSection() {
    VerticalLayout section = new VerticalLayout();
    section.setPadding(false);
    section.setSpacing(true);
    section.add(new H3("Injury Log"));

    if (universeId == null) {
      section.add(new Paragraph("No universe selected."));
      return section;
    }

    List<Injury> injuries =
        wrestlerFacade.getInjuryService().getAllInjuriesForWrestler(wrestler.getId(), universeId);

    if (injuries.isEmpty()) {
      section.add(new Paragraph("No injuries on record."));
      return section;
    }

    Grid<Injury> grid = new Grid<>();
    grid.addColumn(Injury::getName).setHeader("Injury").setAutoWidth(true);
    grid.addColumn(i -> i.getSeverity().getDisplayName())
        .setHeader("Severity")
        .setAutoWidth(true)
        .setFlexGrow(0);
    grid.addColumn(i -> DATE_FMT.format(i.getInjuryDate())).setHeader("Date").setAutoWidth(true);
    grid.addColumn(Injury::getDurationDisplay).setHeader("Duration").setAutoWidth(true);
    grid.addComponentColumn(
            i -> {
              Span badge = new Span(i.isCurrentlyActive() ? "Active" : "Healed");
              badge
                  .getElement()
                  .getThemeList()
                  .add(i.isCurrentlyActive() ? "badge error" : "badge success");
              return badge;
            })
        .setHeader("Status")
        .setAutoWidth(true)
        .setFlexGrow(0);
    grid.setItems(
        injuries.stream().sorted(Comparator.comparing(Injury::getInjuryDate).reversed()).toList());
    grid.setAllRowsVisible(true);
    grid.setWidthFull();

    section.add(grid);
    return section;
  }
}
