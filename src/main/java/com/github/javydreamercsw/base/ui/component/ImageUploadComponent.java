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
package com.github.javydreamercsw.base.ui.component;

import com.github.javydreamercsw.base.ai.image.ImageStorageService;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.server.streams.UploadHandler;
import java.io.IOException;
import java.util.Base64;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

/** Reusable component for uploading images and saving them via ImageStorageService. */
@Slf4j
public class ImageUploadComponent extends Composite<Div> {

  private final Upload upload;

  public ImageUploadComponent(
      ImageStorageService imageStorageService, Consumer<String> onImageSaved) {

    this.upload = new Upload();
    this.upload.setUploadHandler(
        UploadHandler.inMemory(
            (metadata, bytes) -> {
              try {
                String base64Data = Base64.getEncoder().encodeToString(bytes);
                String savedUrl = imageStorageService.saveImage(base64Data, true);

                onImageSaved.accept(savedUrl);

                Notification.show(
                        "Image uploaded successfully", 3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

                this.upload.clearFileList();
              } catch (IOException e) {
                log.error("Failed to save uploaded image", e);
                Notification.show(
                        "Failed to upload image: " + e.getMessage(),
                        5000,
                        Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
              }
            }));

    upload.setAcceptedFileTypes("image/png", "image/jpeg", "image/gif");
    upload.setMaxFiles(1);
    // Set max file size to 5MB
    upload.setMaxFileSize(5 * 1024 * 1024);

    upload.addFileRejectedListener(
        event -> {
          Notification.show(event.getErrorMessage(), 5000, Notification.Position.BOTTOM_END)
              .addThemeVariants(NotificationVariant.LUMO_ERROR);
        });

    getContent().add(upload);
  }

  /**
   * Sets the text displayed on the upload button.
   *
   * @param text The button text.
   */
  public void setUploadButtonText(String text) {
    upload.setUploadButton(new Button(text));
  }
}
