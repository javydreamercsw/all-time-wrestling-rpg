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

import com.github.javydreamercsw.base.domain.faction.Faction;
import com.github.javydreamercsw.base.domain.wrestler.Wrestler;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.service.faction.FactionService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

/**
 * View for managing Factions. Provides a list of factions with create, edit, and delete
 * functionality, including member management.
 */
@Route("faction-list")
@PageTitle("Faction List")
@Menu(order = 5, icon = "vaadin:group", title = "Factions")
@PermitAll
@Slf4j
public class FactionListView extends Main {

  private final FactionService factionService;
  private final WrestlerService wrestlerService;
  private Dialog editDialog;
  private Faction editingFaction;
  private Binder<Faction> binder;
  final TextField name;
  final Button createBtn;
  final Grid<Faction> factionGrid;

  public FactionListView(FactionService factionService, WrestlerService wrestlerService) {
    this.factionService = factionService;
    this.wrestlerService = wrestlerService;

    // Create form components
    name = new TextField();
    name.setPlaceholder("Enter faction name...");
    name.setAriaLabel("Faction Name");
    name.setId("name");
    name.setMaxLength(255);
    name.setMinWidth("20em");

    createBtn = new Button("Create Faction", new Icon(VaadinIcon.PLUS));
    createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    createBtn.setId("create-faction-button");
    createBtn.addClickListener(e -> openCreateDialog());

    // Initialize grid
    factionGrid = new Grid<>(Faction.class, false);
    factionGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
    factionGrid.setId("faction-grid");

    setupGrid();
    setupEditDialog();

    setSizeFull();
    addClassNames(
        LumoUtility.BoxSizing.BORDER,
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Gap.SMALL);

    // Create form layout
    FormLayout formLayout = new FormLayout();
    formLayout.add(name, createBtn);
    formLayout.setResponsiveSteps(
        new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("500px", 2));

    // Toolbar and form in a header row
    add(new ViewToolbar("Faction List", ViewToolbar.group(formLayout)));
    // Grid fills the rest
    add(factionGrid);

    refreshGrid();
  }

  private void setupGrid() {
    // Basic columns
    factionGrid.addColumn(Faction::getName).setHeader("Name").setSortable(true);

    factionGrid
        .addColumn(
            faction -> faction.getLeader() != null ? faction.getLeader().getName() : "No Leader")
        .setHeader("Leader")
        .setSortable(true);

    // Members column - show wrestler names instead of count
    factionGrid
        .addColumn(
            faction -> {
              if (faction.getMembers() == null || faction.getMembers().isEmpty()) {
                return "No members";
              }
              return faction.getMembers().stream()
                  .map(Wrestler::getName)
                  .collect(Collectors.joining(", "));
            })
        .setHeader("Members")
        .setSortable(false)
        .setFlexGrow(2); // Give more space for member names

    factionGrid
        .addColumn(faction -> faction.getIsActive() ? "Active" : "Disbanded")
        .setHeader("Status")
        .setSortable(true);

    factionGrid
        .addColumn(
            faction ->
                faction.getFormedDate() != null
                    ? faction.getFormedDate().atZone(ZoneOffset.UTC).toLocalDate().toString()
                    : "")
        .setHeader("Formed Date")
        .setSortable(true);

    // Actions column
    factionGrid
        .addComponentColumn(
            faction -> {
              HorizontalLayout actions = new HorizontalLayout();
              actions.setSpacing(true);

              Button viewBtn = new Button("View", new Icon(VaadinIcon.EYE));
              viewBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
              viewBtn.setId("view-" + faction.getId());
              viewBtn.addClickListener(e -> openViewDialog(faction));

              Button editBtn = new Button("Edit", new Icon(VaadinIcon.EDIT));
              editBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
              editBtn.setId("edit-" + faction.getId());
              editBtn.addClickListener(e -> openEditDialog(faction));

              Button membersBtn = new Button("Members", new Icon(VaadinIcon.GROUP));
              membersBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
              membersBtn.setId("members-" + faction.getId());
              membersBtn.addClickListener(e -> openMembersDialog(faction));

              Button deleteBtn = new Button("Delete", new Icon(VaadinIcon.TRASH));
              deleteBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
              deleteBtn.setId("delete-" + faction.getId());
              deleteBtn.addClickListener(e -> deleteFaction(faction));

              actions.add(viewBtn, editBtn, membersBtn, deleteBtn);
              return actions;
            })
        .setHeader("Actions")
        .setFlexGrow(1);

    factionGrid.setSizeFull();
  }

  @Transactional(readOnly = true)
  private void refreshGrid() {
    List<Faction> factions = factionService.findAllWithMembersAndTeams();
    factionGrid.setItems(factions);
  }

