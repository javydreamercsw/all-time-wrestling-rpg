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
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.title.TitleReign;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.dto.campaign.CampaignChapterDTO;
import com.github.javydreamercsw.management.dto.campaign.ChapterCriteriaDTO;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CampaignChapterServiceTest {

  private CampaignChapterService chapterService;

  @BeforeEach
  public void setUp() {
    ObjectMapper objectMapper = new ObjectMapper();
    FeatureDataService featureDataService =
        new FeatureDataService(objectMapper, mock(CampaignStateRepository.class));
    chapterService =
        new CampaignChapterService(
            objectMapper,
            featureDataService,
            org.mockito.Mockito.mock(
                com.github.javydreamercsw.management.service.expansion.ExpansionService.class),
            new org.springframework.core.io.support.PathMatchingResourcePatternResolver());
    chapterService.init();
  }

  @Test
  void testGetAllChapters() {
    List<CampaignChapterDTO> chapters = chapterService.getAllChapters();
    assertThat(chapters).isNotEmpty();
    assertThat(chapters.size()).isGreaterThanOrEqualTo(2);
  }

  @Test
  void testGetSpecificChapter() {
    Optional<CampaignChapterDTO> ch1 = chapterService.getChapter("beginning");
    assertThat(ch1).isPresent();
    assertThat(ch1.get().getTitle()).isEqualTo("All or Nothing Campaign");

    Optional<CampaignChapterDTO> ch2 = chapterService.getChapter("tournament");
    assertThat(ch2).isPresent();
    assertThat(ch2.get().getTitle()).isEqualTo("The Tournament");
  }

  @Test
  void testChapterRules() {
    CampaignChapterDTO ch2 = chapterService.getChapter("tournament").get();
    assertThat(ch2.getRules().getQualifyingMatches()).isEqualTo(0);
    assertThat(ch2.getRules().getMinWinsToQualify()).isEqualTo(0);
  }

  @Test
  void testFindAvailableChapters() {
    CampaignState state = new CampaignState();

    Campaign campaign = new Campaign();
    Wrestler wrestler = new Wrestler();
    // Initialize required collections
    wrestler.setReigns(new java.util.LinkedHashSet<>());
    campaign.setWrestler(wrestler);
    state.setCampaign(campaign);

    // Initially ch1 should be available (no criteria)
    List<CampaignChapterDTO> available = chapterService.findAvailableChapters(state);
    assertThat(available).extracting(CampaignChapterDTO::getId).contains("beginning");

    // After completing ch1 with enough VP, ch2 tournament should be available
    state.getCompletedChapterIds().add("beginning");
    state.setVictoryPoints(5); // Requirement for tournament entry

    available = chapterService.findAvailableChapters(state);
    assertThat(available).extracting(CampaignChapterDTO::getId).contains("tournament");
  }

  // -------------------------------------------------------------------------
  // Groovy script evaluation
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("Groovy script returning true passes the criterion")
  void testGroovyScriptReturningTrue() {
    CampaignState state = buildMinimalState();
    assertThat(chapterService.evaluateGroovyScript("true", state)).isTrue();
  }

  @Test
  @DisplayName("Groovy script returning false fails the criterion")
  void testGroovyScriptReturningFalse() {
    CampaignState state = buildMinimalState();
    assertThat(chapterService.evaluateGroovyScript("false", state)).isFalse();
  }

  @Test
  @DisplayName("Groovy script can read state.wins")
  void testGroovyScriptCanAccessState() {
    CampaignState state = buildMinimalState();
    state.setWins(10);
    assertThat(chapterService.evaluateGroovyScript("state.wins >= 10", state)).isTrue();
    assertThat(chapterService.evaluateGroovyScript("state.wins >= 11", state)).isFalse();
  }

  @Test
  @DisplayName("Groovy script that throws returns false and does not propagate")
  void testGroovyScriptExceptionReturnsFalse() {
    CampaignState state = buildMinimalState();
    assertThat(chapterService.evaluateGroovyScript("throw new RuntimeException('boom')", state))
        .isFalse();
  }

  @Test
  @DisplayName("Script is skipped when built-in check already failed")
  void testScriptNotEvaluatedWhenBuiltInFails() {
    CampaignState state = buildMinimalState();
    state.setVictoryPoints(0);

    // minVictoryPoints=5 fails → script returning true should not rescue the criterion
    ChapterCriteriaDTO criteria =
        ChapterCriteriaDTO.builder().minVictoryPoints(5).customEvaluationScript("true").build();

    assertThat(chapterService.isCriteriaMet(criteria, state)).isFalse();
  }

  @Test
  @DisplayName("Script runs when built-in checks pass and determines the result")
  void testScriptRunsAfterBuiltInChecksPassed() {
    CampaignState state = buildMinimalState();
    state.setVictoryPoints(10);
    state.setWins(3);

    // Built-in passes (minVictoryPoints=5 satisfied), script adds extra gate
    ChapterCriteriaDTO passCriteria =
        ChapterCriteriaDTO.builder()
            .minVictoryPoints(5)
            .customEvaluationScript("state.wins >= 3")
            .build();
    assertThat(chapterService.isCriteriaMet(passCriteria, state)).isTrue();

    ChapterCriteriaDTO failCriteria =
        ChapterCriteriaDTO.builder()
            .minVictoryPoints(5)
            .customEvaluationScript("state.wins >= 10")
            .build();
    assertThat(chapterService.isCriteriaMet(failCriteria, state)).isFalse();
  }

  // -------------------------------------------------------------------------
  // isCriteriaMet — feature-data branches
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("tournamentWinner=true passes when featureData contains tournamentWinner=true")
  void testCriteriaTournamentWinnerMatch() {
    CampaignState state = buildMinimalState();
    state.setFeatureData("{\"tournamentWinner\":true}");

    ChapterCriteriaDTO criteria = ChapterCriteriaDTO.builder().tournamentWinner(true).build();
    assertThat(chapterService.isCriteriaMet(criteria, state)).isTrue();
  }

  @Test
  @DisplayName("tournamentWinner=true fails when featureData says false")
  void testCriteriaTournamentWinnerMismatch() {
    CampaignState state = buildMinimalState();
    state.setFeatureData("{\"tournamentWinner\":false}");

    ChapterCriteriaDTO criteria = ChapterCriteriaDTO.builder().tournamentWinner(true).build();
    assertThat(chapterService.isCriteriaMet(criteria, state)).isFalse();
  }

  @Test
  @DisplayName("failedToQualify=true passes when featureData contains failedToQualify=true")
  void testCriteriaFailedToQualifyMatch() {
    CampaignState state = buildMinimalState();
    state.setFeatureData("{\"failedToQualify\":true}");

    ChapterCriteriaDTO criteria = ChapterCriteriaDTO.builder().failedToQualify(true).build();
    assertThat(chapterService.isCriteriaMet(criteria, state)).isTrue();
  }

  @Test
  @DisplayName("wonFinale=true fails when featureData says false")
  void testCriteriaWonFinaleMismatch() {
    CampaignState state = buildMinimalState();
    state.setFeatureData("{\"wonFinale\":false}");

    ChapterCriteriaDTO criteria = ChapterCriteriaDTO.builder().wonFinale(true).build();
    assertThat(chapterService.isCriteriaMet(criteria, state)).isFalse();
  }

  @Test
  @DisplayName("isChampion=true passes when wrestler holds a current title reign")
  void testCriteriaIsChampionTrue() {
    CampaignState state = buildMinimalState();
    TitleReign currentReign = new TitleReign();
    // endDate == null → isCurrentReign() == true
    state.getCampaign().getWrestler().getReigns().add(currentReign);

    ChapterCriteriaDTO criteria = ChapterCriteriaDTO.builder().isChampion(true).build();
    assertThat(chapterService.isCriteriaMet(criteria, state)).isTrue();
  }

  @Test
  @DisplayName("isChampion=true fails when wrestler has no current reign")
  void testCriteriaIsChampionFalse() {
    CampaignState state = buildMinimalState();
    // No reigns at all → not a champion
    ChapterCriteriaDTO criteria = ChapterCriteriaDTO.builder().isChampion(true).build();
    assertThat(chapterService.isCriteriaMet(criteria, state)).isFalse();
  }

  @Test
  @DisplayName("hasFaction=true passes when wrestler has a faction in the campaign universe")
  void testCriteriaHasFactionTrue() {
    CampaignState state = buildMinimalState();

    Universe universe = Universe.builder().name("Test").build();
    universe.setId(1L);
    state.getCampaign().setUniverse(universe);

    Faction faction = Faction.builder().name("nWo").build();
    WrestlerState ws =
        WrestlerState.builder()
            .wrestler(state.getCampaign().getWrestler())
            .universe(universe)
            .faction(faction)
            .build();
    state.getCampaign().getWrestler().getWrestlerStates().add(ws);

    ChapterCriteriaDTO criteria = ChapterCriteriaDTO.builder().hasFaction(true).build();
    assertThat(chapterService.isCriteriaMet(criteria, state)).isTrue();
  }

  @Test
  @DisplayName("hasFaction=true fails when wrestler has no faction in the campaign universe")
  void testCriteriaHasFactionFalse() {
    CampaignState state = buildMinimalState();

    Universe universe = Universe.builder().name("Test").build();
    universe.setId(1L);
    state.getCampaign().setUniverse(universe);

    // WrestlerState exists but no faction
    WrestlerState ws =
        WrestlerState.builder()
            .wrestler(state.getCampaign().getWrestler())
            .universe(universe)
            .build();
    state.getCampaign().getWrestler().getWrestlerStates().add(ws);

    ChapterCriteriaDTO criteria = ChapterCriteriaDTO.builder().hasFaction(true).build();
    assertThat(chapterService.isCriteriaMet(criteria, state)).isFalse();
  }

  @Test
  @DisplayName("requiredAlignmentType passes when wrestler alignment matches")
  void testCriteriaAlignmentMatch() {
    CampaignState state = buildMinimalState();
    WrestlerAlignment alignment = new WrestlerAlignment();
    alignment.setAlignmentType(AlignmentType.FACE);
    alignment.setLevel(5);
    state.getCampaign().getWrestler().setAlignment(alignment);

    ChapterCriteriaDTO criteria =
        ChapterCriteriaDTO.builder().requiredAlignmentType("FACE").minAlignmentLevel(3).build();
    assertThat(chapterService.isCriteriaMet(criteria, state)).isTrue();
  }

  @Test
  @DisplayName("requiredAlignmentType fails when alignment level is below minimum")
  void testCriteriaAlignmentLevelTooLow() {
    CampaignState state = buildMinimalState();
    WrestlerAlignment alignment = new WrestlerAlignment();
    alignment.setAlignmentType(AlignmentType.FACE);
    alignment.setLevel(1);
    state.getCampaign().getWrestler().setAlignment(alignment);

    ChapterCriteriaDTO criteria =
        ChapterCriteriaDTO.builder().requiredAlignmentType("FACE").minAlignmentLevel(3).build();
    assertThat(chapterService.isCriteriaMet(criteria, state)).isFalse();
  }

  @Test
  @DisplayName("requiredAlignmentType fails when wrestler has no alignment")
  void testCriteriaAlignmentNullWrestlerAlignment() {
    CampaignState state = buildMinimalState();
    // wrestler has no alignment set

    ChapterCriteriaDTO criteria =
        ChapterCriteriaDTO.builder().requiredAlignmentType("FACE").build();
    assertThat(chapterService.isCriteriaMet(criteria, state)).isFalse();
  }

  @Test
  @DisplayName("allExpansionsEnabled returns false when a required expansion is disabled")
  void testAllExpansionsEnabledFalse() {
    // ExpansionService mock returns false by default (no stubbing)
    assertThat(chapterService.allExpansionsEnabled(List.of("EXPANSION_X"))).isFalse();
  }

  @Test
  @DisplayName("findAvailableChapters filters out chapters restricted to other wrestler names")
  void testFindAvailableChaptersWrestlerNameFilter() {
    CampaignState state = buildMinimalState();
    // "beginning" chapter has allowedWrestlerNames = [Kurt Angle, ...]; use a name not in the list
    List<CampaignChapterDTO> chapters = chapterService.findAvailableChapters(state, "Unknown Joe");
    assertThat(chapters).extracting(CampaignChapterDTO::getId).doesNotContain("beginning");
  }

  @Test
  @DisplayName("findAvailableChapters with null wrestlerName bypasses wrestler name restriction")
  void testFindAvailableChaptersNullWrestlerName() {
    CampaignState state = buildMinimalState();
    List<CampaignChapterDTO> withNull = chapterService.findAvailableChapters(state, null);
    List<CampaignChapterDTO> withName = chapterService.findAvailableChapters(state, "Unknown Joe");
    // Null name should return at least as many chapters as the restricted call
    assertThat(withNull.size()).isGreaterThanOrEqualTo(withName.size());
  }

  private CampaignState buildMinimalState() {
    CampaignState state = new CampaignState();
    Campaign campaign = new Campaign();
    Wrestler wrestler = new Wrestler();
    wrestler.setReigns(new LinkedHashSet<>());
    wrestler.setWrestlerStates(new LinkedHashSet<>());
    campaign.setWrestler(wrestler);
    state.setCampaign(campaign);
    return state;
  }

  @Test
  void testIsChapterComplete() {
    CampaignState state = new CampaignState();
    state.setCurrentChapterId("beginning");
    state.setMatchesPlayed(0);
    state.setVictoryPoints(0);

    // Not complete yet
    assertThat(chapterService.isChapterComplete(state)).isFalse();

    // Meet criteria (3 matches, 5 VP)
    state.setMatchesPlayed(3);
    state.setVictoryPoints(5);
    assertThat(chapterService.isChapterComplete(state)).isTrue();
  }
}
