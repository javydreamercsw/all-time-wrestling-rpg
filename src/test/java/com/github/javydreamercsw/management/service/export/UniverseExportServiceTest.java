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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignmentRepository;
import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.injury.InjuryRepository;
import com.github.javydreamercsw.management.domain.injury.InjurySeverity;
import com.github.javydreamercsw.management.domain.relationship.RelationshipType;
import com.github.javydreamercsw.management.domain.relationship.WrestlerRelationship;
import com.github.javydreamercsw.management.domain.relationship.WrestlerRelationshipRepository;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.rivalry.RivalryRepository;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleReign;
import com.github.javydreamercsw.management.domain.title.TitleReignRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UniverseExportServiceTest {

  private WrestlerStateRepository wrestlerStateRepository;
  private InjuryRepository injuryRepository;
  private RivalryRepository rivalryRepository;
  private TitleReignRepository titleReignRepository;
  private WrestlerAlignmentRepository wrestlerAlignmentRepository;
  private WrestlerRelationshipRepository wrestlerRelationshipRepository;

  private UniverseExportService service;

  private Universe universe;
  private Wrestler activeWrestler;
  private Wrestler inactiveWrestler;
  private WrestlerState activeState;
  private WrestlerState inactiveState;

  @BeforeEach
  void setUp() {
    wrestlerStateRepository = mock(WrestlerStateRepository.class);
    injuryRepository = mock(InjuryRepository.class);
    rivalryRepository = mock(RivalryRepository.class);
    titleReignRepository = mock(TitleReignRepository.class);
    wrestlerAlignmentRepository = mock(WrestlerAlignmentRepository.class);
    wrestlerRelationshipRepository = mock(WrestlerRelationshipRepository.class);

    service =
        new UniverseExportService(
            wrestlerStateRepository,
            injuryRepository,
            rivalryRepository,
            titleReignRepository,
            wrestlerAlignmentRepository,
            wrestlerRelationshipRepository);

    universe = mock(Universe.class);
    when(universe.getId()).thenReturn(1L);
    when(universe.getName()).thenReturn("TestUniverse");

    activeWrestler = mock(Wrestler.class);
    when(activeWrestler.getId()).thenReturn(10L);
    when(activeWrestler.getName()).thenReturn("Active Wrestler");
    when(activeWrestler.getActive()).thenReturn(true);

    inactiveWrestler = mock(Wrestler.class);
    when(inactiveWrestler.getId()).thenReturn(20L);
    when(inactiveWrestler.getName()).thenReturn("Inactive Wrestler");
    when(inactiveWrestler.getActive()).thenReturn(false);

    activeState = mock(WrestlerState.class);
    when(activeState.getWrestler()).thenReturn(activeWrestler);
    when(activeState.getTier()).thenReturn(WrestlerTier.ROOKIE);
    when(activeState.getFans()).thenReturn(1000L);
    when(activeState.getBumps()).thenReturn(5);
    when(activeState.getMorale()).thenReturn(80);
    when(activeState.getCurrentHealth()).thenReturn(15);
    when(activeState.getManagementStamina()).thenReturn(90);
    when(activeState.getPhysicalCondition()).thenReturn(100);
    when(activeState.getFaction()).thenReturn(null);

    inactiveState = mock(WrestlerState.class);
    when(inactiveState.getWrestler()).thenReturn(inactiveWrestler);
    when(inactiveState.getTier()).thenReturn(WrestlerTier.ROOKIE);
    when(inactiveState.getFans()).thenReturn(500L);
    when(inactiveState.getBumps()).thenReturn(2);
    when(inactiveState.getMorale()).thenReturn(60);
    when(inactiveState.getCurrentHealth()).thenReturn(10);
    when(inactiveState.getManagementStamina()).thenReturn(70);
    when(inactiveState.getPhysicalCondition()).thenReturn(80);
    when(inactiveState.getFaction()).thenReturn(null);

    when(wrestlerStateRepository.findByUniverseId(1L))
        .thenReturn(List.of(activeState, inactiveState));

    // Default: no related data
    when(injuryRepository.findByWrestlerAndUniverse(activeWrestler, universe))
        .thenReturn(List.of());
    when(injuryRepository.findByWrestlerAndUniverse(inactiveWrestler, universe))
        .thenReturn(List.of());
    when(rivalryRepository.findByUniverseWithWrestlers(universe)).thenReturn(List.of());
    when(titleReignRepository.findByChampionsContaining(activeWrestler)).thenReturn(List.of());
    when(titleReignRepository.findByChampionsContaining(inactiveWrestler)).thenReturn(List.of());
    when(wrestlerAlignmentRepository.findByWrestlerAndUniverseId(activeWrestler, 1L))
        .thenReturn(Optional.empty());
    when(wrestlerAlignmentRepository.findByWrestlerAndUniverseId(inactiveWrestler, 1L))
        .thenReturn(Optional.empty());
    when(wrestlerRelationshipRepository.findAllByWrestler(activeWrestler)).thenReturn(List.of());
    when(wrestlerRelationshipRepository.findAllByWrestler(inactiveWrestler)).thenReturn(List.of());
  }

  @Test
  void emptyCategorySet_returnsEmptyPayload() {
    ExportPayload payload = service.collect(universe, Set.of(), WrestlerFilter.all());
    assertThat(payload.data()).isEmpty();
  }

  @Test
  void fullCategorySet_producesSixEntries() {
    ExportPayload payload =
        service.collect(universe, EnumSet.allOf(ExportCategory.class), WrestlerFilter.all());
    assertThat(payload.data()).hasSize(6);
    assertThat(payload.data()).containsKeys(ExportCategory.values());
  }

  @Test
  void universeStateCategory_mapsExpectedFields() {
    ExportPayload payload =
        service.collect(universe, Set.of(ExportCategory.UNIVERSE_STATE), WrestlerFilter.all());

    List<Map<String, Object>> rows = payload.data().get(ExportCategory.UNIVERSE_STATE);
    assertThat(rows).hasSize(2);

    Map<String, Object> activeRow =
        rows.stream()
            .filter(r -> "Active Wrestler".equals(r.get("wrestler")))
            .findFirst()
            .orElseThrow();
    assertThat(activeRow).containsEntry("fans", 1000L);
    assertThat(activeRow).containsEntry("bumps", 5);
    assertThat(activeRow).containsEntry("tier", "ROOKIE");
    assertThat(activeRow).containsEntry("faction", "");
  }

  @Test
  void activeOnlyFilter_excludesInactiveWrestlers() {
    ExportPayload payload =
        service.collect(
            universe, Set.of(ExportCategory.UNIVERSE_STATE), WrestlerFilter.activeOnly());

    List<Map<String, Object>> rows = payload.data().get(ExportCategory.UNIVERSE_STATE);
    assertThat(rows).hasSize(1);
    assertThat(rows.get(0)).containsEntry("wrestler", "Active Wrestler");
  }

  @Test
  void manualFilter_includesOnlySpecifiedWrestlers() {
    ExportPayload payload =
        service.collect(
            universe, Set.of(ExportCategory.UNIVERSE_STATE), WrestlerFilter.manual(Set.of(20L)));

    List<Map<String, Object>> rows = payload.data().get(ExportCategory.UNIVERSE_STATE);
    assertThat(rows).hasSize(1);
    assertThat(rows.get(0)).containsEntry("wrestler", "Inactive Wrestler");
  }

  @Test
  void injuriesCategory_mapsExpectedFields() {
    Injury injury = mock(Injury.class);
    when(injury.getName()).thenReturn("Knee Sprain");
    when(injury.getSeverity()).thenReturn(InjurySeverity.MINOR);
    when(injury.getHealthPenalty()).thenReturn(2);
    when(injury.getStaminaPenalty()).thenReturn(0);
    when(injury.getHandSizePenalty()).thenReturn(0);
    when(injury.getIsActive()).thenReturn(true);
    when(injury.getInjuryDate()).thenReturn(Instant.EPOCH);
    when(injury.getHealedDate()).thenReturn(null);

    when(injuryRepository.findByWrestlerAndUniverse(activeWrestler, universe))
        .thenReturn(List.of(injury));

    ExportPayload payload =
        service.collect(universe, Set.of(ExportCategory.INJURIES), WrestlerFilter.activeOnly());

    List<Map<String, Object>> rows = payload.data().get(ExportCategory.INJURIES);
    assertThat(rows).hasSize(1);
    assertThat(rows.get(0)).containsEntry("wrestler", "Active Wrestler");
    assertThat(rows.get(0)).containsEntry("name", "Knee Sprain");
    assertThat(rows.get(0)).containsEntry("severity", "MINOR");
  }

  @Test
  void rivalriesCategory_deduplicatesSharedRivalries() {
    Rivalry rivalry = mock(Rivalry.class);
    when(rivalry.getId()).thenReturn(99L);
    when(rivalry.getWrestler1()).thenReturn(activeWrestler);
    when(rivalry.getWrestler2()).thenReturn(inactiveWrestler);
    when(rivalry.getHeat()).thenReturn(15);
    when(rivalry.getHeatEvents()).thenReturn(List.of());
    when(rivalry.getIsActive()).thenReturn(true);
    when(rivalry.getStartedDate()).thenReturn(Instant.EPOCH);
    when(rivalry.getEndedDate()).thenReturn(null);
    when(rivalry.getStorylineNotes()).thenReturn(null);

    when(rivalryRepository.findByUniverseWithWrestlers(universe)).thenReturn(List.of(rivalry));

    ExportPayload payload =
        service.collect(universe, Set.of(ExportCategory.RIVALRIES), WrestlerFilter.all());

    List<Map<String, Object>> rows = payload.data().get(ExportCategory.RIVALRIES);
    assertThat(rows).hasSize(1);
    assertThat(rows.get(0)).containsEntry("heat", 15);
  }

  @Test
  void titleReignsCategory_deduplicatesSharedReigns() {
    Title title = mock(Title.class);
    when(title.getName()).thenReturn("World Title");

    TitleReign reign = mock(TitleReign.class);
    when(reign.getId()).thenReturn(55L);
    when(reign.getTitle()).thenReturn(title);
    when(reign.getChampions()).thenReturn(Set.of(activeWrestler, inactiveWrestler));
    when(reign.getReignNumber()).thenReturn(1);
    when(reign.getStartDate()).thenReturn(Instant.EPOCH);
    when(reign.getEndDate()).thenReturn(null);

    when(titleReignRepository.findByChampionsContaining(activeWrestler)).thenReturn(List.of(reign));
    when(titleReignRepository.findByChampionsContaining(inactiveWrestler))
        .thenReturn(List.of(reign));

    ExportPayload payload =
        service.collect(universe, Set.of(ExportCategory.TITLE_REIGNS), WrestlerFilter.all());

    List<Map<String, Object>> rows = payload.data().get(ExportCategory.TITLE_REIGNS);
    assertThat(rows).hasSize(1);
    assertThat(rows.get(0)).containsEntry("title", "World Title");
  }

  @Test
  void alignmentsCategory_mapsExpectedFields() {
    WrestlerAlignment alignment = mock(WrestlerAlignment.class);
    when(alignment.getAlignmentType()).thenReturn(AlignmentType.FACE);
    when(alignment.getLevel()).thenReturn(75);

    when(wrestlerAlignmentRepository.findByWrestlerAndUniverseId(activeWrestler, 1L))
        .thenReturn(Optional.of(alignment));

    ExportPayload payload =
        service.collect(universe, Set.of(ExportCategory.ALIGNMENTS), WrestlerFilter.activeOnly());

    List<Map<String, Object>> rows = payload.data().get(ExportCategory.ALIGNMENTS);
    assertThat(rows).hasSize(1);
    assertThat(rows.get(0)).containsEntry("wrestler", "Active Wrestler");
    assertThat(rows.get(0)).containsEntry("alignmentType", "FACE");
    assertThat(rows.get(0)).containsEntry("level", 75);
  }

  @Test
  void relationshipsCategory_deduplicatesSharedRelationships() {
    WrestlerRelationship rel = mock(WrestlerRelationship.class);
    when(rel.getId()).thenReturn(77L);
    when(rel.getWrestler1()).thenReturn(activeWrestler);
    when(rel.getWrestler2()).thenReturn(inactiveWrestler);
    when(rel.getType()).thenReturn(RelationshipType.MENTOR);
    when(rel.getLevel()).thenReturn(80);
    when(rel.getIsStoryline()).thenReturn(false);
    when(rel.getStartedDate()).thenReturn(Instant.EPOCH);

    when(wrestlerRelationshipRepository.findAllByWrestler(activeWrestler)).thenReturn(List.of(rel));
    when(wrestlerRelationshipRepository.findAllByWrestler(inactiveWrestler))
        .thenReturn(List.of(rel));

    ExportPayload payload =
        service.collect(universe, Set.of(ExportCategory.RELATIONSHIPS), WrestlerFilter.all());

    List<Map<String, Object>> rows = payload.data().get(ExportCategory.RELATIONSHIPS);
    assertThat(rows).hasSize(1);
    assertThat(rows.get(0)).containsEntry("type", "MENTOR");
  }
}
