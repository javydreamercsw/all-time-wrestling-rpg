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

import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.Universe.UniverseType;
import com.github.javydreamercsw.management.service.universe.UniverseService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;
import lombok.NonNull;

public class UniverseFormDialog extends Dialog {

  private final Universe universe;
  private final Binder<Universe> binder = new Binder<>(Universe.class);
  private final UniverseService universeService;

  public UniverseFormDialog(
      @NonNull final UniverseService universeService,
      @NonNull final Universe universe,
      @NonNull final Runnable onSave) {
    this.universeService = universeService;
    this.universe = universe;

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

    add(new FormLayout(name, type));

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
  }
}
