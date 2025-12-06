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
package com.github.javydreamercsw.management.ui.view.injury;

import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.injury.InjurySeverity;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.injury.InjuryService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;
import lombok.NonNull;

public class CreateInjuryDialog extends Dialog {

  private final Binder<Injury> binder = new Binder<>(Injury.class);

  public CreateInjuryDialog(
      @NonNull Wrestler wrestler, @NonNull InjuryService injuryService, @NonNull Runnable onSave) {
    setHeaderTitle("Create Injury for " + wrestler.getName());
    setId("create-injury-dialog");

    FormLayout formLayout = new FormLayout();
    TextField nameField = new TextField("Name");
    nameField.setId("create-injury-name");
    TextArea descriptionField = new TextArea("Description");
    descriptionField.setId("create-injury-description");
    ComboBox<InjurySeverity> severityField = new ComboBox<>("Severity");
    severityField.setItems(
        Arrays.stream(InjurySeverity.values())
            .sorted(Comparator.comparing(InjurySeverity::name))
            .collect(Collectors.toList()));
    severityField.setId("create-injury-severity");
    TextArea injuryNotesField = new TextArea("Injury Notes");
    injuryNotesField.setId("create-injury-notes");

    formLayout.add(nameField, descriptionField, severityField, injuryNotesField);

    binder.forField(nameField).asRequired().bind(Injury::getName, Injury::setName);
    binder.forField(descriptionField).bind(Injury::getDescription, Injury::setDescription);
    binder.forField(severityField).asRequired().bind(Injury::getSeverity, Injury::setSeverity);
    binder.forField(injuryNotesField).bind(Injury::getInjuryNotes, Injury::setInjuryNotes);

    Injury injury = new Injury();
    binder.setBean(injury);

    Button saveButton =
        new Button(
            "Save",
            e -> {
              if (binder.writeBeanIfValid(injury)) {
                injuryService.createInjury(
                    wrestler.getId(),
                    injury.getName(),
                    injury.getDescription(),
                    injury.getSeverity(),
                    injury.getInjuryNotes());
                onSave.run();
                close();
              }
            });
    saveButton.setId("create-injury-save-button");

    Button cancelButton = new Button("Cancel", e -> close());

    getFooter().add(saveButton, cancelButton);
    add(formLayout);
  }
}
