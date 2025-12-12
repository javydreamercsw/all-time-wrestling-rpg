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
package com.github.javydreamercsw.management.ui.view.wrestler;

import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import lombok.NonNull;

public class WrestlerDialog extends Dialog {

  private final WrestlerService wrestlerService;
  private final Wrestler wrestler;
  private final Binder<Wrestler> binder = new Binder<>(Wrestler.class);

  public WrestlerDialog(@NonNull WrestlerService wrestlerService, @NonNull Runnable onSave) {
    this(wrestlerService, createDefaultWrestler(), onSave);
    setHeaderTitle("Create Wrestler");
  }

  private static Wrestler createDefaultWrestler() {
    return Wrestler.builder()
        .name("")
        .description("")
        .deckSize(15)
        .startingHealth(15)
        .lowHealth(0)
        .startingStamina(0)
        .lowStamina(0)
        .fans(0L)
        .isPlayer(false)
        .bumps(0)
        .gender(Gender.MALE)
        .tier(WrestlerTier.ROOKIE)
        .build();
  }

  public WrestlerDialog(
      @NonNull WrestlerService wrestlerService,
      @NonNull Wrestler wrestler,
      @NonNull Runnable onSave) {
    this.wrestlerService = wrestlerService;
    this.wrestler = wrestler;

    setHeaderTitle("Edit Wrestler");

    FormLayout formLayout = new FormLayout();
    TextField nameField = new TextField("Name");
    nameField.setId("wrestler-dialog-name-field");
    ComboBox<Gender> genderField = new ComboBox<>("Gender");
    genderField.setItems(Gender.values());
    genderField.setId("wrestler-dialog-gender-field");
    TextField deckSizeField = new TextField("Deck Size");
    deckSizeField.setId("wrestler-dialog-deck-size-field");
    TextField startingHealthField = new TextField("Starting Health");
    startingHealthField.setId("wrestler-dialog-starting-health-field");
    TextField lowHealthField = new TextField("Low Health");
    lowHealthField.setId("wrestler-dialog-low-health-field");
    TextField startingStaminaField = new TextField("Starting Stamina");
    startingStaminaField.setId("wrestler-dialog-starting-stamina-field");
    TextField lowStaminaField = new TextField("Low Stamina");
    lowStaminaField.setId("wrestler-dialog-low-stamina-field");
    TextField imageUrlField = new TextField("Image URL");
    imageUrlField.setId("wrestler-dialog-image-url-field");

    formLayout.add(
        nameField,
        genderField,
        deckSizeField,
        startingHealthField,
        lowHealthField,
        startingStaminaField,
        lowStaminaField,
        imageUrlField);

    binder.forField(nameField).bind(Wrestler::getName, Wrestler::setName);
    binder.forField(genderField).bind(Wrestler::getGender, Wrestler::setGender);
    binder
        .forField(deckSizeField)
        .withConverter(new StringToIntegerConverter(0, "Must be a number"))
        .bind(Wrestler::getDeckSize, Wrestler::setDeckSize);
    binder
        .forField(startingHealthField)
        .withConverter(new StringToIntegerConverter(0, "Must be a number"))
        .bind(Wrestler::getStartingHealth, Wrestler::setStartingHealth);
    binder
        .forField(lowHealthField)
        .withConverter(new StringToIntegerConverter(0, "Must be a number"))
        .bind(Wrestler::getLowHealth, Wrestler::setLowHealth);
    binder
        .forField(startingStaminaField)
        .withConverter(new StringToIntegerConverter(0, "Must be a number"))
        .bind(Wrestler::getStartingStamina, Wrestler::setStartingStamina);
    binder
        .forField(lowStaminaField)
        .withConverter(new StringToIntegerConverter(0, "Must be a number"))
        .bind(Wrestler::getLowStamina, Wrestler::setLowStamina);
    binder.forField(imageUrlField).bind(Wrestler::getImageUrl, Wrestler::setImageUrl);

    binder.readBean(this.wrestler);

    Button saveButton =
        new Button(
            "Save",
            event -> {
              if (binder.writeBeanIfValid(this.wrestler)) {
                this.wrestlerService.save(this.wrestler);
                onSave.run();
                close();
              }
            });
    saveButton.setId("wrestler-dialog-save-button");
    saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    Button cancelButton = new Button("Cancel", event -> close());
    cancelButton.setId("wrestler-dialog-cancel-button");

    getFooter().add(new HorizontalLayout(saveButton, cancelButton));
    add(formLayout);
  }
}
