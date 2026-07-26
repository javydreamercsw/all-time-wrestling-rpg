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
package com.github.javydreamercsw.management.service.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.dto.campaign.CampaignChapterDTO;
import com.github.javydreamercsw.management.dto.campaign.StaticEncounterDTO;
import com.github.javydreamercsw.management.service.expansion.ExpansionService;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

class ExtremeCampaignChapterTest {

  private static final String PATH_A_ID = "extreme_campaign";
  private static final String PATH_B_ID = "extreme_campaign_outsider";
  private static final List<String> EXTREME_WRESTLERS =
      List.of("Rob Van Dam", "Sabu", "Raven", "Daemon");

  private CampaignChapterService chapterService;
  private CampaignChapterService chapterServiceExtremeDisabled;

  @BeforeEach
  void setUp() {
    ObjectMapper objectMapper = new ObjectMapper();
    FeatureDataService featureDataService =
        new FeatureDataService(objectMapper, mock(CampaignStateRepository.class));

    ExpansionService allEnabled = mock(ExpansionService.class);
    when(allEnabled.isExpansionEnabled(anyString())).thenReturn(true);
    chapterService =
        new CampaignChapterService(
            objectMapper,
            featureDataService,
            allEnabled,
            new PathMatchingResourcePatternResolver());
    chapterService.init();

    ExpansionService allDisabled = mock(ExpansionService.class);
    when(allDisabled.isExpansionEnabled(anyString())).thenReturn(false);
    chapterServiceExtremeDisabled =
        new CampaignChapterService(
            objectMapper,
            featureDataService,
            allDisabled,
            new PathMatchingResourcePatternResolver());
    chapterServiceExtremeDisabled.init();
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private CampaignState baseState() {
    CampaignState state = new CampaignState();
    state.setVictoryPoints(0);
    state.setMatchesPlayed(0);
    Wrestler wrestler = new Wrestler();
    wrestler.setReigns(new LinkedHashSet<>());
    Campaign campaign = new Campaign();
    campaign.setWrestler(wrestler);
    state.setCampaign(campaign);
    return state;
  }

  private Optional<CampaignChapterDTO> findChapter(final String chapterId) {
    return chapterService.getAllChapters().stream()
        .filter(ch -> chapterId.equals(ch.getId()))
        .findFirst();
  }

  // ---------------------------------------------------------------------------
  // Path A — wrestler restriction
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Path A campaign is NOT visible to non-extreme wrestlers")
  void pathA_notVisibleToNonExtremeWrestler() {
    List<CampaignChapterDTO> available =
        chapterService.findAvailableChapters(baseState(), "Oda Tsurugi");

    assertThat(available).extracting(CampaignChapterDTO::getId).doesNotContain(PATH_A_ID);
  }

  @Test
  @DisplayName("Path A campaign is visible to all four extreme wrestlers with VP=0")
  void pathA_visibleToAllFourExtremeWrestlers() {
    for (String wrestler : EXTREME_WRESTLERS) {
      List<CampaignChapterDTO> available =
          chapterService.findAvailableChapters(baseState(), wrestler);

      assertThat(available)
          .as("Path A must be visible to %s", wrestler)
          .extracting(CampaignChapterDTO::getId)
          .contains(PATH_A_ID);
    }
  }

  // ---------------------------------------------------------------------------
  // Path B — open access
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Path B campaign is visible to non-extreme wrestlers")
  void pathB_visibleToNonExtremeWrestler() {
    List<CampaignChapterDTO> available =
        chapterService.findAvailableChapters(baseState(), "Oda Tsurugi");

    assertThat(available).extracting(CampaignChapterDTO::getId).contains(PATH_B_ID);
  }

  @Test
  @DisplayName("Both paths are visible to extreme wrestlers simultaneously")
  void bothPaths_visibleToExtremeWrestler() {
    List<CampaignChapterDTO> available =
        chapterService.findAvailableChapters(baseState(), "Rob Van Dam");

    assertThat(available).extracting(CampaignChapterDTO::getId).contains(PATH_A_ID, PATH_B_ID);
  }

  // ---------------------------------------------------------------------------
  // Encounter filtering by requiredWrestlerName (Path A)
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("RVD signature encounter has requiredWrestlerName = Rob Van Dam")
  void rvd_signature_encounter_filteredByWrestlerName() {
    Optional<CampaignChapterDTO> chapter = findChapter(PATH_A_ID);
    assertThat(chapter).isPresent();

    List<StaticEncounterDTO> rvdEncounters =
        chapter.get().getStaticEncounters().stream()
            .filter(e -> "Rob Van Dam".equals(e.getRequiredWrestlerName()))
            .toList();

    assertThat(rvdEncounters).as("RVD must have at least one signature encounter").isNotEmpty();
  }

  @Test
  @DisplayName("Sabu signature encounter has requiredWrestlerName = Sabu")
  void sabu_signature_encounter_filteredByWrestlerName() {
    Optional<CampaignChapterDTO> chapter = findChapter(PATH_A_ID);
    assertThat(chapter).isPresent();

    List<StaticEncounterDTO> sabuEncounters =
        chapter.get().getStaticEncounters().stream()
            .filter(e -> "Sabu".equals(e.getRequiredWrestlerName()))
            .toList();

    assertThat(sabuEncounters).as("Sabu must have at least one signature encounter").isNotEmpty();
  }

  @Test
  @DisplayName("Raven signature encounter has requiredWrestlerName = Raven")
  void raven_signature_encounter_filteredByWrestlerName() {
    Optional<CampaignChapterDTO> chapter = findChapter(PATH_A_ID);
    assertThat(chapter).isPresent();

    List<StaticEncounterDTO> ravenEncounters =
        chapter.get().getStaticEncounters().stream()
            .filter(e -> "Raven".equals(e.getRequiredWrestlerName()))
            .toList();

    assertThat(ravenEncounters).as("Raven must have at least one signature encounter").isNotEmpty();
  }

  @Test
  @DisplayName("Daemon signature encounter has requiredWrestlerName = Daemon")
  void daemon_signature_encounter_filteredByWrestlerName() {
    Optional<CampaignChapterDTO> chapter = findChapter(PATH_A_ID);
    assertThat(chapter).isPresent();

    List<StaticEncounterDTO> daemonEncounters =
        chapter.get().getStaticEncounters().stream()
            .filter(e -> "Daemon".equals(e.getRequiredWrestlerName()))
            .toList();

    assertThat(daemonEncounters)
        .as("Daemon must have at least one signature encounter")
        .isNotEmpty();
  }

  @Test
  @DisplayName("Shared encounters in Path A have no wrestler restriction")
  void sharedEncounters_haveNoWrestlerRestriction() {
    Optional<CampaignChapterDTO> chapter = findChapter(PATH_A_ID);
    assertThat(chapter).isPresent();

    List<StaticEncounterDTO> shared =
        chapter.get().getStaticEncounters().stream()
            .filter(e -> e.getRequiredWrestlerName() == null)
            .toList();

    // Underground (4) + Championship (4) + Finale (5) shared cards = 13 total
    assertThat(shared).hasSizeGreaterThanOrEqualTo(11);
  }

  // ---------------------------------------------------------------------------
  // Path B — opponentPool contains the 4 extreme wrestlers
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Path B encounters use the extreme wrestlers as opponentPool")
  void pathB_encounters_useExtremeWrestlersAsOpponentPool() {
    Optional<CampaignChapterDTO> chapter = findChapter(PATH_B_ID);
    assertThat(chapter).isPresent();

    var choicesWithPool =
        chapter.get().getStaticEncounters().stream()
            .flatMap(
                e ->
                    e.getChoices() != null
                        ? e.getChoices().stream()
                        : java.util.stream.Stream.empty())
            .filter(c -> c.getOpponentPool() != null && !c.getOpponentPool().isEmpty())
            .toList();

    assertThat(choicesWithPool)
        .as("At least one encounter choice should restrict opponents to the extreme pool")
        .isNotEmpty();

    choicesWithPool.forEach(
        choice ->
            assertThat(choice.getOpponentPool())
                .as("opponentPool must include all four extreme wrestlers")
                .containsAll(EXTREME_WRESTLERS));
  }

  // ---------------------------------------------------------------------------
  // Exit criteria — wonFinale
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Path A exit criteria include wonFinale=true for legend outcome")
  void pathA_exitCriteria_includeWonFinaleTrue() {
    Optional<CampaignChapterDTO> chapter = findChapter(PATH_A_ID);
    assertThat(chapter).isPresent();

    boolean hasWonFinaleTrue =
        chapter.get().getExitPoints().stream()
            .flatMap(p -> p.getCriteria().stream())
            .anyMatch(c -> Boolean.TRUE.equals(c.getWonFinale()));

    assertThat(hasWonFinaleTrue).as("Path A must have an exit point with wonFinale=true").isTrue();
  }

  @Test
  @DisplayName("Path B exit criteria include wonFinale=true for outsider legend outcome")
  void pathB_exitCriteria_includeWonFinaleTrue() {
    Optional<CampaignChapterDTO> chapter = findChapter(PATH_B_ID);
    assertThat(chapter).isPresent();

    boolean hasWonFinaleTrue =
        chapter.get().getExitPoints().stream()
            .flatMap(p -> p.getCriteria().stream())
            .anyMatch(c -> Boolean.TRUE.equals(c.getWonFinale()));

    assertThat(hasWonFinaleTrue).as("Path B must have an exit point with wonFinale=true").isTrue();
  }

  // ---------------------------------------------------------------------------
  // Expansion gating
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Both extreme campaigns are hidden when the EXTREME expansion is disabled")
  void extremeCampaigns_hiddenWhenExpansionDisabled() {
    List<CampaignChapterDTO> available =
        chapterServiceExtremeDisabled.findAvailableChapters(baseState(), "Rob Van Dam");

    assertThat(available)
        .extracting(CampaignChapterDTO::getId)
        .doesNotContain(PATH_A_ID, PATH_B_ID);
  }
}
