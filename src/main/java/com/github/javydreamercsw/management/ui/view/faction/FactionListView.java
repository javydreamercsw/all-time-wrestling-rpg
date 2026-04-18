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
package com.github.javydreamercsw.management.ui.view.faction;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.service.faction.FactionService;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Route(value = "factions", layout = com.github.javydreamercsw.management.ui.view.MainLayout.class)
@PageTitle("Factions | ATW RPG")
@PermitAll
public class FactionListView extends VerticalLayout {

  private final FactionService factionService;
  private final WrestlerService wrestlerService;
  private final NpcService npcService;
  private final WrestlerRepository wrestlerRepository;
  private final UniverseContextService universeContextService;
  private final SecurityUtils securityUtils;
  private Dialog editDialog;
  private Faction editingFaction;
  private Binder<Faction> binder;
  final TextField name;
  final TextArea description;
  final ComboBox<Wrestler> leader;
  final ComboBox<Npc> manager;
  final TextField alignment;
  final Grid<Faction> factionGrid = new Grid<>(Faction.class, false);
  final Button createBtn;

  @Autowired
  public FactionListView(
      @NonNull FactionService factionService,
      @NonNull WrestlerService wrestlerService,
      @NonNull NpcService npcService,
      @NonNull WrestlerRepository wrestlerRepository,
      @NonNull SecurityUtils securityUtils,
      @NonNull UniverseContextService universeContextService) {
    this.factionService = factionService;
    this.wrestlerService = wrestlerService;
    this.npcService = npcService;
    this.wrestlerRepository = wrestlerRepository;
    this.securityUtils = securityUtils;
    this.universeContextService = universeContextService;

    // Create form components
    name = new TextField();
    name.setPlaceholder("Enter faction name...");
    name.setAriaLabel("Faction Name");
    name.setRequired(true);

    description = new TextArea();
    description.setPlaceholder("Enter faction description...");
    description.setAriaLabel("Faction Description");

    leader = new ComboBox<>("Leader");
    leader.setItems(wrestlerService.findAllIncludingInactive());
    leader.setItemLabelGenerator(Wrestler::getName);
    leader.setClearButtonVisible(true);

    manager = new ComboBox<>("Manager");
    manager.setItems(npcService.findAllIncludingInactive());
    manager.setItemLabelGenerator(Npc::getName);
    manager.setClearButtonVisible(true);

    alignment = new TextField("Alignment");
    alignment.setPlaceholder("e.g., Face, Heel...");

    addClassNames(
        LumoUtility.BoxSizing.BORDER,
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.Height.FULL,
        LumoUtility.Padding.NONE);
    setSpacing(false);
    setPadding(false);
    setSizeFull();

    configureGrid();
    setupEditDialog();

    if (securityUtils.canCreate()) {
      createBtn =
          new Button(
              "Create Faction",
              e -> {
                editingFaction = new Faction();
                editingFaction.setUniverse(
                    universeContextService.getCurrentUniverse().orElse(null));
                binder.setBean(editingFaction);
                editDialog.setHeaderTitle("Create Faction");
                editDialog.open();
              });
      createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    } else {
      createBtn = new Button();
      createBtn.setVisible(false);
    }

    ViewToolbar toolbar =
        createBtn.isVisible()
            ? new ViewToolbar("Factions", createBtn)
            : new ViewToolbar("Factions");

    add(toolbar, factionGrid);
    refreshGrid();
  }

  private void configureGrid() {
    factionGrid.addColumn(Faction::getName).setHeader("Name").setSortable(true).setFlexGrow(1);
    factionGrid
        .addColumn(f -> f.getLeader() != null ? f.getLeader().getName() : "None")
        .setHeader("Leader")
        .setSortable(true);
    factionGrid
        .addColumn(f -> f.getManager() != null ? f.getManager().getName() : "None")
        .setHeader("Manager")
        .setSortable(true);
    factionGrid.addColumn(Faction::getMemberCount).setHeader("Members").setSortable(true);
    factionGrid.addColumn(Faction::getAlignment).setHeader("Alignment").setSortable(true);

    factionGrid.addComponentColumn(
        faction -> {
          HorizontalLayout actions = new HorizontalLayout();
          actions.setSpacing(true);

          Button editButton = new Button(new Icon(VaadinIcon.EDIT), e -> editFaction(faction));
          editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
          editButton.setTooltipText("Edit Faction");
          editButton.setVisible(securityUtils.canEdit());

          Button membersButton =
              new Button(new Icon(VaadinIcon.USERS), e -> openMembersDialog(faction));
          membersButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SUCCESS);
          membersButton.setTooltipText("Manage Members");
          membersButton.setVisible(securityUtils.canEdit());

          Button deleteButton =
              new Button(
                  new Icon(VaadinIcon.TRASH),
                  e -> {
                    factionService.deleteById(faction.getId());
                    refreshGrid();
                    Notification.show("Faction deleted.");
                  });
          deleteButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
          deleteButton.setTooltipText("Delete Faction");
          deleteButton.setVisible(securityUtils.canDelete());

          actions.add(editButton, membersButton, deleteButton);
          return actions;
        });

    factionGrid.setItemDetailsRenderer(
        new com.vaadin.flow.data.renderer.ComponentRenderer<>(
            faction -> {
              VerticalLayout details = new VerticalLayout();
              details.setPadding(true);
              details.setSpacing(false);

              details.add(new H3("Faction Details"));
              details.add(new Span(faction.getDescription()));

              if (faction.getMemberCount() > 0) {
                details.add(new H3("Members"));
                Grid<WrestlerState> membersGrid = new Grid<>(WrestlerState.class, false);
                membersGrid
                    .addColumn(s -> s.getWrestler().getName())
                    .setHeader("Name")
                    .setSortable(true);
                membersGrid
                    .addColumn(s -> s.getTier().getDisplayWithEmoji())
                    .setHeader("Tier")
                    .setSortable(true);
                membersGrid.setItems(faction.getMembers());
                membersGrid.setHeight("200px");
                details.add(membersGrid);
              }

              return details;
            }));

