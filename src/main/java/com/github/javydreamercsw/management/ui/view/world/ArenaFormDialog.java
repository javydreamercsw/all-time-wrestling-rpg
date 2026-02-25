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
import com.github.javydreamercsw.management.domain.world.Arena;
import com.github.javydreamercsw.management.domain.world.Arena.AlignmentBias;
import com.github.javydreamercsw.management.domain.world.Location;
import com.github.javydreamercsw.management.service.world.ArenaService;
import com.github.javydreamercsw.management.service.world.LocationService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
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
public class ArenaFormDialog extends Dialog {

  private final ArenaService arenaService;
  private final LocationService locationService;
  private Arena arena;
  private final Runnable onSave;

  private TextField name = new TextField("Name");
  private TextArea description = new TextArea("Description");
  private ComboBox<Location> locationCombo = new ComboBox<>("Location");
  private IntegerField capacity = new IntegerField("Capacity");
  private ComboBox<AlignmentBias> alignmentBias = new ComboBox<>("Alignment Bias");
  private TextField imageUrl = new TextField("Image URL");
  private TextField environmentalTraits = new TextField("Environmental Traits (comma-separated)");
  private Image arenaImage = new Image();

  private Binder<Arena> binder = new BeanValidationBinder<>(Arena.class);

  public ArenaFormDialog(
      ArenaService arenaService,
      LocationService locationService,
      ImageStorageService storageService,
      Arena arena,
      Runnable onSave) {
    this.arenaService = arenaService;
    this.locationService = locationService;
    this.arena = arena;
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
                arenaService.findByName(val).isEmpty()
                    || (arena != null && arena.getName().equals(val)),
            "Arena with this name already exists")
        .bind(Arena::getName, Arena::setName);
    binder.bind(description, Arena::getDescription, Arena::setDescription);
    binder
        .forField(locationCombo)
        .withValidator(val -> val != null, "Location cannot be empty")
        .bind(Arena::getLocation, Arena::setLocation);
    binder
        .forField(capacity)
        .withValidator(val -> val != null && val > 0, "Capacity must be a positive number")
        .bind(Arena::getCapacity, Arena::setCapacity);
    binder
        .forField(alignmentBias)
        .withValidator(val -> val != null, "Alignment Bias cannot be empty")
        .bind(Arena::getAlignmentBias, Arena::setAlignmentBias);
    binder.bind(imageUrl, Arena::getImageUrl, Arena::setImageUrl);
    binder
        .forField(environmentalTraits)
        .bind(
            ar -> String.join(", ", ar.getEnvironmentalTraits()),
            (ar, val) -> {
              Set<String> traits = new HashSet<>();
              if (val != null && !val.trim().isEmpty()) {
                Arrays.stream(val.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(traits::add);
              }
              ar.setEnvironmentalTraits(traits);
            });
  }

  private void initUI(ImageStorageService storageService) {
    setHeaderTitle(arena == null ? "Add New Arena" : "Edit Arena");
    setWidth("800px");

    name.setWidthFull();
    name.setMaxLength(Arena.DESCRIPTION_MAX_LENGTH);
    name.setValueChangeMode(ValueChangeMode.EAGER);
    name.addValueChangeListener(e -> binder.validate());

    description.setWidthFull();
    locationCombo.setWidthFull();
    locationCombo.setItems(locationService.findAll());
    locationCombo.setItemLabelGenerator(Location::getName);

    capacity.setWidthFull();
    capacity.setMin(0);
    capacity.setStepButtonsVisible(true);

    alignmentBias.setWidthFull();
    alignmentBias.setItems(AlignmentBias.values());
    alignmentBias.setItemLabelGenerator(AlignmentBias::getDisplayName);

    imageUrl.setWidthFull();
    imageUrl.setReadOnly(true);
    imageUrl.setPlaceholder("Enter URL or generate with AI");
    imageUrl.addValueChangeListener(e -> updateImagePreview(e.getValue()));

    environmentalTraits.setWidthFull();

    arenaImage.setHeight("150px");
    arenaImage.setWidth("150px");
    arenaImage.addClassNames(LumoUtility.BorderRadius.MEDIUM, LumoUtility.Margin.AUTO);
    arenaImage.getStyle().set("display", "block");
    updateImagePreview(arena != null ? arena.getImageUrl() : null);

    ImageUploadComponent imageUpload =
        new ImageUploadComponent(
            storageService,
            url -> {
              imageUrl.setValue(url);
            });
    imageUpload.setUploadButtonText("Upload Image");

    Button generateImageButton = new Button("Generate AI Image", e -> generateImage());
    generateImageButton.addThemeVariants(
        com.vaadin.flow.component.button.ButtonVariant.LUMO_CONTRAST);
    generateImageButton.setEnabled(
        arena != null && arena.getId() != null); // Can only generate for existing arena

    HorizontalLayout imageButtons = new HorizontalLayout(imageUpload, generateImageButton);
    imageButtons.setSpacing(true);

    VerticalLayout imageLayout = new VerticalLayout(arenaImage, imageUrl, imageButtons);
    imageLayout.setAlignItems(FlexComponent.Alignment.CENTER);
    imageLayout.setSpacing(true);
    imageLayout.setPadding(false);

    FormLayout formLayout = new FormLayout();
    formLayout.add(name, locationCombo, description, capacity, alignmentBias, environmentalTraits);
    formLayout.setColspan(name, 2);
    formLayout.setColspan(description, 2);
    formLayout.setColspan(environmentalTraits, 2);

    Button saveButton = new Button(arena == null ? "Add" : "Save", e -> saveArena());
    saveButton.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY);
    saveButton.setEnabled(false); // Disable initially
    binder.addStatusChangeListener(e -> saveButton.setEnabled(binder.isValid()));

