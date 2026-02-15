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
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/** Service for storing generated images locally. */
@Service
@Slf4j
public class ImageStorageService {

  private final String imageDir;
  private final Environment environment;

  private static final String PUBLIC_PATH = "images/generated/";

  @Autowired
  public ImageStorageService(
      @Value("${image.storage.directory:src/main/resources/META-INF/resources/images/generated}")
          String imageDir,
      Environment environment) {
    this.imageDir = imageDir;
    this.environment = environment;
  }

  /**
   * Saves an image from base64 data or a URL.
   *
   * @param imageData The image data (base64 string or URL).
   * @param isBase64 True if the data is base64 encoded, false if it is a URL.
   * @return The public path to the saved image (e.g., "images/generated/uuid.png").
   * @throws IOException If saving fails.
   */
  public String saveImage(String imageData, boolean isBase64) throws IOException {
    Path sourceDirectory = Paths.get(imageDir);
    if (!Files.exists(sourceDirectory)) {
      Files.createDirectories(sourceDirectory);
    }

    String filename = UUID.randomUUID() + ".png";
    Path sourceFilePath = sourceDirectory.resolve(filename);

    byte[] imageBytes = null;
    if (isBase64) {
      imageBytes = Base64.getDecoder().decode(imageData);
      Files.write(sourceFilePath, imageBytes);
    } else {
      downloadImage(imageData, sourceFilePath);
      // Read back bytes if we need them for the target copy and didn't have them in memory
      imageBytes = Files.readAllBytes(sourceFilePath);
    }

    log.info("Saved generated image to: {}", sourceFilePath);

    // Also write to target directory if we are in a development environment
    // This makes images available immediately without a restart or re-sync
    if (!isProduction()) {
      String targetDir = imageDir.replace("src/main/resources", "target/classes");
      Path targetPath = Paths.get(targetDir);
      if (Files.exists(targetPath.getParent())) {
        if (!Files.exists(targetPath)) {
          Files.createDirectories(targetPath);
        }
        Path targetFilePath = targetPath.resolve(filename);
        Files.write(targetFilePath, imageBytes);
        log.info("Saved generated image to dev target: {}", targetFilePath);
      }
    }

    return PUBLIC_PATH + filename;
  }

  private boolean isProduction() {
    return environment.acceptsProfiles(org.springframework.core.env.Profiles.of("prod"));
  }

  private void downloadImage(String url, Path destination) throws IOException {
    if (url == null || url.isBlank()) {
      throw new IOException("Image URL must not be null or blank");
    }

    // Pollinations (when using an API key) returns a Data URI (data:<mime>;base64,<payload>).
    // Java's HttpClient doesn't support the "data" scheme, so we decode it locally.
    if (url.startsWith("data:")) {
      Files.write(destination, decodeDataUriToBytes(url));
      return;
    }

    URI uri;
    try {
      uri = URI.create(url);
    } catch (IllegalArgumentException e) {
      throw new IOException("Invalid image URL: " + url, e);
    }

    String scheme = uri.getScheme();
    if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
      throw new IOException("Unsupported image URI scheme: " + scheme);
    }

    try {
      // HttpClient is long-lived and doesn't need to be closed.
      @SuppressWarnings("resource")
      HttpClient client = HttpClient.newHttpClient();
      HttpRequest request = HttpRequest.newBuilder().uri(uri).build();

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

  /**
   * Decodes a base64-encoded Data URI string and returns the byte array.
   *
   * <p>Format: {@code data:[<mediatype>][;base64],<data>}
   */
  public static byte[] decodeDataUriToBytes(String dataUri) throws IOException {
    if (dataUri == null || !dataUri.startsWith("data:")) {
      throw new IOException("Invalid data URI: must start with 'data:'");
    }

    int commaIndex = dataUri.indexOf(',');
    if (commaIndex < 0) {
      throw new IOException("Invalid data URI: missing ',' separator");
    }

    String header = dataUri.substring(5, commaIndex); // after "data:"
    String payload = dataUri.substring(commaIndex + 1);

    if (!header.contains(";base64")) {
      throw new IOException("Unsupported data URI encoding (expected base64)");
    }

    // Some encoders may URL-encode '+' as '%2B' etc. Try base64 first, then URL-decode and retry.
    try {
      return Base64.getDecoder().decode(payload);
    } catch (IllegalArgumentException e) {
      try {
        String decodedPayload = URLDecoder.decode(payload, StandardCharsets.UTF_8);
        return Base64.getDecoder().decode(decodedPayload);
      } catch (IllegalArgumentException e2) {
        e2.addSuppressed(e);
        throw new IOException("Invalid base64 payload in data URI", e2);
      }
    }
  }
}
