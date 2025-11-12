package com.github.javydreamercsw.management.ui.view.wrestler;

import com.github.javydreamercsw.management.domain.wrestler.Gender;
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

    formLayout.add(
        nameField,
        genderField,
        deckSizeField,
        startingHealthField,
        lowHealthField,
        startingStaminaField,
        lowStaminaField);

    binder.forField(nameField).bind(Wrestler::getName, Wrestler::setName);
    binder.forField(genderField).bind(Wrestler::getGender, Wrestler::setGender);
    binder
        .forField(deckSizeField)
        .withConverter(new StringToIntegerConverter("Must be a number"))
        .bind(Wrestler::getDeckSize, Wrestler::setDeckSize);
    binder
        .forField(startingHealthField)
        .withConverter(new StringToIntegerConverter("Must be a number"))
        .bind(Wrestler::getStartingHealth, Wrestler::setStartingHealth);
    binder
        .forField(lowHealthField)
        .withConverter(new StringToIntegerConverter("Must be a number"))
        .bind(Wrestler::getLowHealth, Wrestler::setLowHealth);
    binder
        .forField(startingStaminaField)
        .withConverter(new StringToIntegerConverter("Must be a number"))
        .bind(Wrestler::getStartingStamina, Wrestler::setStartingStamina);
    binder
        .forField(lowStaminaField)
        .withConverter(new StringToIntegerConverter("Must be a number"))
        .bind(Wrestler::getLowStamina, Wrestler::setLowStamina);

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
