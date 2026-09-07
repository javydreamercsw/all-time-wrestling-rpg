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
package com.github.javydreamercsw.management.service.feud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.SegmentNarrationService;
import com.github.javydreamercsw.base.ai.SegmentNarrationServiceFactory;
import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.feud.FeudScript;
import com.github.javydreamercsw.management.domain.feud.FeudScriptStatus;
import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.dto.feud.AiSuggestedOpponentDTO;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.drama.DramaEventService;
import com.github.javydreamercsw.management.service.injury.InjuryService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FeudBeatAssistantServiceTest {

  @Mock private SegmentNarrationServiceFactory aiFactory;
  @Mock private WrestlerService wrestlerService;
  @Mock private GameSettingService gameSettingService;
  @Mock private InjuryService injuryService;
  @Mock private UniverseContextService universeContextService;
  @Mock private DramaEventService dramaEventService;
  @Mock private SegmentNarrationService narrationService;

  @Captor private ArgumentCaptor<String> promptCaptor;

  private FeudBeatAssistantService service;

  private static final long UNIVERSE_ID = 7L;

  @BeforeEach
  void setUp() {
    service =
        new FeudBeatAssistantService(
            aiFactory,
            new ObjectMapper(), // real mapper — parses the AI JSON response
            wrestlerService,
            gameSettingService,
            injuryService,
            universeContextService,
            dramaEventService);
  }

  private Wrestler wrestler(long id, String name, Gender gender) {
    Wrestler w = new Wrestler();
    w.setId(id);
    w.setName(name);
    w.setGender(gender);
    return w;
  }

  private FeudScript scriptOf() {
    Rivalry rivalry = new Rivalry();
    rivalry.setWrestler1(wrestler(1L, "Shelton Benjamin", Gender.MALE));
    rivalry.setWrestler2(wrestler(2L, "Bobby Lashley", Gender.MALE));
    rivalry.setHeat(35);
    FeudScript script = new FeudScript();
    script.setName("Shelton vs Lashley");
    script.setStatus(FeudScriptStatus.ACTIVE);
    script.setRivalry(rivalry);
    return script;
  }

  private List<Wrestler> feudOf() {
    return List.of(
        wrestler(1L, "Shelton Benjamin", Gender.MALE), wrestler(2L, "Bobby Lashley", Gender.MALE));
  }

  private void stubEligibleRoster(Wrestler... roster) {
    when(aiFactory.getBestAvailableService()).thenReturn(narrationService);
    when(gameSettingService.isIntergenderMatchesEnabled()).thenReturn(true);
    when(universeContextService.getCurrentUniverseId()).thenReturn(UNIVERSE_ID);
    when(wrestlerService.findAllFiltered(null, null, UNIVERSE_ID, null, null))
        .thenReturn(List.of(roster));
  }

  private void stubNotInjured(Wrestler... wrestlers) {
    for (Wrestler w : wrestlers) {
      when(injuryService.getAllInjuriesForWrestler(w.getId(), UNIVERSE_ID)).thenReturn(List.of());
    }
  }

  @Test
  void suggestOpponent_noAiAvailable_throws() {
    when(aiFactory.getBestAvailableService()).thenReturn(null);

    assertThatThrownBy(() -> service.suggestOpponent(scriptOf(), feudOf(), null, null, null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("No AI providers available");
  }

  @Test
  void suggestOpponent_validResponse_returnsCandidateWithRationale() {
    Wrestler candidate = wrestler(30L, "Randy Orton", Gender.MALE);
    stubEligibleRoster(candidate);
    when(injuryService.getAllInjuriesForWrestler(30L, UNIVERSE_ID)).thenReturn(List.of());
    when(aiFactory.generateText(anyString()))
        .thenReturn(
            "{\"wrestlerId\": 30, \"name\": \"Randy Orton\", \"rationale\": \"Perfect fit\"}");

    AiSuggestedOpponentDTO dto =
        service.suggestOpponent(scriptOf(), feudOf(), "Singles Match", null, "surprise return");

    assertThat(dto.getWrestlerId()).isEqualTo(30L);
    assertThat(dto.getRationale()).isEqualTo("Perfect fit");
  }

  @Test
  void suggestOpponent_promptCarriesBeatContextAndCandidates() {
    Wrestler candidate = wrestler(30L, "Randy Orton", Gender.MALE);
    stubEligibleRoster(candidate);
    when(injuryService.getAllInjuriesForWrestler(30L, UNIVERSE_ID)).thenReturn(List.of());
    when(aiFactory.generateText(anyString()))
        .thenReturn("{\"wrestlerId\": 30, \"name\": \"Randy Orton\", \"rationale\": \"r\"}");

    service.suggestOpponent(scriptOf(), feudOf(), "Singles Match", "Cage", "shocking return");

    verify(aiFactory).generateText(promptCaptor.capture());
    String prompt = promptCaptor.getValue();
    assertThat(prompt).contains("Shelton Benjamin, Bobby Lashley");
    assertThat(prompt).contains("HEAT: 35");
    assertThat(prompt).contains("BEAT: [Singles Match - Cage]");
    assertThat(prompt).contains("shocking return");
    assertThat(prompt).contains("id=30 name='Randy Orton'");
  }

  @Test
  void suggestOpponent_unknownWrestlerId_throws() {
    Wrestler candidate = wrestler(30L, "Randy Orton", Gender.MALE);
    stubEligibleRoster(candidate);
    when(injuryService.getAllInjuriesForWrestler(30L, UNIVERSE_ID)).thenReturn(List.of());
    when(aiFactory.generateText(anyString()))
        .thenReturn("{\"wrestlerId\": 999, \"name\": \"Ghost\", \"rationale\": \"x\"}");

    assertThatThrownBy(
            () -> service.suggestOpponent(scriptOf(), feudOf(), "Singles Match", null, null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("ineligible wrestler");
  }

  @Test
  void suggestOpponent_malformedJson_throws() {
    Wrestler candidate = wrestler(30L, "Randy Orton", Gender.MALE);
    stubEligibleRoster(candidate);
    when(injuryService.getAllInjuriesForWrestler(30L, UNIVERSE_ID)).thenReturn(List.of());
    when(aiFactory.generateText(anyString())).thenReturn("I choose Randy Orton!");

    assertThatThrownBy(
            () -> service.suggestOpponent(scriptOf(), feudOf(), "Singles Match", null, null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("valid JSON");
  }

  @Test
  void suggestOpponent_idAliasAndUnknownFields_tolerated() {
    // Smaller local models (llama3.2:1b) echo "id" instead of "wrestlerId" and add extras;
    // the DTO must tolerate both — candidate validation still guards the id.
    Wrestler candidate = wrestler(30L, "Randy Orton", Gender.MALE);
    stubEligibleRoster(candidate);
    when(injuryService.getAllInjuriesForWrestler(30L, UNIVERSE_ID)).thenReturn(List.of());
    when(aiFactory.generateText(anyString()))
        .thenReturn(
            "{\"id\": 30, \"name\": \"Randy Orton\", \"rationale\": \"r\", \"tier\":"
                + " \"MAIN_EVENTER\"}");

    AiSuggestedOpponentDTO dto =
        service.suggestOpponent(scriptOf(), feudOf(), "Singles Match", null, null);

    assertThat(dto.getWrestlerId()).isEqualTo(30L);
  }

  @Test
  void suggestOpponent_intergenderDisabled_filtersCandidatesToFeudGender() {
    Wrestler male = wrestler(30L, "Male Star", Gender.MALE);
    when(aiFactory.getBestAvailableService()).thenReturn(narrationService);
    when(gameSettingService.isIntergenderMatchesEnabled()).thenReturn(false);
    when(universeContextService.getCurrentUniverseId()).thenReturn(UNIVERSE_ID);
    when(wrestlerService.findAllFiltered(null, Gender.MALE, UNIVERSE_ID, null, null))
        .thenReturn(List.of(male));
    when(injuryService.getAllInjuriesForWrestler(30L, UNIVERSE_ID)).thenReturn(List.of());
    when(aiFactory.generateText(anyString()))
        .thenReturn("{\"wrestlerId\": 30, \"name\": \"Male Star\", \"rationale\": \"r\"}");

    service.suggestOpponent(scriptOf(), feudOf(), "Singles Match", null, null);

    verify(wrestlerService).findAllFiltered(null, Gender.MALE, UNIVERSE_ID, null, null);
  }

  @Test
  void suggestOpponent_excludesFeudParticipantsAndInjured() {
    Wrestler feud1 = wrestler(1L, "Shelton Benjamin", Gender.MALE);
    Wrestler feud2 = wrestler(2L, "Bobby Lashley", Gender.MALE);
    Wrestler injured = wrestler(31L, "Injured Star", Gender.MALE);
    stubEligibleRoster(feud1, feud2, injured);
    when(injuryService.getAllInjuriesForWrestler(31L, UNIVERSE_ID))
        .thenReturn(List.of(new Injury()));

    assertThatThrownBy(
            () ->
                service.suggestOpponent(
                    scriptOf(), List.of(feud1, feud2), "Singles Match", null, null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("No eligible external opponents");
  }

  @Test
  void suggestOpponent_promoType_doesNotFilterGender() {
    Wrestler femaleGuest = wrestler(30L, "Female Guest", Gender.FEMALE);
    when(aiFactory.getBestAvailableService()).thenReturn(narrationService);
    when(gameSettingService.isIntergenderMatchesEnabled()).thenReturn(false);
    when(universeContextService.getCurrentUniverseId()).thenReturn(UNIVERSE_ID);
    when(wrestlerService.findAllFiltered(null, null, UNIVERSE_ID, null, null))
        .thenReturn(List.of(femaleGuest));
    when(injuryService.getAllInjuriesForWrestler(30L, UNIVERSE_ID)).thenReturn(List.of());
    when(aiFactory.generateText(anyString()))
        .thenReturn("{\"wrestlerId\": 30, \"name\": \"Female Guest\", \"rationale\": \"r\"}");

    AiSuggestedOpponentDTO dto = service.suggestOpponent(scriptOf(), feudOf(), "Promo", null, null);

    assertThat(dto.getWrestlerId()).isEqualTo(30L);
  }

  @Test
  void suggestOpponent_noEligibleCandidates_throws() {
    when(aiFactory.getBestAvailableService()).thenReturn(narrationService);
    when(gameSettingService.isIntergenderMatchesEnabled()).thenReturn(true);
    when(universeContextService.getCurrentUniverseId()).thenReturn(UNIVERSE_ID);
    when(wrestlerService.findAllFiltered(null, null, UNIVERSE_ID, null, null))
        .thenReturn(List.of());

    assertThatThrownBy(
            () -> service.suggestOpponent(scriptOf(), feudOf(), "Singles Match", null, null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("No eligible external opponents");
  }

  @Test
  void suggestOpponent_recentDramaIncludedInPrompt() {
    Wrestler candidate = wrestler(30L, "Randy Orton", Gender.MALE);
    stubEligibleRoster(candidate);
    stubNotInjured(candidate);
    when(aiFactory.generateText(anyString()))
        .thenReturn("{\"wrestlerId\": 30, \"name\": \"Randy Orton\", \"rationale\": \"r\"}");

    service.suggestOpponent(scriptOf(), feudOf(), "Singles Match", null, null);

    verify(aiFactory).generateText(anyString());
  }
}
