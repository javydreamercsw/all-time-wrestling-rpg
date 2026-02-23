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
package com.github.javydreamercsw.management.ui.view.npc;

import com.github.javydreamercsw.base.ai.image.ImageStorageService;
import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.ui.component.ImageUploadComponent;
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.npc.NpcType;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NpcFormDialog extends Dialog {

  private final NpcService npcService;
  private final Npc npc;
  private final Runnable onSave;
  private final Binder<Npc> binder = new Binder<>(Npc.class);

  private final TextField nameField = new TextField("Name");
  private final ComboBox<String> npcTypeField = new ComboBox<>("Type");
  private final ComboBox<Gender> genderField = new ComboBox<>("Gender");
  private final ComboBox<AlignmentType> alignmentField = new ComboBox<>("Alignment");
  private final NumberField awarenessField = new NumberField("Referee Awareness");
  private final TextArea descriptionField = new TextArea("Biography");
  private final TextField imageUrlField = new TextField("Image URL");

  public NpcFormDialog(
      NpcService npcService, ImageStorageService storageService, Npc npc, Runnable onSave) {
    this.npcService = npcService;
    this.npc = npc;
    this.onSave = onSave;

    setHeaderTitle(npc.getId() == null ? "Create NPC" : "Edit NPC");

    FormLayout formLayout = new FormLayout();

    npcTypeField.setItems(Stream.of(NpcType.values()).map(NpcType::getName).toList());
    genderField.setItems(Gender.values());
    alignmentField.setItems(AlignmentType.values());

    awarenessField.setMin(0);
    awarenessField.setMax(100);
    awarenessField.setStepButtonsVisible(true);
    awarenessField.setVisible("Referee".equalsIgnoreCase(npc.getNpcType()));

    npcTypeField.addValueChangeListener(
        e -> {
          awarenessField.setVisible("Referee".equalsIgnoreCase(e.getValue()));
        });

    descriptionField.setWidthFull();
    descriptionField.setMinHeight("150px");

    imageUrlField.setReadOnly(true);
    ImageUploadComponent imageUpload =
        new ImageUploadComponent(
            storageService,
            url -> {
              imageUrlField.setValue(url);
            });
    imageUpload.setUploadButtonText("Upload Image");

    formLayout.add(
        nameField,
        npcTypeField,
        genderField,
        alignmentField,
        awarenessField,
        descriptionField,
        imageUrlField,
        imageUpload);
    formLayout.setColspan(descriptionField, 2);
    formLayout.setColspan(imageUrlField, 2);

    add(formLayout);

    // Binding
    binder.forField(nameField).asRequired("Name is required").bind(Npc::getName, Npc::setName);
    binder
        .forField(npcTypeField)
        .asRequired("Type is required")
        .bind(Npc::getNpcType, Npc::setNpcType);
    binder.forField(genderField).bind(Npc::getGender, Npc::setGender);
    binder.forField(alignmentField).bind(Npc::getAlignment, Npc::setAlignment);
    binder.forField(descriptionField).bind(Npc::getDescription, Npc::setDescription);
    binder.forField(imageUrlField).bind(Npc::getImageUrl, Npc::setImageUrl);

    binder
        .forField(awarenessField)
        .bind(
            n -> (double) npcService.getAwareness(n),
            (n, val) -> {
              if (val != null) {
                npcService.setAwareness(n, val.intValue());
              }
            });

    binder.setBean(npc);

    Button saveButton = new Button("Save", e -> save());
    saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    Button cancelButton = new Button("Cancel", e -> close());

    getFooter().add(cancelButton, saveButton);
  }

  private void save() {
    if (binder.validate().isOk()) {
      npcService.save(npc);
      Notification.show("NPC saved!").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
      if (onSave != null) {
        onSave.run();
      }
      close();
    }
  }
}
