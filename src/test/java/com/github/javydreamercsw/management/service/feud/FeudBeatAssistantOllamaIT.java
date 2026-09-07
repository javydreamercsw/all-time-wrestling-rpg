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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.SegmentNarrationServiceFactory;
import com.github.javydreamercsw.base.ai.ollama.OllamaSegmentNarrationService;
import com.github.javydreamercsw.base.ai.service.AiSettingsService;
import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.feud.FeudScript;
import com.github.javydreamercsw.management.domain.feud.FeudScriptStatus;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.dto.feud.AiSuggestedOpponentDTO;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.drama.DramaEventService;
import com.github.javydreamercsw.management.service.injury.InjuryService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Verifies the real prompt → model → JSON contract of {@link FeudBeatAssistantService} against a
 * local Ollama instance — the boundary every other test in this suite mocks. Runs only when {@code
 * OLLAMA_BASE_URL} is set (e.g. {@code http://localhost:11434}) and the configured model is
 * reachable; skips otherwise so CI without Ollama stays green.
 *
 * <p>Run locally:
 *
 * <pre>
 * OLLAMA_BASE_URL=http://localhost:11434 OLLAMA_MODEL=qwen3:30b \
 *   mvn test -Dtest=FeudBeatAssistantOllamaIT -Dsurefire.skip=false
 * </pre>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfEnvironmentVariable(named = "OLLAMA_BASE_URL", matches = ".+")
class FeudBeatAssistantOllamaIT {

  private static final long UNIVERSE_ID = 7L;

  private FeudBeatAssistantService service;
  private List<Wrestler> feudParticipants;
  private List<Wrestler> candidates;

  @BeforeAll
  void setUp() {
    String baseUrl = System.getenv("OLLAMA_BASE_URL");
    assumeOllamaReachable(baseUrl);

    // Real HTTP client against the local Ollama; everything else stays mocked.
    AiSettingsService settings = mock(AiSettingsService.class);
    when(settings.getOllamaBaseUrl()).thenReturn(baseUrl);
    when(settings.getOllamaModel()).thenReturn(System.getenv("OLLAMA_MODEL")); // null → default
    when(settings.getAiTimeout()).thenReturn(120);
    OllamaSegmentNarrationService ollama = new OllamaSegmentNarrationService(settings);
    assertThat(ollama.isAvailable()).as("Ollama base URL configured").isTrue();

    SegmentNarrationServiceFactory factory = mock(SegmentNarrationServiceFactory.class);
    when(factory.getBestAvailableService()).thenReturn(ollama);
    when(factory.generateText(anyString()))
        .thenAnswer(inv -> ollama.generateText(inv.getArgument(0)));

    WrestlerService wrestlerService = mock(WrestlerService.class);
    GameSettingService gameSettingService = mock(GameSettingService.class);
    InjuryService injuryService = mock(InjuryService.class);
    UniverseContextService universeContextService = mock(UniverseContextService.class);
    DramaEventService dramaEventService = mock(DramaEventService.class);

    when(gameSettingService.isIntergenderMatchesEnabled()).thenReturn(true);
    when(universeContextService.getCurrentUniverseId()).thenReturn(UNIVERSE_ID);
    when(dramaEventService.getRecentEvents()).thenReturn(List.of());

    Wrestler w1 = wrestler(1L, "Shelton Benjamin", Gender.MALE);
    Wrestler w2 = wrestler(2L, "Bobby Lashley", Gender.MALE);
    feudParticipants = List.of(w1, w2);
    candidates = List.of(w1, w2, wrestler(30L, "Randy Orton", Gender.MALE));
    when(wrestlerService.findAllFiltered(null, null, UNIVERSE_ID, null, null))
        .thenReturn(candidates);
    when(injuryService.getAllInjuriesForWrestler(anyLong(), eq(UNIVERSE_ID))).thenReturn(List.of());

    service =
        new FeudBeatAssistantService(
            factory,
            new ObjectMapper(),
            wrestlerService,
            gameSettingService,
            injuryService,
            universeContextService,
            dramaEventService);
  }

  private void assumeOllamaReachable(String baseUrl) {
    try {
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(baseUrl.replaceAll("/$", "") + "/api/tags"))
              .timeout(Duration.ofSeconds(5))
              .GET()
              .build();
      HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString()).statusCode();
    } catch (Exception e) {
      Assumptions.assumeTrue(false, "Ollama not reachable at " + baseUrl + ": " + e.getMessage());
    }
  }

  @Test
  void ollama_returnsParseableSuggestionForCandidateRoster() {
    AiSuggestedOpponentDTO dto =
        service.suggestOpponent(
            scriptOf(), feudParticipants, "Singles Match", null, "A shocking return is expected");

    assertThat(dto).isNotNull();
    assertThat(dto.getWrestlerId()).isNotNull();
    assertThat(candidates.stream().map(Wrestler::getId).filter(dto.getWrestlerId()::equals).count())
        .as("AI-picked wrestler must be one of the offered candidates")
        .isEqualTo(1);
    assertThat(dto.getName()).isNotBlank();
    assertThat(dto.getRationale()).isNotBlank();
  }

  @Test
  void ollama_responseStaysWithinCandidateList_whenFewCandidates() {
    AiSuggestedOpponentDTO dto =
        service.suggestOpponent(scriptOf(), feudParticipants, "Singles Match", null, null);

    assertThat(dto.getWrestlerId()).isIn(candidates.stream().map(Wrestler::getId).toList());
  }

  private FeudScript scriptOf() {
    Rivalry rivalry = new Rivalry();
    rivalry.setWrestler1(feudParticipants.get(0));
    rivalry.setWrestler2(feudParticipants.get(1));
    rivalry.setHeat(35);
    FeudScript script = new FeudScript();
    script.setName("Shelton vs Lashley");
    script.setStatus(FeudScriptStatus.ACTIVE);
    script.setRivalry(rivalry);
    return script;
  }

  private Wrestler wrestler(long id, String name, Gender gender) {
    Wrestler w = new Wrestler();
    w.setId(id);
    w.setName(name);
    w.setGender(gender);
    return w;
  }
}
