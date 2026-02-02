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
package com.github.javydreamercsw.base.ai.image;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Service for storing generated images locally. */
@Service
@Slf4j
public class ImageStorageService {

  private static final String IMAGE_DIR = "src/main/resources/META-INF/resources/images/generated";
  private static final String PUBLIC_PATH = "images/generated/";

  /**
   * Saves an image from base64 data or a URL.
   *
   * @param imageData The image data (base64 string or URL).
   * @param isBase64 True if the data is base64 encoded, false if it is a URL.
   * @return The public path to the saved image (e.g., "images/generated/uuid.png").
   * @throws IOException If saving fails.
   */
  public String saveImage(String imageData, boolean isBase64) throws IOException {
    Path directory = Paths.get(IMAGE_DIR);
    if (!Files.exists(directory)) {
      Files.createDirectories(directory);
    }

    String filename = UUID.randomUUID() + ".png";
    Path filePath = directory.resolve(filename);

    if (isBase64) {
      byte[] imageBytes = Base64.getDecoder().decode(imageData);
      Files.write(filePath, imageBytes);
    } else {
      downloadImage(imageData, filePath);
    }

    log.info("Saved generated image to: {}", filePath);
    return PUBLIC_PATH + filename;
  }

  private void downloadImage(String url, Path destination) throws IOException {
    try {
      HttpClient client = HttpClient.newHttpClient();
      HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();

      HttpResponse<InputStream> response =
          client.send(request, HttpResponse.BodyHandlers.ofInputStream());

      if (response.statusCode() == 200) {
        try (InputStream is = response.body()) {
          Files.copy(is, destination);
        }
      } else {
        throw new IOException(
            "Failed to download image from URL: "
                + url
                + " (Status: "
                + response.statusCode()
                + ")");
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Download interrupted", e);
    }
  }
}
