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
package com.github.javydreamercsw.management.ui.view.tournament;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentRecurrence;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.ShowFacade;
import com.github.javydreamercsw.management.service.tournament.TournamentFormat;
import com.github.javydreamercsw.management.service.tournament.TournamentService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerFacade;
import com.github.javydreamercsw.management.ui.ViewContext;
import com.github.javydreamercsw.management.ui.view.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "tournament-list", layout = MainLayout.class)
@PageTitle("Tournaments | ATW RPG")
@Menu(order = 7, icon = "vaadin:sitemap", title = "Tournaments")
@RolesAllowed({"ADMIN", "BOOKER", "PLAYER", "VIEWER"})
@Slf4j
public class TournamentListView extends VerticalLayout {

  private final TournamentService tournamentService;
  private final WrestlerFacade wrestlerFacade;
  private final SegmentRuleService segmentRuleService;
  private final SegmentTypeService segmentTypeService;
  private final UniverseContextService universeContextService;
  private final SecurityUtils securityUtils;
  private final ShowFacade showFacade;

  private final Grid<Tournament> grid;
  private Checkbox showPastEditionsCheckbox;

  @Autowired
  public TournamentListView(
      TournamentService tournamentService,
      WrestlerFacade wrestlerFacade,
      ShowFacade showFacade,
      ViewContext viewContext) {
    this.tournamentService = tournamentService;
    this.wrestlerFacade = wrestlerFacade;
    this.showFacade = showFacade;
    this.segmentRuleService = showFacade.getSegmentRuleService();
    this.segmentTypeService = showFacade.getSegmentTypeService();
    this.universeContextService = viewContext.getUniverseContextService();
    this.securityUtils = viewContext.getSecurityUtils();

    setSizeFull();
    setPadding(false);

    grid = buildGrid();
    Div gridWrapper = new Div(grid);
    gridWrapper.addClassName("grid-scroll-container");
    add(buildToolbar(), gridWrapper);
    refresh();
  }

  private ViewToolbar buildToolbar() {
    Button newBtn = new Button("New Tournament", e -> openCreationWizard());
    newBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    // ATW-o4ad: recurring chains list their latest edition by default; this toggle reveals
    // the completed earlier editions of each chain.
    showPastEditionsCheckbox = new Checkbox("Show past editions");
    showPastEditionsCheckbox.setValue(false);
    showPastEditionsCheckbox.addValueChangeListener(e -> refresh());
    return new ViewToolbar(
        "Tournaments", ViewToolbar.group(newBtn), ViewToolbar.group(showPastEditionsCheckbox));
  }

  private Grid<Tournament> buildGrid() {
    Grid<Tournament> g = new Grid<>(Tournament.class, false);
    g.setSizeFull();

    g.addColumn(Tournament::getName).setHeader("Name").setSortable(true).setFlexGrow(2);
    g.addColumn(t -> t.getFormatId().replace('_', ' ')).setHeader("Format").setSortable(true);
    // Edition badge (ATW-o4ad): roman-numeral ordinal for recurring chains, blank for one-shots.
    g.addColumn(t -> editionBadgeOf(t)).setHeader("Edition").setSortable(true);
    // Entries are lazy and rows render outside a transaction — count through the service
    // instead of touching the collection (LazyInitializationException otherwise).
    g.addColumn(tournamentService::countEntries).setHeader("Entrants");
    g.addColumn(t -> t.getStatus().name()).setHeader("Status").setSortable(true);
    // EAGER mapping — safe to read on detached rows (ATW-xbn4).
    g.addColumn(
            t ->
                t.getPayoffShow() != null
                    ? t.getPayoffShow().getName()
                        + (t.getPayoffShow().getShowDate() != null
                            ? " — " + t.getPayoffShow().getShowDate()
                            : "")
                    : "")
        .setHeader("Host Show");

    // AutoWidth so both buttons fit: a fixed narrow width clips the Delete button once the
    // Host Show column takes its share of the row (user-reported). The key lets tests target
    // the cell (Karibu _getCellComponent).
    g.addComponentColumn(this::buildRowActions)
        .setKey("actions")
        .setHeader("Actions")
        .setAutoWidth(true)
        .setFlexGrow(0);

    g.addItemClickListener(
        e -> {
          // Clicks inside the Actions cell belong to the Edit/Delete buttons — the buttons
          // open their dialogs; navigating away here would instantly destroy the just-opened
          // dialog (user-reported: Delete appeared dead). Anywhere else opens the detail view.
          if (e.getColumn() != null && "actions".equals(e.getColumn().getKey())) {
            return;
          }
          UI.getCurrent().navigate("tournament-detail/" + e.getItem().getId());
        });
    return g;
  }