  private void setupEditDialog() {
    editDialog = new Dialog();
    editDialog.setWidth("600px");
    editDialog.setCloseOnEsc(true);
    editDialog.setCloseOnOutsideClick(false);

    // Form fields
    TextField editName = new TextField("Name");
    editName.setRequired(true);
    editName.setId("edit-name");
    editName.setMaxLength(255);

    TextArea editDescription = new TextArea("Description");
    editDescription.setMaxLength(1000);
    editDescription.setId("edit-description");
    editDescription.setHeight("100px");

    ComboBox<Wrestler> editLeader = new ComboBox<>("Leader");
    editLeader.setItems(
        wrestlerService.findAll().stream()
            .sorted(Comparator.comparing(Wrestler::getName))
            .collect(Collectors.toList()));
    editLeader.setId("edit-leader");
    editLeader.setItemLabelGenerator(Wrestler::getName);

    DatePicker editFormedDate = new DatePicker("Formed Date");
    editFormedDate.setId("edit-formed-date");
    DatePicker editDisbandedDate = new DatePicker("Disbanded Date");
    editDisbandedDate.setId("edit-disbanded-date");

    // Form layout
    FormLayout formLayout = new FormLayout();
    formLayout.add(editName, editLeader, editFormedDate, editDisbandedDate, editDescription);
    formLayout.setResponsiveSteps(
        new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("500px", 2));
    formLayout.setColspan(editDescription, 2);

    // Buttons
    Button saveBtn = new Button("Save", e -> saveFaction());
    saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    saveBtn.setId("save-button");

    Button cancelBtn = new Button("Cancel", e -> editDialog.close());

    HorizontalLayout buttonLayout = new HorizontalLayout(saveBtn, cancelBtn);
    buttonLayout.setJustifyContentMode(HorizontalLayout.JustifyContentMode.END);

    VerticalLayout dialogLayout = new VerticalLayout(formLayout, buttonLayout);
    editDialog.add(dialogLayout);

    // Setup binder
    binder = new Binder<>(Faction.class);
    binder
        .forField(editName)
        .asRequired("Name is required")
        .bind(Faction::getName, Faction::setName);
    binder.forField(editDescription).bind(Faction::getDescription, Faction::setDescription);

    binder.forField(editLeader).bind(Faction::getLeader, Faction::setLeader);
    binder
        .forField(editFormedDate)
        .bind(
            faction ->
                faction.getFormedDate() != null
                    ? faction.getFormedDate().atZone(ZoneOffset.UTC).toLocalDate()
                    : null,
            (faction, date) ->
                faction.setFormedDate(
                    date != null ? date.atStartOfDay().toInstant(ZoneOffset.UTC) : null));
    binder
        .forField(editDisbandedDate)
        .bind(
            faction ->
                faction.getDisbandedDate() != null
                    ? faction.getDisbandedDate().atZone(ZoneOffset.UTC).toLocalDate()
                    : null,
            (faction, date) ->
                faction.setDisbandedDate(
                    date != null ? date.atStartOfDay().toInstant(ZoneOffset.UTC) : null));
  }

  private void openCreateDialog() {
    editingFaction = Faction.builder().build();
    editingFaction.setIsActive(true); // Default to active
    editDialog.setHeaderTitle("Create Faction");
    binder.readBean(editingFaction);
    editDialog.open();
  }

  private void openEditDialog(@NonNull Faction faction) {
    editingFaction = faction;
    editDialog.setHeaderTitle("Edit Faction: " + faction.getName());
    binder.readBean(editingFaction);
    editDialog.open();
  }

