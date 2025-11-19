package com.github.javydreamercsw.management.ui.view.show;

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import lombok.NonNull;

public class EditShowNameDialog extends Dialog {

  private final ShowService showService;
  private final Show show;
  private final Binder<Show> binder = new Binder<>(Show.class);

  private final TextField nameField = new TextField("Show Name");

  public EditShowNameDialog(@NonNull ShowService showService, @NonNull Show show) {
    this.showService = showService;
    this.show = show;

    setHeaderTitle("Edit Show Name");
    setModal(true);
    setResizable(false);
    setCloseOnEsc(true);
    setCloseOnOutsideClick(false);

    // Configure form fields
    nameField.setValue(show.getName());
    nameField.setWidthFull();
    nameField.setRequired(true);

    // Bind fields
    binder.bind(nameField, Show::getName, Show::setName);

    // Buttons
    Button saveButton = new Button("Save", e -> saveShowName());
    saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    Button cancelButton = new Button("Cancel", e -> close());

    HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton);
    buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
    buttonLayout.setWidthFull();

    FormLayout formLayout = new FormLayout(nameField);
    formLayout.setWidthFull();

    VerticalLayout dialogLayout = new VerticalLayout(formLayout, buttonLayout);
    dialogLayout.setSpacing(true);
    dialogLayout.setPadding(false);
    dialogLayout.setWidth("400px");

    add(dialogLayout);
  }

  private void saveShowName() {
    if (binder.isValid()) {
      try {
        showService.updateShow(
            show.getId(),
            nameField.getValue(),
            show.getDescription(),
            show.getType().getId(),
            show.getShowDate(),
            show.getSeason().getId(),
            show.getTemplate().getId());
        Notification.show("Show name updated successfully!", 3000, Notification.Position.BOTTOM_END)
            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        close();
      } catch (Exception e) {
        Notification.show(
                "Error updating show name: " + e.getMessage(), 5000, Notification.Position.MIDDLE)
            .addThemeVariants(NotificationVariant.LUMO_ERROR);
      }
    } else {
      Notification.show(
              "Please correct the errors in the form.", 3000, Notification.Position.MIDDLE)
          .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
  }
}
