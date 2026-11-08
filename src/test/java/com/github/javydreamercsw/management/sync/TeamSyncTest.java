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
package com.github.javydreamercsw.management.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.team.Team;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.team.TeamSize;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.dto.TeamImportDTO;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.team.TeamService;
import com.github.javydreamercsw.management.sync.review.SeedProvenanceManifest;
import com.github.javydreamercsw.management.sync.review.SeedUpdateReviewService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Unit tests for the {@code teams.json} catalog sync (ATW-01g2) + seed review detection. */
@DisplayName("TeamSync Tests")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TeamSyncTest {

  @Mock private TeamService teamService;
  @Mock private TeamRepository teamRepository;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private NpcService npcService;
  @Mock private UniverseRepository universeRepository;
  @Mock private WrestlerStateRepository wrestlerStateRepository;
  @Mock private SeedUpdateReviewService seedReviewService;

  private static final SeedProvenanceManifest EMPTY_MANIFEST = SeedProvenanceManifest.empty();

  private static final SeedSyncSupport VERIFY_SUPPORT = new SeedSyncSupport("verify");

  @Test
  @DisplayName("Catalog entry without teamSize parses as DUO")
  void catalogWithoutTeamSizeSeedsDuo() throws Exception {
    // Given - a legacy catalog entry without a teamSize field
    String json =
        """
        [
          {
            "name": "Legacy Duo",
            "description": "An old-shape entry",
            "members": ["Wrestler A", "Wrestler B"]
          }
        ]
        """;

    // When
    List<TeamImportDTO> dtos = new ObjectMapper().readValue(json, new TypeReference<>() {});

    // Then
    assertThat(dtos).hasSize(1);
    TeamImportDTO dto = dtos.getFirst();
    assertThat(dto.getTeamSize()).isEqualTo(TeamSize.DUO.name());
    assertThat(dto.getMembers()).containsExactly("Wrestler A", "Wrestler B");
  }

  private TeamSync sync(String mode) {
    return new TeamSync(
        mode,
        teamService,
        teamRepository,
        wrestlerRepository,
        npcService,
        new ObjectMapper(),
        universeRepository,
        wrestlerStateRepository,
        seedReviewService,
        EMPTY_MANIFEST);
  }

  @org.junit.jupiter.api.BeforeEach
  void setUp() {
    when(teamService.count()).thenReturn(0L);
  }

  private static Team team(String name, String description, TeamSize size) {
    Team t = Team.builder().name(name).description(description).build();
    t.setTeamSize(size);
    return t;
  }

  @Test
  @DisplayName("Version-change boot parks the tier-2 collision and preserves the admin edit")
  void versionChangeBootsParkCollisions() {
    // THREE-TIER pin, end-to-end through the wired pipeline. "The Trailblazers" is the tier-1 row
    // (description stale vs the stamp, seed current); "The British Bulldogs" the tier-2 row
    // (admin-tuned description while the seed also changed it). Every other field of both rows
    // matches the incoming seed exactly.
    Team tier1row = team("The Trailblazers", "stale description", TeamSize.DUO);
    tier1row.setSeedFingerprint(VERIFY_SUPPORT.fingerprint("stale description", "DUO"));

    Team tier2row = team("The British Bulldogs", "admin's description", TeamSize.DUO);
    // The stamp carries the PREVIOUS release's seed description while the row shows the admin's
    // edit and the incoming seed ships a third value → genuine tier-2 collision.
    tier2row.setSeedFingerprint(VERIFY_SUPPORT.fingerprint("previous seed description", "DUO"));

    when(teamService.count()).thenReturn(2L);
    when(teamRepository.findByName("The Trailblazers")).thenReturn(Optional.of(tier1row));
    when(teamRepository.findByName("The British Bulldogs")).thenReturn(Optional.of(tier2row));

    java.util.Set<String> parkedCoordinates = new java.util.HashSet<>();
    SeedUpdateReviewService parking = org.mockito.Mockito.mock(SeedUpdateReviewService.class);
    org.mockito.Mockito.doAnswer(
            inv -> {
              parkedCoordinates.add(inv.getArgument(1) + "|" + inv.getArgument(3));
              return null;
            })
        .when(parking)
        .parkFieldConflict(
            anyString(), anyString(), anyString(), anyString(), any(), any(), anyString());

    TeamSync detectionSync =
        new TeamSync(
            "true",
            teamService,
            teamRepository,
            wrestlerRepository,
            npcService,
            new ObjectMapper(),
            universeRepository,
            wrestlerStateRepository,
            parking,
            EMPTY_MANIFEST);
    detectionSync.detectVersionChangeCollisions();

    // Tier 1: the untouched description updated silently to the NEW seed value.
    assertEquals(
        "High flying duo with amazing moves. With talent that transcends time.",
        tier1row.getDescription(),
        "tier 1: untouched field with a changed seed value must update silently");
    // Tier 2: the admin's description kept its value; the row parked the collision.
    assertEquals(
        "admin's description", tier2row.getDescription(), "tier 2: admin edit must keep its value");
    org.mockito.Mockito.verify(parking)
        .parkFieldConflict(
            eq("team"),
            eq("team:the-british-bulldogs"),
            eq("The British Bulldogs"),
            eq("description"),
            eq("admin's description"),
            eq(
                "The legendary professional wrestling tag team of cousins Davey Boy Smith (The"
                    + " British Bulldog) and Dynamite Kid (Tom Billington), known for their"
                    + " high-impact style in the 1980s"),
            anyString());
    assertEquals(
        java.util.Set.of("team:the-british-bulldogs|description"),
        parkedCoordinates,
        "exactly the one tier-2 collision parked; tier-1 and tier-3 fields never park");
  }

  @Test
  @DisplayName("FULL-mode detection is inert; empty-table gate returns early")
  void normalBootBehaviorUnchanged() {
    TeamSync fullSync = sync("false");
    when(teamService.count()).thenReturn(2L);

    fullSync.detectVersionChangeCollisions(); // FULL mode → inert

    org.mockito.Mockito.verifyNoInteractions(seedReviewService);
    org.mockito.Mockito.verify(teamRepository, org.mockito.Mockito.never()).findByName(anyString());
  }

  @Test
  @DisplayName("FULL-mode load stamps freshly seeded rows")
  void fullModeStampsFreshRows() {
    Universe universe = Universe.builder().name("Default Universe").build();
    Wrestler w1 = Wrestler.builder().name("Jax Felix").build();
    Wrestler w2 = Wrestler.builder().name("Rey Jaguar").build();
    Team created = team("The Trailblazers", null, null);
    when(teamService.count()).thenReturn(0L);
    when(wrestlerRepository.findAll()).thenReturn(List.of(w1, w2));
    when(npcService.findAllUnfiltered()).thenReturn(List.of());
    when(universeRepository.findAll()).thenReturn(List.of(universe));
    when(wrestlerStateRepository.findByWrestlerAndUniverse(any(), any()))
        .thenReturn(Optional.empty());
    when(teamService.getTeamByName(anyString())).thenReturn(Optional.empty());
    when(teamService.createTeam(anyString(), any(), any(), any(), any(), any(), any()))
        .thenReturn(Optional.of(created));

    sync("false").sync();

    org.mockito.Mockito.verify(teamRepository, org.mockito.Mockito.atLeastOnce())
        .save(
            org.mockito.ArgumentMatchers.argThat(t -> t != null && t.getSeedFingerprint() != null));
  }
}
