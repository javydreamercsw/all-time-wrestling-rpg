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
import lombok.NonNull;

public class CreateInjuryDialog extends Dialog {

  private final Binder<Injury> binder = new Binder<>(Injury.class);

  public CreateInjuryDialog(
      @NonNull Wrestler wrestler, @NonNull InjuryService injuryService, @NonNull Runnable onSave) {
    setHeaderTitle("Create Injury for " + wrestler.getName());

    FormLayout formLayout = new FormLayout();
    TextField nameField = new TextField("Name");
    TextArea descriptionField = new TextArea("Description");
    ComboBox<InjurySeverity> severityField = new ComboBox<>("Severity");
    severityField.setItems(InjurySeverity.values());
    TextArea injuryNotesField = new TextArea("Injury Notes");

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

    Button cancelButton = new Button("Cancel", e -> close());

    getFooter().add(saveButton, cancelButton);
    add(formLayout);
  }
}