  private void saveFaction() {
    try {
      binder.writeBean(editingFaction);

      // Update status based on disbanded date
      editingFaction.setIsActive(editingFaction.getDisbandedDate() == null);

      factionService.save(editingFaction);
      refreshGrid();
      editDialog.close();

      Notification.show("Faction saved successfully", 3000, Notification.Position.BOTTOM_END)
          .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

    } catch (ValidationException e) {
      Notification.show("Please fix the validation errors", 3000, Notification.Position.BOTTOM_END)
          .addThemeVariants(NotificationVariant.LUMO_ERROR);
    } catch (Exception e) {
      log.error("Error saving faction", e);
      Notification.show(
              "Error saving faction: " + e.getMessage(), 5000, Notification.Position.BOTTOM_END)
          .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
  }

  private void deleteFaction(@NonNull Faction faction) {
    ConfirmDialog confirmDialog = new ConfirmDialog();
    confirmDialog.setHeader("Delete Faction");
    confirmDialog.setText(
        "Are you sure you want to delete the faction '"
            + faction.getName()
            + "'? "
            + "This action cannot be undone.");
    confirmDialog.setCancelable(true);
    confirmDialog.setConfirmText("Delete");
    confirmDialog.setConfirmButtonTheme("error primary");
    confirmDialog.setId("delete-confirm-dialog");

    confirmDialog.addConfirmListener(
        e -> {
          try {
            factionService.delete(faction);
            refreshGrid();
            Notification.show(
                    "Faction deleted successfully", 3000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
          } catch (Exception ex) {
            log.error("Error deleting faction", ex);
            Notification.show(
                    "Error deleting faction: " + ex.getMessage(),
                    5000,
                    Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
          }
        });

    confirmDialog.open();
  }

  private void openViewDialog(@NonNull Faction faction) {
    // Reload faction with members to avoid lazy initialization issues
    Optional<Faction> factionWithMembers =
        factionService.getFactionByIdWithMembers(faction.getId());
    if (factionWithMembers.isEmpty()) {
      Notification.show("Faction not found", 3000, Notification.Position.BOTTOM_END)
          .addThemeVariants(NotificationVariant.LUMO_ERROR);
      return;
    }

    Faction loadedFaction = factionWithMembers.get();

    Dialog viewDialog = new Dialog();
    viewDialog.setWidth("800px");
    viewDialog.setHeaderTitle("Faction Details: " + loadedFaction.getName());

    VerticalLayout layout = new VerticalLayout();
    layout.setSpacing(true);
    layout.setPadding(true);

    // Basic information
    Div basicInfo = new Div();
    basicInfo.add(new H3("Basic Information"));
    basicInfo.add(new Div("Name: " + loadedFaction.getName()));
    basicInfo.add(new Div("Status: " + (loadedFaction.getIsActive() ? "Active" : "Disbanded")));
    basicInfo.add(
        new Div(
            "Leader: "
                + (loadedFaction.getLeader() != null
                    ? loadedFaction.getLeader().getName()
                    : "No Leader")));

    if (loadedFaction.getFormedDate() != null) {
      basicInfo.add(
          new Div(
              "Formed Date: "
                  + loadedFaction.getFormedDate().atZone(ZoneOffset.UTC).toLocalDate()));
    }

    if (loadedFaction.getDisbandedDate() != null) {
      basicInfo.add(
          new Div(
              "Disbanded Date: "
                  + loadedFaction.getDisbandedDate().atZone(ZoneOffset.UTC).toLocalDate()));
    }

    if (loadedFaction.getDescription() != null
        && !loadedFaction.getDescription().trim().isEmpty()) {
      basicInfo.add(new Div("Description: " + loadedFaction.getDescription()));
    }

    layout.add(basicInfo);

    // Members section
    if (!loadedFaction.getMembers().isEmpty()) {
      Div membersInfo = new Div();
      membersInfo.add(new H3("Members (" + loadedFaction.getMemberCount() + ")"));

      Grid<Wrestler> membersGrid = new Grid<>(Wrestler.class, false);
      membersGrid.addColumn(Wrestler::getName).setHeader("Name");
      membersGrid
          .addColumn(wrestler -> wrestler.getTier() != null ? wrestler.getTier().name() : "")
          .setHeader("Tier");
      membersGrid.setItems(loadedFaction.getMembers());
      membersGrid.setHeight("200px");

      membersInfo.add(membersGrid);
      layout.add(membersInfo);
    } else {
      layout.add(new Div("No members in this faction."));
    }

    Button closeBtn = new Button("Close", e -> viewDialog.close());
    closeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    HorizontalLayout buttonLayout = new HorizontalLayout(closeBtn);
    buttonLayout.setJustifyContentMode(HorizontalLayout.JustifyContentMode.END);

    layout.add(buttonLayout);
    viewDialog.add(layout);
    viewDialog.open();
  }

  private void openMembersDialog(@NonNull Faction faction) {
    // Reload faction with members to avoid lazy initialization issues
    Optional<Faction> factionWithMembers =
        factionService.getFactionByIdWithMembers(faction.getId());
    if (factionWithMembers.isEmpty()) {
      Notification.show("Faction not found", 3000, Notification.Position.BOTTOM_END)
          .addThemeVariants(NotificationVariant.LUMO_ERROR);
      return;
    }

    Faction loadedFaction = factionWithMembers.get();

    Dialog membersDialog = new Dialog();
    membersDialog.setWidth("700px");
    membersDialog.setHeaderTitle("Manage Members: " + loadedFaction.getName());

    VerticalLayout layout = new VerticalLayout();
    layout.setSpacing(true);
    layout.setPadding(true);

    // Current members grid
    H3 currentMembersTitle = new H3("Current Members (" + loadedFaction.getMemberCount() + ")");
    Grid<Wrestler> currentMembersGrid = new Grid<>(Wrestler.class, false);
    currentMembersGrid.addColumn(Wrestler::getName).setHeader("Name");
    currentMembersGrid
        .addColumn(wrestler -> wrestler.getTier() != null ? wrestler.getTier().name() : "")
        .setHeader("Tier");
    currentMembersGrid.addColumn(Wrestler::getFans).setHeader("Fans");
    currentMembersGrid.setId("members-grid");

    // Remove member button
    currentMembersGrid
        .addComponentColumn(
            wrestler -> {
              Button removeBtn = new Button("Remove", new Icon(VaadinIcon.MINUS));
              removeBtn.setId("remove-member-" + wrestler.getId());
              removeBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
              removeBtn.addClickListener(
                  e -> {
                    try {
                      factionService.removeMemberFromFaction(
                          loadedFaction.getId(), wrestler.getId(), "Removed via UI");
                      refreshMembersDialog(loadedFaction, currentMembersGrid, membersDialog);
                      refreshGrid(); // Refresh main grid
                      Notification.show(
                              wrestler.getName() + " removed from " + loadedFaction.getName(),
                              3000,
                              Notification.Position.BOTTOM_END)
                          .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    } catch (Exception ex) {
                      log.error("Error removing member", ex);
                      Notification.show(
                              "Error removing member: " + ex.getMessage(),
                              5000,
                              Notification.Position.BOTTOM_END)
                          .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }
                  });
              return removeBtn;
            })
        .setHeader("Actions");

    currentMembersGrid.setItems(loadedFaction.getMembers());
    currentMembersGrid.setHeight("200px");

    // Add member section
    H3 addMemberTitle = new H3("Add Member");
    ComboBox<Wrestler> wrestlerCombo = new ComboBox<>("Select Wrestler");
    wrestlerCombo.setId("add-member-wrestler-combo");
    wrestlerCombo.setItems(
        wrestlerService.findAll().stream()
            .filter(w -> !loadedFaction.hasMember(w))
            .sorted(Comparator.comparing(Wrestler::getName))
            .collect(Collectors.toList()));
    wrestlerCombo.setItemLabelGenerator(Wrestler::getName);
    wrestlerCombo.setWidth("300px");

    Button addBtn = new Button("Add Member", new Icon(VaadinIcon.PLUS));
    addBtn.setId("add-member-button");
    addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    addBtn.addClickListener(
        e -> {
          Wrestler selectedWrestler = wrestlerCombo.getValue();
          if (selectedWrestler != null) {
            try {
              factionService.addMemberToFaction(loadedFaction.getId(), selectedWrestler.getId());
              wrestlerCombo.clear();
              refreshMembersDialog(loadedFaction, currentMembersGrid, membersDialog);
              refreshGrid(); // Refresh main grid
              Notification.show(
                      selectedWrestler.getName() + " added to " + loadedFaction.getName(),
                      3000,
                      Notification.Position.BOTTOM_END)
                  .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
              log.error("Error adding member", ex);
              Notification.show(
                      "Error adding member: " + ex.getMessage(),
                      5000,
                      Notification.Position.BOTTOM_END)
                  .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
          }
        });

    HorizontalLayout addMemberLayout = new HorizontalLayout(wrestlerCombo, addBtn);
    addMemberLayout.setAlignItems(HorizontalLayout.Alignment.END);

    // Close button
    Button closeBtn = new Button("Close", e -> membersDialog.close());
    closeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    HorizontalLayout buttonLayout = new HorizontalLayout(closeBtn);
    buttonLayout.setJustifyContentMode(HorizontalLayout.JustifyContentMode.END);

    layout.add(
        currentMembersTitle, currentMembersGrid, addMemberTitle, addMemberLayout, buttonLayout);
    membersDialog.add(layout);
    membersDialog.open();
  }

  private void refreshMembersDialog(
      @NonNull Faction faction, @NonNull Grid<Wrestler> membersGrid, @NonNull Dialog dialog) {
    // Refresh faction data from database with members eagerly loaded
    Optional<Faction> updatedFaction = factionService.getFactionByIdWithMembers(faction.getId());
    if (updatedFaction.isPresent()) {
      membersGrid.setItems(updatedFaction.get().getMembers());
      dialog.setHeaderTitle(
          "Manage Members: "
              + updatedFaction.get().getName()
              + " ("
              + updatedFaction.get().getMemberCount()
              + ")");
    }
  }
}
