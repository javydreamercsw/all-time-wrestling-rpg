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
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.npc.NpcRepository;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.world.ArenaRepository;
import com.github.javydreamercsw.management.domain.world.LocationRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Round-trip test: export produces a ZIP, import restores files to a fresh directory, verify files
 * are present and content matches.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ImageRoundTripTest {

  @TempDir Path sourceDir;
  @TempDir Path targetDir;

  @Mock private StorageProperties exportStorage;
  @Mock private StorageProperties importStorage;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private FactionRepository factionRepository;
  @Mock private TitleRepository titleRepository;
  @Mock private TeamRepository teamRepository;
  @Mock private NpcRepository npcRepository;
  @Mock private ShowTemplateRepository showTemplateRepository;
  @Mock private ArenaRepository arenaRepository;
  @Mock private LocationRepository locationRepository;

  private ImageExportService exportService;
  private ImageImportService importService;

  @BeforeEach
  void setUp() throws IOException {
    // Source dirs (export side)
    Path generatedSrc = sourceDir.resolve("images/generated");
    Path defaultsSrc = sourceDir.resolve("images/defaults");
    Files.createDirectories(generatedSrc);
    Files.createDirectories(defaultsSrc.resolve("wrestlers"));

    when(exportStorage.getResolvedImageDir()).thenReturn(generatedSrc);
    when(exportStorage.getResolvedDefaultImageDir()).thenReturn(defaultsSrc);

    // Target dirs (import side)
    Path generatedTgt = targetDir.resolve("images/generated");
    Path defaultsTgt = targetDir.resolve("images/defaults");
    when(importStorage.getResolvedImageDir()).thenReturn(generatedTgt);
    when(importStorage.getResolvedDefaultImageDir()).thenReturn(defaultsTgt);

    // All entity repos return empty except wrestler
    when(factionRepository.findAll()).thenReturn(List.of());
    when(titleRepository.findAll()).thenReturn(List.of());
    when(teamRepository.findAll()).thenReturn(List.of());
    when(npcRepository.findAll()).thenReturn(List.of());
    when(showTemplateRepository.findAll()).thenReturn(List.of());
    when(arenaRepository.findAll()).thenReturn(List.of());
    when(locationRepository.findAll()).thenReturn(List.of());

    exportService =
        new ImageExportService(
            exportStorage,
            wrestlerRepository,
            factionRepository,
            titleRepository,
            teamRepository,
            npcRepository,
            showTemplateRepository,
            arenaRepository,
            locationRepository);

    importService = new ImageImportService(importStorage);
  }

  @Test
  void roundTrip_generatedImages_restoredWithCorrectContent() throws IOException {
    // Arrange: seed a generated image referenced by a wrestler entity
    Path generatedSrc = sourceDir.resolve("images/generated");
    byte[] imageBytes = new byte[] {1, 2, 3, 4, 5};
    Files.write(generatedSrc.resolve("abc123.png"), imageBytes);

    Wrestler w = new Wrestler();
    w.setImageUrl(ImageExportService.GENERATED_PREFIX + "abc123.png");
    when(wrestlerRepository.findAll()).thenReturn(List.of(w));

    // Act: export then import
    byte[] zip = exportService.exportImages();
    ImageImportService.ImportSummary summary =
        importService.importImages(new ByteArrayInputStream(zip));

    // Assert
    assertThat(summary.written()).isEqualTo(1);
    assertThat(summary.errors()).isEqualTo(0);
    Path restored = targetDir.resolve("images/generated/abc123.png");
    assertThat(restored).exists();
    assertThat(Files.readAllBytes(restored)).isEqualTo(imageBytes);
  }

  @Test
  void roundTrip_defaultImages_restoredUnderSubdir() throws IOException {
    // Arrange: seed a custom profile image under images/defaults/wrestlers/
    when(wrestlerRepository.findAll()).thenReturn(List.of());
    Path defaultsSrc = sourceDir.resolve("images/defaults");
    byte[] imageBytes = new byte[] {9, 8, 7, 6};
    Files.write(defaultsSrc.resolve("wrestlers/John Doe.png"), imageBytes);

    // Act
    byte[] zip = exportService.exportImages();
    ImageImportService.ImportSummary summary =
        importService.importImages(new ByteArrayInputStream(zip));

    // Assert
    assertThat(summary.written()).isEqualTo(1);
    Path restored = targetDir.resolve("images/defaults/wrestlers/John Doe.png");
    assertThat(restored).exists();
    assertThat(Files.readAllBytes(restored)).isEqualTo(imageBytes);
  }

  @Test
  void roundTrip_mixedImages_allRestored() throws IOException {
    // Arrange: one generated + one default
    Path generatedSrc = sourceDir.resolve("images/generated");
    Path defaultsSrc = sourceDir.resolve("images/defaults");

    byte[] gen = new byte[] {1, 2};
    byte[] def = new byte[] {3, 4};
    Files.write(generatedSrc.resolve("img1.png"), gen);
    Files.write(defaultsSrc.resolve("wrestlers/Custom.png"), def);

    Wrestler w = new Wrestler();
    w.setImageUrl(ImageExportService.GENERATED_PREFIX + "img1.png");
    when(wrestlerRepository.findAll()).thenReturn(List.of(w));

    // Act
    byte[] zip = exportService.exportImages();
    ImageImportService.ImportSummary summary =
        importService.importImages(new ByteArrayInputStream(zip));

    // Assert
    assertThat(summary.written()).isEqualTo(2);
    assertThat(targetDir.resolve("images/generated/img1.png")).exists();
    assertThat(targetDir.resolve("images/defaults/wrestlers/Custom.png")).exists();
  }

  @Test
  void roundTrip_missingFileOnDisk_skippedDuringExport() throws IOException {
    // Arrange: DB references an image that doesn't exist on disk
    when(wrestlerRepository.findAll()).thenReturn(List.of());
    Wrestler w = new Wrestler();
    w.setImageUrl(ImageExportService.GENERATED_PREFIX + "ghost.png");
    when(wrestlerRepository.findAll()).thenReturn(List.of(w));

    // Act
    byte[] zip = exportService.exportImages();
    ImageImportService.ImportSummary summary =
        importService.importImages(new ByteArrayInputStream(zip));

    // Assert: ZIP is empty, import has nothing to write
    assertThat(summary.written()).isEqualTo(0);
    assertThat(targetDir.resolve("images/generated/ghost.png")).doesNotExist();
  }
}