    factionGrid.setSizeFull();
  }

  @Transactional(readOnly = true)
  private void refreshGrid() {
    Long universeId = universeContextService.getCurrentUniverseId();
    List<Faction> factions = factionService.findAllByUniverse(universeId);
    factionGrid.setItems(factions);
  }

  private void setupEditDialog() {
    editDialog = new Dialog();
    editDialog.setWidth("500px");

    binder = new Binder<>(Faction.class);
    binder.forField(name).asRequired("Name is required").bind(Faction::getName, Faction::setName);
    binder.bind(description, Faction::getDescription, Faction::setDescription);
    binder.bind(leader, Faction::getLeader, Faction::setLeader);
    binder.bind(manager, Faction::getManager, Faction::setManager);
    binder.bind(alignment, Faction::getAlignment, Faction::setAlignment);

    VerticalLayout layout = new VerticalLayout(name, description, leader, manager, alignment);
    layout.setPadding(true);
    layout.setSpacing(true);

    Button saveButton = new Button("Save", e -> saveFaction());
    saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    Button cancelButton = new Button("Cancel", e -> editDialog.close());

    editDialog.add(layout);
    editDialog.getFooter().add(cancelButton, saveButton);
  }

  private void editFaction(Faction faction) {
    editingFaction = faction;
    binder.setBean(editingFaction);
    editDialog.setHeaderTitle("Edit Faction: " + faction.getName());
    editDialog.open();
  }

  private void saveFaction() {
    if (binder.validate().isOk()) {
      factionService.save(editingFaction);
      editDialog.close();
      refreshGrid();
      Notification.show("Faction saved successfully.");
    }
  }

  private void openMembersDialog(Faction faction) {
    Dialog dialog = new Dialog();
    dialog.setWidth("800px");
    dialog.setHeight("600px");

    // Fetch fresh faction data to ensure members are loaded
    Optional<Faction> updatedFaction = factionService.getFactionById(faction.getId());
    if (updatedFaction.isEmpty()) return;

    Faction loadedFaction = updatedFaction.get();
    dialog.setHeaderTitle(
        "Manage Members: " + loadedFaction.getName() + " (" + loadedFaction.getMemberCount() + ")");

    VerticalLayout content = new VerticalLayout();
    content.setSizeFull();

    H3 currentMembersTitle = new H3("Current Members (" + loadedFaction.getMemberCount() + ")");
    Grid<WrestlerState> currentMembersGrid = new Grid<>(WrestlerState.class, false);
    currentMembersGrid
        .addColumn(s -> s.getWrestler().getName())
        .setHeader("Name")
        .setSortable(true);
    currentMembersGrid
        .addColumn(s -> s.getTier().getDisplayWithEmoji())
        .setHeader("Tier")
        .setSortable(true);
    currentMembersGrid
        .addColumn(s -> String.format("%,d", s.getFans()))
        .setHeader("Fans")
        .setSortable(true);
    currentMembersGrid.setId("members-grid");
    currentMembersGrid.setItems(loadedFaction.getMembers());

    currentMembersGrid.addComponentColumn(
        memberState -> {
          Button removeBtn =
              new Button(
                  new Icon(VaadinIcon.MINUS),
                  e -> {
                    loadedFaction.removeMember(memberState);
                    factionService.save(loadedFaction);
                    dialog.close();
                    openMembersDialog(loadedFaction);
                    refreshGrid();
                  });
          removeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
          removeBtn.setTooltipText("Remove from Faction");
          return removeBtn;
        });

    H3 availableWrestlersTitle = new H3("Available Wrestlers");
    Grid<WrestlerState> availableWrestlersGrid = new Grid<>(WrestlerState.class, false);
    availableWrestlersGrid
        .addColumn(s -> s.getWrestler().getName())
        .setHeader("Name")
        .setSortable(true);
    availableWrestlersGrid
        .addColumn(s -> s.getTier().getDisplayWithEmoji())
        .setHeader("Tier")
        .setSortable(true);

    Long universeId = universeContextService.getCurrentUniverseId();
    List<WrestlerState> available =
        wrestlerService.findAllIncludingInactive().stream()
            .map(w -> wrestlerService.getOrCreateState(w.getId(), universeId))
            .filter(s -> s.getFaction() == null)
            .toList();
    availableWrestlersGrid.setItems(available);

    availableWrestlersGrid.addComponentColumn(
        wState -> {
          Button addBtn =
              new Button(
                  new Icon(VaadinIcon.PLUS),
                  e -> {
                    loadedFaction.addMember(wState);
                    factionService.save(loadedFaction);
                    dialog.close();
                    openMembersDialog(loadedFaction);
                    refreshGrid();
                  });
          addBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SUCCESS);
          addBtn.setTooltipText("Add to Faction");
          return addBtn;
        });

    content.add(
        currentMembersTitle,
        currentMembersGrid,
        new Div(),
        availableWrestlersTitle,
        availableWrestlersGrid);
    content.expand(currentMembersGrid, availableWrestlersGrid);

    Button closeButton = new Button("Done", e -> dialog.close());
    dialog.getFooter().add(closeButton);
    dialog.add(content);
    dialog.open();
  }
}