    Button deleteButton = new Button("Delete", e -> deleteArena());
    deleteButton.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_ERROR);
    deleteButton.setVisible(arena != null && arena.getId() != null);

    Button cancelButton = new Button("Cancel", e -> close());

    HorizontalLayout buttons = new HorizontalLayout(saveButton, deleteButton, cancelButton);
    buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
    buttons.setWidthFull();

    VerticalLayout dialogLayout = new VerticalLayout(formLayout, imageLayout, buttons);
    dialogLayout.setPadding(true);
    dialogLayout.setSpacing(true);
    add(dialogLayout);
  }

  private void populateForm() {
    if (arena != null) {
      binder.readBean(arena);
    } else {
      arena = new Arena();
      binder.readBean(arena);
    }
  }

  private void saveArena() {
    if (binder.isValid()) {
      try {
        binder.writeBean(arena);
        if (arena.getId() == null) {
          arenaService.createArena(
              arena.getName(),
              arena.getDescription(),
              arena.getLocation().getId(),
              arena.getCapacity(),
              arena.getAlignmentBias(),
              arena.getEnvironmentalTraits());
        } else {
          arenaService.updateArena(
              arena.getId(),
              arena.getName(),
              arena.getDescription(),
              arena.getLocation().getId(),
              arena.getCapacity(),
              arena.getAlignmentBias(),
              arena.getImageUrl(),
              arena.getEnvironmentalTraits());
        }
        onSave.run();
        close();
      } catch (Exception e) {
        log.error("Error saving arena", e);
        Notification.show(
                "Error saving arena: " + e.getMessage(), 5000, Notification.Position.MIDDLE)
            .addThemeVariants(NotificationVariant.LUMO_ERROR);
      }
    }
  }

  private void deleteArena() {
    if (arena != null && arena.getId() != null) {
      try {
        arenaService.deleteArena(arena.getId());
        onSave.run();
        Notification.show("Arena deleted successfully!", 3000, Notification.Position.BOTTOM_START);
        close();
      } catch (Exception e) {
        log.error("Error deleting arena", e);
        Notification.show(
                "Error deleting arena: " + e.getMessage(), 5000, Notification.Position.MIDDLE)
            .addThemeVariants(NotificationVariant.LUMO_ERROR);
      }
    }
  }

  private void updateImagePreview(String url) {
    if (url != null && !url.trim().isEmpty()) {
      arenaImage.setSrc(url);
      arenaImage.setVisible(true);
    } else {
      arenaImage.setSrc("https://via.placeholder.com/150");
      arenaImage.setVisible(true);
    }
  }

  private void generateImage() {
    if (arena != null && arena.getId() != null) {
      try {
        arenaService
            .generateArenaImage(arena.getId())
            .ifPresentOrElse(
                generatedUrl -> {
                  imageUrl.setValue(generatedUrl);
                  updateImagePreview(generatedUrl);
                  Notification.show(
                      "AI Image generated!", 3000, Notification.Position.BOTTOM_START);
                },
                () -> {
                  Notification.show(
                          "Failed to generate AI Image.", 5000, Notification.Position.MIDDLE)
                      .addThemeVariants(NotificationVariant.LUMO_ERROR);
                });
      } catch (Exception e) {
        log.error("Error generating AI image for arena", e);
        Notification.show(
                "Error generating AI image: " + e.getMessage(), 5000, Notification.Position.MIDDLE)
            .addThemeVariants(NotificationVariant.LUMO_ERROR);
      }
    } else {
      Notification.show(
              "Save the arena first before generating an AI image.",
              3000,
              Notification.Position.MIDDLE)
          .addThemeVariants(NotificationVariant.LUMO_WARNING);
    }
  }
}
