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

import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
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
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "tournament-list", layout = MainLayout.class)
@PageTitle("Tournaments | ATW RPG")
@RolesAllowed({"ADMIN", "BOOKER", "PLAYER", "VIEWER"})
@Slf4j
public class TournamentListView extends VerticalLayout {

  private final TournamentService tournamentService;
  private final WrestlerFacade wrestlerFacade;
  private final UniverseContextService universeContextService;

  private Grid<Tournament> grid;

  @Autowired
  public TournamentListView(
      TournamentService tournamentService, WrestlerFacade wrestlerFacade, ViewContext viewContext) {
    this.tournamentService = tournamentService;
    this.wrestlerFacade = wrestlerFacade;
    this.universeContextService = viewContext.getUniverseContextService();

    setSizeFull();
    setPadding(false);

    grid = buildGrid();
    add(buildToolbar(), grid);
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
    g.addColumn(t -> t.getEntries().size()).setHeader("Entrants");
    g.addColumn(t -> t.getStatus().name()).setHeader("Status").setSortable(true);
    g.addColumn(Tournament::getStartDate).setHeader("Start Date").setSortable(true);

    g.addItemClickListener(
        e -> UI.getCurrent().navigate("tournament-detail/" + e.getItem().getId()));
    return g;
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
    dialog.setWidth("600px");

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

    VerticalLayout tab1Content = new VerticalLayout(nameField, formatCombo, titleCombo, startDate);
    tab1Content.setPadding(false);

    // Tab 2: Seeding
    ComboBox<String> seedingMode = new ComboBox<>("Seeding Method");
    seedingMode.setItems("Auto (by fan count)", "Manual (pick wrestlers)");
    seedingMode.setValue("Auto (by fan count)");
    seedingMode.setWidthFull();

    IntegerField countField = new IntegerField("Number of Entrants");
    countField.setValue(8);
    countField.setMin(3);
    countField.setMax(64);
    countField.setWidthFull();

    MultiSelectComboBox<Wrestler> wrestlerPicker = new MultiSelectComboBox<>("Select Wrestlers");
    wrestlerPicker.setItems(wrestlerFacade.getWrestlerService().getAllWrestlers());
    wrestlerPicker.setItemLabelGenerator(Wrestler::getName);
    wrestlerPicker.setWidthFull();
    wrestlerPicker.setVisible(false);

    seedingMode.addValueChangeListener(
        e -> {
          boolean manual = "Manual (pick wrestlers)".equals(e.getValue());
          countField.setVisible(!manual);
          wrestlerPicker.setVisible(manual);
        });

    VerticalLayout tab2Content = new VerticalLayout(seedingMode, countField, wrestlerPicker);
    tab2Content.setPadding(false);

    tabs.add(tab1, tab1Content);
    tabs.add(tab2, tab2Content);
    tabs.setSizeFull();

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
                        startDate.getValue());

                boolean auto = !"Manual (pick wrestlers)".equals(seedingMode.getValue());
                if (auto) {
                  tournamentService.seedAuto(
                      t, countField.getValue(), universe.map(Universe::getId).orElse(1L));
                } else {
                  List<Wrestler> selected = new ArrayList<>(wrestlerPicker.getSelectedItems());
                  for (int i = 0; i < selected.size(); i++) {
                    tournamentService.addEntry(t, selected.get(i), i + 1);
                  }
                }

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

    dialog.getFooter().add(cancel, create);
    dialog.add(tabs);
    dialog.open();
  }
}