  /** Edit / Delete controls, mirroring TitleListView's action column. */
  private HorizontalLayout buildRowActions(@NonNull Tournament tournament) {
    HorizontalLayout actions = new HorizontalLayout();
    actions.setSpacing(true);

    Button editBtn = new Button("Edit", new Icon(VaadinIcon.EDIT));
    editBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
    editBtn.addClickListener(e -> openEditDialog(tournament));
    editBtn.setVisible(securityUtils.canEdit());

    Button deleteBtn = new Button("Delete", new Icon(VaadinIcon.TRASH));
    deleteBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
    deleteBtn.addClickListener(e -> confirmDelete(tournament));
    deleteBtn.setVisible(securityUtils.canDelete());

    actions.add(editBtn, deleteBtn);
    return actions;
  }

  private void openEditDialog(@NonNull Tournament tournament) {
    // The grid row is detached — its linkedTitle is an uninitialized proxy. Re-read with the
    // graph initialized before binding lazy values to the dialog fields.
    Tournament managed =
        tournamentService.findByIdWithDetails(tournament.getId()).orElse(tournament);
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle("Edit Tournament");
    dialog.setWidth("min(600px, 95vw)");

    TextField nameField = new TextField("Tournament Name");
    nameField.setValue(managed.getName());
    nameField.setRequired(true);
    nameField.setWidthFull();

    ComboBox<TournamentFormat> formatCombo = new ComboBox<>("Format");
    formatCombo.setItems(tournamentService.getAvailableFormats());
    formatCombo.setItemLabelGenerator(TournamentFormat::getDisplayName);
    formatCombo.setWidthFull();
    tournamentService.findFormat(managed.getFormatId()).ifPresent(formatCombo::setValue);

    ComboBox<Title> titleCombo = new ComboBox<>("Linked Championship (optional)");
    titleCombo.setItems(wrestlerFacade.getTitleService().findAll());
    titleCombo.setItemLabelGenerator(Title::getName);
    titleCombo.setValue(managed.getLinkedTitle());
    titleCombo.setWidthFull();
    titleCombo.setClearButtonVisible(true);

    // One-time host-show binding (ATW-xbn4).
    ComboBox<Show> hostShowCombo = new ComboBox<>("Host Show (optional)");
    hostShowCombo.setItems(showFacade.getShowService().getUpcomingShows(50));
    hostShowCombo.setItemLabelGenerator(
        s -> s.getName() + (s.getShowDate() != null ? " — " + s.getShowDate() : ""));
    hostShowCombo.setValue(managed.getPayoffShow());
    hostShowCombo.setWidthFull();
    hostShowCombo.setClearButtonVisible(true);
    hostShowCombo.setAllowCustomValue(false);

    ComboBox<SegmentType> payoffTypeCombo = new ComboBox<>("Payoff Match Type (optional)");
    payoffTypeCombo.setItems(segmentTypeService.findAll());
    payoffTypeCombo.setItemLabelGenerator(SegmentType::getName);
    payoffTypeCombo.setValue(managed.getPayoffSegmentType());
    payoffTypeCombo.setWidthFull();
    payoffTypeCombo.setClearButtonVisible(true);
    payoffTypeCombo.setHelperText("Defaults to One on One.");

    ComboBox<SegmentRule> payoffRuleCombo = new ComboBox<>("Payoff Rule (optional)");
    payoffRuleCombo.setItems(segmentRuleService.findAll());
    payoffRuleCombo.setItemLabelGenerator(SegmentRule::getName);
    payoffRuleCombo.setValue(managed.getPayoffSegmentRule());
    payoffRuleCombo.setWidthFull();
    payoffRuleCombo.setClearButtonVisible(true);
    payoffTypeCombo.setEnabled(managed.getPayoffShow() != null);
    payoffRuleCombo.setEnabled(managed.getPayoffShow() != null);
    hostShowCombo.addValueChangeListener(
        e -> {
          boolean hasHost = e.getValue() != null;
          payoffTypeCombo.setEnabled(hasHost);
          payoffRuleCombo.setEnabled(hasHost);
          if (!hasHost) {
            payoffTypeCombo.clear();
            payoffRuleCombo.clear();
          }
        });

    MultiSelectComboBox<SegmentRule> rulesPicker =
        new MultiSelectComboBox<>("Allowed Segment Rules (optional)");
    rulesPicker.setItems(segmentRuleService.findAll());
    rulesPicker.setItemLabelGenerator(SegmentRule::getName);
    rulesPicker.setValue(new HashSet<>(managed.getAllowedRules()));
    rulesPicker.setWidthFull();

    Button cancel = new Button("Cancel", e -> dialog.close());
    Button save =
        new Button(
            "Save",
            e -> {
              if (nameField.isEmpty()) {
                Notification.show("Name is required.", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
              }
              try {
                tournamentService.updateTournament(
                    tournament.getId(),
                    nameField.getValue(),
                    formatCombo.getValue() != null ? formatCombo.getValue().getFormatId() : null,
                    titleCombo.getValue(),
                    managed.getStartDate(),
                    new ArrayList<>(rulesPicker.getSelectedItems()),
                    hostShowCombo.getValue(),
                    payoffTypeCombo.getValue(),
                    payoffRuleCombo.getValue(),
                    true);
                dialog.close();
                refresh();
                Notification.show("Tournament updated!", 3000, Notification.Position.BOTTOM_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
              } catch (Exception ex) {
                log.error("Error updating tournament", ex);
                Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
              }
            });
    save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    dialog.getFooter().add(cancel, save);
    dialog.add(
        new VerticalLayout(
            nameField,
            formatCombo,
            titleCombo,
            hostShowCombo,
            payoffTypeCombo,
            payoffRuleCombo,
            rulesPicker));
    dialog.open();
  }

  private void confirmDelete(@NonNull Tournament tournament) {
    ConfirmDialog confirmDialog = new ConfirmDialog();
    confirmDialog.setHeader("Delete Tournament");
    confirmDialog.setText(
        "Are you sure you want to delete the tournament '"
            + tournament.getName()
            + "'?"
            + " Only scheduled (not started) tournaments can be deleted.");
    confirmDialog.setCancelable(true);
    confirmDialog.setConfirmText("Delete");
    confirmDialog.setConfirmButtonTheme("error primary");

    confirmDialog.addConfirmListener(
        e -> {
          try {
            assert tournament.getId() != null;
            boolean deleted = tournamentService.deleteTournament(tournament.getId());
            if (deleted) {
              refresh();
              Notification.show("Tournament deleted", 3000, Notification.Position.BOTTOM_END)
                  .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
              Notification.show("Tournament not found.", 5000, Notification.Position.BOTTOM_END)
                  .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
          } catch (Exception ex) {
            log.error("Error deleting tournament", ex);
            Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
          }
        });
    confirmDialog.open();
  }

  private void refresh() {
    Optional<Universe> universe = universeContextService.getCurrentUniverse();
    List<Tournament> items =
        universe.map(tournamentService::findByUniverse).orElseGet(tournamentService::findAll);
    if (!showPastEditionsCheckbox.getValue()) {
      // Recurring chains show the latest edition only (ATW-o4ad) — past editions surface via
      // the toggle, and one-shot tournaments (no ordinal) always list.
      items = latestEditionPerChain(items);
    }
    grid.setItems(items);
  }

  /**
   * Keeps only the highest-ordinal edition of each recurring chain; one-shots (no ordinal) pass
   * through. Chains are walked via {@code parent} (EAGER), so a completed edition with a successor
   * is hidden from the default view.
   */
  private static List<Tournament> latestEditionPerChain(@NonNull List<Tournament> items) {
    Map<Long, Tournament> latestByChainRoot = new HashMap<>();
    List<Tournament> oneShots = new ArrayList<>();
    for (Tournament t : items) {
      if (t.getEditionOrdinal() == null) {
        oneShots.add(t);
      }
    }
    // Root each edition at its chain's first edition id (walk parents), keep the max ordinal.
    for (Tournament t : items) {
      if (t.getEditionOrdinal() == null) {
        continue;
      }
      Tournament walk = t;
      while (walk.getParent() != null && walk.getParent().getId() != null) {
        walk = walk.getParent();
      }
      Long root = walk.getId();
      Tournament current = latestByChainRoot.get(root);
      if (current == null
          || t.getEditionOrdinal() > current.getEditionOrdinal()
          || (t.getEditionOrdinal().equals(current.getEditionOrdinal())
              && t.getId() != null
              && t.getId() > current.getId())) {
        latestByChainRoot.put(root, t);
      }
    }
    List<Tournament> result = new ArrayList<>(oneShots);
    result.addAll(latestByChainRoot.values());
    return result;
  }

  /** "II" / "III" — the roman-numeral edition ordinal, blank for one-shot tournaments. */
  private static String editionBadgeOf(@NonNull Tournament t) {
    return t.getEditionOrdinal() != null && t.getEditionOrdinal() > 1
        ? TournamentService.editionName("", t.getEditionOrdinal()).trim()
        : "";
  }

  // ── Creation wizard ───────────────────────────────────────────────────────

  private void openCreationWizard() {
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle("New Tournament");
    dialog.setWidth("min(600px, 95vw)");

    TabSheet tabs = new TabSheet();
    Tab tab1 = new Tab("1. Details");
    Tab tab2 = new Tab("2. Seeding");

    // Tab 1: Name, format, title link, start date
    TextField nameField = new TextField("Tournament Name");
    nameField.setRequired(true);
    nameField.setWidthFull();

    ComboBox<TournamentFormat> formatCombo = new ComboBox<>("Format");
    formatCombo.setItems(tournamentService.getAvailableFormats());
    formatCombo.setItemLabelGenerator(TournamentFormat::getDisplayName);
    formatCombo.setRequired(true);
    formatCombo.setWidthFull();

    ComboBox<Title> titleCombo = new ComboBox<>("Linked Championship (optional)");
    titleCombo.setItems(wrestlerFacade.getTitleService().findAll());
    titleCombo.setItemLabelGenerator(Title::getName);
    titleCombo.setWidthFull();

    // One-time host-show binding (ATW-xbn4): the payoff books on this show exactly once; the
    // non-final rounds pace automatically onto the weekly shows before it. Template pairing
    // stays reserved for recurring tournaments.
    ComboBox<Show> hostShowCombo = new ComboBox<>("Host Show (optional)");
    hostShowCombo.setItems(
        showFacade.getShowService().getUpcomingShows(50).stream()
            .filter(
                s ->
                    universeContextService.getCurrentUniverse().isEmpty()
                        || s.getUniverse() == null
                        || universeContextService
                            .getCurrentUniverse()
                            .map(u -> u.getId().equals(s.getUniverse().getId()))
                            .orElse(false))
            .toList());
    hostShowCombo.setItemLabelGenerator(
        s -> s.getName() + (s.getShowDate() != null ? " — " + s.getShowDate() : ""));
    hostShowCombo.setWidthFull();
    hostShowCombo.setClearButtonVisible(true);
    hostShowCombo.setAllowCustomValue(false);
    hostShowCombo.setHelperText(
        "Payoff books on this show exactly once; rounds pace onto the weekly shows before"
            + " it automatically.");

    ComboBox<SegmentType> payoffTypeCombo = new ComboBox<>("Payoff Match Type (optional)");
    payoffTypeCombo.setItems(segmentTypeService.findAll());
    payoffTypeCombo.setItemLabelGenerator(SegmentType::getName);
    payoffTypeCombo.setWidthFull();
    payoffTypeCombo.setClearButtonVisible(true);
    payoffTypeCombo.setHelperText("Defaults to One on One.");

    ComboBox<SegmentRule> payoffRuleCombo = new ComboBox<>("Payoff Rule (optional)");
    payoffRuleCombo.setItems(segmentRuleService.findAll());
    payoffRuleCombo.setItemLabelGenerator(SegmentRule::getName);
    payoffRuleCombo.setWidthFull();
    payoffRuleCombo.setClearButtonVisible(true);
    // The payoff only exists on a show — keep the payoff pickers inert until a host is chosen.
    payoffTypeCombo.setEnabled(false);
    payoffRuleCombo.setEnabled(false);
    hostShowCombo.addValueChangeListener(
        e -> {
          boolean hasHost = e.getValue() != null;
          payoffTypeCombo.setEnabled(hasHost);
          payoffRuleCombo.setEnabled(hasHost);
          if (!hasHost) {
            payoffTypeCombo.clear();
            payoffRuleCombo.clear();
          }
        });

    // Edition cadence (ATW-o4ad): recurring editions re-arm the PLE template pairing each cycle
    // — the next edition auto-creates when the payoff books, instead of consuming the pairing.
    ComboBox<TournamentRecurrence> recurrenceCombo =
        new ComboBox<>("Edition Cadence (recurring tournaments)");
    recurrenceCombo.setItems(TournamentRecurrence.values());
    recurrenceCombo.setItemLabelGenerator(
        r ->
            switch (r) {
              case NONE -> "One-time";
              case ANNUAL -> "Annual";
            });
    recurrenceCombo.setValue(TournamentRecurrence.NONE);
    recurrenceCombo.setWidthFull();
    recurrenceCombo.setHelperText(
        "Annual editions pair with a PLE show template: when a payoff books, the next edition"
            + " is created automatically and the template pairing re-points to it — no manual"
            + " re-arming each year.");

    MultiSelectComboBox<SegmentRule> rulesPicker =
        new MultiSelectComboBox<>("Allowed Segment Rules (optional)");
    rulesPicker.setItems(segmentRuleService.findAll());
    rulesPicker.setItemLabelGenerator(SegmentRule::getName);
    rulesPicker.setWidthFull();
    rulesPicker.setHelperText(
        "Rules randomly applied to matches. A fixed rule can be set per round later.");

    VerticalLayout tab1Content =
        new VerticalLayout(
            nameField,
            formatCombo,
            titleCombo,
            hostShowCombo,
            payoffTypeCombo,
            payoffRuleCombo,
            recurrenceCombo,
            rulesPicker);
    tab1Content.setPadding(false);

    // Tab 2: Seeding
    ComboBox<String> seedingMode = new ComboBox<>("Seeding Method");
    seedingMode.setItems(
        "Auto (by fan count)", "Manual (pick wrestlers)", "Don't seed now (seed later)");
    seedingMode.setValue("Auto (by fan count)");
    seedingMode.setWidthFull();
    seedingMode.setHelperText(
        "Auto seeds the top wrestlers by fan count. A tournament paired with a show template"
            + " can also be seeded automatically when the paired show is approved.");

    IntegerField countField = new IntegerField("Number of Entrants");
    countField.setValue(8);
    countField.setMin(3);
    countField.setMax(64);
    countField.setWidthFull();
    countField.setHelperText("Capped at the number of eligible active wrestlers.");

    // Cap the entrant count at the eligible roster (narrowed by the linked championship's
    // gender constraint) — the format's 64 max means nothing to a 12-wrestler universe.
    Runnable refreshEntrantCap =
        () -> {
          int eligible = tournamentService.countEligibleEntrants(titleCombo.getValue());
          int cap = Math.clamp(eligible, 3, 64);
          countField.setMax(cap);
          if (countField.getValue() == null || countField.getValue() > cap) {
            countField.setValue(cap);
          }
        };
    titleCombo.addValueChangeListener(e -> refreshEntrantCap.run());
    refreshEntrantCap.run();

    MultiSelectComboBox<Wrestler> wrestlerPicker = new MultiSelectComboBox<>("Select Wrestlers");
    wrestlerPicker.setItems(wrestlerFacade.getWrestlerService().getAllWrestlers());
    wrestlerPicker.setItemLabelGenerator(Wrestler::getName);
    wrestlerPicker.setWidthFull();
    wrestlerPicker.setVisible(false);

    seedingMode.addValueChangeListener(
        e -> {
          boolean manual = "Manual (pick wrestlers)".equals(e.getValue());
          boolean skip = "Don't seed now (seed later)".equals(e.getValue());
          countField.setVisible(!manual && !skip);
          wrestlerPicker.setVisible(manual);
        });

    // Match-up preview for Auto seeding: what Create will build, before it commits.
    Span matchupPreview = new Span();
    matchupPreview.getStyle().set("color", "var(--lumo-secondary-text-color)");
    matchupPreview.setVisible(false);
    Runnable refreshMatchups =
        () -> {
          boolean autoMode = "Auto (by fan count)".equals(seedingMode.getValue());
          Integer entrants = countField.getValue();
          if (!autoMode || entrants == null || entrants < 2) {
            matchupPreview.setVisible(false);
            return;
          }
          // Fans walk the lazy wrestlerStates collection — rank inside the service's
          // transaction instead of touching detached entities here.
          List<Wrestler> pool =
              new ArrayList<>(
                  tournamentService.findEligibleWrestlersSortedByFans(
                      titleCombo.getValue(),
                      universeContextService.getCurrentUniverse().map(Universe::getId).orElse(1L)));
          int take = Math.min(entrants, pool.size());
          StringBuilder sb = new StringBuilder();
          for (int i = 0; i < take / 2; i++) {
            if (!sb.isEmpty()) {
              sb.append(" · ");
            }
            sb.append(pool.get(i).getName())
                .append(" vs ")
                .append(pool.get(take - 1 - i).getName());
          }
          matchupPreview.setText(sb.isEmpty() ? "No eligible wrestlers yet." : sb.toString());
          matchupPreview.setVisible(true);
        };
    countField.addValueChangeListener(e -> refreshMatchups.run());
    titleCombo.addValueChangeListener(e -> refreshMatchups.run());
    seedingMode.addValueChangeListener(e -> refreshMatchups.run());

    VerticalLayout tab2Content =
        new VerticalLayout(seedingMode, countField, wrestlerPicker, matchupPreview);
    tab2Content.setPadding(false);

    tabs.add(tab1, tab1Content);
    tabs.add(tab2, tab2Content);
    tabs.setSizeFull();

    // Wizard gating: only the Details tab is selectable until name + format are filled — the
    // Create button lives on the last step, so nothing can be created with an invalid step 1.
    Button back = new Button("Back", e -> tabs.setSelectedIndex(0));
    back.setEnabled(false);

    Button next =
        new Button(
            "Next",
            e -> {
              if (nameField.isEmpty() || formatCombo.isEmpty()) {
                Notification.show(
                        "Name and format are required before seeding.",
                        3000,
                        Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
              }
              tabs.setSelectedIndex(1);
            });

    Button cancel = new Button("Cancel", e -> dialog.close());
    Button create =
        new Button(
            "Create Tournament",
            e -> {
              if (nameField.isEmpty() || formatCombo.isEmpty()) {
                Notification.show(
                        "Name and format are required.", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
              }
              try {
                Optional<Universe> universe = universeContextService.getCurrentUniverse();
                Tournament t =
                    tournamentService.createTournament(
                        nameField.getValue(),
                        formatCombo.getValue().getFormatId(),
                        universe.orElse(null),
                        titleCombo.getValue(),
                        LocalDate.now(),
                        new ArrayList<>(rulesPicker.getSelectedItems()),
                        hostShowCombo.getValue(),
                        payoffTypeCombo.getValue(),
                        payoffRuleCombo.getValue());
                t.setRecurrence(recurrenceCombo.getValue());
                tournamentService.save(t);

                boolean auto = "Auto (by fan count)".equals(seedingMode.getValue());
                boolean manual = "Manual (pick wrestlers)".equals(seedingMode.getValue());
                if (auto) {
                  tournamentService.seedAuto(
                      t, countField.getValue(), universe.map(Universe::getId).orElse(1L));
                } else if (manual) {
                  List<Wrestler> selected = new ArrayList<>(wrestlerPicker.getSelectedItems());
                  for (int i = 0; i < selected.size(); i++) {
                    tournamentService.addEntry(t, selected.get(i), i + 1);
                  }
                } // "Don't seed now": seed later from the detail view or via a paired show.

                dialog.close();
                refresh();
                Notification.show("Tournament created!", 3000, Notification.Position.BOTTOM_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                UI.getCurrent().navigate("tournament-detail/" + t.getId());
              } catch (Exception ex) {
                log.error("Error creating tournament", ex);
                Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
              }
            });
    create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    // Wizard button visibility follows the selected tab: on Details only Next is offered, so
    // Create is unreachable until the seeding step.
    Runnable updateWizardButtons =
        () -> {
          boolean onDetails = Objects.equals(tabs.getSelectedIndex(), 0);
          back.setEnabled(!onDetails);
          next.setVisible(onDetails);
          create.setVisible(!onDetails);
        };
    tabs.addSelectedChangeListener(e -> updateWizardButtons.run());
    updateWizardButtons.run();

    dialog.getFooter().add(cancel, back, next, create);
    dialog.add(tabs);
    dialog.open();
  }

  /** Test hooks: drive the dialogs directly (Karibu tests can't traverse grid cell components). */
  void openCreationWizardForTest() {
    openCreationWizard();
  }

  void refreshGridForTest() {
    refresh();
  }

  void openEditDialogForTest(final Tournament tournament) {
    openEditDialog(tournament);
  }

  void confirmDeleteForTest(final Tournament tournament) {
    confirmDelete(tournament);
  }
}
