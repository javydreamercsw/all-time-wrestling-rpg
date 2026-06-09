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
package com.github.javydreamercsw.management.service.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.config.StorageProperties;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ImageImportServiceTest {

  @TempDir Path tempDir;

  @Mock private StorageProperties storageProperties;

  private ImageImportService service;

  @BeforeEach
  void setUp() {
    service = new ImageImportService(storageProperties);
    when(storageProperties.getResolvedImageDir()).thenReturn(tempDir.resolve("images/generated"));
    when(storageProperties.getResolvedDefaultImageDir())
        .thenReturn(tempDir.resolve("images/defaults"));
  }

  @Test
  void importImages_generatedImage_writtenToImageDir() throws IOException {
    byte[] zip = buildZip(ImageImportService.GENERATED_PREFIX + "uuid.png", new byte[] {1, 2, 3});

    ImageImportService.ImportSummary summary = service.importImages(new ByteArrayInputStream(zip));

    assertThat(summary.written()).isEqualTo(1);
    assertThat(summary.skipped()).isEqualTo(0);
    assertThat(summary.errors()).isEqualTo(0);
    Path written = tempDir.resolve("images/generated/uuid.png");
    assertThat(written).exists();
    assertThat(Files.readAllBytes(written)).isEqualTo(new byte[] {1, 2, 3});
  }

  @Test
  void importImages_defaultsImage_writtenToDefaultsSubdir() throws IOException {
    byte[] zip =
        buildZip(ImageImportService.DEFAULTS_PREFIX + "wrestlers/John.png", new byte[] {9, 8, 7});

    ImageImportService.ImportSummary summary = service.importImages(new ByteArrayInputStream(zip));

    assertThat(summary.written()).isEqualTo(1);
    assertThat(tempDir.resolve("images/defaults/wrestlers/John.png")).exists();
  }

  @Test
  void importImages_outsideAllowedPath_skipped() throws IOException {
    byte[] zip = buildZip("etc/passwd", new byte[] {0});

    ImageImportService.ImportSummary summary = service.importImages(new ByteArrayInputStream(zip));

    assertThat(summary.written()).isEqualTo(0);
    assertThat(summary.skipped()).isEqualTo(1);
  }

  @Test
  void importImages_pathTraversalAttempt_skipped() throws IOException {
    byte[] zip = buildZip("images/generated/../../../etc/passwd", new byte[] {0});

    ImageImportService.ImportSummary summary = service.importImages(new ByteArrayInputStream(zip));

    assertThat(summary.written()).isEqualTo(0);
    assertThat(summary.skipped()).isEqualTo(1);
  }

  @Test
  void importImages_subdirectoryUnderGenerated_skipped() throws IOException {
    byte[] zip = buildZip(ImageImportService.GENERATED_PREFIX + "sub/uuid.png", new byte[] {1});

    ImageImportService.ImportSummary summary = service.importImages(new ByteArrayInputStream(zip));

    assertThat(summary.written()).isEqualTo(0);
    assertThat(summary.skipped()).isEqualTo(1);
  }

  @Test
  void importImages_deepSubdirectoryUnderDefaults_skipped() throws IOException {
    byte[] zip = buildZip(ImageImportService.DEFAULTS_PREFIX + "a/b/c.png", new byte[] {1});

    ImageImportService.ImportSummary summary = service.importImages(new ByteArrayInputStream(zip));

    assertThat(summary.written()).isEqualTo(0);
    assertThat(summary.skipped()).isEqualTo(1);
  }

  @Test
  void importImages_replaceExistingFile() throws IOException {
    byte[] zip = buildZip(ImageImportService.GENERATED_PREFIX + "uuid.png", new byte[] {1, 2, 3});
    service.importImages(new ByteArrayInputStream(zip));

    byte[] zip2 = buildZip(ImageImportService.GENERATED_PREFIX + "uuid.png", new byte[] {9, 9, 9});
    service.importImages(new ByteArrayInputStream(zip2));

    assertThat(Files.readAllBytes(tempDir.resolve("images/generated/uuid.png")))
        .isEqualTo(new byte[] {9, 9, 9});
  }

  @Test
  void importSummary_toMessage_formatsCorrectly() {
    assertThat(new ImageImportService.ImportSummary(5, 2, 1).toMessage())
        .isEqualTo("Image restore complete: 5 restored, 2 skipped, 1 errors");
  }

  private byte[] buildZip(final String entryName, final byte[] content) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(out)) {
      zip.putNextEntry(new ZipEntry(entryName));
      zip.write(content);
      zip.closeEntry();
    }
    return out.toByteArray();
  }
}
