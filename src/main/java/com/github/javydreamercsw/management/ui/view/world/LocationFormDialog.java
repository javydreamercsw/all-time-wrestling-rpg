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
package com.github.javydreamercsw.management.ui.view.world;

import com.github.javydreamercsw.base.ai.image.ImageStorageService;
import com.github.javydreamercsw.base.ui.component.ImageUploadComponent;
import com.github.javydreamercsw.management.domain.world.Location;
import com.github.javydreamercsw.management.service.world.LocationService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LocationFormDialog extends Dialog {

  private final LocationService service;
  private Location location;
  private final Runnable onSave;

  private TextField name = new TextField("Name");
  private TextArea description = new TextArea("Description");
  private TextField culturalTags = new TextField("Cultural Tags (comma-separated)");
  private TextField imageUrl = new TextField("Image URL");
  private Image locationImage = new Image();

  private Binder<Location> binder = new BeanValidationBinder<>(Location.class);

  public LocationFormDialog(
      LocationService service,
      ImageStorageService storageService,
      Location location,
      Runnable onSave) {
    this.service = service;
    this.location = location;
    this.onSave = onSave;

    configureBinder();
    initUI(storageService);
    populateForm();
  }

  private void configureBinder() {
    binder
        .forField(name)
        .withValidator(val -> val != null && !val.trim().isEmpty(), "Name cannot be empty")
        .withValidator(
            val ->
                service.findByName(val).isEmpty()
                    || (location != null && location.getName().equals(val)),
            "Location with this name already exists")
        .bind(Location::getName, Location::setName);
    binder.bind(description, Location::getDescription, Location::setDescription);
    binder.bind(imageUrl, Location::getImageUrl, Location::setImageUrl);
    binder
        .forField(culturalTags)
        .bind(
            loc -> String.join(", ", loc.getCulturalTags()),
            (loc, val) -> {
              Set<String> tags = new HashSet<>();
              if (val != null && !val.trim().isEmpty()) {
                Arrays.stream(val.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(tags::add);
              }
              loc.setCulturalTags(tags);
            });
  }

  private void initUI(ImageStorageService storageService) {
    setHeaderTitle(location == null ? "Add New Location" : "Edit Location");
    setWidth("800px");

    name.setWidthFull();
    name.setMaxLength(Location.DESCRIPTION_MAX_LENGTH);
    name.setValueChangeMode(ValueChangeMode.EAGER);
    name.addValueChangeListener(e -> binder.validate());

    description.setWidthFull();
    culturalTags.setWidthFull();

    imageUrl.setWidthFull();
    imageUrl.setReadOnly(true);
    imageUrl.addValueChangeListener(e -> updateImagePreview(e.getValue()));

    locationImage.setHeight("150px");
    locationImage.setWidth("150px");
    locationImage.addClassNames(LumoUtility.BorderRadius.MEDIUM, LumoUtility.Margin.AUTO);
    locationImage.getStyle().set("display", "block");
    updateImagePreview(location != null ? location.getImageUrl() : null);

    ImageUploadComponent imageUpload =
        new ImageUploadComponent(
            storageService,
            url -> {
              imageUrl.setValue(url);
            });
    imageUpload.setUploadButtonText("Upload Image");

    VerticalLayout imageLayout = new VerticalLayout(locationImage, imageUrl, imageUpload);
    imageLayout.setAlignItems(FlexComponent.Alignment.CENTER);
    imageLayout.setSpacing(true);
    imageLayout.setPadding(false);

    FormLayout formLayout = new FormLayout();
    formLayout.add(name, description, culturalTags);
    formLayout.setColspan(name, 2);
    formLayout.setColspan(description, 2);
    formLayout.setColspan(culturalTags, 2);

    Button saveButton = new Button(location == null ? "Add" : "Save", e -> saveLocation());
    saveButton.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY);
    saveButton.setEnabled(false); // Disable initially
    binder.addStatusChangeListener(e -> saveButton.setEnabled(binder.isValid()));

    Button deleteButton = new Button("Delete", e -> deleteLocation());
    deleteButton.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_ERROR);
    deleteButton.setVisible(location != null && location.getId() != null);

    Button cancelButton = new Button("Cancel", e -> close());

    HorizontalLayout buttons = new HorizontalLayout(saveButton, deleteButton, cancelButton);
    buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
    buttons.setWidthFull();

    VerticalLayout dialogLayout = new VerticalLayout(formLayout, imageLayout, buttons);
    dialogLayout.setPadding(true);
    dialogLayout.setSpacing(true);
    add(dialogLayout);
  }

  private void updateImagePreview(String url) {
    if (url != null && !url.trim().isEmpty()) {
      locationImage.setSrc(url);
      locationImage.setVisible(true);
    } else {
      locationImage.setSrc("https://via.placeholder.com/150");
      locationImage.setVisible(true);
    }
  }

  private void populateForm() {
    if (location != null) {
      binder.readBean(location);
    } else {
      location = new Location();
      binder.readBean(location);
    }
  }

  private void saveLocation() {
    if (binder.isValid()) {
      try {
        binder.writeBean(location);
        if (location.getId() == null) {
          service.createLocation(
              location.getName(),
              location.getDescription(),
              location.getImageUrl(),
              location.getCulturalTags());
        } else {
          service.updateLocation(
              location.getId(),
              location.getName(),
              location.getDescription(),
              location.getImageUrl(),
              location.getCulturalTags());
        }
        onSave.run();
        close();
      } catch (Exception e) {
        log.error("Error saving location", e);
        Notification.show(
                "Error saving location: " + e.getMessage(), 5000, Notification.Position.MIDDLE)
            .addThemeVariants(NotificationVariant.LUMO_ERROR);
      }
    }
  }

  private void deleteLocation() {
    if (location != null && location.getId() != null) {
      try {
        service.deleteLocation(location.getId());
        onSave.run();
        Notification.show(
            "Location deleted successfully!", 3000, Notification.Position.BOTTOM_START);
        close();
      } catch (Exception e) {
        log.error("Error deleting location", e);
        Notification.show(
                "Error deleting location: " + e.getMessage(), 5000, Notification.Position.MIDDLE)
            .addThemeVariants(NotificationVariant.LUMO_ERROR);
      }
    }
  }
}
