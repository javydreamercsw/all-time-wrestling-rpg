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
import com.github.javydreamercsw.management.dto.campaign.StaticEncounterDTO.StaticChoiceDTO.BonusVpCondition;
import com.github.javydreamercsw.management.service.expansion.ExpansionService;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

class AllOrNothingCampaignChapterTest {

  private static final String CHAPTER_ID = "beginning";
  private static final List<String> AON_WRESTLERS =
      List.of("Randy Savage", "The British Bulldog", "Kurt Angle");

  private CampaignChapterService chapterService;

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

  private Optional<CampaignChapterDTO> findChapter() {
    return chapterService.getAllChapters().stream()
        .filter(ch -> CHAPTER_ID.equals(ch.getId()))
        .findFirst();
  }

  private StaticEncounterDTO findEncounter(final String encounterId) {
    return findChapter()
        .map(
            ch ->
                ch.getStaticEncounters().stream()
                    .filter(e -> encounterId.equals(e.getId()))
                    .findFirst()
                    .orElse(null))
        .orElse(null);
  }

  private StaticEncounterDTO.StaticChoiceDTO findChoice(
      final StaticEncounterDTO encounter, final String choiceId) {
    if (encounter == null || encounter.getChoices() == null) {
      return null;
    }
    return encounter.getChoices().stream()
        .filter(c -> choiceId.equals(c.getId()))
        .findFirst()
        .orElse(null);
  }

  // ---------------------------------------------------------------------------
  // Wrestler gating
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("AON chapter is visible to all three allowed wrestlers at VP=0")
  void aon_visibleToAllThreeWrestlers() {
    for (String wrestler : AON_WRESTLERS) {
      List<CampaignChapterDTO> available =
          chapterService.findAvailableChapters(baseState(), wrestler);
      assertThat(available)
          .as("AON chapter must be visible to %s", wrestler)
          .extracting(CampaignChapterDTO::getId)
          .contains(CHAPTER_ID);
    }
  }

  @Test
  @DisplayName("AON chapter is NOT visible to wrestlers outside the allowed list")
  void aon_notVisibleToOtherWrestlers() {
    List<CampaignChapterDTO> available =
        chapterService.findAvailableChapters(baseState(), "Oda Tsurugi");
    assertThat(available).extracting(CampaignChapterDTO::getId).doesNotContain(CHAPTER_ID);
  }

  // ---------------------------------------------------------------------------
  // Encounter presence and requiredWrestlerName filtering
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Randy Savage encounters all carry requiredWrestlerName = Randy Savage")
  void randySavage_encountersHaveWrestlerFilter() {
    Optional<CampaignChapterDTO> chapter = findChapter();
    assertThat(chapter).isPresent();

    List<StaticEncounterDTO> rsEncounters =
        chapter.get().getStaticEncounters().stream()
            .filter(e -> "Randy Savage".equals(e.getRequiredWrestlerName()))
            .toList();

    assertThat(rsEncounters).as("Randy Savage must have wrestler-filtered encounters").isNotEmpty();
  }

  @Test
  @DisplayName("British Bulldog encounters all carry requiredWrestlerName = British Bulldog")
  void britishBulldog_encountersHaveWrestlerFilter() {
    Optional<CampaignChapterDTO> chapter = findChapter();
    assertThat(chapter).isPresent();

    List<StaticEncounterDTO> bbEncounters =
        chapter.get().getStaticEncounters().stream()
            .filter(e -> "British Bulldog".equals(e.getRequiredWrestlerName()))
            .toList();

    assertThat(bbEncounters)
        .as("British Bulldog must have wrestler-filtered encounters")
        .isNotEmpty();
  }

  @Test
  @DisplayName("Kurt Angle encounters all carry requiredWrestlerName = Kurt Angle")
  void kurtAngle_encountersHaveWrestlerFilter() {
    Optional<CampaignChapterDTO> chapter = findChapter();
    assertThat(chapter).isPresent();

    List<StaticEncounterDTO> kaEncounters =
        chapter.get().getStaticEncounters().stream()
            .filter(e -> "Kurt Angle".equals(e.getRequiredWrestlerName()))
            .toList();

    assertThat(kaEncounters).as("Kurt Angle must have wrestler-filtered encounters").isNotEmpty();
  }

  // ---------------------------------------------------------------------------
  // Randy Savage — face/heel branch (rs_part_4)
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("rs_part_4 face choice has alignmentShift +1 and routes to rs_part_5a")
  void randySavage_faceChoice_alignmentAndRouting() {
    StaticEncounterDTO encounter = findEncounter("rs_part_4");
    assertThat(encounter).as("rs_part_4 must exist").isNotNull();

    StaticEncounterDTO.StaticChoiceDTO face = findChoice(encounter, "rs_part_4_face");
    assertThat(face).as("rs_part_4_face choice must exist").isNotNull();
    assertThat(face.getAlignmentShift()).as("face choice must give +1 alignment").isEqualTo(1);
    assertThat(face.getNextEncounterId())
        .as("face choice must route to rs_part_5a")
        .isEqualTo("rs_part_5a");
  }

