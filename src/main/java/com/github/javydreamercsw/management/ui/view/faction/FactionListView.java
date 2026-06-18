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

import com.github.javydreamercsw.base.ai.image.ImageStorageService;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.ui.component.ImageUploadComponent;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.service.faction.FactionService;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Route(
    value = "faction-list",
    layout = com.github.javydreamercsw.management.ui.view.MainLayout.class)
@PageTitle("Factions | ATW RPG")
@PermitAll
public class FactionListView extends VerticalLayout {

  private final FactionService factionService;
  private final WrestlerService wrestlerService;
  private final UniverseContextService universeContextService;
  private final SecurityUtils securityUtils;
  private final ImageStorageService imageStorageService;
  private Dialog editDialog;
  private Faction editingFaction;
  private Binder<Faction> binder;
  final TextField name;
  final TextArea description;
  final ComboBox<Wrestler> leader;
  final ComboBox<Npc> manager;
  final TextField alignment;
  final Checkbox isActive;
  final IntegerField affinity;
  final TextField imageUrl;
  final DatePicker formedDate;
  final DatePicker disbandedDate;
  final Grid<Faction> factionGrid = new Grid<>(Faction.class, false);
  final Button createBtn;

  @Autowired
  public FactionListView(
      @NonNull final FactionService factionService,
      @NonNull final WrestlerService wrestlerService,
      @NonNull final NpcService npcService,
      @NonNull final SecurityUtils securityUtils,
      @NonNull final UniverseContextService universeContextService,
      @NonNull final ImageStorageService imageStorageService) {
    this.factionService = factionService;
    this.wrestlerService = wrestlerService;
    this.securityUtils = securityUtils;
    this.universeContextService = universeContextService;
    this.imageStorageService = imageStorageService;

    // Create form components
    name = new TextField();
    name.setPlaceholder("Enter faction name...");
    name.setAriaLabel("Faction Name");
    name.setRequired(true);
    name.setId("edit-name");

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

    isActive = new Checkbox("Active");

    affinity = new IntegerField("Affinity");
    affinity.setTooltipText("Faction synergy score built through shared victories");
    affinity.setWidthFull();

    imageUrl = new TextField("Image URL");
    imageUrl.setReadOnly(true);
    imageUrl.setWidthFull();

    formedDate = new DatePicker("Formed Date");
    formedDate.setWidthFull();

    disbandedDate = new DatePicker("Disbanded Date");
    disbandedDate.setClearButtonVisible(true);
    disbandedDate.setWidthFull();

    addClassNames(
        LumoUtility.BoxSizing.BORDER,
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.Height.FULL,
        LumoUtility.Padding.NONE);
    setSpacing(false);
    setPadding(false);
    setSizeFull();

    factionGrid.setId("faction-grid");
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
      createBtn.setId("create-faction-button");
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
    factionGrid
        .addComponentColumn(
            faction -> {
              Image image = new Image(factionService.resolveFactionImage(faction), "Faction Image");
              image.setHeight("50px");
              image.setWidth("50px");
              image.addClassName(LumoUtility.BorderRadius.SMALL);
              return image;
            })
        .setHeader("Art")
        .setFlexGrow(0)
        .setWidth("70px");
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
    factionGrid.addColumn(Faction::isActive).setHeader("Active").setSortable(true);
    factionGrid
        .addColumn(Faction::getAffinity)
        .setHeader("Synergy")
        .setSortable(true)
        .setTooltipGenerator(f -> "Faction affinity/synergy score built through shared victories");

    factionGrid.addComponentColumn(
        faction -> {
          HorizontalLayout actions = new HorizontalLayout();
          actions.setSpacing(true);

          Button editButton = new Button(new Icon(VaadinIcon.EDIT), e -> editFaction(faction));
          editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
          editButton.setTooltipText("Edit Faction");
          editButton.setVisible(securityUtils.canEdit());
          editButton.setId("edit-" + faction.getId());

          Button membersButton =
              new Button(new Icon(VaadinIcon.USERS), e -> openMembersDialog(faction));
          membersButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SUCCESS);
          membersButton.setTooltipText("Manage Members");
          membersButton.setVisible(securityUtils.canEdit());
          membersButton.setId("members-" + faction.getId());

          Button deleteButton =
              new Button(
                  new Icon(VaadinIcon.TRASH),
                  e -> {
                    ConfirmDialog confirm = new ConfirmDialog();
                    confirm.setHeader("Delete Faction");
                    confirm.setText(
                        "Are you sure you want to delete \"" + faction.getName() + "\"?");
                    confirm.setConfirmText("Delete");
                    confirm.setConfirmButtonTheme("error primary");
                    confirm.setCancelable(true);
                    confirm.addConfirmListener(
                        ev -> {
                          factionService.deleteById(faction.getId());
                          refreshGrid();
                          Notification.show("Faction deleted.");
                        });
                    confirm.open();
                  });
          deleteButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
          deleteButton.setTooltipText("Delete Faction");
          deleteButton.setVisible(securityUtils.canDelete());
          deleteButton.setId("delete-" + faction.getId());

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
    editDialog.setWidth("min(600px, 95vw)");
    editDialog.setMaxHeight("85vh");

    binder = new Binder<>(Faction.class);
    binder.forField(name).asRequired("Name is required").bind(Faction::getName, Faction::setName);
    binder.bind(description, Faction::getDescription, Faction::setDescription);
    binder.bind(leader, Faction::getLeader, Faction::setLeader);
    binder.bind(manager, Faction::getManager, Faction::setManager);
    binder.bind(alignment, Faction::getAlignment, Faction::setAlignment);
    binder.bind(isActive, Faction::isActive, Faction::setActive);
    binder.bind(affinity, Faction::getAffinity, Faction::setAffinity);
    binder.forField(imageUrl).bind(Faction::getImageUrl, Faction::setImageUrl);
    binder
        .forField(formedDate)
        .withConverter(
            ld -> ld != null ? ld.atStartOfDay(ZoneOffset.UTC).toInstant() : Instant.now(),
            instant -> instant != null ? instant.atZone(ZoneOffset.UTC).toLocalDate() : null)
        .bind(Faction::getFormedDate, Faction::setFormedDate);
    binder
        .forField(disbandedDate)
        .withConverter(
            ld -> ld != null ? ld.atStartOfDay(ZoneOffset.UTC).toInstant() : null,
            instant -> instant != null ? instant.atZone(ZoneOffset.UTC).toLocalDate() : null)
        .bind(Faction::getDisbandedDate, Faction::setDisbandedDate);

    ImageUploadComponent imageUpload =
        new ImageUploadComponent(imageStorageService, url -> imageUrl.setValue(url));
    imageUpload.setUploadButtonText("Upload Image");
    imageUpload.setVisible(securityUtils.canEdit());

    HorizontalLayout imageRow = new HorizontalLayout(imageUrl, imageUpload);
    imageRow.setAlignItems(FlexComponent.Alignment.BASELINE);
    imageRow.setWidthFull();

    VerticalLayout layout =
        new VerticalLayout(
            name,
            description,
            leader,
            manager,
            alignment,
            isActive,
            affinity,
            imageRow,
            formedDate,
            disbandedDate);
    layout.setPadding(true);
    layout.setSpacing(true);

    Button saveButton = new Button("Save", e -> saveFaction());
    saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    saveButton.setId("save-button");
    Button cancelButton = new Button("Cancel", e -> editDialog.close());

    editDialog.add(layout);
    editDialog.getFooter().add(cancelButton, saveButton);
  }

  private void editFaction(final Faction faction) {
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

  private void openMembersDialog(final Faction faction) {
    Dialog dialog = new Dialog();
    dialog.setWidth("min(800px, 95vw)");
    dialog.setHeight("min(600px, 90vh)");

    // Fetch fresh faction data with members eagerly loaded
    Optional<Faction> updatedFaction = factionService.getFactionByIdWithMembers(faction.getId());
    if (updatedFaction.isEmpty()) {
      return;
    }

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
        .addColumn(s -> "%,d".formatted(s.getFans()))
        .setHeader("Fans")
        .setSortable(true);
    currentMembersGrid.setId("members-grid");
    currentMembersGrid.setItems(loadedFaction.getMembers());

    currentMembersGrid.addComponentColumn(
        memberState -> {
          Button removeBtn = new Button(new Icon(VaadinIcon.MINUS));
          removeBtn.addClickListener(
              e -> {
                factionService.removeMemberFromFaction(
                    loadedFaction.getId(), memberState.getWrestler().getId(), "Removed via UI");
                dialog.close();
                openMembersDialog(loadedFaction);
                refreshGrid();
              });
          removeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
          removeBtn.setTooltipText("Remove from Faction");
          removeBtn.setId("remove-member-" + memberState.getWrestler().getId());
          return removeBtn;
        });

    // Add wrestler section — ComboBox + button so tests can select by name
    H3 addWrestlerTitle = new H3("Add Wrestler");
    Long universeId = universeContextService.getCurrentUniverseId();
    List<Wrestler> available =
        wrestlerService.findAllIncludingInactive().stream()
            .filter(
                w -> {
                  WrestlerState state = wrestlerService.getOrCreateState(w.getId(), universeId);
                  return state.getFaction() == null;
                })
            .toList();

    ComboBox<Wrestler> wrestlerCombo = new ComboBox<>("Wrestler");
    wrestlerCombo.setItems(available);
    wrestlerCombo.setItemLabelGenerator(Wrestler::getName);
    wrestlerCombo.setId("add-member-wrestler-combo");

    Button addMemberButton = new Button("Add Member");
    addMemberButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
    addMemberButton.setId("add-member-button");
    addMemberButton.addClickListener(
        e -> {
          Wrestler selected = wrestlerCombo.getValue();
          if (selected != null) {
            factionService.addMemberToFaction(loadedFaction.getId(), selected.getId());
            dialog.close();
            openMembersDialog(loadedFaction);
            refreshGrid();
          }
        });

    HorizontalLayout addRow = new HorizontalLayout(wrestlerCombo, addMemberButton);
    addRow.setAlignItems(Alignment.BASELINE);

    content.add(currentMembersTitle, currentMembersGrid, new Div(), addWrestlerTitle, addRow);
    content.expand(currentMembersGrid);

    Button closeButton = new Button("Done", e -> dialog.close());
    dialog.getFooter().add(closeButton);
    dialog.add(content);
    dialog.open();
  }
}
