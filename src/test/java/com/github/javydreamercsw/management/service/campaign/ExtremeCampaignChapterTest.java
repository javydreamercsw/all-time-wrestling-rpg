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

  private StaticEncounterDTO findEncounterInPathA(final String encounterId) {
    return findChapter(PATH_A_ID)
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

    // Underground (8) + Championship (14) + Proving (4) + Finale (5) shared cards = 31 total
    assertThat(shared).hasSizeGreaterThanOrEqualTo(25);
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

  // ---------------------------------------------------------------------------
  // Win/loss branching — title shot loss creates a comeback arc
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Losing the title shot routes to the comeback_betrayal encounter")
  void titleShotLoss_routesToComebackBetrayal() {
    StaticEncounterDTO encounter = findEncounterInPathA("title_shot_earned");
    assertThat(encounter).as("title_shot_earned must exist in Path A").isNotNull();

    StaticEncounterDTO.StaticChoiceDTO choice = findChoice(encounter, "challenge_for_title");
    assertThat(choice).as("challenge_for_title choice must exist").isNotNull();
    assertThat(choice.getOnLossNextEncounterId())
        .as("Loss from title shot must route to comeback_betrayal")
        .isEqualTo("comeback_betrayal");
  }

  @Test
  @DisplayName("comeback_betrayal regroup choice routes to comeback_fan_support")
  void comebackArc_betray_routesToFanSupport() {
    StaticEncounterDTO encounter = findEncounterInPathA("comeback_betrayal");
    assertThat(encounter).as("comeback_betrayal must exist in Path A").isNotNull();

    StaticEncounterDTO.StaticChoiceDTO choice = findChoice(encounter, "regroup_alone");
    assertThat(choice).as("regroup_alone choice must exist in comeback_betrayal").isNotNull();
    assertThat(choice.getNextEncounterId())
        .as("Regroup choice must route to comeback_fan_support")
        .isEqualTo("comeback_fan_support");
  }

  @Test
  @DisplayName("comeback_fan_support routes to comeback_qualifier")
  void comebackArc_fanSupport_routesToQualifier() {
    StaticEncounterDTO encounter = findEncounterInPathA("comeback_fan_support");
    assertThat(encounter).as("comeback_fan_support must exist in Path A").isNotNull();

    StaticEncounterDTO.StaticChoiceDTO choice = findChoice(encounter, "channel_the_support");
    assertThat(choice).as("channel_the_support choice must exist").isNotNull();
    assertThat(choice.getNextEncounterId())
        .as("Fan support must route to comeback_qualifier")
        .isEqualTo("comeback_qualifier");
  }

  @Test
  @DisplayName("Winning comeback_qualifier routes back to title_shot_earned")
  void comebackArc_qualifier_winRoutesBackToTitleShot() {
    StaticEncounterDTO encounter = findEncounterInPathA("comeback_qualifier");
    assertThat(encounter).as("comeback_qualifier must exist in Path A").isNotNull();

    StaticEncounterDTO.StaticChoiceDTO choice = findChoice(encounter, "win_the_qualifier");
    assertThat(choice).as("win_the_qualifier choice must exist").isNotNull();
    assertThat(choice.getOnWinNextEncounterId())
        .as("Qualifier win must route back to title_shot_earned")
        .isEqualTo("title_shot_earned");
  }

  // ---------------------------------------------------------------------------
  // Proving Ground phase — new 4th phase encounters
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Proving Ground shared encounters exist in Path A")
  void provingGround_sharedEncountersExist() {
    Optional<CampaignChapterDTO> chapter = findChapter(PATH_A_ID);
    assertThat(chapter).isPresent();

    List<String> ids =
        chapter.get().getStaticEncounters().stream().map(StaticEncounterDTO::getId).toList();

    assertThat(ids)
        .as("All four Proving Ground shared encounters must be present")
        .contains(
            "proving_war_games",
            "proving_contract_signing_tlc",
            "proving_betrayal_setup",
            "proving_championship_press");
  }

  @Test
  @DisplayName("Proving Ground wrestler-specific encounters exist for all four wrestlers")
  void provingGround_wrestlerSpecificEncountersExist() {
    Optional<CampaignChapterDTO> chapter = findChapter(PATH_A_ID);
    assertThat(chapter).isPresent();

    List<String> ids =
        chapter.get().getStaticEncounters().stream().map(StaticEncounterDTO::getId).toList();

    assertThat(ids)
        .as("Proving Ground wrestler-specific encounters must exist for RVD, Sabu, Raven, Daemon")
        .contains(
            "rvd_proving_aerial",
            "sabu_proving_table",
            "raven_proving_monologue",
            "daemon_proving_pain");
  }

  @Test
  @DisplayName("Proving Ground RVD encounter has requiredWrestlerName = Rob Van Dam")
  void provingGround_rvd_hasWrestlerRestriction() {
    StaticEncounterDTO encounter = findEncounterInPathA("rvd_proving_aerial");
    assertThat(encounter).as("rvd_proving_aerial must exist").isNotNull();
    assertThat(encounter.getRequiredWrestlerName()).isEqualTo("Rob Van Dam");
  }

  @Test
  @DisplayName("Proving Ground Raven encounter has requiredWrestlerName = Raven")
  void provingGround_raven_hasWrestlerRestriction() {
    StaticEncounterDTO encounter = findEncounterInPathA("raven_proving_monologue");
    assertThat(encounter).as("raven_proving_monologue must exist").isNotNull();
    assertThat(encounter.getRequiredWrestlerName()).isEqualTo("Raven");
  }

  // ---------------------------------------------------------------------------
  // New match types — all six custom types present in Path A
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("All six new match types are present in at least one Path A encounter choice")
  void newMatchTypes_allSixPresentInPathA() {
    Optional<CampaignChapterDTO> chapter = findChapter(PATH_A_ID);
    assertThat(chapter).isPresent();

    List<String> allRules =
        chapter.get().getStaticEncounters().stream()
            .filter(e -> e.getChoices() != null)
            .flatMap(e -> e.getChoices().stream())
            .filter(c -> c.getSegmentRules() != null)
            .flatMap(c -> c.getSegmentRules().stream())
            .toList();

    assertThat(allRules)
        .as("Free-For-All must appear in at least one choice")
        .contains("Free-For-All");
    assertThat(allRules)
        .as("Elimination must appear in at least one choice")
        .contains("Elimination");
    assertThat(allRules)
        .as("Ladder Match must appear in at least one choice")
        .contains("Ladder Match");
    assertThat(allRules)
        .as("TLC must appear in at least one choice")
        .contains("Tables, Ladders and Chairs (TLC)");
    assertThat(allRules).as("Gauntlet must appear in at least one choice").contains("Gauntlet");
    assertThat(allRules)
        .as("War Games must appear in at least one choice")
        .contains("🪖 War Games");
  }

  // ---------------------------------------------------------------------------
  // Feature flags — choices that set flags used by exit criteria and branching
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("underground_locker_room_message sets accepted_locker_challenge featureFlag")
  void featureFlag_acceptedLockerChallenge_setByChoice() {
    StaticEncounterDTO encounter = findEncounterInPathA("underground_locker_room_message");
    assertThat(encounter).as("underground_locker_room_message must exist in Path A").isNotNull();

    boolean flagSet =
        encounter.getChoices().stream()
            .filter(c -> c.getFeatureFlags() != null)
            .anyMatch(
                c -> Boolean.TRUE.equals(c.getFeatureFlags().get("accepted_locker_challenge")));

    assertThat(flagSet)
        .as("A choice in underground_locker_room_message must set accepted_locker_challenge=true")
        .isTrue();
  }

  @Test
  @DisplayName("championship_dirty_politics sets challenged_booker featureFlag")
  void featureFlag_challengedBooker_setByChoice() {
    StaticEncounterDTO encounter = findEncounterInPathA("championship_dirty_politics");
    assertThat(encounter).as("championship_dirty_politics must exist in Path A").isNotNull();

    boolean flagSet =
        encounter.getChoices().stream()
            .filter(c -> c.getFeatureFlags() != null)
            .anyMatch(c -> Boolean.TRUE.equals(c.getFeatureFlags().get("challenged_booker")));

    assertThat(flagSet)
        .as("A choice in championship_dirty_politics must set challenged_booker=true")
        .isTrue();
  }

  @Test
  @DisplayName("championship_faction_offer sets refused_faction featureFlag")
  void featureFlag_refusedFaction_setByChoice() {
    StaticEncounterDTO encounter = findEncounterInPathA("championship_faction_offer");
    assertThat(encounter).as("championship_faction_offer must exist in Path A").isNotNull();

    boolean flagSet =
        encounter.getChoices().stream()
            .filter(c -> c.getFeatureFlags() != null)
            .anyMatch(c -> Boolean.TRUE.equals(c.getFeatureFlags().get("refused_faction")));

    assertThat(flagSet)
        .as("A choice in championship_faction_offer must set refused_faction=true")
        .isTrue();
  }

  @Test
  @DisplayName("proving_betrayal_setup sets survived_betrayal featureFlag")
  void featureFlag_survivedBetrayal_setByChoice() {
    StaticEncounterDTO encounter = findEncounterInPathA("proving_betrayal_setup");
    assertThat(encounter).as("proving_betrayal_setup must exist in Path A").isNotNull();

    boolean flagSet =
        encounter.getChoices().stream()
            .filter(c -> c.getFeatureFlags() != null)
            .anyMatch(c -> Boolean.TRUE.equals(c.getFeatureFlags().get("survived_betrayal")));

    assertThat(flagSet)
        .as("A choice in proving_betrayal_setup must set survived_betrayal=true")
        .isTrue();
  }

  @Test
  @DisplayName("raven_proving_monologue sets raven_monologue featureFlag")
  void featureFlag_ravenMonologue_setByChoice() {
    StaticEncounterDTO encounter = findEncounterInPathA("raven_proving_monologue");
    assertThat(encounter).as("raven_proving_monologue must exist in Path A").isNotNull();

    boolean flagSet =
        encounter.getChoices().stream()
            .filter(c -> c.getFeatureFlags() != null)
            .anyMatch(c -> Boolean.TRUE.equals(c.getFeatureFlags().get("raven_monologue")));

    assertThat(flagSet)
        .as("A choice in raven_proving_monologue must set raven_monologue=true")
        .isTrue();
  }
}
