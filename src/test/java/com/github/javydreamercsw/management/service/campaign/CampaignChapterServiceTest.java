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
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.dto.campaign.CampaignChapterDTO;
import com.github.javydreamercsw.management.dto.campaign.ChapterCriteriaDTO;
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

  private CampaignState buildMinimalState() {
    CampaignState state = new CampaignState();
    Campaign campaign = new Campaign();
    Wrestler wrestler = new Wrestler();
    wrestler.setReigns(new java.util.LinkedHashSet<>());
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
