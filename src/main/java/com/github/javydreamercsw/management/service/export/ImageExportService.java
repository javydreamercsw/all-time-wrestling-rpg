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

import com.github.javydreamercsw.base.config.StorageProperties;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.npc.NpcRepository;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.world.ArenaRepository;
import com.github.javydreamercsw.management.domain.world.LocationRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Collects all custom images referenced by entities and packages them into a single ZIP for
 * download. The ZIP preserves relative paths (e.g. {@code images/generated/uuid.png}) so that the
 * companion import can restore files to the correct location.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ImageExportService {

  static final String IMAGE_PATH_PREFIX = "images/generated/";

  private final StorageProperties storageProperties;
  private final WrestlerRepository wrestlerRepository;
  private final FactionRepository factionRepository;
  private final TitleRepository titleRepository;
  private final TeamRepository teamRepository;
  private final NpcRepository npcRepository;
  private final ShowTemplateRepository showTemplateRepository;
  private final ArenaRepository arenaRepository;
  private final LocationRepository locationRepository;

  /**
   * Builds a ZIP containing every generated image referenced by any entity. Files missing from disk
   * are skipped with a warning rather than failing the whole export.
   *
   * @return the ZIP bytes, never null (may be an empty ZIP if no images are found)
   */
  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  @Transactional(readOnly = true)
  public byte[] exportImages() throws IOException {
    Set<String> urls = collectImageUrls();
    Path imageDir = storageProperties.getResolvedImageDir();

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
      for (String url : urls) {
        String filename = url.substring(IMAGE_PATH_PREFIX.length());
        Path file = imageDir.resolve(filename);
        if (!Files.exists(file)) {
          log.warn("Referenced image not found on disk, skipping: {}", file);
          continue;
        }
        zip.putNextEntry(new ZipEntry(url));
        Files.copy(file, zip);
        zip.closeEntry();
        log.debug("Added image to export ZIP: {}", url);
      }
    }

    log.info("Image export complete: {} referenced URLs, {} bytes", urls.size(), out.size());
    return out.toByteArray();
  }

  private Set<String> collectImageUrls() {
    Set<String> urls = new LinkedHashSet<>();
    addUrls(urls, wrestlerRepository.findAll().stream().map(e -> e.getImageUrl()).toList());
    addUrls(urls, factionRepository.findAll().stream().map(e -> e.getImageUrl()).toList());
    addUrls(urls, titleRepository.findAll().stream().map(e -> e.getImageUrl()).toList());
    addUrls(urls, teamRepository.findAll().stream().map(e -> e.getImageUrl()).toList());
    addUrls(urls, npcRepository.findAll().stream().map(e -> e.getImageUrl()).toList());
    addUrls(urls, showTemplateRepository.findAll().stream().map(e -> e.getImageUrl()).toList());
    addUrls(urls, arenaRepository.findAll().stream().map(e -> e.getImageUrl()).toList());
    addUrls(urls, locationRepository.findAll().stream().map(e -> e.getImageUrl()).toList());
    return urls;
  }

  private void addUrls(final Set<String> target, final List<String> candidates) {
    for (String url : candidates) {
      if (url != null && url.startsWith(IMAGE_PATH_PREFIX)) {
        target.add(url);
      }
    }
  }
}
