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
package com.github.javydreamercsw.management.ui.view.universe;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.Universe.UniverseType;
import com.github.javydreamercsw.management.domain.universe.UniverseMembership;
import com.github.javydreamercsw.management.domain.universe.UniverseMembership.UniverseMemberRole;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.AccountService;
import com.github.javydreamercsw.management.service.expansion.Expansion;
import com.github.javydreamercsw.management.service.universe.UniverseMembershipService;
import com.github.javydreamercsw.management.service.universe.UniverseService;
import com.github.javydreamercsw.management.service.universe.UniverseSettingsService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;

public class UniverseFormDialog extends Dialog {

  private static final DateTimeFormatter DATE_FMT =
      DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault());

  private final Universe universe;
  private final Binder<Universe> binder = new Binder<>(Universe.class);
  private final UniverseService universeService;

  public UniverseFormDialog(
      @NonNull final UniverseService universeService,
      @NonNull final Universe universe,
      @NonNull final Runnable onSave) {
    this(universeService, null, null, null, null, universe, onSave);
  }

  public UniverseFormDialog(
      @NonNull final UniverseService universeService,
      final UniverseMembershipService membershipService,
      final AccountService accountService,
      @NonNull final Universe universe,
      @NonNull final Runnable onSave) {
    this(universeService, membershipService, accountService, null, null, universe, onSave);
  }

  public UniverseFormDialog(
      @NonNull final UniverseService universeService,
      final UniverseMembershipService membershipService,
      final AccountService accountService,
      final UniverseSettingsService settingsService,
      final WrestlerRepository wrestlerRepository,
      @NonNull final Universe universe,
      @NonNull final Runnable onSave) {
    this.universeService = universeService;
    this.universe = universe;

    boolean isEdit = universe.getId() != null;

    if (isEdit && membershipService != null && accountService != null) {
      TabSheet tabs = new TabSheet();
      tabs.setWidthFull();
      tabs.add(new Tab("Details"), buildDetailsLayout(onSave));
      tabs.add(new Tab("Members"), buildMembersLayout(membershipService, accountService));
      if (settingsService != null && wrestlerRepository != null) {
        tabs.add(new Tab("Settings"), buildSettingsLayout(settingsService, wrestlerRepository));
      }
      add(tabs);
    } else {
      add(buildDetailsLayout(onSave));
    }
  }

  private VerticalLayout buildDetailsLayout(final Runnable onSave) {
    TextField name = new TextField("Name");
    name.setRequired(true);
    name.setMaxLength(255);
    name.setWidthFull();

    ComboBox<UniverseType> type = new ComboBox<>("Type");
    type.setItems(
        Arrays.stream(UniverseType.values())
            .sorted(Comparator.comparing(UniverseType::name))
            .collect(Collectors.toList()));
    type.setItemLabelGenerator(
        t ->
            switch (t) {
              case GLOBAL -> "Global";
              case LEAGUE -> "League";
              case CAMPAIGN -> "Campaign";
            });
    type.setRequired(true);
    type.setWidthFull();

    binder
        .forField(name)
        .asRequired("Name is required.")
        .bind(Universe::getName, Universe::setName);
    binder
        .forField(type)
        .asRequired("Type is required.")
        .bind(Universe::getType, Universe::setType);

    binder.readBean(universe);

    Button saveButton =
        new Button(
            "Save",
            event -> {
              if (binder.writeBeanIfValid(universe)) {
                try {
                  universeService.save(universe);
                  onSave.run();
                  close();
                } catch (IllegalArgumentException ex) {
                  Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE)
                      .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
              }
            });
    saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    Button cancelButton = new Button("Cancel", event -> close());
    getFooter().add(new HorizontalLayout(saveButton, cancelButton));

    VerticalLayout layout = new VerticalLayout(new FormLayout(name, type));
    layout.setPadding(false);
    return layout;
  }

  private VerticalLayout buildMembersLayout(
      final UniverseMembershipService membershipService, final AccountService accountService) {

    Grid<UniverseMembership> grid = new Grid<>(UniverseMembership.class, false);
    grid.addColumn(m -> m.getAccount().getUsername()).setHeader("Username").setSortable(true);
    grid.addColumn(m -> m.getRole().name()).setHeader("Role").setSortable(true);
    grid.addColumn(m -> m.getJoinedDate() != null ? DATE_FMT.format(m.getJoinedDate()) : "")
        .setHeader("Joined");
    grid.addComponentColumn(
            m -> {
              Button remove = new Button("Remove");
              remove.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
              remove.addClickListener(
                  e -> {
                    try {
                      membershipService.removeMember(universe, m.getAccount());
                      refreshMembersGrid(grid, membershipService);
                    } catch (IllegalStateException ex) {
                      Notification.show(ex.getMessage(), 4000, Notification.Position.BOTTOM_END)
                          .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }
                  });
              return remove;
            })
        .setHeader("Actions");

    refreshMembersGrid(grid, membershipService);

    ComboBox<Account> accountPicker = new ComboBox<>("Add Account");
    accountPicker.setItems(accountService.findAll());
    accountPicker.setItemLabelGenerator(Account::getUsername);
    accountPicker.setWidthFull();

    ComboBox<UniverseMemberRole> rolePicker = new ComboBox<>("Role");
    rolePicker.setItems(UniverseMemberRole.values());
    rolePicker.setValue(UniverseMemberRole.MEMBER);
    rolePicker.setWidthFull();

    Button addButton = new Button("Add Member");
    addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
    addButton.addClickListener(
        e -> {
          Account selected = accountPicker.getValue();
          UniverseMemberRole role = rolePicker.getValue();
          if (selected == null || role == null) {
            return;
          }
          try {
            membershipService.addMember(universe, selected, role);
            accountPicker.clear();
            refreshMembersGrid(grid, membershipService);
          } catch (IllegalStateException ex) {
            Notification.show(ex.getMessage(), 4000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
          }
        });

    HorizontalLayout addRow = new HorizontalLayout(accountPicker, rolePicker, addButton);
    addRow.setAlignItems(Alignment.BASELINE);
    addRow.setWidthFull();

    VerticalLayout layout = new VerticalLayout(grid, addRow);
    layout.setPadding(false);
    layout.setWidthFull();
    return layout;
  }

  private VerticalLayout buildSettingsLayout(
      final UniverseSettingsService settingsService, final WrestlerRepository wrestlerRepository) {

    // --- Expansion overrides ---
    H4 expansionHeader = new H4("Expansion Settings");
    Grid<Expansion> expansionGrid = new Grid<>(Expansion.class, false);
    expansionGrid.addColumn(Expansion::getName).setHeader("Expansion").setSortable(true);
    expansionGrid.addColumn(Expansion::getCode).setHeader("Code").setSortable(true);
    expansionGrid
        .addComponentColumn(
            exp -> {
              Checkbox cb = new Checkbox(exp.isEnabled());
              cb.setId("universe-expansion-" + exp.getCode());
              cb.addValueChangeListener(
                  ev -> {
                    settingsService.setExpansionEnabled(universe, exp.getCode(), ev.getValue());
                    String state = ev.getValue() ? "enabled" : "disabled";
                    Notification.show(
                            "Expansion '%s' %s for this universe.".formatted(exp.getName(), state),
                            3000,
                            Notification.Position.BOTTOM_END)
                        .addThemeVariants(
                            ev.getValue()
                                ? NotificationVariant.LUMO_SUCCESS
                                : NotificationVariant.LUMO_CONTRAST);
                  });
              return cb;
            })
        .setHeader("Enabled");
    expansionGrid.setItems(settingsService.getExpansionsForUniverse(universe));
    expansionGrid.setHeight("200px");

    // --- Wrestler exclusions ---
    H4 exclusionHeader = new H4("Excluded Wrestlers");
    Grid<Wrestler> exclusionGrid = new Grid<>(Wrestler.class, false);
    exclusionGrid.addColumn(Wrestler::getName).setHeader("Wrestler").setSortable(true);
    exclusionGrid
        .addComponentColumn(
            w -> {
              Button include = new Button("Remove Exclusion");
              include.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
              include.addClickListener(
                  e -> {
                    settingsService.includeWrestler(universe, w);
                    refreshExclusionGrid(exclusionGrid, settingsService);
                  });
              return include;
            })
        .setHeader("Actions");
    refreshExclusionGrid(exclusionGrid, settingsService);
    exclusionGrid.setHeight("200px");

    ComboBox<Wrestler> wrestlerPicker = new ComboBox<>("Exclude Wrestler");
    wrestlerPicker.setItems(
        wrestlerRepository.findAllByActiveTrue().stream()
            .sorted(Comparator.comparing(Wrestler::getName))
            .collect(Collectors.toList()));
    wrestlerPicker.setItemLabelGenerator(Wrestler::getName);
    wrestlerPicker.setWidthFull();

    Button excludeButton = new Button("Exclude");
    excludeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
    excludeButton.addClickListener(
        e -> {
          Wrestler selected = wrestlerPicker.getValue();
          if (selected == null) {
            return;
          }
          Set<Wrestler> excluded = settingsService.getExcludedWrestlers(universe);
          if (excluded.contains(selected)) {
            Notification.show(
                selected.getName() + " is already excluded.",
                3000,
                Notification.Position.BOTTOM_END);
            return;
          }
          settingsService.excludeWrestler(universe, selected);
          wrestlerPicker.clear();
          refreshExclusionGrid(exclusionGrid, settingsService);
        });

    HorizontalLayout addExclusionRow = new HorizontalLayout(wrestlerPicker, excludeButton);
    addExclusionRow.setAlignItems(Alignment.BASELINE);
    addExclusionRow.setWidthFull();

    VerticalLayout layout =
        new VerticalLayout(
            expansionHeader, expansionGrid, exclusionHeader, exclusionGrid, addExclusionRow);
    layout.setPadding(false);
    layout.setWidthFull();
    return layout;
  }

  private void refreshMembersGrid(
      final Grid<UniverseMembership> grid, final UniverseMembershipService membershipService) {
    grid.setItems(membershipService.getMembersForUniverse(universe));
  }

  private void refreshExclusionGrid(
      final Grid<Wrestler> grid, final UniverseSettingsService settingsService) {
    grid.setItems(settingsService.getExcludedWrestlers(universe));
  }
}
