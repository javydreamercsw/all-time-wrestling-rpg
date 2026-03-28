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
package com.github.javydreamercsw.base.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.team.Team;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.world.Arena;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.faction.FactionService;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.team.TeamService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.world.ArenaService;
import com.github.javydreamercsw.management.service.world.LocationService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("h2")
@Transactional
class EntityImageResolutionIntegrationTest {

  @Autowired private WrestlerService wrestlerService;
  @Autowired private NpcService npcService;
  @Autowired private ShowTemplateService showTemplateService;
  @Autowired private TitleService titleService;
  @Autowired private TeamService teamService;
  @Autowired private FactionService factionService;
  @Autowired private ArenaService arenaService;
  @Autowired private LocationService locationService;

  @Test
  void testWrestlerImageResolution() {
    // Princess Aussie.png exists in src/main/resources/images/wrestlers/
    Wrestler wrestler = Wrestler.builder().name("Princess Aussie").build();
    String imageUrl = wrestlerService.resolveWrestlerImage(wrestler);
    assertNotNull(imageUrl);
    assertEquals("images/wrestlers/Princess Aussie.png", imageUrl);
  }

  @Test
  void testWrestlerFallbackResolution() {
    // Should fallback to generic-wrestler.png
    Wrestler wrestler = Wrestler.builder().name("Unknown Wrestler").build();
    String imageUrl = wrestlerService.resolveWrestlerImage(wrestler);
    assertNotNull(imageUrl);
    assertEquals("images/generic-wrestler.png", imageUrl);
  }

  @Test
  void testNpcFallbackResolution() {
    // Should fallback to generic-npc.png
    Npc npc = Npc.builder().name("Unknown NPC").build();
    String imageUrl = npcService.resolveNpcImage(npc);
    assertNotNull(imageUrl);
    assertEquals("images/generic-npc.png", imageUrl);
  }

  @Test
  void testShowTemplateFallbackResolution() {
    ShowTemplate template = new ShowTemplate();
    template.setName("Unknown Show");
    String imageUrl = showTemplateService.resolveShowTemplateImage(template);
    assertNotNull(imageUrl);
    assertEquals("images/generic-show.png", imageUrl);
  }

  @Test
  void testTitleFallbackResolution() {
    Title title = new Title();
    title.setName("Unknown Title");
    String imageUrl = titleService.resolveTitleImage(title);
    assertNotNull(imageUrl);
    assertEquals("images/generic-title.png", imageUrl);
  }

  @Test
  void testTitleKebabCaseResolution() {
    // atw-extreme.png exists in src/main/resources/META-INF/resources/images/championships/
    Title title = new Title();
    title.setName("ATW Extreme");
    String imageUrl = titleService.resolveTitleImage(title);
    assertNotNull(imageUrl);
    assertEquals("images/championships/atw-extreme.png", imageUrl);
  }

  @Test
  void testTeamFallbackResolution() {
    Team team = new Team();
    team.setName("Unknown Team");
    String imageUrl = teamService.resolveTeamImage(team);
    assertNotNull(imageUrl);
    assertEquals("images/generic-team.png", imageUrl);
  }

  @Test
  void testFactionFallbackResolution() {
    Faction faction = Faction.builder().name("Unknown Faction").build();
    String imageUrl = factionService.resolveFactionImage(faction);
    assertNotNull(imageUrl);
    assertEquals("images/generic-faction.png", imageUrl);
  }

  @Test
  void testArenaFallbackResolution() {
    Arena arena = Arena.builder().name("Unknown Arena").build();
    String imageUrl = arenaService.resolveArenaImage(arena);
    assertNotNull(imageUrl);
    assertEquals("images/generic-venue.png", imageUrl);
  }

  @Test
  void testLocationFallbackResolution() {
    com.github.javydreamercsw.management.domain.world.Location location =
        com.github.javydreamercsw.management.domain.world.Location.builder()
            .name("Unknown Location")
            .build();
    String imageUrl = locationService.resolveLocationImage(location);
    assertNotNull(imageUrl);
    assertEquals("images/generic-venue.png", imageUrl);
  }
}
