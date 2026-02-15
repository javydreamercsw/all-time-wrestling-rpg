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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.npc.NpcRepository;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ImageCleanupServiceTest {

  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private NpcRepository npcRepository;
  @Mock private ShowTemplateRepository showTemplateRepository;

  private ImageCleanupService imageCleanupService;

  @TempDir Path tempDir;

  @BeforeEach
  void setUp() throws IOException {
    imageCleanupService =
        new ImageCleanupService(
            wrestlerRepository, npcRepository, showTemplateRepository, tempDir.toString());
  }

  @Test
  void testCleanupUnusedImages() throws IOException {
    // Create some test files
    String referenced1 = UUID.randomUUID() + ".png";
    String referenced2 = UUID.randomUUID() + ".png";
    String referenced3 = UUID.randomUUID() + ".png";
    String unused = UUID.randomUUID() + ".png";

    Files.createFile(tempDir.resolve(referenced1));
    Files.createFile(tempDir.resolve(referenced2));
    Files.createFile(tempDir.resolve(referenced3));
    Files.createFile(tempDir.resolve(unused));

    List<Wrestler> wrestlers = new ArrayList<>();
    Wrestler w = new Wrestler();
    w.setImageUrl("images/generated/" + referenced1);
    wrestlers.add(w);
    when(wrestlerRepository.findAll()).thenReturn(wrestlers);

    List<Npc> npcs = new ArrayList<>();
    Npc n = new Npc();
    n.setImageUrl("images/generated/" + referenced2);
    npcs.add(n);
    when(npcRepository.findAll()).thenReturn(npcs);

    List<ShowTemplate> showTemplates = new ArrayList<>();
    ShowTemplate st = new ShowTemplate();
    st.setImageUrl("images/generated/" + referenced3);
    showTemplates.add(st);
    when(showTemplateRepository.findAll()).thenReturn(showTemplates);

    int deletedCount = imageCleanupService.cleanupUnusedImages();

    assertEquals(1, deletedCount);
    assertEquals(true, Files.exists(tempDir.resolve(referenced1)));
    assertEquals(true, Files.exists(tempDir.resolve(referenced2)));
    assertEquals(true, Files.exists(tempDir.resolve(referenced3)));
    assertEquals(false, Files.exists(tempDir.resolve(unused)));
  }
}
