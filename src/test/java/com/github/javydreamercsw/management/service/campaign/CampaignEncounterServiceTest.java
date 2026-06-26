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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.SegmentNarrationServiceFactory;
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignEncounter;
import com.github.javydreamercsw.management.domain.campaign.CampaignEncounterRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignStoryline;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment;
import com.github.javydreamercsw.management.domain.commentator.CommentatorRepository;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.dto.campaign.CampaignChapterDTO;
import com.github.javydreamercsw.management.dto.campaign.CampaignEncounterResponseDTO;
import com.github.javydreamercsw.management.dto.campaign.StaticEncounterDTO;
import com.github.javydreamercsw.management.dto.campaign.StaticEncounterDTO.StaticChoiceDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CampaignEncounterServiceTest {

  @Mock private SegmentNarrationServiceFactory aiFactory;
  @Mock private CampaignEncounterRepository encounterRepository;
  @Mock private CampaignStateRepository stateRepository;
  @Mock private CampaignChapterService chapterService;
  @Mock private CampaignService campaignService;
  @Mock private StorylineDirectorService storylineDirectorService;
  @Mock private WrestlerStatusService wrestlerStatusService;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private TeamRepository teamRepository;
  @Mock private FactionRepository factionRepository;
  @Mock private CommentatorRepository commentatorRepository;
  @Mock private FeatureDataService featureDataService;

  @Mock
  private com.github.javydreamercsw.management.service.expansion.ExpansionService expansionService;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @InjectMocks private CampaignEncounterService encounterService;

  private Campaign campaign;
  private CampaignChapterDTO chapter;

  @BeforeEach
  public void setUp() {
    encounterService =
        new CampaignEncounterService(
            aiFactory,
            encounterRepository,
            stateRepository,
            campaignService,
            storylineDirectorService,
            wrestlerStatusService,
            wrestlerRepository,
            commentatorRepository,
            objectMapper,
            featureDataService,
            expansionService);

    Wrestler wrestler = new Wrestler();
    wrestler.setName("Test Wrestler");
    WrestlerAlignment alignment = new WrestlerAlignment();
    alignment.setAlignmentType(AlignmentType.NEUTRAL);
    alignment.setLevel(0);
    wrestler.setAlignment(alignment);

    CampaignState state = new CampaignState();
    state.setCurrentChapterId("beginning");

    campaign = new Campaign();
    campaign.setWrestler(wrestler);
    campaign.setState(state);

    chapter =
        CampaignChapterDTO.builder()
            .id("beginning")
            .title("Chapter 1")
            .aiSystemPrompt("Test Prompt")
            .build();
  }

  @Test
  void testGenerateEncounter() {
    when(campaignService.getCurrentChapter(campaign)).thenReturn(Optional.of(chapter));
    when(encounterRepository.findByCampaignOrderByEncounterDateAsc(campaign))
        .thenReturn(new ArrayList<>());

    String aiJsonResponse =
        """
        {"narrative": "Story", "choices": [{"text": "Choice 1", "label": "BTN",\
         "alignmentShift": 1, "vpReward": 0, "nextPhase": "MATCH", "matchType":\
         "One on One"}]}\
        """;
    when(aiFactory.generateText(anyString())).thenReturn(aiJsonResponse);

    CampaignEncounterResponseDTO response = encounterService.generateEncounter(campaign);

    assertThat(response.getNarrative()).isEqualTo("Story");
    assertThat(response.getChoices()).hasSize(1);
    assertThat(response.getChoices().get(0).getLabel()).isEqualTo("BTN");

    verify(encounterRepository).save(org.mockito.ArgumentMatchers.any(CampaignEncounter.class));
  }

  private CampaignChapterDTO staticChapterWith(StaticEncounterDTO... encounters) {
    return CampaignChapterDTO.builder()
        .id("test-chapter")
        .staticEncounters(List.of(encounters))
        .build();
  }

  private StaticEncounterDTO encounter(String id, String narrative, StaticChoiceDTO... choices) {
    return StaticEncounterDTO.builder()
        .id(id)
        .title("Title " + id)
        .narrativeText(narrative)
        .choices(List.of(choices))
        .build();
  }

  private StaticChoiceDTO choice(String text) {
    return StaticChoiceDTO.builder().text(text).label("BTN").build();
  }

  @Test
  void testGenerateStaticEncounter_idBasedRouting() {
    StaticEncounterDTO enc1 = encounter("card1", "Narrative A", choice("Go to A"));
    StaticEncounterDTO enc2 = encounter("card2", "Narrative B", choice("Go to B"));
    CampaignChapterDTO ch = staticChapterWith(enc1, enc2);
    campaign.getState().setCurrentEncounterId("card2");

    CampaignEncounterResponseDTO response = encounterService.generateStaticEncounter(campaign, ch);

    assertThat(response.getNarrative()).contains("Narrative B");
  }

  @Test
  void testGenerateStaticEncounter_pendingWinRouting_resolvesToWinCard() {
    StaticEncounterDTO winCard = encounter("cardWin", "You won!", choice("Continue"));
    StaticEncounterDTO lossCard = encounter("cardLoss", "You lost.", choice("Continue"));
    CampaignChapterDTO ch = staticChapterWith(winCard, lossCard);

    doReturn("cardWin")
        .when(featureDataService)
        .getFeatureValue(any(), eq("_pendingWinCard"), eq(String.class), any());
    doReturn("cardLoss")
        .when(featureDataService)
        .getFeatureValue(any(), eq("_pendingLossCard"), eq(String.class), any());
    doReturn(0)
        .when(featureDataService)
        .getFeatureValue(any(), eq("_preMatchWins"), eq(Integer.class), any());
    campaign.getState().setWins(1); // wins(1) > preMatchWins(0) → won

    CampaignEncounterResponseDTO response = encounterService.generateStaticEncounter(campaign, ch);

    assertThat(response.getNarrative()).contains("You won!");
    assertThat(campaign.getState().getCurrentEncounterId()).isEqualTo("cardWin");
  }

  @Test
  void testGenerateStaticEncounter_pendingLossRouting_resolvesToLossCard() {
    StaticEncounterDTO winCard = encounter("cardWin", "You won!", choice("Continue"));
    StaticEncounterDTO lossCard = encounter("cardLoss", "You lost.", choice("Continue"));
    CampaignChapterDTO ch = staticChapterWith(winCard, lossCard);

    doReturn("cardWin")
        .when(featureDataService)
        .getFeatureValue(any(), eq("_pendingWinCard"), eq(String.class), any());
    doReturn("cardLoss")
        .when(featureDataService)
        .getFeatureValue(any(), eq("_pendingLossCard"), eq(String.class), any());
    doReturn(0)
        .when(featureDataService)
        .getFeatureValue(any(), eq("_preMatchWins"), eq(Integer.class), any());
    campaign.getState().setWins(0); // wins(0) == preMatchWins(0) → loss

    CampaignEncounterResponseDTO response = encounterService.generateStaticEncounter(campaign, ch);

    assertThat(response.getNarrative()).contains("You lost.");
  }

  @Test
  void testGenerateStaticEncounter_expansionGatedEncounterSkipped() {
    StaticEncounterDTO base = encounter("base-enc", "Base story", choice("Go"));
    StaticEncounterDTO gated =
        StaticEncounterDTO.builder()
            .id("gated-enc")
            .title("Gated")
            .narrativeText("Expansion story")
            .requiredExpansion("EDDIE")
            .choices(List.of(choice("Go")))
            .build();
    CampaignChapterDTO ch = staticChapterWith(base, gated);
    when(expansionService.isExpansionEnabled("EDDIE")).thenReturn(false);
    when(encounterRepository.countByCampaignAndChapterId(campaign, "test-chapter")).thenReturn(0L);

    CampaignEncounterResponseDTO response = encounterService.generateStaticEncounter(campaign, ch);

    assertThat(response.getNarrative()).contains("Base story");
  }

  @Test
  void testGenerateStaticEncounter_expansionGatedChoiceFiltered() {
    StaticChoiceDTO baseChoice = choice("Base choice");
    StaticChoiceDTO gatedChoice =
        StaticChoiceDTO.builder()
            .text("Expansion choice")
            .label("EXP")
            .requiredExpansion("EDDIE")
            .build();
    StaticEncounterDTO enc =
        StaticEncounterDTO.builder()
            .id("enc1")
            .title("Enc")
            .narrativeText("Story")
            .choices(List.of(baseChoice, gatedChoice))
            .build();
    CampaignChapterDTO ch = staticChapterWith(enc);
    campaign.getState().setCurrentEncounterId("enc1");
    when(expansionService.isExpansionEnabled("EDDIE")).thenReturn(false);

    CampaignEncounterResponseDTO response = encounterService.generateStaticEncounter(campaign, ch);

    assertThat(response.getChoices()).hasSize(1);
    assertThat(response.getChoices().get(0).getText()).isEqualTo("Base choice");
  }

  @Test
  void testRecordEncounterChoice_nextEncounterId_setsCurrentEncounterId() {
    CampaignEncounter encounter = new CampaignEncounter();
    encounter.setNarrativeText("Story");
    when(encounterRepository.findByCampaignOrderByEncounterDateAsc(campaign))
        .thenReturn(List.of(encounter));

    CampaignEncounterResponseDTO.Choice choice = new CampaignEncounterResponseDTO.Choice();
    choice.setText("Take the shortcut");
    choice.setNextPhase("BACKSTAGE");
    choice.setNextEncounterId("card-47");

    encounterService.recordEncounterChoice(campaign, choice);

    assertThat(campaign.getState().getCurrentEncounterId()).isEqualTo("card-47");
    verify(stateRepository).save(campaign.getState());
  }

  @Test
  void testRecordEncounterChoice_matchWithRouting_storesPendingCards() {
    CampaignEncounter encounter = new CampaignEncounter();
    encounter.setNarrativeText("Story");
    when(encounterRepository.findByCampaignOrderByEncounterDateAsc(campaign))
        .thenReturn(List.of(encounter));

    CampaignEncounterResponseDTO.Choice choice = new CampaignEncounterResponseDTO.Choice();
    choice.setText("Challenge him");
    choice.setNextPhase("MATCH");
    choice.setOnWinNextEncounterId("card-win");
    choice.setOnLossNextEncounterId("card-loss");
    choice.setVpReward(3);
    campaign.getState().setWins(2);

    encounterService.recordEncounterChoice(campaign, choice);

    verify(featureDataService).setFeatureValue(campaign.getState(), "_pendingWinCard", "card-win");
    verify(featureDataService)
        .setFeatureValue(campaign.getState(), "_pendingLossCard", "card-loss");
    verify(featureDataService).setFeatureValue(campaign.getState(), "_preMatchWins", 2);
    assertThat(campaign.getState().getCurrentEncounterId()).isNull();
  }

  @Test
  void testGenerateStaticEncounter_matchChoice_propagatesMatchSetupFields() {
    // match-type, rules, and forcedOpponentName must flow through to the response DTO
    StaticChoiceDTO matchChoice =
        StaticChoiceDTO.builder()
            .text("Step into the ring")
            .label("Fight")
            .nextPhase(com.github.javydreamercsw.management.domain.campaign.CampaignPhase.MATCH)
            .matchType("One on One")
            .segmentRules(List.of("Normal"))
            .forcedOpponentName("The Villain")
            .build();
    StaticEncounterDTO enc = encounter("first_match", "Your debut awaits.", matchChoice);
    CampaignChapterDTO ch = staticChapterWith(enc);
    campaign.getState().setCurrentEncounterId("first_match");

    CampaignEncounterResponseDTO response = encounterService.generateStaticEncounter(campaign, ch);

    assertThat(response.getChoices()).hasSize(1);
    CampaignEncounterResponseDTO.Choice choice = response.getChoices().get(0);
    assertThat(choice.getNextPhase()).isEqualTo("MATCH");
    assertThat(choice.getMatchType()).isEqualTo("One on One");
    assertThat(choice.getSegmentRules()).containsExactly("Normal");
    assertThat(choice.getForcedOpponentName()).isEqualTo("The Villain");
  }

  @Test
  void testRecordEncounterChoice_matchNoRouting_doesNotSetPendingCards() {
    // A simple MATCH choice with no win/loss routing should not store pending cards
    // and should NOT set currentEncounterId to null
    CampaignEncounter encounter = new CampaignEncounter();
    encounter.setNarrativeText("Story");
    when(encounterRepository.findByCampaignOrderByEncounterDateAsc(campaign))
        .thenReturn(List.of(encounter));

    CampaignEncounterResponseDTO.Choice choice = new CampaignEncounterResponseDTO.Choice();
    choice.setText("Step into the ring");
    choice.setNextPhase("MATCH");
    // no onWinNextEncounterId / onLossNextEncounterId
    campaign.getState().setCurrentEncounterId("first_match");

    encounterService.recordEncounterChoice(campaign, choice);

    // Pending cards must NOT be stored when there is no routing
    verify(featureDataService, org.mockito.Mockito.never())
        .setFeatureValue(any(), eq("_pendingWinCard"), any());
    verify(featureDataService, org.mockito.Mockito.never())
        .setFeatureValue(any(), eq("_pendingLossCard"), any());
    // currentEncounterId must remain unchanged (not cleared) for sequential fallback to work
    assertThat(campaign.getState().getCurrentEncounterId()).isEqualTo("first_match");
    verify(stateRepository).save(campaign.getState());
  }

  @Test
  void testRecordEncounterChoice() {
    CampaignEncounter encounter = new CampaignEncounter();
    encounter.setNarrativeText("Story");
    when(encounterRepository.findByCampaignOrderByEncounterDateAsc(campaign))
        .thenReturn(List.of(encounter));

    CampaignEncounterResponseDTO.Choice choice = new CampaignEncounterResponseDTO.Choice();
    choice.setText("Choice 1");
    choice.setAlignmentShift(-1);
    choice.setVpReward(5);
    choice.setNextPhase("MATCH");

    encounterService.recordEncounterChoice(campaign, choice);

    assertThat(encounter.getPlayerChoice()).isEqualTo("Choice 1");
    assertThat(encounter.getAlignmentShift()).isEqualTo(-1);
    assertThat(encounter.getVpReward()).isEqualTo(5);

    verify(campaignService).shiftAlignment(campaign, -1);
    verify(stateRepository).save(campaign.getState());
  }

  @Test
  void recordEncounterChoice_intendedPath_evaluatesProgressWithSuccess() {
    CampaignStoryline storyline = new CampaignStoryline();
    storyline.setStatus(CampaignStoryline.StorylineStatus.ACTIVE);
    campaign.getState().setActiveStoryline(storyline);

    CampaignEncounter encounter = new CampaignEncounter();
    when(encounterRepository.findByCampaignOrderByEncounterDateAsc(campaign))
        .thenReturn(List.of(encounter));

    CampaignEncounterResponseDTO.Choice choice =
        CampaignEncounterResponseDTO.Choice.builder()
            .text("Right choice")
            .intendedPath(true)
            .build();

    encounterService.recordEncounterChoice(campaign, choice);

    verify(storylineDirectorService).evaluateProgress(campaign, true);
  }

  @Test
  void recordEncounterChoice_failurePath_evaluatesProgressWithFailure() {
    CampaignStoryline storyline = new CampaignStoryline();
    storyline.setStatus(CampaignStoryline.StorylineStatus.ACTIVE);
    campaign.getState().setActiveStoryline(storyline);

    CampaignEncounter encounter = new CampaignEncounter();
    when(encounterRepository.findByCampaignOrderByEncounterDateAsc(campaign))
        .thenReturn(List.of(encounter));

    CampaignEncounterResponseDTO.Choice choice =
        CampaignEncounterResponseDTO.Choice.builder()
            .text("Wrong choice")
            .intendedPath(false)
            .build();

    encounterService.recordEncounterChoice(campaign, choice);

    verify(storylineDirectorService).evaluateProgress(campaign, false);
  }

  @Test
  void recordEncounterChoice_matchChoice_doesNotEvaluateStorylineProgress() {
    CampaignStoryline storyline = new CampaignStoryline();
    storyline.setStatus(CampaignStoryline.StorylineStatus.ACTIVE);
    campaign.getState().setActiveStoryline(storyline);

    CampaignEncounter encounter = new CampaignEncounter();
    when(encounterRepository.findByCampaignOrderByEncounterDateAsc(campaign))
        .thenReturn(List.of(encounter));

    // MATCH choices are evaluated later via POST_MATCH processing — not here
    CampaignEncounterResponseDTO.Choice choice =
        CampaignEncounterResponseDTO.Choice.builder()
            .text("Fight!")
            .nextPhase("MATCH")
            .intendedPath(true)
            .build();

    encounterService.recordEncounterChoice(campaign, choice);

    org.mockito.Mockito.verifyNoInteractions(storylineDirectorService);
  }

  @Test
  void staticChoice_intendedPath_defaultsToTrue() {
    StaticChoiceDTO sc = StaticChoiceDTO.builder().text("A choice").label("BTN").build();
    assertThat(sc.isIntendedPath()).isTrue();
  }

  @Test
  void staticChoice_intendedPath_canBeSetFalse() {
    StaticChoiceDTO sc =
        StaticChoiceDTO.builder().text("Wrong path").label("BTN").intendedPath(false).build();
    assertThat(sc.isIntendedPath()).isFalse();
  }
}
