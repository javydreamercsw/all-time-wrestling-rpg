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

import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.npc.NpcRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@Transactional
public class ImageCleanupService {

  private static final String DEFAULT_IMAGE_DIR =
      "src/main/resources/META-INF/resources/images/generated";
  private static final String PUBLIC_PATH_PREFIX = "images/generated/";

  private final WrestlerRepository wrestlerRepository;
  private final NpcRepository npcRepository;
  private final String imageDir;

  @Autowired
  public ImageCleanupService(WrestlerRepository wrestlerRepository, NpcRepository npcRepository) {
    this(wrestlerRepository, npcRepository, DEFAULT_IMAGE_DIR);
  }

  public ImageCleanupService(
      WrestlerRepository wrestlerRepository, NpcRepository npcRepository, String imageDir) {
    this.wrestlerRepository = wrestlerRepository;
    this.npcRepository = npcRepository;
    this.imageDir = imageDir;
  }

  /**
   * Identifies and deletes generated images that are no longer referenced by any Wrestler or NPC.
   *
   * @return The number of deleted images.
   * @throws IOException If file operations fail.
   */
  @PreAuthorize("hasRole('ADMIN')")
  public int cleanupUnusedImages() throws IOException {
    Path directory = Paths.get(imageDir);
    if (!Files.exists(directory)) {
      log.info("Image directory does not exist: {}", imageDir);
      return 0;
    }

    // 1. Get all referenced image paths from DB
    Set<String> referencedImages = new HashSet<>();

    List<Wrestler> wrestlers = wrestlerRepository.findAll();
    for (Wrestler w : wrestlers) {
      if (w.getImageUrl() != null && w.getImageUrl().startsWith(PUBLIC_PATH_PREFIX)) {
        referencedImages.add(w.getImageUrl().substring(PUBLIC_PATH_PREFIX.length()));
      }
    }

    List<Npc> npcs = npcRepository.findAll();
    for (Npc n : npcs) {
      if (n.getImageUrl() != null && n.getImageUrl().startsWith(PUBLIC_PATH_PREFIX)) {
        referencedImages.add(n.getImageUrl().substring(PUBLIC_PATH_PREFIX.length()));
      }
    }

    // 2. List all files in the directory
    int deletedCount = 0;
    try (Stream<Path> files = Files.list(directory)) {
      List<Path> filesToDelete =
          files
              .filter(Files::isRegularFile)
              .filter(path -> !referencedImages.contains(path.getFileName().toString()))
              .collect(Collectors.toList());

      for (Path path : filesToDelete) {
        log.info("Deleting unused generated image: {}", path);
        Files.delete(path);
        deletedCount++;
      }
    }

    log.info("Cleanup complete. Deleted {} unused images.", deletedCount);
    return deletedCount;
  }
}
