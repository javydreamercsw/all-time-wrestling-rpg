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
package com.github.javydreamercsw.base.ai.image.ui;

import com.github.javydreamercsw.base.ai.image.ImageGenerationService;
import com.github.javydreamercsw.base.ai.image.ImageGenerationServiceFactory;
import com.github.javydreamercsw.base.ai.image.ImageStorageService;
import com.github.javydreamercsw.base.ai.service.AiSettingsService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GenericImageGenerationDialog extends Dialog {

  private final Supplier<String> promptSupplier;
  private final Consumer<String> imageSaver;
  private final ImageGenerationServiceFactory imageFactory;
  private final ImageStorageService storageService;
  private final AiSettingsService aiSettingsService;
  private final Runnable onSave;

  private final TextArea promptArea;
  private final TextField modelField;
  private final Image previewImage;
  private final Button saveButton;
  private String currentImageData;
  private boolean isBase64;

  public GenericImageGenerationDialog(
      Supplier<String> promptSupplier,
      Consumer<String> imageSaver,
      ImageGenerationServiceFactory imageFactory,
      ImageStorageService storageService,
      AiSettingsService aiSettingsService,
      Runnable onSave) {
    this.promptSupplier = promptSupplier;
    this.imageSaver = imageSaver;
    this.imageFactory = imageFactory;
    this.storageService = storageService;
    this.aiSettingsService = aiSettingsService;
    this.onSave = onSave;

    setHeaderTitle("Generate Image");

    VerticalLayout layout = new VerticalLayout();
    layout.setPadding(false);
    layout.setSpacing(true);
    layout.setWidth("500px");

    promptArea = new TextArea("Prompt");
    promptArea.setWidthFull();
    promptArea.setMinHeight("100px");
    promptArea.setValue(this.promptSupplier.get());

    modelField = new TextField("Model");
    modelField.setWidthFull();
    modelField.setPlaceholder("Leave empty to use default");

    // Pre-fill model based on available service
    ImageGenerationService service = imageFactory.getBestAvailableService();
    if (service != null) {
      if ("OpenAI".equals(service.getProviderName())) {
        modelField.setValue(aiSettingsService.getOpenAIImageModel());
      }
    }

    previewImage = new Image();
    previewImage.setMaxWidth("100%");
    previewImage.setVisible(false);

    Button generateButton = new Button("Generate", e -> generateImage());
    generateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    generateButton.setId("generate-image");

    saveButton = new Button("Save & Apply", e -> saveImage());
    saveButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
    saveButton.setEnabled(false);
    saveButton.setId("save-image");

    Button cancelButton = new Button("Cancel", e -> close());

    layout.add(promptArea, modelField, generateButton, previewImage);
    add(layout);
    getFooter().add(cancelButton, saveButton);
  }

  private void generateImage() {
    ImageGenerationService service = imageFactory.getBestAvailableService();
    if (service == null) {
      Notification.show("No image generation service available.")
          .addThemeVariants(NotificationVariant.LUMO_ERROR);
      return;
    }

    try {
      Notification.show("Generating image... This may take a moment.");

      // Determine format based on provider?
      // LocalAI often works well with URLs if accessible, or b64_json.
      // Let's default to URL for now, but handle b64 if needed.
      // OpenAI returns URLs.
      String format = "url";
      if (service.getProviderName().equals("Mock AI")) {
        format = "b64_json"; // Test base64 path
      }

      String model = modelField.getValue();
      if (model != null && model.trim().isEmpty()) {
        model = null;
      }

      ImageGenerationService.ImageRequest request =
          ImageGenerationService.ImageRequest.builder()
              .prompt(promptArea.getValue())
              .responseFormat(format)
              .model(model)
              .build();

      currentImageData = service.generateImage(request);
      isBase64 = !"url".equals(format);

      if (isBase64) {
        previewImage.setSrc("data:image/png;base64," + currentImageData);
      } else {
        previewImage.setSrc(currentImageData);
      }
      previewImage.setVisible(true);
      saveButton.setEnabled(true);

    } catch (Exception e) {
      log.error("Failed to generate image", e);
      Notification.show("Generation failed: " + e.getMessage())
          .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
  }

  private void saveImage() {
    if (currentImageData == null) return;

    try {
      String savedPath = storageService.saveImage(currentImageData, isBase64);
      imageSaver.accept(savedPath);

      Notification.show("Image saved successfully!")
          .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

      if (onSave != null) {
        onSave.run();
      }
      close();
    } catch (IOException e) {
      log.error("Failed to save image", e);
      Notification.show("Failed to save image: " + e.getMessage())
          .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
  }
}