  @Test
  @DisplayName("rs_part_4 heel choice has alignmentShift -1 and routes to rs_part_5b")
  void randySavage_heelChoice_alignmentAndRouting() {
    StaticEncounterDTO encounter = findEncounter("rs_part_4");
    assertThat(encounter).as("rs_part_4 must exist").isNotNull();

    StaticEncounterDTO.StaticChoiceDTO heel = findChoice(encounter, "rs_part_4_heel");
    assertThat(heel).as("rs_part_4_heel choice must exist").isNotNull();
    assertThat(heel.getAlignmentShift()).as("heel choice must give -1 alignment").isEqualTo(-1);
    assertThat(heel.getNextEncounterId())
        .as("heel choice must route to rs_part_5b")
        .isEqualTo("rs_part_5b");
  }

  // ---------------------------------------------------------------------------
  // Randy Savage — must-win retry loop (rs_part_6a)
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("rs_part_6a loss routes to rs_part_6a_retry")
  void randySavage_mustWin_lossRoutesToRetry() {
    StaticEncounterDTO encounter = findEncounter("rs_part_6a");
    assertThat(encounter).as("rs_part_6a must exist").isNotNull();

    StaticEncounterDTO.StaticChoiceDTO match = findChoice(encounter, "rs_part_6a_match");
    assertThat(match).as("rs_part_6a_match choice must exist").isNotNull();
    assertThat(match.getOnLossNextEncounterId())
        .as("Loss from rs_part_6a must route to retry")
        .isEqualTo("rs_part_6a_retry");
  }

  @Test
  @DisplayName("rs_part_6a_retry deducts 2 VP and loops back to rs_part_6a")
  void randySavage_retry_deductsVpAndLoopsBack() {
    StaticEncounterDTO retry = findEncounter("rs_part_6a_retry");
    assertThat(retry).as("rs_part_6a_retry must exist").isNotNull();

    StaticEncounterDTO.StaticChoiceDTO go = findChoice(retry, "rs_part_6a_retry_go");
    assertThat(go).as("rs_part_6a_retry_go choice must exist").isNotNull();
    assertThat(go.getVpReward()).as("retry must deduct 2 VP").isEqualTo(-2);
    assertThat(go.getNextEncounterId())
        .as("retry must loop back to rs_part_6a")
        .isEqualTo("rs_part_6a");
  }

  // ---------------------------------------------------------------------------
  // British Bulldog — bonus VP on bb_part_4 (pinfall + finisher)
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("bb_part_4 match choice has bonusVpCondition: +2 VP for pinfall win with finisher")
  void britishBulldog_part4_hasBonusVpCondition() {
    StaticEncounterDTO encounter = findEncounter("bb_part_4");
    assertThat(encounter).as("bb_part_4 must exist").isNotNull();

    StaticEncounterDTO.StaticChoiceDTO match = findChoice(encounter, "bb_part_4_match");
    assertThat(match).as("bb_part_4_match choice must exist").isNotNull();
    assertThat(match.getBonusVpConditions())
        .as("bb_part_4_match must have bonus VP conditions")
        .isNotEmpty();

    BonusVpCondition condition = match.getBonusVpConditions().get(0);
    assertThat(condition.getVpReward()).as("bonus VP reward must be 2").isEqualTo(2);
    assertThat(condition.isRequireWin()).as("bonus must require a win").isTrue();
    assertThat(condition.getFinishType()).as("bonus must require PINFALL").isEqualTo("PINFALL");
    assertThat(condition.isRequireFinisherCard())
        .as("bonus must require the finisher card")
        .isTrue();
  }

  // ---------------------------------------------------------------------------
  // Kurt Angle — bonus VP on ka_part_3 (card execution groups)
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName(
      "ka_part_3 match choice has bonusVpCondition: +2 VP for 2+ German Suplexes + Olympic Slam")
  void kurtAngle_part3_hasBonusVpConditionWithCardGroups() {
    StaticEncounterDTO encounter = findEncounter("ka_part_3");
    assertThat(encounter).as("ka_part_3 must exist").isNotNull();

    StaticEncounterDTO.StaticChoiceDTO match = findChoice(encounter, "ka_part_3_match");
    assertThat(match).as("ka_part_3_match choice must exist").isNotNull();
    assertThat(match.getBonusVpConditions())
        .as("ka_part_3_match must have bonus VP conditions")
        .isNotEmpty();

    BonusVpCondition condition = match.getBonusVpConditions().get(0);
    assertThat(condition.getVpReward()).as("bonus VP reward must be 2").isEqualTo(2);
    assertThat(condition.isRequireWin()).as("bonus must NOT require a win").isFalse();
    assertThat(condition.getCardGroups())
        .as("condition must have two card execution groups")
        .hasSize(2);

    BonusVpCondition.CardExecutionGroup germanGroup = condition.getCardGroups().get(0);
    assertThat(germanGroup.getMinCount()).as("German Suplex group needs min 2").isEqualTo(2);
    assertThat(germanGroup.getAnyOf())
        .as("German Suplex group must include at least one German Suplex variant")
        .contains("German Suplex");

    BonusVpCondition.CardExecutionGroup slamGroup = condition.getCardGroups().get(1);
    assertThat(slamGroup.getMinCount()).as("Olympic Slam group needs min 1").isEqualTo(1);
    assertThat(slamGroup.getAnyOf())
        .as("Olympic Slam group must include Olympic Slam")
        .contains("Olympic Slam");
  }
}
