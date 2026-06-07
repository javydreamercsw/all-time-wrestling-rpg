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
import com.github.javydreamercsw.management.domain.faction.Faction;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ImageExportServiceTest {

  @TempDir Path tempDir;

  @Mock private StorageProperties storageProperties;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private FactionRepository factionRepository;
  @Mock private TitleRepository titleRepository;
  @Mock private TeamRepository teamRepository;
  @Mock private NpcRepository npcRepository;
  @Mock private ShowTemplateRepository showTemplateRepository;
  @Mock private ArenaRepository arenaRepository;
  @Mock private LocationRepository locationRepository;

  private ImageExportService service;

  @BeforeEach
  void setUp() {
    service =
        new ImageExportService(
            storageProperties,
            wrestlerRepository,
            factionRepository,
            titleRepository,
            teamRepository,
            npcRepository,
            showTemplateRepository,
            arenaRepository,
            locationRepository);
    when(storageProperties.getResolvedImageDir()).thenReturn(tempDir);
    // Default: all other repos return empty
    when(factionRepository.findAll()).thenReturn(List.of());
    when(titleRepository.findAll()).thenReturn(List.of());
    when(teamRepository.findAll()).thenReturn(List.of());
    when(npcRepository.findAll()).thenReturn(List.of());
    when(showTemplateRepository.findAll()).thenReturn(List.of());
    when(arenaRepository.findAll()).thenReturn(List.of());
    when(locationRepository.findAll()).thenReturn(List.of());
  }

  @Test
  void exportImages_noEntities_returnsEmptyZip() throws IOException {
    when(wrestlerRepository.findAll()).thenReturn(List.of());

    byte[] zip = service.exportImages();

    assertThat(zip).isNotNull();
    assertThat(zipEntryNames(zip)).isEmpty();
  }

  @Test
  void exportImages_wrestlerWithImage_includesFileInZip() throws IOException {
    Path imgFile = tempDir.resolve("uuid.png");
    Files.write(imgFile, new byte[] {1, 2, 3});

    Wrestler w = new Wrestler();
    w.setImageUrl(ImageExportService.IMAGE_PATH_PREFIX + "uuid.png");
    when(wrestlerRepository.findAll()).thenReturn(List.of(w));

    byte[] zip = service.exportImages();

    assertThat(zipEntryNames(zip))
        .containsExactly(ImageExportService.IMAGE_PATH_PREFIX + "uuid.png");
  }

  @Test
  void exportImages_missingFileOnDisk_skipsWithoutError() throws IOException {
    Wrestler w = new Wrestler();
    w.setImageUrl(ImageExportService.IMAGE_PATH_PREFIX + "missing.png");
    when(wrestlerRepository.findAll()).thenReturn(List.of(w));

    byte[] zip = service.exportImages();

    assertThat(zipEntryNames(zip)).isEmpty();
  }

  @Test
  void exportImages_deduplicatesSharedUrls() throws IOException {
    Path imgFile = tempDir.resolve("shared.png");
    Files.write(imgFile, new byte[] {9});

    String url = ImageExportService.IMAGE_PATH_PREFIX + "shared.png";
    Wrestler w1 = new Wrestler();
    w1.setImageUrl(url);
    Wrestler w2 = new Wrestler();
    w2.setImageUrl(url);
    when(wrestlerRepository.findAll()).thenReturn(List.of(w1, w2));

    byte[] zip = service.exportImages();

    assertThat(zipEntryNames(zip)).containsExactly(url);
  }

  @Test
  void exportImages_nullAndNonPrefixedUrls_ignored() throws IOException {
    Wrestler noUrl = new Wrestler();
    Wrestler externalUrl = new Wrestler();
    externalUrl.setImageUrl("https://cdn.example.com/img.png");
    when(wrestlerRepository.findAll()).thenReturn(List.of(noUrl, externalUrl));

    byte[] zip = service.exportImages();

    assertThat(zipEntryNames(zip)).isEmpty();
  }

  @Test
  void exportImages_allEntityTypes_collected() throws IOException {
    Path img = tempDir.resolve("x.png");
    Files.write(img, new byte[] {7});
    String url = ImageExportService.IMAGE_PATH_PREFIX + "x.png";

    when(wrestlerRepository.findAll()).thenReturn(List.of());
    Faction f = new Faction();
    f.setImageUrl(url);
    when(factionRepository.findAll()).thenReturn(List.of(f));

    byte[] zip = service.exportImages();
    assertThat(zipEntryNames(zip)).containsExactly(url);
  }

  private List<String> zipEntryNames(final byte[] zipBytes) throws IOException {
    List<String> names = new java.util.ArrayList<>();
    try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        names.add(entry.getName());
        zis.closeEntry();
      }
    }
    return names;
  }
}
