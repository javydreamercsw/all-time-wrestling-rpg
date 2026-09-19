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
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
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
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "tournament-list", layout = MainLayout.class)
@PageTitle("Tournaments | ATW RPG")
@Menu(order = 7, icon = "vaadin:trophy", title = "Tournaments")
@RolesAllowed({"ADMIN", "BOOKER", "PLAYER", "VIEWER"})
@Slf4j
public class TournamentListView extends VerticalLayout {

  private final TournamentService tournamentService;
  private final WrestlerFacade wrestlerFacade;
  private final SegmentRuleService segmentRuleService;
  private final UniverseContextService universeContextService;
  private final SecurityUtils securityUtils;

  private Grid<Tournament> grid;

  @Autowired
  public TournamentListView(
      TournamentService tournamentService,
      WrestlerFacade wrestlerFacade,
      ShowFacade showFacade,
      ViewContext viewContext) {
    this.tournamentService = tournamentService;
    this.wrestlerFacade = wrestlerFacade;
    this.segmentRuleService = showFacade.getSegmentRuleService();
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
    return new ViewToolbar("Tournaments", ViewToolbar.group(newBtn));
  }

  private Grid<Tournament> buildGrid() {
    Grid<Tournament> g = new Grid<>(Tournament.class, false);
    g.setSizeFull();

    g.addColumn(Tournament::getName).setHeader("Name").setSortable(true).setFlexGrow(2);
    g.addColumn(t -> t.getFormatId().replace('_', ' ')).setHeader("Format").setSortable(true);
    // Entries are lazy and rows render outside a transaction — count through the service
    // instead of touching the collection (LazyInitializationException otherwise).
    g.addColumn(t -> tournamentService.countEntries(t)).setHeader("Entrants");
    g.addColumn(t -> t.getStatus().name()).setHeader("Status").setSortable(true);
    g.addColumn(Tournament::getStartDate).setHeader("Start Date").setSortable(true);

    g.addComponentColumn(this::buildRowActions).setHeader("Actions").setFlexGrow(0);

    g.addItemClickListener(
        e -> UI.getCurrent().navigate("tournament-detail/" + e.getItem().getId()));
    return g;
  }

  /** Edit / Delete controls, mirroring TitleListView's action column. */
  private HorizontalLayout buildRowActions(Tournament tournament) {
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

  private void openEditDialog(Tournament tournament) {
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

    DatePicker startDate = new DatePicker("Start Date");
    startDate.setValue(managed.getStartDate());
    startDate.setWidthFull();

    MultiSelectComboBox<SegmentRule> rulesPicker =
        new MultiSelectComboBox<>("Allowed Segment Rules (optional)");
    rulesPicker.setItems(segmentRuleService.findAll());
    rulesPicker.setItemLabelGenerator(SegmentRule::getName);
    rulesPicker.setValue(new java.util.HashSet<>(managed.getAllowedRules()));
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
                    startDate.getValue(),
                    new ArrayList<>(rulesPicker.getSelectedItems()));
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
    dialog.add(new VerticalLayout(nameField, formatCombo, titleCombo, startDate, rulesPicker));
    dialog.open();
  }

  private void confirmDelete(Tournament tournament) {
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
    grid.setItems(items);
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

    DatePicker startDate = new DatePicker("Start Date");
    startDate.setValue(LocalDate.now());
    startDate.setWidthFull();

    MultiSelectComboBox<SegmentRule> rulesPicker =
        new MultiSelectComboBox<>("Allowed Segment Rules (optional)");
    rulesPicker.setItems(segmentRuleService.findAll());
    rulesPicker.setItemLabelGenerator(SegmentRule::getName);
    rulesPicker.setWidthFull();
    rulesPicker.setHelperText(
        "Rules randomly applied to matches. A fixed rule can be set per round later.");

    VerticalLayout tab1Content =
        new VerticalLayout(nameField, formatCombo, titleCombo, startDate, rulesPicker);
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
          int cap = Math.max(3, Math.min(64, eligible));
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
            if (sb.length() > 0) {
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
                        startDate.getValue(),
                        new ArrayList<>(rulesPicker.getSelectedItems()));

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
}
